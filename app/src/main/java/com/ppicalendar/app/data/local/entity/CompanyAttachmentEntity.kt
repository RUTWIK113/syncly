package com.ppicalendar.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ppicalendar.app.domain.model.AttachmentType
import com.ppicalendar.app.domain.model.CompanyAttachment

@Entity(
    tableName = "company_attachments",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["companyId"])]
)
data class CompanyAttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val title: String,
    val uriString: String?,
    val textContent: String?,
    val type: String,
    val fileSizeFormatted: String?,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): CompanyAttachment {
        return CompanyAttachment(
            id = id,
            companyId = companyId,
            title = title,
            uriString = uriString,
            textContent = textContent,
            type = try { AttachmentType.valueOf(type) } catch (e: Exception) { AttachmentType.NOTE },
            fileSizeFormatted = fileSizeFormatted,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(attachment: CompanyAttachment): CompanyAttachmentEntity {
            return CompanyAttachmentEntity(
                id = attachment.id,
                companyId = attachment.companyId,
                title = attachment.title,
                uriString = attachment.uriString,
                textContent = attachment.textContent,
                type = attachment.type.name,
                fileSizeFormatted = attachment.fileSizeFormatted,
                createdAt = attachment.createdAt
            )
        }
    }
}
