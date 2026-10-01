package com.example.model

import androidx.compose.ui.graphics.Color

enum class TradingPlatform(
    val displayName: String,
    val primaryColor: Color,
    val badgeColor: Color,
    val shortCode: String
) {
    ZERODHA("Zerodha", Color(0xFF0052CC), Color(0xFFE0E7FF), "Z"),
    GROWW("Groww", Color(0xFF00B386), Color(0xFFD1FAE5), "G"),
    SAHI("Sahi", Color(0xFF4F46E5), Color(0xFFEDE9FE), "S"),
    DHAN("Dhan", Color(0xFF7E22CE), Color(0xFFF3E8FF), "D"),
    ANGEL_ONE("Angel One", Color(0xFFE11D48), Color(0xFFFFE4E6), "A"),
    UPSTOX("Upstox", Color(0xFF6B21A8), Color(0xFFF5F3FF), "U"),
    OTHER("Other", Color(0xFF475569), Color(0xFFF1F5F9), "O");

    companion object {
        fun fromString(name: String): TradingPlatform {
            val clean = name.trim()
            return entries.find {
                it.displayName.equals(clean, ignoreCase = true) ||
                it.name.equals(clean, ignoreCase = true) ||
                (it == SAHI && (clean.contains("sahi", ignoreCase = true) || clean.contains("shoonya", ignoreCase = true))) ||
                (it == ZERODHA && clean.contains("zerodha", ignoreCase = true)) ||
                (it == GROWW && clean.contains("groww", ignoreCase = true)) ||
                (it == DHAN && clean.contains("dhan", ignoreCase = true))
            } ?: OTHER
        }
    }
}

enum class FundTransactionType(val code: String, val displayName: String, val description: String) {
    CREDIT("CREDIT", "Amount Credited", "Added/Deposited to Trading Account"),
    DEBIT("DEBIT", "Amount Debited", "Withdrawn/Debited to Bank Account");

    companion object {
        fun fromString(type: String): FundTransactionType {
            return if (type.equals("DEBIT", ignoreCase = true) || type.contains("withdraw", ignoreCase = true)) {
                DEBIT
            } else {
                CREDIT
            }
        }
    }
}

data class PlatformFundSummary(
    val platformName: String,
    val tradingPlatform: TradingPlatform,
    val totalCredited: Double,
    val totalDebited: Double,
    val netCapital: Double,
    val transactionCount: Int
)

data class TotalFundsSummary(
    val totalCredited: Double,
    val totalDebited: Double,
    val netCapital: Double,
    val totalTransactions: Int,
    val platformSummaries: List<PlatformFundSummary>
)
