package com.example.data.model

enum class CallerType {
    CONTACT,
    BUSINESS,
    VERIFIED,
    SPAM,
    UNKNOWN
}

data class CallerInfo(
    val id: Long = 0,
    val phoneNumber: String,
    val normalizedNumber: String,
    val name: String,
    val callerType: CallerType = CallerType.UNKNOWN,
    val category: String = "Unknown",
    val spamScore: Int = 0,
    val reportsCount: Int = 0,
    val city: String = "Unknown",
    val country: String = "US",
    val carrier: String = "Cellular",
    val isBlocked: Boolean = false,
    val isFavorite: Boolean = false,
    val note: String? = null,
    val tag: String? = null,
    val avatarUrl: String? = null
) {
    val isSpam: Boolean get() = spamScore >= 50 || callerType == CallerType.SPAM
    val isVerified: Boolean get() = callerType == CallerType.VERIFIED || callerType == CallerType.CONTACT
}
