package com.ppicalendar.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ppicalendar.app.domain.model.CompanyProfile

@Entity(
    tableName = "companies",
    indices = [Index(value = ["name"], unique = true)]
)
data class CompanyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val notes: String? = null,
    val payScale: String? = null,
    val roleNames: String? = null,
    val website: String? = null,
    val applied: Boolean? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): CompanyProfile {
        return CompanyProfile(
            id = id,
            name = name,
            notes = notes,
            payScale = payScale,
            roleNames = roleNames,
            website = website,
            applied = applied,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(profile: CompanyProfile): CompanyEntity {
            return CompanyEntity(
                id = profile.id,
                name = profile.name.trim(),
                notes = profile.notes,
                payScale = profile.payScale,
                roleNames = profile.roleNames,
                website = profile.website,
                applied = profile.applied,
                createdAt = profile.createdAt
            )
        }
    }
}
