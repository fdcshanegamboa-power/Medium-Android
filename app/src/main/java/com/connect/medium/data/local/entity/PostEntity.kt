package com.connect.medium.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val postId: String,
    val authorUid: String,
    val authorUsername: String,
    val authorProfileImageUrl: String,
    val imageUrl: String,
    val caption: String,
    val likeCount: Int,
    val commentCount: Int,
    val createdAt: Long
)