package com.ppicalendar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ppicalendar.app.data.local.entity.CompanyAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyAttachmentDao {

    @Query("SELECT * FROM company_attachments WHERE companyId = :companyId ORDER BY createdAt DESC")
    fun getAttachmentsForCompanyFlow(companyId: Long): Flow<List<CompanyAttachmentEntity>>

    @Query("SELECT * FROM company_attachments WHERE companyId = :companyId ORDER BY createdAt DESC")
    suspend fun getAttachmentsForCompany(companyId: Long): List<CompanyAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: CompanyAttachmentEntity): Long

    @Update
    suspend fun update(attachment: CompanyAttachmentEntity)

    @Query("DELETE FROM company_attachments WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
