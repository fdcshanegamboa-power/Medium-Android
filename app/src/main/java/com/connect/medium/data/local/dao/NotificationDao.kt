package com.connect.medium.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.connect.medium.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications WHERE toUid = :uid ORDER BY createdAt DESC")
    fun getNotificationsForUser(uid: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE toUid = :uid AND isRead = 0")
    fun getUnreadCount(uid: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE toUid = :uid")
    suspend fun markAllAsRead(uid: String)

    @Query("DELETE FROM notifications WHERE toUid = :uid")
    suspend fun clearNotifications(uid: String)
}