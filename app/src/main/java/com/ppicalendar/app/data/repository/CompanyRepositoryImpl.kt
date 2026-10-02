package com.ppicalendar.app.data.repository

import com.ppicalendar.app.data.local.dao.CompanyAttachmentDao
import com.ppicalendar.app.data.local.dao.CompanyDao
import com.ppicalendar.app.data.local.entity.CompanyAttachmentEntity
import com.ppicalendar.app.data.local.entity.CompanyEntity
import com.ppicalendar.app.domain.model.CompanyAttachment
import com.ppicalendar.app.domain.model.CompanyProfile
import com.ppicalendar.app.domain.repository.CompanyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CompanyRepositoryImpl(
    private val companyDao: CompanyDao,
    private val attachmentDao: CompanyAttachmentDao
) : CompanyRepository {

    override fun getAllCompaniesFlow(): Flow<List<CompanyProfile>> {
        return companyDao.getAllCompaniesFlow().map { list ->
            list.map { entity ->
                val attachments = attachmentDao.getAttachmentsForCompany(entity.id)
                entity.toDomain().copy(
                    attachments = attachments.map { it.toDomain() }
                )
            }
        }
    }

    override fun getAttachmentsForCompanyFlow(companyId: Long): Flow<List<CompanyAttachment>> {
        return attachmentDao.getAttachmentsForCompanyFlow(companyId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getCompanyById(id: Long): CompanyProfile? {
        val entity = companyDao.getCompanyById(id) ?: return null
        val attachments = attachmentDao.getAttachmentsForCompany(id).map { it.toDomain() }
        return entity.toDomain().copy(attachments = attachments)
    }

    override suspend fun getCompanyByName(name: String): CompanyProfile? {
        val entity = companyDao.getCompanyByName(name.trim()) ?: return null
        val attachments = attachmentDao.getAttachmentsForCompany(entity.id).map { it.toDomain() }
        return entity.toDomain().copy(attachments = attachments)
    }

    override suspend fun getOrCreateCompanyByName(name: String): CompanyProfile {
        val cleanName = name.trim()
        val existing = companyDao.getCompanyByName(cleanName)
        if (existing != null) {
            val attachments = attachmentDao.getAttachmentsForCompany(existing.id).map { it.toDomain() }
            return existing.toDomain().copy(attachments = attachments)
        }

        val newId = companyDao.insert(CompanyEntity(name = cleanName))
        val inserted = if (newId > 0) {
            companyDao.getCompanyById(newId)
        } else {
            companyDao.getCompanyByName(cleanName)
        }
        return inserted?.toDomain() ?: CompanyProfile(name = cleanName)
    }

    override suspend fun insertOrUpdateCompany(company: CompanyProfile): Long {
        return if (company.id == 0L) {
            val existing = companyDao.getCompanyByName(company.name.trim())
            if (existing != null) {
                companyDao.update(CompanyEntity.fromDomain(company.copy(id = existing.id)))
                existing.id
            } else {
                companyDao.insert(CompanyEntity.fromDomain(company))
            }
        } else {
            companyDao.update(CompanyEntity.fromDomain(company))
            company.id
        }
    }

    override suspend fun deleteCompany(id: Long) {
        companyDao.deleteById(id)
    }

    override suspend fun addAttachment(attachment: CompanyAttachment): Long {
        return attachmentDao.insert(CompanyAttachmentEntity.fromDomain(attachment))
    }

    override suspend fun deleteAttachment(attachmentId: Long) {
        attachmentDao.deleteById(attachmentId)
    }
}
