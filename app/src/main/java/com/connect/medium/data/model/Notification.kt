package com.connect.medium.data.model

data class Notification(
    val notificationId: String = "",
    val toUid: String = "",
    val fromUid: String = "",
    val fromUsername: String = "",
    val fromProfileImageUrl: String = "",
    val type: NotificationType = NotificationType.LIKE,
    val postId: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class NotificationType {
    LIKE, COMMENT, FOLLOW
}