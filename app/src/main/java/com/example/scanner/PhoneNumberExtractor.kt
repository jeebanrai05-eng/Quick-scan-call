package com.example.scanner

import com.example.data.model.ScanResult
import com.example.data.model.ScanSource
import java.util.regex.Pattern

object PhoneNumberExtractor {

    // Regex pattern matching global phone numbers:
    // +?([0-9]{1,4})?[-. ]?(\(?[0-9]{2,4}\)?)?[-. ]?[0-9]{3,4}[-. ]?[0-9]{3,4}
    private val PHONE_PATTERNS = listOf(
        // International with country code (+1, +44, +91, etc.)
        Pattern.compile("""(?:\+?\d{1,4}[-.\s]?)?\(?\d{2,4}\)?[-.\s]?\d{3,4}[-.\s]?\d{3,4}"""),
        // Standard 10-12 digits with separators
        Pattern.compile("""\b\d{3,5}[-.\s]\d{3,4}[-.\s]\d{3,4}\b"""),
        // Continuous 10-13 digits
        Pattern.compile("""\b(?:\+?\d{10,13})\b"""),
        // Toll-free / customer care (1800 / 800)
        Pattern.compile("""\b(?:1800|800|888|877|866)[-.\s]?\d{3}[-.\s]?\d{4}\b""")
    )

    private val LABEL_KEYWORDS = mapOf(
        "whatsapp" to "WhatsApp",
        "wa" to "WhatsApp",
        "mobile" to "Mobile",
        "mob" to "Mobile",
        "cell" to "Cell",
        "phone" to "Phone",
        "tel" to "Telephone",
        "call" to "Direct Call",
        "contact" to "Contact",
        "office" to "Office",
        "fax" to "Fax",
        "care" to "Customer Care",
        "support" to "Customer Support",
        "helpline" to "Helpline",
        "toll free" to "Toll-Free"
    )

    fun extractPhoneNumbers(text: String, source: ScanSource = ScanSource.LIVE_CAMERA): List<ScanResult> {
        val results = mutableListOf<ScanResult>()
        val seenNumbers = mutableSetOf<String>()

        val lines = text.lines()

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            // Determine line label if any
            val lowerLine = trimmedLine.lowercase()
            var detectedLabel = "Phone Number"
            for ((key, label) in LABEL_KEYWORDS) {
                if (lowerLine.contains(key)) {
                    detectedLabel = label
                    break
                }
            }

            for (pattern in PHONE_PATTERNS) {
                val matcher = pattern.matcher(trimmedLine)
                while (matcher.find()) {
                    val rawMatch = matcher.group()
                    val normalized = normalizePhoneNumber(rawMatch)

                    // Must have at least 7 digits (local) up to 15 digits (E.164 max)
                    if (isValidPhoneNumber(normalized) && seenNumbers.add(normalized)) {
                        val formatted = formatPhoneNumber(rawMatch, normalized)
                        results.add(
                            ScanResult(
                                rawText = trimmedLine,
                                phoneNumber = rawMatch.trim(),
                                formattedNumber = formatted,
                                label = detectedLabel,
                                source = source,
                                confidence = if (rawMatch.startsWith("+") || normalized.length == 10) 0.98f else 0.88f
                            )
                        )
                    }
                }
            }
        }

        return results
    }

    fun normalizePhoneNumber(number: String): String {
        return number.replace(Regex("[^0-9+]"), "").removePrefix("+")
    }

    fun isValidPhoneNumber(normalizedDigits: String): Boolean {
        // Strip any leading zeros or country codes for length check
        val cleanDigits = normalizedDigits.filter { it.isDigit() }
        if (cleanDigits.length !in 7..15) return false

        // Check it's not a dummy string like "0000000000" or sequential "1234567890"
        val distinctChars = cleanDigits.toSet().size
        if (distinctChars <= 2) return false

        return true
    }

    fun formatPhoneNumber(raw: String, normalized: String): String {
        val digits = normalized.filter { it.isDigit() }
        return when {
            digits.length == 10 -> {
                "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
            }
            digits.length == 11 && digits.startsWith("1") -> {
                "+1 (${digits.substring(1, 4)}) ${digits.substring(4, 7)}-${digits.substring(7)}"
            }
            digits.length == 12 && digits.startsWith("91") -> {
                "+91 ${digits.substring(2, 7)} ${digits.substring(7)}"
            }
            else -> raw.trim()
        }
    }
}
