package com.example.scanner

import com.example.data.model.ScanResult
import com.example.data.model.ScanSource

object DamagedNumberRepairer {

    // Common OCR character confusions for scratched / broken digits
    private val GLITCH_TO_DIGIT = mapOf(
        'O' to '0', 'o' to '0', 'Q' to '0', 'D' to '0',
        'l' to '1', 'I' to '1', '|' to '1', '!' to '1', ']' to '1', '[' to '1', 'i' to '1',
        'Z' to '2', 'z' to '2',
        'E' to '3',
        'A' to '4',
        'S' to '5', 's' to '5', '$' to '5',
        'G' to '6', 'b' to '6',
        'T' to '7',
        'B' to '8',
        'g' to '9', 'q' to '9'
    )

    private val LABEL_PREFIXES = listOf(
        "tel", "phone", "mob", "mobile", "cell", "call", "ph", "wa", "whatsapp",
        "contact", "hotline", "helpline", "toll free", "support", "office", "fax", "direct"
    )

    /**
     * Inspects lines of OCR text, discovers corrupted/broken number sequences,
     * repairs glitched characters, and returns restored phone numbers.
     */
    fun repairAndExtract(
        fullText: String,
        source: ScanSource = ScanSource.LIVE_CAMERA
    ): List<ScanResult> {
        val results = mutableListOf<ScanResult>()
        val seen = mutableSetOf<String>()

        val lines = fullText.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.length < 7) continue

            // 1. Look for labeled lines (e.g., "Ph: 98O1234567" or "Tel: +977-98l234S678")
            val lower = trimmed.lowercase()
            val hasLabel = LABEL_PREFIXES.any { lower.contains(it) }

            // 2. Extract tokens separated by punctuation/spacing
            val words = trimmed.split(Regex("""[\s,;|]+"""))
            for (word in words) {
                val candidate = sanitizeToken(word)
                if (candidate.length in 7..16) {
                    val repaired = attemptRepair(candidate)
                    if (repaired != null && seen.add(repaired)) {
                        results.add(
                            ScanResult(
                                rawText = trimmed,
                                phoneNumber = repaired,
                                formattedNumber = PhoneNumberExtractor.formatPhoneNumber(repaired, repaired),
                                label = if (hasLabel) "Repaired Number" else "OCR Restored",
                                source = source,
                                confidence = 0.94f
                            )
                        )
                    }
                }
            }

            // 3. Whole-line multi-word phone repair (e.g., "98 41 23 45 67" or "+977 984 123 4567")
            val consolidatedLine = trimmed.replace(Regex("""[^0-9a-zA-Z+]"""), "")
            if (consolidatedLine.length in 9..15) {
                val repaired = attemptRepair(consolidatedLine)
                if (repaired != null && seen.add(repaired)) {
                    results.add(
                        ScanResult(
                            rawText = trimmed,
                            phoneNumber = repaired,
                            formattedNumber = PhoneNumberExtractor.formatPhoneNumber(repaired, repaired),
                            label = "Multi-Pass Recovered",
                            source = source,
                            confidence = 0.92f
                        )
                    )
                }
            }
        }

        return results
    }

    private fun sanitizeToken(token: String): String {
        return token.replace(Regex("""^[^\w+]+|[^\w]+$"""), "")
    }

    /**
     * Attempts to repair cracked, scratched or misread digits in a candidate string.
     */
    fun attemptRepair(candidate: String): String? {
        val hasPlus = candidate.startsWith("+")
        val cleanCandidate = if (hasPlus) candidate.drop(1) else candidate

        val sb = StringBuilder()
        var letterCount = 0
        var digitCount = 0

        for (ch in cleanCandidate) {
            if (ch.isDigit()) {
                sb.append(ch)
                digitCount++
            } else if (GLITCH_TO_DIGIT.containsKey(ch)) {
                sb.append(GLITCH_TO_DIGIT[ch])
                letterCount++
            } else if (ch == '-' || ch == '.' || ch == ' ') {
                // Ignore separators
            } else {
                // Unknown character, likely not a phone number
                return null
            }
        }

        // Only allow if majority of characters were already digits or valid glitches
        val totalValid = digitCount + letterCount
        if (totalValid !in 7..15) return null
        if (letterCount > 4) return null // If too many letters, it was probably regular text

        val finalDigits = (if (hasPlus) "+" else "") + sb.toString()

        // Validate structure against phone numbering patterns
        if (isValidTelecomFormat(sb.toString())) {
            return finalDigits
        }

        return null
    }

    private fun isValidTelecomFormat(digitsOnly: String): Boolean {
        if (digitsOnly.length !in 7..15) return false

        // Check for non-trivial distribution
        if (digitsOnly.toSet().size <= 2) return false

        // 1. Nepal 10-digit mobile (984, 985, 986, 980, 981, 982, 974, 975, 976, 972)
        if (digitsOnly.length == 10 && (digitsOnly.startsWith("98") || digitsOnly.startsWith("97"))) {
            return true
        }

        // 2. Nepal with country code (97798... or 97797...)
        if (digitsOnly.length == 13 && digitsOnly.startsWith("977")) {
            return true
        }

        // 3. Kuwait 8-digit mobile/landline (starts with 5, 6, 9, 4, 2)
        if (digitsOnly.length == 8 && digitsOnly[0] in listOf('2', '4', '5', '6', '9')) {
            return true
        }

        // 4. Kuwait with country code (965XXXXXXXX)
        if (digitsOnly.length == 11 && digitsOnly.startsWith("965")) {
            return true
        }

        // 5. Nepal landline (01-XXXXXXX -> 8 digits or 021, 061, 071 -> 8-9 digits)
        if (digitsOnly.length in 8..9 && digitsOnly.startsWith("0")) {
            return true
        }

        // 6. Standard North America 10-digit (NXX-NXX-XXXX)
        if (digitsOnly.length == 10 && digitsOnly[0] in '2'..'9' && digitsOnly[3] in '2'..'9') {
            return true
        }

        // 7. India 10-digit mobile (starts with 6, 7, 8, 9)
        if (digitsOnly.length == 10 && digitsOnly[0] in '6'..'9') {
            return true
        }

        // 8. India with country code (91XXXXXXXXXX)
        if (digitsOnly.length == 12 && digitsOnly.startsWith("91")) {
            return true
        }

        // 9. General international length
        return digitsOnly.length in 8..14
    }
}
