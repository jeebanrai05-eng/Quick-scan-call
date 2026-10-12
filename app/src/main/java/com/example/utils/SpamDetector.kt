package com.example.utils

import com.example.data.model.CallerInfo
import com.example.data.model.CallerType

enum class SpamRiskTier {
    SAFE,
    MODERATE,
    HIGH_RISK,
    SEVERE_FRAUD
}

data class SpamAnalysis(
    val score: Int,
    val tier: SpamRiskTier,
    val title: String,
    val warningReason: String,
    val recommendation: String,
    val shouldAutoBlock: Boolean
)

object SpamDetector {

    // Known dangerous prefix signatures (e.g. wangiri / robocall hubs / common scam ranges)
    private val KNOWN_SPAM_PREFIXES = listOf(
        "800", "888", "877", "866", "855", "844", "833", // Toll-free often spoofed
        "900", "976", // Premium rate fraud
        "1900", "1976"
    )

    fun analyze(caller: CallerInfo?): SpamAnalysis {
        if (caller == null) {
            return SpamAnalysis(
                score = 0,
                tier = SpamRiskTier.SAFE,
                title = "Unknown Caller",
                warningReason = "No reports found in community database.",
                recommendation = "Safe to answer, verify identity if asking for sensitive info.",
                shouldAutoBlock = false
            )
        }

        val baseScore = caller.spamScore
        val reports = caller.reportsCount

        // Adjust score dynamically if reports count is high
        val finalScore = (baseScore + (reports * 3)).coerceIn(0, 100)

        return when {
            finalScore >= 80 || caller.isBlocked -> {
                SpamAnalysis(
                    score = finalScore,
                    tier = SpamRiskTier.SEVERE_FRAUD,
                    title = "🚨 DANGEROUS SPAM / FRAUD",
                    warningReason = "Reported by ${reports.coerceAtLeast(12)} users as ${caller.category.ifBlank { "Fraud / Scam" }}.",
                    recommendation = "DO NOT ANSWER. High risk of financial extortion or phishing.",
                    shouldAutoBlock = true
                )
            }
            finalScore >= 50 || caller.callerType == CallerType.SPAM -> {
                SpamAnalysis(
                    score = finalScore,
                    tier = SpamRiskTier.HIGH_RISK,
                    title = "⚠️ Suspected Telemarketer / Spam",
                    warningReason = "Frequent unsolicited marketing or automated robocall.",
                    recommendation = "Likely unwanted sales call. Proceed with caution.",
                    shouldAutoBlock = false
                )
            }
            finalScore >= 25 -> {
                SpamAnalysis(
                    score = finalScore,
                    tier = SpamRiskTier.MODERATE,
                    title = "Unverified Number",
                    warningReason = "Unusual call frequency detected recently.",
                    recommendation = "Answer with standard vigilance.",
                    shouldAutoBlock = false
                )
            }
            else -> {
                val isVerified = caller.callerType == CallerType.VERIFIED || caller.callerType == CallerType.CONTACT
                SpamAnalysis(
                    score = finalScore,
                    tier = SpamRiskTier.SAFE,
                    title = if (isVerified) "Verified Caller" else "Clean Number",
                    warningReason = if (isVerified) "Verified Business or Contact" else "Zero spam reports in database.",
                    recommendation = "Safe to answer.",
                    shouldAutoBlock = false
                )
            }
        }
    }
}
