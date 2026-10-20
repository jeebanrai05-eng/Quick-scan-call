package com.example.scanner

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.ScanResult
import com.example.data.model.ScanSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object AiNumberScanner {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isAiAvailable(): Boolean {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            key.isNotBlank() && key != "DEFAULT_KEY" && key != "MY_GEMINI_API_KEY"
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Sends an image to Gemini 3.5 Flash to reconstruct blurry, cracked, scratched, or damaged numbers.
     */
    suspend fun analyzeImageWithAi(
        bitmap: Bitmap,
        source: ScanSource = ScanSource.SCREENSHOT
    ): Pair<List<ScanResult>, String> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "DEFAULT_KEY" || apiKey == "MY_GEMINI_API_KEY") {
            // Local fallback with multi-pass CV enhancement
            val localResults = OcrScanner.processBitmapMultiPass(bitmap, source).phoneNumbers
            return@withContext Pair(
                localResults,
                "Used Turbo Multi-Pass on-device engine (offline high-accuracy)."
            )
        }

        // Scale bitmap if excessively large to keep request fast and reliable
        val scaled = scaleBitmap(bitmap, maxDimension = 1200)
        val base64Image = bitmapToBase64(scaled)

        val prompt = """
            You are a specialized optical telephone number extraction and restoration AI.
            Analyze this image carefully. The image contains one or more phone numbers which may be blurry (madhuro), faint, cracked, scratched, partially broken, handwritten, or low contrast.
            
            Tasks:
            1. Find all phone numbers visible on the cards, paper, bills, or signboards.
            2. If digits are scratched, cracked, or faint, restore the true digits using contextual patterns (e.g. Kuwait +965, Nepal 984/985/986/980/981/982/974/975, India 9/8/7/6, US 10-digit).
            3. Detect any associated label or business name (e.g. Mobile, WhatsApp, Office, Support, Personal).
            
            Output strictly a valid JSON array of objects with the following keys for each number:
            [
              {
                "phoneNumber": "+97798XXXXXXXX",
                "label": "Mobile (AI Restored)",
                "rawContext": "Line context found near number",
                "confidence": 0.98
              }
            ]
            If no numbers are found, return an empty JSON array []. Do not include markdown formatting or backticks.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            })
        }

        val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(requestUrl)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                // Fall back to multi-pass local scanner
                val localResults = OcrScanner.processBitmapMultiPass(bitmap, source).phoneNumbers
                return@withContext Pair(
                    localResults,
                    "AI service unavailable (Code ${response.code}). Restored using Turbo Multi-Pass."
                )
            }

            val parsedResults = parseGeminiResponse(responseBody, source)
            if (parsedResults.isEmpty()) {
                // Double check with local multi-pass engine
                val localResults = OcrScanner.processBitmapMultiPass(bitmap, source).phoneNumbers
                return@withContext Pair(
                    localResults,
                    if (localResults.isEmpty()) "No numbers detected in image." else "Recovered ${localResults.size} number(s) via Local CV Engine."
                )
            }

            Pair(parsedResults, "AI successfully reconstructed ${parsedResults.size} number(s) with high accuracy!")
        } catch (e: Exception) {
            val localResults = OcrScanner.processBitmapMultiPass(bitmap, source).phoneNumbers
            Pair(
                localResults,
                "AI network error: ${e.message ?: "fallback applied"}. Used Turbo Multi-Pass."
            )
        }
    }

    private fun parseGeminiResponse(jsonResponse: String, source: ScanSource): List<ScanResult> {
        val results = mutableListOf<ScanResult>()
        try {
            val root = JSONObject(jsonResponse)
            val candidates = root.optJSONArray("candidates") ?: return emptyList()
            val firstCandidate = candidates.optJSONObject(0) ?: return emptyList()
            val content = firstCandidate.optJSONObject("content") ?: return emptyList()
            val parts = content.optJSONArray("parts") ?: return emptyList()
            val text = parts.optJSONObject(0)?.optString("text") ?: return emptyList()

            // Text is expected to be a JSON array
            val cleanJson = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val array = JSONArray(cleanJson)

            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val rawNumber = item.optString("phoneNumber")
                val label = item.optString("label", "AI Restored")
                val rawContext = item.optString("rawContext", rawNumber)
                val confidence = item.optDouble("confidence", 0.96).toFloat()

                val cleanDigits = rawNumber.replace(Regex("[^0-9+]"), "")
                if (cleanDigits.length >= 7) {
                    val formatted = PhoneNumberExtractor.formatPhoneNumber(rawNumber, cleanDigits)
                    results.add(
                        ScanResult(
                            rawText = rawContext,
                            phoneNumber = cleanDigits,
                            formattedNumber = formatted,
                            label = "✨ $label",
                            source = source,
                            confidence = confidence
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Error parsing JSON
        }
        return results
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (ratio > 1) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}
