package com.connect.medium.utils

import com.connect.medium.data.local.entity.*
import com.connect.medium.data.model.*

// User
fun User.toEntity(): UserEntity = UserEntity(
    uid, username, displayName, bio,
    profileImageUrl, followerCount, followingCount, postCount, createdAt
)

fun UserEntity.toModel(): User = User(
    uid, username, displayName, bio,
    profileImageUrl, followerCount, followingCount, postCount, createdAt
)

// Post
fun Post.toEntity(): PostEntity = PostEntity(
    postId, authorUid, authorUsername, authorProfileImageUrl,
    imageUrl, caption, likeCount, commentCount, createdAt
)

fun PostEntity.toModel(): Post = Post(
    postId, authorUid, authorUsername, authorProfileImageUrl,
    imageUrl, caption, likeCount, commentCount, createdAt
)

// Notification
fun Notification.toEntity(): NotificationEntity = NotificationEntity(
    notificationId, toUid, fromUid, fromUsername,
    fromProfileImageUrl, type.name, postId, isRead, createdAt
)

fun NotificationEntity.toModel(): Notification = Notification(
    notificationId, toUid, fromUid, fromUsername,
    fromProfileImageUrl, NotificationType.valueOf(type), postId, isRead, createdAt
)