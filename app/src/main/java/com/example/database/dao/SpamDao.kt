package com.example.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.database.entity.SpamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpamDao {
    @Query("SELECT * FROM spam_reports ORDER BY reportedAt DESC")
    fun getAllSpamReports(): Flow<List<SpamEntity>>

    @Query("SELECT * FROM spam_reports WHERE normalizedNumber = :normalizedNumber LIMIT 1")
    suspend fun getSpamReport(normalizedNumber: String): SpamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpamReport(report: SpamEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpamReports(reports: List<SpamEntity>)

    @Query("UPDATE spam_reports SET reportedCount = reportedCount + 1, reportedAt = :now WHERE normalizedNumber = :normalizedNumber")
    suspend fun incrementReportCount(normalizedNumber: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM spam_reports WHERE id = :id")
    suspend fun deleteSpamReportById(id: Long)

    @Query("SELECT COUNT(*) FROM spam_reports")
    suspend fun getSpamCount(): Int
}
