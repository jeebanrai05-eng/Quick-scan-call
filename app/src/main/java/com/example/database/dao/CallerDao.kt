package com.example.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.database.entity.CallerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallerDao {
    @Query("SELECT * FROM callers ORDER BY name ASC")
    fun getAllCallers(): Flow<List<CallerEntity>>

    @Query("SELECT * FROM callers WHERE normalizedNumber = :normalizedNumber LIMIT 1")
    suspend fun getCallerByNormalizedNumber(normalizedNumber: String): CallerEntity?

    @Query("SELECT * FROM callers WHERE normalizedNumber LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%'")
    fun searchCallers(query: String): Flow<List<CallerEntity>>

    @Query("SELECT * FROM callers WHERE isBlocked = 1")
    fun getBlockedCallers(): Flow<List<CallerEntity>>

    @Query("SELECT * FROM callers WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteCallers(): Flow<List<CallerEntity>>

    @Query("SELECT * FROM callers WHERE callerType = 'SPAM' OR spamScore >= 50")
    fun getSpamCallers(): Flow<List<CallerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaller(caller: CallerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallers(callers: List<CallerEntity>)

    @Update
    suspend fun updateCaller(caller: CallerEntity)

    @Query("UPDATE callers SET isBlocked = :blocked WHERE normalizedNumber = :normalizedNumber")
    suspend fun setBlockedStatus(normalizedNumber: String, blocked: Boolean)

    @Query("UPDATE callers SET isFavorite = :favorite WHERE normalizedNumber = :normalizedNumber")
    suspend fun setFavoriteStatus(normalizedNumber: String, favorite: Boolean)

    @Query("UPDATE callers SET note = :note, tag = :tag WHERE normalizedNumber = :normalizedNumber")
    suspend fun updateCallerNoteAndTag(normalizedNumber: String, note: String?, tag: String?)

    @Query("UPDATE callers SET spamScore = :spamScore, reportsCount = reportsCount + 1, callerType = 'SPAM' WHERE normalizedNumber = :normalizedNumber")
    suspend fun reportCallerAsSpam(normalizedNumber: String, spamScore: Int)

    @Query("DELETE FROM callers WHERE id = :id")
    suspend fun deleteCallerById(id: Long)

    @Query("SELECT COUNT(*) FROM callers")
    suspend fun getCallerCount(): Int
}
