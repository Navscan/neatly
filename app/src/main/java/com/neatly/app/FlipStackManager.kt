package com.neatly.app

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object FlipStackManager {

    private const val APP_ID = "neatly--830e"
    private const val BASE_URL = "https://api.flipstack.store/api"

    private fun getUserId(context: Context): String {
        val prefs = context.getSharedPreferences("flipstack_prefs", Context.MODE_PRIVATE)
        var userId = prefs.getString("flipstack_user_id", null)
        if (userId == null) {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            userId = "neatly_user_$androidId"
            prefs.edit().putString("flipstack_user_id", userId).apply()
        }
        return userId
    }

    fun initiateCheckout(
        context: Context,
        email: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = getUserId(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BASE_URL/checkout/initiate")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val payload = JSONObject().apply {
                    put("app_id", APP_ID)
                    put("email", email)
                    put("user_id", userId)
                }

                val os = conn.outputStream
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(payload.toString())
                writer.flush()
                writer.close()

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    val jsonResponse = JSONObject(response.toString())
                    val accessCode = jsonResponse.optString("access_code")
                    
                    withContext(Dispatchers.Main) {
                        if (accessCode.isNotEmpty()) {
                            onSuccess(accessCode)
                        } else {
                            onError("Failed to parse access code from FlipStack.")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("Checkout initialization failed. HTTP $responseCode")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Network error: ${e.message}")
                }
            }
        }
    }

    fun checkEntitlement(context: Context, callback: (Boolean) -> Unit) {
        val userId = getUserId(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Assuming standard entitlement check endpoint based on BYOK patterns
                val url = URL("$BASE_URL/entitlements/check?app_id=$APP_ID&user_id=$userId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    val jsonResponse = JSONObject(response.toString())
                    val isPro = jsonResponse.optBoolean("is_pro", false)
                    
                    withContext(Dispatchers.Main) {
                        callback(isPro)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback(false)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }
}
