package com.example.model

data class DailyPnL(
    val dateLabel: String,      // e.g., "14 Sep"
    val timestamp: Long,
    val netPnL: Double,
    val tradeCount: Int,
    val winCount: Int
)

data class MonthlyPnL(
    val monthLabel: String,     // e.g., "Sep 2026"
    val netPnL: Double,
    val tradeCount: Int,
    val winRate: Double
)

data class YearlyPnL(
    val yearLabel: String,      // e.g., "2026"
    val netPnL: Double,
    val tradeCount: Int,
    val winRate: Double
)

data class SetupPerformance(
    val setupName: String,
    val totalTrades: Int,
    val winCount: Int,
    val winRate: Double,
    val netPnL: Double
)

data class InstrumentPerformance(
    val instrumentName: String,
    val totalTrades: Int,
    val winCount: Int,
    val winRate: Double,
    val netPnL: Double
)

data class EmotionPerformance(
    val emotionName: String,
    val emoji: String,
    val totalTrades: Int,
    val netPnL: Double,
    val winRate: Double
)

data class TradingSummary(
    val totalTrades: Int = 0,
    val winTrades: Int = 0,
    val lossTrades: Int = 0,
    val breakEvenTrades: Int = 0,
    val winRate: Double = 0.0,
    val grossPnL: Double = 0.0,
    val totalCharges: Double = 0.0,
    val netPnL: Double = 0.0,
    val profitFactor: Double = 0.0,
    val averageWin: Double = 0.0,
    val averageLoss: Double = 0.0,
    val avgRiskReward: Double = 0.0,
    val bestTradePnL: Double = 0.0,
    val worstTradePnL: Double = 0.0,
    val todayPnL: Double = 0.0,
    val thisWeekPnL: Double = 0.0,
    val thisMonthPnL: Double = 0.0,
    val lastMonthPnL: Double = 0.0,
    val thisYearPnL: Double = 0.0,
    val lastYearPnL: Double = 0.0
)

enum class DateFilter(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year"),
    LAST_YEAR("Last Year"),
    CUSTOM_MONTH_YEAR("Month / Year")
}
