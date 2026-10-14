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
 *   1. Fetch https://api.github.com/repos/<owner>/<repo>/releases/latest
 *      (cached for 15 minutes).
 *   2. Compare the release's version.json versionCode against
 *      BuildConfig.VERSION_CODE.
 *   3. If remote > local AND the active network is unmetered, silently
 *      download the APK into cacheDir/updates/.
 *   4. Call [onUpdateReady] so the caller can show a footer indicator.
 *
 * All failures are silent.
 */
object Updater {

    private const val REPO_OWNER = "Navscan"
    private const val REPO_NAME = "neatly"
    private const val RELEASES_URL =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
    private const val USER_AGENT = "Neatly-Android"

    // 15 minutes between checks. Aggressive enough to catch updates
    // within a normal session, well within GitHub's 60 req/hour limit.
    private const val CACHE_TTL_MS = 15L * 60L * 1000L

    private const val APK_FILENAME = "Neatly.apk"
    private const val VERSION_FILENAME = "version.json"

    private const val PREFS = "neatly_updater"
    private const val KEY_LAST_CHECK = "last_check_ms"

    var pendingApk: File? = null
        private set

    /**
     * Non-blocking check. Always returns quickly. Safe to call from
     * onResume().
     */
    suspend fun checkForUpdate(context: Context, onUpdateReady: () -> Unit) {
        try {
            if (!isUnmetered(context)) return
            if (throttled(context)) return

            val release = fetchLatestRelease() ?: return
            val remoteCode = fetchRemoteVersionCode(release) ?: return

            // Mark checked only after a successful API round-trip.
            markChecked(context)

            if (remoteCode <= BuildConfig.VERSION_CODE) return

            val apk = downloadApk(context, release) ?: return
            pendingApk = apk
            withContext(Dispatchers.Main) { onUpdateReady() }
        } catch (_: Throwable) {
            // Silent by design.
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

        val conn = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            instanceFollowRedirects = true
            connectTimeout = 15000
            readTimeout = 30000
        }
        return try {
            if (conn.responseCode != 200) return null
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            }
            out
        } catch (_: Throwable) {
            out.delete()
            null
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Fire the system installer. Requires the caller to have already
     * granted REQUEST_INSTALL_PACKAGES in the manifest.
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
