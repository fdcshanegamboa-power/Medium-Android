package com.connect.medium.data.repository

import com.connect.medium.data.local.dao.FollowDao
import com.connect.medium.data.local.dao.UserDao
import com.connect.medium.data.local.entity.FollowEntity
import com.connect.medium.data.model.Notification
import com.connect.medium.data.model.NotificationType
import com.connect.medium.data.model.User
import com.connect.medium.data.remote.FirestoreDataSource
import com.connect.medium.utils.FCMSender
import com.connect.medium.utils.Resource
import com.connect.medium.utils.toEntity
import com.connect.medium.utils.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.util.UUID

class UserRepository(
    private val firestoreDataSource: FirestoreDataSource,
    private val userDao: UserDao,
    private val followDao: FollowDao
) {

    suspend fun createUser(user: User): Resource<Unit> {
        return try {
            firestoreDataSource.createUser(user)
            userDao.insertUser(user.toEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create user")
        }
    }

    suspend fun searchUsers(query: String): Resource<List<User>> {
        return try {
            val users = firestoreDataSource.searchUsers(query)
            Resource.Success(users)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Search failed")
        }
    }

    fun observeUser(uid: String): Flow<User?> {
        return firestoreDataSource.observeUser(uid)
            .onEach { user ->
                // cache to Room whenever Firestore updates
                user?.let { userDao.insertUser(it.toEntity()) }
            }
    }

    fun getCachedUser(uid: String): Flow<User?> {
        return userDao.getUserById(uid).map { it?.toModel() }
    }

    suspend fun updateUser(uid: String, fields: Map<String, Any>): Resource<Unit> {
        return try {
            firestoreDataSource.updateUser(uid, fields)

            val newImageUrl = fields["profileImageUrl"] as? String
            if (newImageUrl != null) {
                firestoreDataSource.updateAuthorProfileImageOnPosts(uid, newImageUrl)
                firestoreDataSource.updateAuthorProfileImageOnComments(uid, newImageUrl)
            }
            val updatedUser = firestoreDataSource.getUser(uid)
            updatedUser?.let { userDao.insertUser(it.toEntity()) }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update user")
        }
    }

    suspend fun followUser(currentUid: String, targetUid: String, fromUser: User): Resource<Unit> {
        return try {
            followDao.insertFollow(FollowEntity(
                followerUid = currentUid,
                targetUid = targetUid
            ))
            firestoreDataSource.followUser(currentUid, targetUid)

            val notification = Notification(
                notificationId = UUID.randomUUID().toString(),
                toUid = targetUid,
                fromUid = currentUid,
                fromUsername = fromUser.username,
                fromProfileImageUrl = fromUser.profileImageUrl,
                type = NotificationType.FOLLOW,
                postId = "",
                createdAt = System.currentTimeMillis()
            )
            firestoreDataSource.sendNotification(notification)

            val targetToken = firestoreDataSource.getUserFcmToken(targetUid)
            if (!targetToken.isNullOrEmpty()) {
                FCMSender.sendNotification(
                    targetToken = targetToken,
                    title = fromUser.username,
                    body = "started following you",
                    type = "FOLLOW"
                )
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            followDao.deleteFollow(currentUid, targetUid)
            Resource.Error(e.message ?: "Failed to follow user")
        }
    }


    suspend fun isFollowedBy(currentUid: String, targetUid: String): Boolean {
        return try{
            firestoreDataSource.isFollowingUser(targetUid, currentUid)
        }catch (e: Exception){
            false
        }
    }
    fun observeIsFollowedBy(currentUid: String, targetUid: String): Flow<Boolean> {
        return firestoreDataSource.observeIsFollowedBy(currentUid, targetUid)
    }

    suspend fun unfollowUser(currentUid: String, targetUid: String): Resource<Unit> {
        return try {
            followDao.deleteFollow(currentUid, targetUid)
            firestoreDataSource.unfollowUser(currentUid, targetUid)
            Resource.Success(Unit)
        } catch (e: Exception) {
            followDao.insertFollow(FollowEntity(
                followerUid = currentUid,
                targetUid = targetUid
            ))
            Resource.Error(e.message ?: "Failed to unfollow user")
        }
    }
    fun isFollowing(currentUid: String, targetUid: String): Flow<Boolean> {
        return followDao.isFollowing(currentUid, targetUid)
    }

    suspend fun syncFollowingList(currentUid: String) {
        try {
            val following = firestoreDataSource.getFollowingList(currentUid)
            followDao.clearFollowing(currentUid)
            following.forEach { uid ->
                followDao.insertFollow(FollowEntity(
                    followerUid = currentUid,
                    targetUid = uid
                ))
            }
        } catch (e: Exception) {
            // fail silently
        }
    }

    suspend fun isFollowingUser(currentUid: String, targetUid: String): Boolean {
        return firestoreDataSource.isFollowingUser(currentUid, targetUid)
    }
}