package com.connect.medium.data.remote

import android.app.Application
import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.connect.medium.BuildConfig


class CloudinaryDataSource(private val application: Application) {

    init {
        try {
            MediaManager.get() // check if already initialized
        } catch (e: Exception) {
            MediaManager.init(application, mapOf(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
            ))
        }
    }

    fun uploadImage(
        uri: Uri,
        folder: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onProgress: (Int) -> Unit
    ) {
        val fileSize = application.contentResolver.openFileDescriptor(uri, "r")?.statSize ?: 0
        val maxSize = 50 * 1024 * 1024L // 50MB

        if (fileSize > maxSize) {
            onError("File size exceeds 50MB limit")
            return
        }

        MediaManager.get()
            .upload(uri)
            .option("folder", folder)
            .option("resource_type", "auto") // handles both image and video
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