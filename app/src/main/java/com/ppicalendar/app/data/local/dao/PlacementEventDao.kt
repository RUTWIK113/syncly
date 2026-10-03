package com.ppicalendar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ppicalendar.app.data.local.entity.PlacementEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlacementEventDao {

    @Query("SELECT * FROM placement_events ORDER BY createdAt DESC")
    fun getAllEvents(): Flow<List<PlacementEventEntity>>

    @Query("SELECT * FROM placement_events WHERE status != 'CREATED_IN_CALENDAR' AND status != 'DISMISSED' ORDER BY createdAt DESC")
    fun getPendingEvents(): Flow<List<PlacementEventEntity>>

    @Query("SELECT * FROM placement_events WHERE id = :id")
    suspend fun getEventById(id: Long): PlacementEventEntity?

    @Query("SELECT * FROM placement_events WHERE notificationKey = :key LIMIT 1")
    suspend fun getEventByNotificationKey(key: String): PlacementEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: PlacementEventEntity): Long

    @Update
    suspend fun update(event: PlacementEventEntity)

    @Query("UPDATE placement_events SET status = :status, calendarEventId = :calendarId WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, calendarId: Long?)

    @Query("DELETE FROM placement_events WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM placement_events WHERE status = 'DISMISSED'")
    suspend fun clearDismissed(): Int
}
