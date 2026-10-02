package com.ppicalendar.app.domain.model

data class IncentivePoint(
    val id: Long = 0,
    val title: String,
    val points: Int,
    val date: String,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
