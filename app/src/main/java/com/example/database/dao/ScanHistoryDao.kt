package com.example.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.database.entity.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScanned(): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanned(entity: ScanHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScannedList(entities: List<ScanHistoryEntity>)

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteScannedById(id: Long)

    @Query("DELETE FROM scan_history WHERE phoneNumber = :phoneNumber")
    suspend fun deleteByPhoneNumber(phoneNumber: String)

    @Query("DELETE FROM scan_history")
    suspend fun clearAllScanned()

    @Query("SELECT COUNT(*) FROM scan_history")
    suspend fun getScannedCount(): Int
}
