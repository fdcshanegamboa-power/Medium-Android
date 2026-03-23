package com.connect.medium.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FCMHelper {

    fun saveTokenToFirestore(uid: String) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .update("fcmToken", token)
                        .await()
                    Log.d("FCMHelper", "Token saved: $token")
                } catch (e: Exception) {
                    Log.e("FCMHelper", "Failed to save token: ${e.message}")
                }
            }
        }
    }
}