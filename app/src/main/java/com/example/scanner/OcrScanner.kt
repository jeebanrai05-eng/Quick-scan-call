package com.example.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.data.model.ScanResult
import com.example.data.model.ScanSource
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.coroutines.resume

object OcrScanner {

    private val recognizer by lazy {
        runCatching {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        }.getOrNull()
    }

    /**
     * Single-pass OCR recognition with damaged number repair.
     */
    suspend fun processBitmap(
        bitmap: Bitmap,
        source: ScanSource = ScanSource.SCREENSHOT
    ): List<ScanResult> = suspendCancellableCoroutine { continuation ->
        try {
            val client = recognizer
            if (client == null || bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
                if (continuation.isActive) continuation.resume(emptyList())
                return@suspendCancellableCoroutine
            }

            val image = InputImage.fromBitmap(bitmap, 0)
            client.process(image)
                .addOnSuccessListener { visionText ->
                    val standard = PhoneNumberExtractor.extractPhoneNumbers(visionText.text, source)
                    val repaired = DamagedNumberRepairer.repairAndExtract(visionText.text, source)

                    val merged = mergeAndDeduplicate(standard, repaired)
                    if (continuation.isActive) {
                        continuation.resume(merged)
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume(emptyList())
                    }
                }
        } catch (_: Throwable) {
            if (continuation.isActive) {
                continuation.resume(emptyList())
            }
        }
    }

    /**
     * Multi-Pass Turbo Computer Vision Scanner:
     * Restores blurry, faint (madhuro) and corrupted/scratched (crash) numbers by running:
     * 1. Standard Pass
     * 2. High-Contrast & Edge Sharpening Pass (unsharp masking)
     * 3. Grayscale Luminance Boost Pass
     * 4. Inverted Pass (for white numbers on dark/neon background)
     */
    suspend fun processBitmapMultiPass(
        bitmap: Bitmap,
        source: ScanSource = ScanSource.SCREENSHOT
    ): List<ScanResult> = withContext(Dispatchers.Default) {
        val allDetected = mutableListOf<ScanResult>()

        runCatching {
            // Pass 1: Original image
            val pass1 = processBitmap(bitmap, source)
            allDetected.addAll(pass1)

            // Pass 2: Enhanced Contrast (for blurry / faint / low-contrast)
            val enhanced = ImageEnhancer.enhanceContrast(bitmap, contrast = 1.8f, brightness = -15f)
            val pass2 = processBitmap(enhanced, source)
            for (item in pass2) {
                allDetected.add(item.copy(label = if (item.label.contains("Repair")) item.label else "Contrast Enhanced"))
            }

            // Pass 3: Grayscale boost
            val grayscale = ImageEnhancer.enhanceGrayscale(bitmap)
            val pass3 = processBitmap(grayscale, source)
            for (item in pass3) {
                allDetected.add(item.copy(label = if (item.label.contains("Repair")) item.label else "Luminance Boosted"))
            }

            // Pass 4: Inverted color (white on black/neon)
            val inverted = ImageEnhancer.invertColors(bitmap)
            val pass4 = processBitmap(inverted, source)
            for (item in pass4) {
                allDetected.add(item.copy(label = if (item.label.contains("Repair")) item.label else "Inverted Mode"))
            }
        }

        mergeAndDeduplicate(allDetected, emptyList())
    }

    suspend fun processUri(
        context: Context,
        uri: Uri,
        source: ScanSource = ScanSource.SCREENSHOT,
        useMultiPass: Boolean = true
    ): List<ScanResult> {
        val bitmap = runCatching {
            var inputStream: InputStream? = null
            try {
                inputStream = context.contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(inputStream)
            } finally {
                inputStream?.close()
            }
        }.getOrNull() ?: return emptyList()

        return if (useMultiPass) {
            processBitmapMultiPass(bitmap, source)
        } else {
            processBitmap(bitmap, source)
        }
    }

    private fun mergeAndDeduplicate(listA: List<ScanResult>, listB: List<ScanResult>): List<ScanResult> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<ScanResult>()

        for (item in (listA + listB)) {
            val normalized = PhoneNumberExtractor.normalizePhoneNumber(item.phoneNumber)
            if (PhoneNumberExtractor.isValidPhoneNumber(normalized) && seen.add(normalized)) {
                result.add(item)
            }
        }
        return result
    }

    class LiveImageAnalyzer(
        private val onNumbersDetected: (List<ScanResult>, String) -> Unit
    ) : ImageAnalysis.Analyzer {

        private var lastAnalyzedTimestamp = 0L

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val currentTimestamp = System.currentTimeMillis()
            // Throttle to 1 analysis every 280ms for ultra-fast, smooth tracking
            if (currentTimestamp - lastAnalyzedTimestamp < 280) {
                imageProxy.close()
                return
            }

            try {
                val client = recognizer
                val mediaImage = imageProxy.image
                if (client != null && mediaImage != null) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    client.process(image)
                        .addOnSuccessListener { visionText ->
                            runCatching {
                                val text = visionText.text
                                val standardNumbers = PhoneNumberExtractor.extractPhoneNumbers(text, ScanSource.LIVE_CAMERA)
                                val repairedNumbers = DamagedNumberRepairer.repairAndExtract(text, ScanSource.LIVE_CAMERA)

                                val allNumbers = mergeAndDeduplicate(standardNumbers, repairedNumbers)

                                if (allNumbers.isNotEmpty() || text.isNotBlank()) {
                                    onNumbersDetected(allNumbers, text)
                                }
                            }
                        }
                        .addOnCompleteListener {
                            lastAnalyzedTimestamp = currentTimestamp
                            runCatching { imageProxy.close() }
                        }
                } else {
                    imageProxy.close()
                }
            } catch (_: Throwable) {
                runCatching { imageProxy.close() }
            }
        }
    }
}
