package com.connect.medium.data.model

data class User(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val profileImageUrl: String = "",
    val fcmToken: String = "",
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val postCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)