package com.connect.medium.data.repository

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
                notificationDao.insertNotifications(notifications.map { it.toEntity() })
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
            notificationDao.markAllAsRead(uid)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark notifications as read")
        }
    }
}