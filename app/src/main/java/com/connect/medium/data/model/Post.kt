package com.connect.medium.data.model

data class Post(
    val postId: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val authorProfileImageUrl: String = "",
    val imageUrl: String = "",
    val caption: String = "",
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)