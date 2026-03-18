package com.connect.medium.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.connect.medium.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications WHERE toUid = :uid ORDER BY createdAt DESC")
    fun getNotificationsForUser(uid: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE toUid = :uid AND read = 0")
    fun getUnreadCount(uid: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Transaction
    suspend fun upsertNotifications(notifications: List<NotificationEntity>) {
        notifications.forEach { notification ->
            val existing = getNotificationById(notification.notificationId)
            if (existing == null) {
                insertNotification(notification)
            }
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("SELECT * FROM notifications WHERE notificationId = :id LIMIT 1")
    suspend fun getNotificationById(id: String): NotificationEntity?

    @Query("UPDATE notifications SET read = 1 WHERE toUid = :uid")
    suspend fun markAllAsRead(uid: String)

    @Query("DELETE FROM notifications WHERE toUid = :uid")
    suspend fun clearNotifications(uid: String)
}