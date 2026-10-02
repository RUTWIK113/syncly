package com.ppicalendar.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incentive_points")
data class IncentivePointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val points: Int,
    val date: String,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
