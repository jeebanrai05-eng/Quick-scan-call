package com.example.utils

object T9Matcher {

    private val CHAR_TO_DIGIT_MAP = mapOf(
        'a' to '2', 'b' to '2', 'c' to '2',
        'd' to '3', 'e' to '3', 'f' to '3',
        'g' to '4', 'h' to '4', 'i' to '4',
        'j' to '5', 'k' to '5', 'l' to '5',
        'm' to '6', 'n' to '6', 'o' to '6',
        'p' to '7', 'q' to '7', 'r' to '7', 's' to '7',
        't' to '8', 'u' to '8', 'v' to '8',
        'w' to '9', 'x' to '9', 'y' to '9', 'z' to '9'
    )

    fun toT9Digits(name: String): String {
        val sb = StringBuilder()
        for (ch in name.lowercase()) {
            if (CHAR_TO_DIGIT_MAP.containsKey(ch)) {
                sb.append(CHAR_TO_DIGIT_MAP[ch])
            } else if (ch.isDigit()) {
                sb.append(ch)
            } else if (ch == ' ') {
                sb.append(' ')
            }
        }
        return sb.toString()
    }

    /**
     * Checks if a contact name or phone number matches the typed T9 query.
     */
    fun matches(queryDigits: String, name: String, phoneNumber: String): Boolean {
        if (queryDigits.isBlank()) return true

        val cleanQuery = queryDigits.replace("[^0-9*#+]".toRegex(), "")
        if (cleanQuery.isEmpty()) return false

        // 1. Direct phone number digits substring match
        val cleanNumber = phoneNumber.replace("[^0-9]".toRegex(), "")
        if (cleanNumber.contains(cleanQuery)) {
            return true
        }

        // 2. Name converted to T9 digits match
        val t9Name = toT9Digits(name)
        // Matches full string or any individual word prefix (e.g. John Doe matching "363" for Doe)
        if (t9Name.replace(" ", "").contains(cleanQuery)) {
            return true
        }
        val words = t9Name.split(" ")
        for (word in words) {
            if (word.startsWith(cleanQuery)) {
                return true
            }
        }

        return false
    }
}
