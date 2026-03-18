package com.connect.medium.data.model

data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val authorProfileImageUrl: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)