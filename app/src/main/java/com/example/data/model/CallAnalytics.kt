package com.example.data.model

data class CallAnalytics(
    val totalCalls: Int = 0,
    val spamBlocked: Int = 0,
    val incomingCount: Int = 0,
    val outgoingCount: Int = 0,
    val missedCount: Int = 0,
    val minutesSaved: Int = 0,
    val safeVerifiedRatio: Float = 1.0f,
    val spamRiskPercent: Int = 0
)
