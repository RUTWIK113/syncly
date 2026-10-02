package com.ppicalendar.app.data.repository

import com.ppicalendar.app.data.local.dao.ProcessedNotificationDao
import com.ppicalendar.app.data.local.entity.ProcessedNotificationEntity
import com.ppicalendar.app.domain.repository.ProcessedNotificationRepository

class ProcessedNotificationRepositoryImpl(
    private val dao: ProcessedNotificationDao
) : ProcessedNotificationRepository {

    override suspend fun isNotificationProcessed(notificationKey: String): Boolean {
        return dao.exists(notificationKey) > 0
    }

    override suspend fun markNotificationProcessed(notificationKey: String, sender: String?) {
        dao.insert(
            ProcessedNotificationEntity(
                notificationKey = notificationKey,
                sender = sender,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun clearOldProcessedNotifications(olderThanMs: Long) {
        val threshold = System.currentTimeMillis() - olderThanMs
        dao.deleteOlderThan(threshold)
    }
}
