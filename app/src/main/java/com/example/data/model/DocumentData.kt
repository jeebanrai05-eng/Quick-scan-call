package com.example.data.model

import android.graphics.Bitmap
import android.graphics.RectF

/**
 * Structured document analysis containing categorized contact and organization fields.
 */
data class DocumentData(
    val businessName: String? = null,
    val contactName: String? = null,
    val phoneNumbers: List<ScanResult> = emptyList(),
    val emails: List<String> = emptyList(),
    val websites: List<String> = emptyList(),
    val addresses: List<String> = emptyList(),
    val detectedCountry: String? = null,
    val detectedCountryCode: String? = null,
    val textBlocks: List<TextHighlightBlock> = emptyList(),
    val fullRawText: String = "",
    val confidence: Float = 0.95f
)

/**
 * Text element with normalized coordinates (0.0 to 1.0) for visual overlay highlight.
 */
data class TextHighlightBlock(
    val text: String,
    val isPhoneNumber: Boolean,
    val bounds: RectF
)

/**
 * A captured page in a multi-page batch scan session.
 */
data class BatchPage(
    val pageIndex: Int,
    val bitmap: Bitmap,
    val documentData: DocumentData,
    val timestamp: Long = System.currentTimeMillis()
)
