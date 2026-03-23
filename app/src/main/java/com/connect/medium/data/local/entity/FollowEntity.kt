package com.connect.medium.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "follows",
    primaryKeys = ["followerUid", "targetUid"]
)
data class FollowEntity(
    val followerUid: String,
    val targetUid: String,
    val createdAt: Long = System.currentTimeMillis()
)