package com.connect.medium.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.connect.medium.data.local.entity.FollowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowDao {

    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE followerUid = :currentUid AND targetUid = :targetUid)")
    fun isFollowing(currentUid: String, targetUid: String): Flow<Boolean>

    @Query("SELECT * FROM follows WHERE followerUid = :currentUid")
    fun getFollowing(currentUid: String): Flow<List<FollowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: FollowEntity)

    @Query("DELETE FROM follows WHERE followerUid = :followerUid AND targetUid = :targetUid")
    suspend fun deleteFollow(followerUid: String, targetUid: String)

    @Query("DELETE FROM follows WHERE followerUid = :currentUid")
    suspend fun clearFollowing(currentUid: String)
}