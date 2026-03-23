package com.connect.medium.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val username: String,
    val displayName: String,
    val bio: String,
    val profileImageUrl: String,
    val fcmToken: String = "",
    val followerCount: Int,
    val followingCount: Int,
    val postCount: Int,
    val createdAt: Long
)