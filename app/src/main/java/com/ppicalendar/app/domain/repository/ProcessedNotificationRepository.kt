package com.ppicalendar.app.domain.repository

interface ProcessedNotificationRepository {
    suspend fun isNotificationProcessed(notificationKey: String): Boolean
    suspend fun markNotificationProcessed(notificationKey: String, sender: String? = null)
    suspend fun clearOldProcessedNotifications(olderThanMs: Long)
}
