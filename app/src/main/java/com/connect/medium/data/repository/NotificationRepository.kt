package com.connect.medium.data.repository

import android.util.Log
import com.connect.medium.data.local.dao.NotificationDao
import com.connect.medium.data.model.Notification
import com.connect.medium.data.remote.FirestoreDataSource
import com.connect.medium.utils.Resource
import com.connect.medium.utils.toEntity
import com.connect.medium.utils.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class NotificationRepository(
    private val firestoreDataSource: FirestoreDataSource,
    private val notificationDao: NotificationDao
) {

    fun observeNotifications(uid: String): Flow<List<Notification>> {
        return firestoreDataSource.observeNotifications(uid)
            .onEach { notifications ->
                Log.d("NotifDebug", "From Firestore: ${notifications.map { "${it.notificationId} read=${it.read}" }}")
                notificationDao.upsertNotifications(notifications.map { it.toEntity() })
                val roomCount = notificationDao.getUnreadCount(uid)
                Log.d("NotifDebug", "Room unread count after upsert: $roomCount")

            }
    }

    fun getCachedNotifications(uid: String): Flow<List<Notification>> {
        return notificationDao.getNotificationsForUser(uid).map { list -> list.map { it.toModel() } }
    }

    fun getUnreadCount(uid: String): Flow<Int> {
        return notificationDao.getUnreadCount(uid)
    }

    suspend fun sendNotification(notification: Notification): Resource<Unit> {
        return try {
            firestoreDataSource.sendNotification(notification)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send notification")
        }
    }

    suspend fun markAllAsRead(uid: String): Resource<Unit> {
        return try {
            firestoreDataSource.markAllNotificationsAsRead(uid)
            notificationDao.markAllAsRead(uid)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark notifications as read")
        }
    }
}