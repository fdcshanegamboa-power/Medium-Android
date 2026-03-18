package com.connect.medium.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "follows")
data class FollowEntity(
    @PrimaryKey
    val targetUid: String,      // the person being followed
    val followerUid: String,    // the current user
    val createdAt: Long = System.currentTimeMillis()
)