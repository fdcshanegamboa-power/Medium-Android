package com.connect.medium.data.remote

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback


class CloudinaryDataSource(context: Context) {

    init {
        MediaManager.init(context, mapOf(
            "cloud_name" to "YOUR_CLOUD_NAME",
            "api_key" to "YOUR_API_KEY",
            "api_secret" to "YOUR_API_SECRET"
        ))
    }

    fun uploadImage(
        uri: Uri,
        folder: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onProgress: (Int) -> Unit
    ) {
        MediaManager.get()
            .upload(uri)
            .option("folder", folder)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val progress = ((bytes.toDouble() / totalBytes) * 100).toInt()
                    onProgress(progress)
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as String
                    onSuccess(url)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    onError(error.description)
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }
}