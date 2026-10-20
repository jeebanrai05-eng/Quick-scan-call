package com.example.scanner

import com.example.data.model.DocumentData
import com.example.data.model.ScanResult
import com.example.data.model.ScanSource
import com.example.data.model.TextHighlightBlock
import android.graphics.RectF
import java.util.regex.Pattern

object PhoneNumberExtractor {

    // Regex patterns for international and local numbers
    private val PHONE_PATTERNS = listOf(
        // Kuwait with country code (+965 or 00965)
        Pattern.compile("""(?:\+965|00965)[-.\s]?[24569]\d{3}[-.\s]?\d{4}\b"""),
        // Nepal with country code (+977 or 00977)
        Pattern.compile("""(?:\+977|00977)[-.\s]?(?:9[78]\d{8}|0?1[-.\s]?\d{7}|0?[2-9]\d[-.\s]?\d{6,7})\b"""),
        // International with bracketed zero (e.g. +44 (0) 20 7946 0958)
        Pattern.compile("""(?:\+\d{1,3}[-.\s]?)?\(0\)[-.\s]?\d{2,4}[-.\s]?\d{3,4}[-.\s]?\d{3,4}"""),
        // Standard international with country code: +1, +44, +91, +971, +974, +966, etc.
        Pattern.compile("""(?:\+\d{1,4}[-.\s]?)?\(?\d{2,4}\)?[-.\s]?\d{3,4}[-.\s]?\d{3,4}"""),
        // Nepal standard 10-digit mobile (984/985/986/980/981/982/974/975/976/972)
        Pattern.compile("""\b(?:98[0-6]|97[2-6])\d{7}\b"""),
        // Kuwait local 8-digit mobile/landline (starts with 5, 6, 9, 4, 2)
        Pattern.compile("""\b[24569]\d{3}[-.\s]?\d{4}\b"""),
        // India standard 10-digit mobile (starts with 6, 7, 8, 9)
        Pattern.compile("""\b[6-9]\d{4}[-.\s]?\d{5}\b"""),
        // Toll-free / customer care (1800, 800, 888, 877, 866, 0800)
        Pattern.compile("""\b(?:1800|800|888|877|866|0800)[-.\s]?\d{3}[-.\s]?\d{4}\b"""),
        // Standard hyphen/space separated numbers
        Pattern.compile("""\b\d{3,5}[-.\s]\d{3,4}[-.\s]\d{3,4}\b"""),
        // Continuous 8 to 14 digits
        Pattern.compile("""\b(?:\+?\d{8,14})\b""")
    )

    private val EMAIL_PATTERN = Pattern.compile(
        """[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}"""
    )

    private val WEBSITE_PATTERN = Pattern.compile(
        """\b(?:https?://|www\.)[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(?:/[^\s]*)?\b""",
        Pattern.CASE_INSENSITIVE
    )

    private val DATE_PATTERNS = listOf(
        Pattern.compile("""\b\d{4}[-/.]\d{1,2}[-/.]\d{1,2}\b"""),
        Pattern.compile("""\b\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4}\b""")
    )

