package com.example.data.model

import android.graphics.RectF

enum class ScanSource {
    VISITING_CARD,
    POSTER,
    PAPER,
    SHOP_BANNER,
    SCREENSHOT,
    LIVE_CAMERA,
    BATCH_DOCUMENT
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
    val callerInfo: CallerInfo? = null,
    // Upgraded document & telecom fields
    val contactName: String? = null,
    val businessName: String? = null,
    val email: String? = null,
    val website: String? = null,
    val address: String? = null,
    val countryCode: String? = null,
    val countryName: String? = null,
    val boxLeft: Float = 0f,
    val boxTop: Float = 0f,
    val boxRight: Float = 0f,
    val boxBottom: Float = 0f,
    val pageNumber: Int = 1
) {
    fun hasBoundingBox(): Boolean = boxRight > boxLeft && boxBottom > boxTop

    fun getBoundingBox(): RectF? {
        return if (hasBoundingBox()) RectF(boxLeft, boxTop, boxRight, boxBottom) else null
    }
}
