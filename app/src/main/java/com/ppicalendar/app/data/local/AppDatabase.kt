package com.ppicalendar.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@Database(
    entities = [
        ProcessedNotificationEntity::class,
        PlacementEventEntity::class,
        CompanyEntity::class,
        CompanyAttachmentEntity::class,
        IncentivePointEntity::class
    ],
    version = 6,
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

        private fun hasColumn(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
            return try {
                val cursor = db.query("PRAGMA table_info($tableName)")
                val nameIndex = cursor.getColumnIndex("name")
                var found = false
                while (cursor.moveToNext()) {
                    if (nameIndex != -1 && cursor.getString(nameIndex).equals(columnName, ignoreCase = true)) {
                        found = true
                        break
                    }
                }
                cursor.close()
                found
            } catch (e: Exception) {
                false
            }
        }

        private fun ensureSchemaV6(db: SupportSQLiteDatabase) {
            try {
                // Ensure applied column on companies table
                if (!hasColumn(db, "companies", "applied")) {
                    db.execSQL("ALTER TABLE companies ADD COLUMN applied INTEGER DEFAULT NULL")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                // Ensure title column on placement_events table
                if (!hasColumn(db, "placement_events", "title")) {
                    db.execSQL("ALTER TABLE placement_events ADD COLUMN title TEXT NOT NULL DEFAULT '-------'")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                // Ensure deadlineDate column on placement_events table
                if (!hasColumn(db, "placement_events", "deadlineDate")) {
                    db.execSQL("ALTER TABLE placement_events ADD COLUMN deadlineDate TEXT DEFAULT NULL")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                // Ensure deadlineTime column on placement_events table
                if (!hasColumn(db, "placement_events", "deadlineTime")) {
                    db.execSQL("ALTER TABLE placement_events ADD COLUMN deadlineTime TEXT DEFAULT NULL")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                ensureSchemaV6(database)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                ensureSchemaV6(database)
            }
        }

        val MIGRATION_4_6 = object : Migration(4, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                ensureSchemaV6(database)
            }
        }

        val MIGRATION_1_6 = object : Migration(1, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                ensureSchemaV6(database)
            }
        }

        val MIGRATION_2_6 = object : Migration(2, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                ensureSchemaV6(database)
            }
        }

        val MIGRATION_3_6 = object : Migration(3, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                ensureSchemaV6(database)
            }
        }

        private fun backupDatabaseFile(context: Context) {
            try {
                val dbFile = context.getDatabasePath("syncly_database")
                if (dbFile.exists() && dbFile.length() > 0) {
                    val backupFile = File(context.filesDir, "syncly_database_pre_migration.bak")
                    FileInputStream(dbFile).use { input ->
                        FileOutputStream(backupFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                backupDatabaseFile(context)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "syncly_database"
                )
                    .addMigrations(
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_4_6,
                        MIGRATION_1_6,
                        MIGRATION_2_6,
                        MIGRATION_3_6
                    )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
