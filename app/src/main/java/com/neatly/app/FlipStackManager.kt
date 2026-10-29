package com.neatly.app

import android.content.Context
import android.util.Log
import co.paystack.android.PaystackSdk
import co.paystack.android.model.Card
import co.paystack.android.ui.payments.Pop
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object FlipStackManager {
    private const val PREFS = "flipstack_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_TIER = "tier"
    private const val BASE_URL = "https://api.flipstack.store/api"

    fun getUserId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var userId = prefs.getString(KEY_USER_ID, null)
        if (userId == null) {
            userId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_USER_ID, userId).apply()
        }
        return userId
    }

    fun getCachedTier(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TIER, "free") ?: "free"
    }

    fun checkEntitlement(context: Context, callback: (Boolean) -> Unit) {
        Thread {
            try {
                val appId = context.getString(R.string.flipstack_app_id)
                val userId = getUserId(context)
                val url = URL("$BASE_URL/entitlement?app_id=$appId&user_id=$userId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = reader.readText()
                    reader.close()
                    
                    val json = JSONObject(response)
                    val tier = json.optString("tier", "free")
                    
                    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    prefs.edit().putString(KEY_TIER, tier).apply()
                    
                    callback(tier == "pro")
                } else {
                    callback(getCachedTier(context) == "pro")
                }
            } catch (e: Exception) {
                Log.e("FlipStack", "Entitlement check failed", e)
                callback(getCachedTier(context) == "pro")
            }
        }.start()
    }

    fun initiateCheckout(context: Context, email: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        Thread {
            try {
                val appId = context.getString(R.string.flipstack_app_id)
                val userId = getUserId(context)
                val url = URL("$BASE_URL/checkout/initiate")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val payload = JSONObject().apply {
                    put("app_id", appId)
                    put("email", email)
                    put("user_id", userId)
                }

                val os = OutputStreamWriter(conn.outputStream)
                os.write(payload.toString())
                os.flush()
                os.close()

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = reader.readText()
                    reader.close()
                    
                    val json = JSONObject(response)
                    if (json.optBoolean("success")) {
                        val accessCode = json.optString("access_code")
                        onSuccess(accessCode)
                    } else {
                        onError("Checkout initiation failed.")
                    }
                } else {
                    onError("Server error: ${conn.responseCode}")
                }
            } catch (e: Exception) {
                Log.e("FlipStack", "Checkout failed", e)
                onError(e.message ?: "Unknown error")
            }
        }.start()
    }
}
