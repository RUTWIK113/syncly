package com.ppicalendar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ppicalendar.app.data.local.entity.IncentivePointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncentivePointDao {

    @Query("SELECT * FROM incentive_points ORDER BY createdAt DESC")
    fun getAllPointsFlow(): Flow<List<IncentivePointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: IncentivePointEntity): Long

    @Query("DELETE FROM incentive_points WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
