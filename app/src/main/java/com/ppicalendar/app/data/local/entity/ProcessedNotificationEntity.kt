package com.ppicalendar.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "processed_notifications",
    indices = [Index(value = ["notificationKey"], unique = true)]
)
data class ProcessedNotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notificationKey: String,
    val sender: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
