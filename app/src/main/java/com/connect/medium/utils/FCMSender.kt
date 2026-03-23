package com.connect.medium.utils

import android.util.Log
import com.connect.medium.ConnectApplication
import com.google.android.gms.tasks.Tasks
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.installations.FirebaseInstallations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object FCMSender {

    private const val FCM_URL = "https://fcm.googleapis.com/v1/projects/medium-5e53e/messages:send"
    suspend fun sendNotification(
        targetToken: String,
        title: String,
        body: String,
        type: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                val accessToken = getAccessToken() ?: return@withContext

                val json = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("token", targetToken)
                        put("notification", JSONObject().apply {
                            put("title", title)
                            put("body", body)
                        })
                        put("data", JSONObject().apply {
                            put("type", type)
                        })
                    })
                }

                val url = URL(FCM_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    outputStream.write(json.toString().toByteArray())
                }

                val responseCode = conn.responseCode
                Log.d("FCMSender", "Response: $responseCode")
                conn.disconnect()

            } catch (e: Exception) {
                Log.e("FCMSender", "Failed to send notification: ${e.message}")
            }
        }
    }

    private fun getAccessToken(): String? {
        return try {
            val stream = ConnectApplication.instance.assets.open("service_account.json")
            val credentials = GoogleCredentials
                .fromStream(stream)
                .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
            credentials.refreshIfExpired()
            val token = credentials.accessToken.tokenValue
            Log.d("FCMSender", "Token obtained successfully")
            token
        } catch (e: Exception) {
            Log.e("FCMSender", "Failed to get access token: ${e.message}", e)
            null
        }
    }
}