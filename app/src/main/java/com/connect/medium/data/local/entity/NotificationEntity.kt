package com.connect.medium.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val notificationId: String,
    val toUid: String,
    val fromUid: String,
    val fromUsername: String,
    val fromProfileImageUrl: String,
    val type: String,
    val postId: String,
    val isRead: Boolean,
    val createdAt: Long
)