package com.example.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.SpamReport

@Entity(
    tableName = "spam_reports",
    indices = [Index(value = ["normalizedNumber"], unique = true)]
)
data class SpamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val normalizedNumber: String,
    val reportReason: String,
    val reportedCount: Int = 1,
    val severity: String = "HIGH",
    val reportedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): SpamReport {
        return SpamReport(
            id = id,
            phoneNumber = phoneNumber,
            normalizedNumber = normalizedNumber,
            reportReason = reportReason,
            reportedCount = reportedCount,
            severity = severity,
            reportedAt = reportedAt
        )
    }

    companion object {
        fun fromDomain(report: SpamReport): SpamEntity {
            return SpamEntity(
                id = report.id,
                phoneNumber = report.phoneNumber,
                normalizedNumber = report.normalizedNumber,
                reportReason = report.reportReason,
                reportedCount = report.reportedCount,
                severity = report.severity,
                reportedAt = report.reportedAt
            )
        }
    }
}
