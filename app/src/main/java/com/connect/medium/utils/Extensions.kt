package com.connect.medium.utils

import com.connect.medium.data.local.entity.*
import com.connect.medium.data.model.*
import com.google.gson.Gson

// User
fun User.toEntity(): UserEntity = UserEntity(
    uid, username, displayName, bio,
    profileImageUrl, fcmToken, followerCount,
    followingCount, postCount, createdAt
)

fun UserEntity.toModel(): User = User(
    uid, username, displayName, bio,
    profileImageUrl, fcmToken, followerCount,
    followingCount, postCount, createdAt
)

// Post
fun Post.toEntity(): PostEntity = PostEntity(
    postId = postId,
    authorUid = authorUid,
    authorUsername = authorUsername,
    authorProfileImageUrl = authorProfileImageUrl,
    mediaUrls = Gson().toJson(mediaUrls),
    mediaTypes = Gson().toJson(mediaTypes),
    caption = caption,
    likeCount = likeCount,
    commentCount = commentCount,
    createdAt = createdAt
)

fun PostEntity.toModel(): Post = Post(
    postId = postId,
    authorUid = authorUid,
    authorUsername = authorUsername,
    authorProfileImageUrl = authorProfileImageUrl,
    mediaUrls = Gson().fromJson(mediaUrls, Array<String>::class.java).toList(),
    mediaTypes = Gson().fromJson(mediaTypes, Array<String>::class.java).toList(),
    caption = caption,
    likeCount = likeCount,
    commentCount = commentCount,
    createdAt = createdAt
)

// Notification
fun Notification.toEntity(): NotificationEntity = NotificationEntity(
    notificationId, toUid, fromUid, fromUsername,
    fromProfileImageUrl, type.name, postId, read, createdAt
)

fun NotificationEntity.toModel(): Notification = Notification(
    notificationId, toUid, fromUid, fromUsername,
    fromProfileImageUrl, NotificationType.valueOf(type), postId, read, createdAt
)