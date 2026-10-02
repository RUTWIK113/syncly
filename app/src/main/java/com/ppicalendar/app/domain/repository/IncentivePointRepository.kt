package com.ppicalendar.app.domain.repository

import com.ppicalendar.app.domain.model.IncentivePoint
import kotlinx.coroutines.flow.Flow

interface IncentivePointRepository {
    fun getAllPointsFlow(): Flow<List<IncentivePoint>>
    suspend fun addPoint(point: IncentivePoint): Long
    suspend fun deletePoint(id: Long)
}
