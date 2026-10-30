package com.example.util

/**
 * Utility for normalizing phone numbers and performing fuzzy matching
 * based on the last 10 digits as requested.
 *
 * Example matches:
 * "+91 98765 43210", "098765 43210", and "9876543210" all resolve to "9876543210".
 * Unknown, private, or short numbers (under 7 digits) never match.
 */
object PhoneNumberMatcher {

    /**
     * Strips all non-digit characters from the input string.
     */
    fun normalize(phoneNumber: String?): String {
        if (phoneNumber.isNullOrBlank()) return ""
        return phoneNumber.filter { it.isDigit() }
    }

    /**
     * Extracts the last 10 digits from a normalized phone number string.
     * If the number has fewer than 10 digits, returns the available digits.
     */
    fun last10Digits(normalized: String): String {
        return if (normalized.length >= 10) {
            normalized.takeLast(10)
        } else {
            normalized
        }
    }

    /**
     * Compares an incoming phone number with a stored VIP number.
     *
     * Rules:
     * - Unknown, private, empty, or short caller IDs (under 7 digits) return false.
     * - Compares the last 10 digits of each number.
     */
    fun matches(incomingNumber: String?, storedNumber: String?): Boolean {
        if (incomingNumber.isNullOrBlank() || storedNumber.isNullOrBlank()) {
            return false
        }

        // Catch string indicators of private/withheld/unknown calls
        val lowerIncoming = incomingNumber.trim().lowercase()
        if (lowerIncoming in listOf("private", "unknown", "withheld", "restricted", "anonymous", "null", "-1", "-2")) {
            return false
        }

        val normIncoming = normalize(incomingNumber)
        val normStored = normalize(storedNumber)

        // Must have at least 7 digits to prevent false matches on short codes or prefixes
        if (normIncoming.length < 7 || normStored.length < 7) {
            return false
        }

        val incomingKey = last10Digits(normIncoming)
        val storedKey = last10Digits(normStored)

        return incomingKey == storedKey
    }
}
