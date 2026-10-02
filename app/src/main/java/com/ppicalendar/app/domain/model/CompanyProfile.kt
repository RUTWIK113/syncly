package com.ppicalendar.app.domain.model

enum class AttachmentType {
    PDF,
    IMAGE,
    NOTE,
    OTHER
}

data class CompanyAttachment(
    val id: Long = 0,
    val companyId: Long,
    val title: String,
    val uriString: String? = null,
    val textContent: String? = null,
    val type: AttachmentType = AttachmentType.NOTE,
    val fileSizeFormatted: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class CompanyProfile(
    val id: Long = 0,
    val name: String,
    val notes: String? = null,
    val payScale: String? = null,
    val roleNames: String? = null,
    val website: String? = null,
    val attachments: List<CompanyAttachment> = emptyList(),
    val eventCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
