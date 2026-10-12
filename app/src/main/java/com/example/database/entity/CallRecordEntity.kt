package com.example.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.CallRecord
import com.example.data.model.CallType

@Entity(tableName = "call_records")
data class CallRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val callerName: String?,
    val callType: String = CallType.INCOMING.name,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val isSpam: Boolean = false,
    val spamReason: String? = null,
    val simSlot: Int = 1,
    val category: String? = null
) {
    fun toDomain(): CallRecord {
        val type = try {
            CallType.valueOf(callType)
        } catch (_: Exception) {
            CallType.INCOMING
        }
        return CallRecord(
            id = id,
            phoneNumber = phoneNumber,
            callerName = callerName,
            callType = type,
            timestamp = timestamp,
            durationSeconds = durationSeconds,
            isSpam = isSpam,
            spamReason = spamReason,
            simSlot = simSlot,
            category = category
        )
    }

    companion object {
        fun fromDomain(record: CallRecord): CallRecordEntity {
            return CallRecordEntity(
                id = record.id,
                phoneNumber = record.phoneNumber,
                callerName = record.callerName,
                callType = record.callType.name,
                timestamp = record.timestamp,
                durationSeconds = record.durationSeconds,
                isSpam = record.isSpam,
                spamReason = record.spamReason,
                simSlot = record.simSlot,
                category = record.category
            )
        }
    }
}
