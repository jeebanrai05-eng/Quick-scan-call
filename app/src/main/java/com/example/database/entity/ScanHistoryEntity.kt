package com.example.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.ScanResult
import com.example.data.model.ScanSource

@Entity(
    tableName = "scan_history",
    indices = [Index(value = ["phoneNumber"])]
)
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val formattedNumber: String,
    val label: String,
    val rawText: String,
    val source: String = ScanSource.LIVE_CAMERA.name,
    val carrier: String = "Unknown",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): ScanResult {
        val scanSource = try {
            ScanSource.valueOf(source)
        } catch (_: Exception) {
            ScanSource.LIVE_CAMERA
        }
        return ScanResult(
            phoneNumber = phoneNumber,
            formattedNumber = formattedNumber,
            label = label,
            rawText = rawText,
            source = scanSource
        )
    }

    companion object {
        fun fromDomain(result: ScanResult, carrier: String = "Unknown"): ScanHistoryEntity {
            return ScanHistoryEntity(
                phoneNumber = result.phoneNumber,
                formattedNumber = result.formattedNumber,
                label = result.label,
                rawText = result.rawText,
                source = result.source.name,
                carrier = carrier,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
