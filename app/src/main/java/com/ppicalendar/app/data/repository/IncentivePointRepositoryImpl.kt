package com.ppicalendar.app.data.repository

import com.ppicalendar.app.data.local.dao.IncentivePointDao
import com.ppicalendar.app.data.local.entity.IncentivePointEntity
import com.ppicalendar.app.domain.model.IncentivePoint
import com.ppicalendar.app.domain.repository.IncentivePointRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IncentivePointRepositoryImpl(
    private val pointDao: IncentivePointDao
) : IncentivePointRepository {

    override fun getAllPointsFlow(): Flow<List<IncentivePoint>> {
        return pointDao.getAllPointsFlow().map { list ->
            list.map {
                IncentivePoint(
                    id = it.id,
                    title = it.title,
                    points = it.points,
                    date = it.date,
                    note = it.note,
                    createdAt = it.createdAt
                )
            }
        }
    }

    override suspend fun addPoint(point: IncentivePoint): Long {
        return pointDao.insert(
            IncentivePointEntity(
                id = point.id,
                title = point.title,
                points = point.points,
                date = point.date,
                note = point.note,
                createdAt = point.createdAt
            )
        )
    }

    override suspend fun deletePoint(id: Long) {
        pointDao.deleteById(id)
    }
}
