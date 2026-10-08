package com.ppicalendar.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ppicalendar.app.data.local.dao.CompanyAttachmentDao
import com.ppicalendar.app.data.local.dao.CompanyDao
import com.ppicalendar.app.data.local.dao.IncentivePointDao
import com.ppicalendar.app.data.local.dao.PlacementEventDao
import com.ppicalendar.app.data.local.dao.ProcessedNotificationDao
import com.ppicalendar.app.data.local.entity.CompanyAttachmentEntity
import com.ppicalendar.app.data.local.entity.CompanyEntity
import com.ppicalendar.app.data.local.entity.IncentivePointEntity
import com.ppicalendar.app.data.local.entity.PlacementEventEntity
import com.ppicalendar.app.data.local.entity.ProcessedNotificationEntity

@Database(
    entities = [
        ProcessedNotificationEntity::class,
        PlacementEventEntity::class,
        CompanyEntity::class,
        CompanyAttachmentEntity::class,
        IncentivePointEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun processedNotificationDao(): ProcessedNotificationDao
    abstract fun placementEventDao(): PlacementEventDao
    abstract fun companyDao(): CompanyDao
    abstract fun companyAttachmentDao(): CompanyAttachmentDao
    abstract fun incentivePointDao(): IncentivePointDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "syncly_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
