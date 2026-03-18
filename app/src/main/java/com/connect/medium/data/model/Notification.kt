package com.connect.medium.data.model

import com.google.firebase.firestore.PropertyName

data class Notification(
    val notificationId: String = "",
    val toUid: String = "",
    val fromUid: String = "",
    val fromUsername: String = "",
    val fromProfileImageUrl: String = "",
    val type: NotificationType = NotificationType.LIKE,
    val postId: String = "",
    var read: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class NotificationType {
    LIKE, COMMENT, FOLLOW
}