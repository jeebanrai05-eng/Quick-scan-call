package com.example.data.model

data class SpamReport(
    val id: Long = 0,
    val phoneNumber: String,
    val normalizedNumber: String,
    val reportReason: String,
    val reportedCount: Int = 1,
    val severity: String = "HIGH",
    val reportedAt: Long = System.currentTimeMillis()
)