    private val TIME_PATTERN = Pattern.compile("""\b\d{1,2}:\d{2}(?::\d{2})?\s*(?:am|pm)?\b""", Pattern.CASE_INSENSITIVE)

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
        "hotline" to "Hotline",
        "fax" to "Fax",
        "care" to "Customer Care",
        "support" to "Customer Support",
        "helpline" to "Helpline",
        "toll free" to "Toll-Free"
    )

    private val BUSINESS_SUFFIXES = listOf(
        "pvt", "ltd", "llc", "w.l.l", "wll", "corp", "inc", "co.", "company",
        "enterprises", "trading", "group", "holdings", "solutions", "technologies",
        "store", "shop", "restaurant", "hotel", "cafe", "clinic", "hospital", "motors",
        "travels", "tours", "agency", "consultancy", "jewellers", "stationery", "hardware"
    )

    private val TITLE_PREFIXES = listOf(
        "mr.", "mrs.", "ms.", "dr.", "er.", "ca.", "prof.", "engr.", "adv."
    )

    private val ROLE_KEYWORDS = listOf(
        "manager", "director", "founder", "ceo", "proprietor", "owner",
        "partner", "president", "chairman", "consultant", "engineer",
        "executive", "officer", "supervisor", "agent", "specialist"
    )

    private val ADDRESS_KEYWORDS = listOf(
        "road", "rd.", "street", "st.", "marg", "chowk", "tole", "block",
        "sector", "floor", "avenue", "ave.", "blvd", "suite", "po box",
        "p.o. box", "dist.", "district", "city", "kuwait", "kathmandu",
        "pokhara", "nepal", "delhi", "mumbai", "dubai", "doha", "salmiya",
        "hawally", "farwaniya", "shuwaikh"
    )

    /**
     * Extracts phone numbers with rich metadata from raw text.
     */
    fun extractPhoneNumbers(
        text: String,
        source: ScanSource = ScanSource.LIVE_CAMERA,
        pageNumber: Int = 1
    ): List<ScanResult> {
        val documentData = extractFullDocument(text, source, pageNumber)
        return documentData.phoneNumbers
    }

    /**
     * Full document intelligence extraction: separates business name, contact person,
     * phone numbers with international resolution, emails, websites, and addresses.
     */
    fun extractFullDocument(
        text: String,
        source: ScanSource = ScanSource.LIVE_CAMERA,
        pageNumber: Int = 1
    ): DocumentData {
        val results = mutableListOf<ScanResult>()
        val seenNormalized = mutableSetOf<String>()
        val emails = mutableListOf<String>()
        val websites = mutableListOf<String>()
        val addresses = mutableListOf<String>()

        var detectedBusiness: String? = null
        var detectedPerson: String? = null

        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        // Contextual country detection from whole document
        val fullTextLower = text.lowercase()
        val detectedCountryCode: String?
        val detectedCountryName: String?
        when {
            fullTextLower.contains("kuwait") || fullTextLower.contains("+965") || fullTextLower.contains("salmiya") || fullTextLower.contains("hawally") || fullTextLower.contains("shuwaikh") -> {
                detectedCountryCode = "+965"
                detectedCountryName = "Kuwait 🇰🇼"
            }
            fullTextLower.contains("nepal") || fullTextLower.contains("+977") || fullTextLower.contains("kathmandu") || fullTextLower.contains("pokhara") || fullTextLower.contains("lalitpur") -> {
                detectedCountryCode = "+977"
                detectedCountryName = "Nepal 🇳🇵"
            }
            fullTextLower.contains("india") || fullTextLower.contains("+91") || fullTextLower.contains("delhi") || fullTextLower.contains("mumbai") -> {
                detectedCountryCode = "+91"
                detectedCountryName = "India 🇮🇳"
            }
            fullTextLower.contains("uae") || fullTextLower.contains("dubai") || fullTextLower.contains("+971") -> {
                detectedCountryCode = "+971"
                detectedCountryName = "UAE 🇦🇪"
            }
            fullTextLower.contains("qatar") || fullTextLower.contains("doha") || fullTextLower.contains("+974") -> {
                detectedCountryCode = "+974"
                detectedCountryName = "Qatar 🇶🇦"
            }
            fullTextLower.contains("saudi") || fullTextLower.contains("riyadh") || fullTextLower.contains("+966") -> {
                detectedCountryCode = "+966"
                detectedCountryName = "Saudi Arabia 🇸🇦"
            }
            else -> {
                detectedCountryCode = null
                detectedCountryName = null
            }
        }

        // 1. Extract Emails
        val emailMatcher = EMAIL_PATTERN.matcher(text)
        while (emailMatcher.find()) {
            val email = emailMatcher.group().trim()
            if (!emails.contains(email)) emails.add(email)
        }

        // 2. Extract Websites
        val webMatcher = WEBSITE_PATTERN.matcher(text)
        while (webMatcher.find()) {
            val web = webMatcher.group().trim()
            if (!websites.contains(web) && !emails.any { it.contains(web) }) {
                websites.add(web)
            }
        }

        // 3. Line-by-line categorization for Business, Person Name, Address, and Phone Numbers
        for ((index, line) in lines.withIndex()) {
            val lowerLine = line.lowercase()

            // Skip lines that are purely dates or times
            if (isDateOrTime(line)) continue

            // Check for Address
            if (ADDRESS_KEYWORDS.any { lowerLine.contains(it) } && !isPhoneLine(line)) {
                if (line.length in 8..80 && !addresses.contains(line)) {
                    addresses.add(line)
                }
            }

            // Check for Business / Organization Name
            if (detectedBusiness == null && (index <= 3 || BUSINESS_SUFFIXES.any { lowerLine.contains(it) })) {
                if (BUSINESS_SUFFIXES.any { lowerLine.contains(it) } ||
                    (index == 0 && line.length in 4..45 && !containsPhoneNumber(line) && !lowerLine.contains("welcome"))
                ) {
                    detectedBusiness = line
                }
            }

            // Check for Contact Person Name
            if (detectedPerson == null) {
                if (TITLE_PREFIXES.any { lowerLine.startsWith(it) } ||
                    ROLE_KEYWORDS.any { lowerLine.contains(it) } ||
                    (index in 1..3 && line.length in 5..30 && line.split(" ").size in 2..4 && !containsPhoneNumber(line) && !lowerLine.contains("road") && !lowerLine.contains("street"))
                ) {
                    detectedPerson = line.replace(Regex("""(?i)\s*[-|:]\s*(general\s+)?(manager|director|founder|ceo|proprietor|partner|president|chairman|consultant|engineer|executive|officer|supervisor|agent|specialist)\b.*"""), "")
                        .replace(Regex("""(?i)\b(manager|director|founder|ceo|proprietor|partner|president|chairman)\b.*"""), "")
                        .trim().removeSuffix("-").removeSuffix("|").removeSuffix(":").trim()
                    if (detectedPerson.isBlank()) detectedPerson = line
                }
            }

            // Determine line label
            var detectedLabel = "Phone Number"
            for ((key, label) in LABEL_KEYWORDS) {
                if (lowerLine.contains(key)) {
                    detectedLabel = label
                    break
                }
            }

            // 4. Extract phone numbers from line
            for (pattern in PHONE_PATTERNS) {
                val matcher = pattern.matcher(line)
                while (matcher.find()) {
                    val rawMatch = matcher.group()
                    val normalized = normalizePhoneNumber(rawMatch)

                    if (isValidPhoneNumber(normalized, rawMatch, line)) {
                        // Deduplicate with country-aware key
                        val dedupeKey = getCanonicalDedupeKey(normalized, detectedCountryCode)
                        if (seenNormalized.add(dedupeKey)) {
                            val (cCode, cName) = resolveCountry(rawMatch, normalized, detectedCountryCode, detectedCountryName)
                            val formatted = formatPhoneNumber(rawMatch, normalized, cCode)

                            results.add(
                                ScanResult(
                                    rawText = line,
                                    phoneNumber = if (rawMatch.startsWith("+")) rawMatch.trim() else if (cCode != null) "$cCode $normalized" else rawMatch.trim(),
                                    formattedNumber = formatted,
                                    label = detectedLabel,
                                    source = source,
                                    confidence = calculateConfidence(rawMatch, normalized, cCode),
                                    contactName = detectedPerson,
                                    businessName = detectedBusiness,
                                    email = emails.firstOrNull(),
                                    website = websites.firstOrNull(),
                                    address = addresses.firstOrNull(),
                                    countryCode = cCode,
                                    countryName = cName,
                                    pageNumber = pageNumber
                                )
                            )
                        }
                    }
                }
            }
        }

        return DocumentData(
            businessName = detectedBusiness,
            contactName = detectedPerson,
            phoneNumbers = results,
            emails = emails,
            websites = websites,
            addresses = addresses,
            detectedCountry = detectedCountryName,
            detectedCountryCode = detectedCountryCode,
            fullRawText = text,
            confidence = if (results.isNotEmpty()) results.map { it.confidence }.average().toFloat() else 0.90f
        )
    }

    /**
     * Resolves the country code and country name for a given phone number.
     */
    fun resolveCountry(
        rawMatch: String,
        normalized: String,
        contextCode: String?,
        contextName: String?
    ): Pair<String?, String?> {
        val clean = normalized.filter { it.isDigit() }

        return when {
            // Explicit +965 or 965 prefix -> Kuwait
            rawMatch.startsWith("+965") || clean.startsWith("965") && clean.length in 11..12 -> {
                Pair("+965", "Kuwait 🇰🇼")
            }
            // Explicit +977 or 977 prefix -> Nepal
            rawMatch.startsWith("+977") || clean.startsWith("977") && clean.length in 12..13 -> {
                Pair("+977", "Nepal 🇳🇵")
            }
            // Nepal 10-digit mobile (starts with 98 or 97)
            clean.length == 10 && (clean.startsWith("98") || clean.startsWith("97")) -> {
                Pair("+977", "Nepal 🇳🇵")
            }
            // Kuwait 8-digit mobile/landline (starts with 5, 6, 9, 4, 2)
            clean.length == 8 && clean[0] in listOf('2', '4', '5', '6', '9') -> {
                Pair("+965", "Kuwait 🇰🇼")
            }
            // India (+91)
            rawMatch.startsWith("+91") || clean.startsWith("91") && clean.length == 12 || (clean.length == 10 && clean[0] in '6'..'9' && contextName == "India") -> {
                Pair("+91", "India 🇮🇳")
            }
            // UAE (+971)
            rawMatch.startsWith("+971") || clean.startsWith("971") -> {
                Pair("+971", "UAE 🇦🇪")
            }
            // Qatar (+974)
            rawMatch.startsWith("+974") || clean.startsWith("974") -> {
                Pair("+974", "Qatar 🇶🇦")
            }
            // Saudi Arabia (+966)
            rawMatch.startsWith("+966") || clean.startsWith("966") -> {
                Pair("+966", "Saudi Arabia 🇸🇦")
            }
            // USA / Canada (+1)
            rawMatch.startsWith("+1") || clean.length == 11 && clean.startsWith("1") -> {
                Pair("+1", "USA / Canada 🇺🇸")
            }
            // UK (+44)
            rawMatch.startsWith("+44") || clean.startsWith("44") -> {
                Pair("+44", "UK 🇬🇧")
            }
            // Context-based fallback
            contextCode != null -> Pair(contextCode, contextName)
            else -> Pair(null, null)
        }
    }

    fun normalizePhoneNumber(number: String): String {
        return number.replace(Regex("[^0-9+]"), "").removePrefix("+")
    }

    private fun getCanonicalDedupeKey(normalized: String, contextCode: String?): String {
        val clean = normalized.filter { it.isDigit() }
        // If it starts with country code, strip to local core for deduplication
        return when {
            clean.startsWith("965") && clean.length == 11 -> clean.drop(3)
            clean.startsWith("977") && clean.length == 13 -> clean.drop(3)
            clean.startsWith("91") && clean.length == 12 -> clean.drop(2)
            clean.startsWith("1") && clean.length == 11 -> clean.drop(1)
            else -> clean
        }
    }

    /**
     * Strict verification to eliminate dates, PIN codes, currencies, and junk sequences.
     */
    fun isValidPhoneNumber(normalizedDigits: String, rawText: String, fullLine: String): Boolean {
        val cleanDigits = normalizedDigits.filter { it.isDigit() }
        if (cleanDigits.length !in 7..15) return false

        // Check if string contains currency symbols
        if (rawText.contains("$") || rawText.contains("€") || rawText.contains("£") ||
            fullLine.contains("Rs.", ignoreCase = true) || fullLine.contains("KD", ignoreCase = true) ||
            fullLine.contains("NPR", ignoreCase = true)
        ) {
            // If it has decimal point and 2 decimals, it's a price/currency
            if (rawText.contains(".") && rawText.substringAfterLast(".").length == 2) {
                return false
            }
        }

        // Check if string is a date
        for (pattern in DATE_PATTERNS) {
            if (pattern.matcher(rawText).find()) return false
        }

        // Check if string is a time
        if (TIME_PATTERN.matcher(rawText).find()) return false

        // Filter out dummy/repeated strings like "00000000", "11111111"
        val distinctChars = cleanDigits.toSet().size
        if (distinctChars <= 2) return false

        // Filter out sequential sequences like "12345678", "98765432"
        if (isSequential(cleanDigits)) return false

        return true
    }

    private fun isSequential(digits: String): Boolean {
        if (digits.length < 6) return false
        var isAscending = true
        var isDescending = true
        for (i in 0 until digits.length - 1) {
            if (digits[i + 1] - digits[i] != 1) isAscending = false
            if (digits[i] - digits[i + 1] != 1) isDescending = false
        }
        return isAscending || isDescending
    }

    private fun isDateOrTime(line: String): Boolean {
        for (pattern in DATE_PATTERNS) {
            if (pattern.matcher(line).matches()) return true
        }
        return TIME_PATTERN.matcher(line).matches()
    }

    private fun isPhoneLine(line: String): Boolean {
        return PHONE_PATTERNS.any { it.matcher(line).find() }
    }

    private fun containsPhoneNumber(line: String): Boolean {
        return PHONE_PATTERNS.any { it.matcher(line).find() }
    }

    /**
     * Nicely formats phone numbers according to their national / international standard.
     */
    fun formatPhoneNumber(raw: String, normalized: String, countryCode: String? = null): String {
        val digits = normalized.filter { it.isDigit() }

        // Kuwait (+965): 8 digits -> e.g. +965 9123 4567 or (965) 2244 1122
        if (countryCode == "+965" || digits.startsWith("965") && digits.length == 11 || digits.length == 8) {
            val local = if (digits.startsWith("965")) digits.drop(3) else digits
            if (local.length == 8) {
                return "+965 ${local.substring(0, 4)} ${local.substring(4)}"
            }
        }

        // Nepal (+977): 10-digit mobile -> +977 984-1234567
        if (countryCode == "+977" || digits.startsWith("977") && digits.length == 13 || (digits.length == 10 && (digits.startsWith("98") || digits.startsWith("97")))) {
            val local = if (digits.startsWith("977")) digits.drop(3) else digits
            if (local.length == 10) {
                return "+977 ${local.substring(0, 3)}-${local.substring(3, 6)}-${local.substring(6)}"
            }
        }

        // USA / Canada (+1)
        if (digits.length == 10) {
            return "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
        }
        if (digits.length == 11 && digits.startsWith("1")) {
            return "+1 (${digits.substring(1, 4)}) ${digits.substring(4, 7)}-${digits.substring(7)}"
        }

        // India (+91)
        if (digits.length == 12 && digits.startsWith("91")) {
            return "+91 ${digits.substring(2, 7)} ${digits.substring(7)}"
        }

        // UK (+44)
        if (digits.length >= 11 && digits.startsWith("44")) {
            return "+44 ${digits.substring(2, 6)} ${digits.substring(6)}"
        }

        return raw.trim()
    }

    private fun calculateConfidence(rawMatch: String, normalized: String, countryCode: String?): Float {
        return when {
            rawMatch.startsWith("+") -> 0.99f
            countryCode != null -> 0.97f
            normalized.length in 8..11 -> 0.94f
            else -> 0.88f
        }
    }
}
