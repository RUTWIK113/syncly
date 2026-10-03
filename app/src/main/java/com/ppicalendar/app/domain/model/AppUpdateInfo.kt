package com.ppicalendar.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppUpdateInfo(
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val changelog: String = "",
    val downloadUrl: String = "https://rutwik113.github.io/syncly/syncly.apk",
    val isMandatory: Boolean = false
)
