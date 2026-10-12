package com.example.data.model

enum class ScanSource {
    VISITING_CARD,
    POSTER,
    PAPER,
    SHOP_BANNER,
    SCREENSHOT,
    LIVE_CAMERA
}

data class ScanResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val rawText: String,
    val phoneNumber: String,
    val formattedNumber: String,
    val label: String = "Detected Number",
    val source: ScanSource = ScanSource.LIVE_CAMERA,
    val confidence: Float = 0.95f,
    val timestamp: Long = System.currentTimeMillis(),
    val callerInfo: CallerInfo? = null
)
