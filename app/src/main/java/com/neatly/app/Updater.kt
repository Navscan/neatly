package com.neatly.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * In-app auto-updater.
 *
 * On launch and on every return to foreground:
 *   1. Fetch GitHub's latest release metadata (cached 15 minutes).
 *   2. Compare version.json versionCode against BuildConfig.VERSION_CODE.
 *   3. If remote > local AND the active network is unmetered, download
 *      the APK into cacheDir/updates/.
 *   4. Set [pendingApk] and signal readiness so the UI can show a banner.
 *
 * The public entry point returns an [UpdateStatus] so the UI can display
 * what happened on the last check. All network operations run on
 * Dispatchers.IO — see [checkForUpdate].
 */
object Updater {

    private const val REPO_OWNER = "Navscan"
    private const val REPO_NAME = "neatly"
    private const val RELEASES_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
    private const val USER_AGENT = "Neatly-Android"

    private const val CACHE_TTL_MS = 15L * 60L * 1000L

    private const val APK_FILENAME = "Neatly.apk"
    private const val VERSION_FILENAME = "version.json"

    private const val PREFS = "neatly_updater"
    private const val KEY_LAST_CHECK = "last_check_ms"

    var pendingApk: File? = null
        private set

    var lastStatus: UpdateStatus = UpdateStatus.IDLE
        private set

    enum class UpdateStatus {
        IDLE,
        METERED,
        THROTTLED,
        NETWORK_ERROR,
        UP_TO_DATE,
        UPDATE_READY,
        DOWNLOAD_FAILED
    }

    /**
     * Non-blocking from the caller's perspective. All blocking network
     * work runs on Dispatchers.IO. Callers on the main thread are safe.
     */
    suspend fun checkForUpdate(context: Context): UpdateStatus {
        val status = withContext(Dispatchers.IO) {
            performCheck(context)
        }
        lastStatus = status
        return status
    }

    /**
     * Runs entirely on a background thread. Returns the outcome.
     */
    private fun performCheck(context: Context): UpdateStatus {
        try {
            if (!isUnmetered(context)) return UpdateStatus.METERED
            if (throttled(context)) return UpdateStatus.THROTTLED

            val release = fetchLatestRelease() ?: return UpdateStatus.NETWORK_ERROR
            val remoteCode = fetchRemoteVersionCode(release) ?: return UpdateStatus.NETWORK_ERROR

            markChecked(context)

            if (remoteCode <= BuildConfig.VERSION_CODE) return UpdateStatus.UP_TO_DATE

            val apk = downloadApk(context, release) ?: return UpdateStatus.DOWNLOAD_FAILED
            pendingApk = apk
            return UpdateStatus.UPDATE_READY
        } catch (_: Throwable) {
            return UpdateStatus.NETWORK_ERROR
        }
    }

    private fun throttled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_CHECK, 0L)
        return System.currentTimeMillis() - last < CACHE_TTL_MS
    }

    private fun markChecked(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
            .apply()
    }

    private fun isUnmetered(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return false
        val active: Network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    private fun fetchLatestRelease(): JSONObject? {
        val conn = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 8000
            readTimeout = 8000
        }
        return try {
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body)
        } catch (_: Throwable) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchRemoteVersionCode(release: JSONObject): Int? {
        val assets = release.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            if (asset.optString("name") == VERSION_FILENAME) {
                val url = asset.optString("browser_download_url")
                if (url.isNullOrEmpty()) return null
                return fetchVersionCode(url)
            }
        }
        return null
    }

    private fun fetchVersionCode(url: String): Int? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            connectTimeout = 8000
            readTimeout = 8000
        }
        return try {
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body).optInt("versionCode", 0)
        } catch (_: Throwable) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun downloadApk(context: Context, release: JSONObject): File? {
        val assets = release.optJSONArray("assets") ?: return null
        var apkUrl: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            if (asset.optString("name") == APK_FILENAME) {
                apkUrl = asset.optString("browser_download_url")
                break
            }
        }
        if (apkUrl.isNullOrEmpty()) return null

        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val out = File(dir, APK_FILENAME)

        return try {
            downloadWithRedirects(apkUrl, out, redirectsLeft = 5)
            if (out.length() > 0L) out else null
        } catch (_: Throwable) {
            out.delete()
            null
        }
    }

    /**
     * GitHub serves release assets via a redirect from github.com to
     * objects.githubusercontent.com. Java's HttpURLConnection does not
     * reliably preserve custom headers across cross-host redirects, and
     * GitHub returns 403 if the User-Agent is dropped. We follow the
     * redirect chain manually, re-applying the User-Agent on every hop.
     */
    private fun downloadWithRedirects(urlStr: String, out: File, redirectsLeft: Int) {
        if (redirectsLeft <= 0) throw IllegalStateException("Too many redirects")

        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/octet-stream")
            instanceFollowRedirects = false
            connectTimeout = 15000
            readTimeout = 30000
        }
        try {
            val code = conn.responseCode
            when (code) {
                in 200..299 -> {
                    conn.inputStream.use { input ->
                        out.outputStream().use { output ->
                            input.copyTo(output, bufferSize = 64 * 1024)
                        }
                    }
                }
                in 300..399 -> {
                    val location = conn.getHeaderField("Location")
                        ?: throw IllegalStateException("Redirect with no Location")
                    downloadWithRedirects(location, out, redirectsLeft - 1)
                }
                else -> throw IllegalStateException("HTTP $code")
            }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Fire the system installer. Requires REQUEST_INSTALL_PACKAGES in
     * the manifest, and (on API 26+) the caller must have granted
     * "install unknown apps".
     */
    fun install(context: Context) {
        val apk = pendingApk ?: return
        if (!apk.exists()) return

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun canInstall(context: Context): Boolean {
        return context.packageManager.canRequestPackageInstalls()
    }

    fun requestInstallPermission(activity: Activity) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        activity.startActivity(intent)
    }
}
