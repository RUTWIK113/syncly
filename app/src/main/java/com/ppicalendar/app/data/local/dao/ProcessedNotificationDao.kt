package com.ppicalendar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ppicalendar.app.data.local.entity.ProcessedNotificationEntity

@Dao
interface ProcessedNotificationDao {

    @Query("SELECT COUNT(*) FROM processed_notifications WHERE notificationKey = :key")
    suspend fun exists(key: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ProcessedNotificationEntity): Long

    @Query("DELETE FROM processed_notifications WHERE timestamp < :olderThanMs")
    suspend fun deleteOlderThan(olderThanMs: Long): Int
}
