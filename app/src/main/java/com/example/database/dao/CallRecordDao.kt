package com.example.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.database.entity.CallRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallRecordDao {
    @Query("SELECT * FROM call_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records WHERE callType = :type ORDER BY timestamp DESC")
    fun getRecordsByType(type: String): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records WHERE isSpam = 1 ORDER BY timestamp DESC")
    fun getSpamRecords(): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records WHERE phoneNumber LIKE '%' || :query || '%' OR callerName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchRecords(query: String): Flow<List<CallRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: CallRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<CallRecordEntity>)

    @Query("DELETE FROM call_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM call_records")
    suspend fun clearAllRecords()

    @Query("SELECT COUNT(*) FROM call_records")
    suspend fun getRecordCount(): Int
}
