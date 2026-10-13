package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.database.dao.CallRecordDao
import com.example.database.dao.CallerDao
import com.example.database.dao.ScanHistoryDao
import com.example.database.dao.SpamDao
import com.example.database.entity.CallRecordEntity
import com.example.database.entity.CallerEntity
import com.example.database.entity.ScanHistoryEntity
import com.example.database.entity.SpamEntity

@Database(
    entities = [
        CallerEntity::class,
        CallRecordEntity::class,
        SpamEntity::class,
        ScanHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callerDao(): CallerDao
    abstract fun callRecordDao(): CallRecordDao
    abstract fun spamDao(): SpamDao
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quick_call_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
