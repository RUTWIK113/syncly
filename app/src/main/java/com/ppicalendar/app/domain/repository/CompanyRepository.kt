package com.ppicalendar.app.domain.repository

import com.ppicalendar.app.domain.model.CompanyAttachment
import com.ppicalendar.app.domain.model.CompanyProfile
import kotlinx.coroutines.flow.Flow

interface CompanyRepository {
    fun getAllCompaniesFlow(): Flow<List<CompanyProfile>>
    fun getAttachmentsForCompanyFlow(companyId: Long): Flow<List<CompanyAttachment>>
    suspend fun getCompanyById(id: Long): CompanyProfile?
    suspend fun getCompanyByName(name: String): CompanyProfile?
    suspend fun getOrCreateCompanyByName(name: String): CompanyProfile
    suspend fun insertOrUpdateCompany(company: CompanyProfile): Long
    suspend fun deleteCompany(id: Long)
    suspend fun addAttachment(attachment: CompanyAttachment): Long
    suspend fun deleteAttachment(attachmentId: Long)
}
