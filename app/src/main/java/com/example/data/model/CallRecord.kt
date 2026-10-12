package com.example.data.model

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED,
    SPAM_BLOCKED
}

data class CallRecord(
    val id: Long = 0,
    val phoneNumber: String,
    val callerName: String?,
    val callType: CallType = CallType.INCOMING,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val isSpam: Boolean = false,
    val spamReason: String? = null,
    val simSlot: Int = 1,
    val category: String? = null
)
