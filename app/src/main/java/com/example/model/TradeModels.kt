package com.example.model

enum class Instrument(val displayName: String, val defaultLotSize: Int) {
    NIFTY("NIFTY", 65),
    SENSEX("Sensex", 20),
    BANK_NIFTY("Bank Nifty", 30),
    MIDCPNIFTY("MIDCPNIFTY", 120),
    FINNIFTY("FINNIFTY", 60),
    CRUDE_OIL_MINI("Crude Oil Mini", 10),
    NATURAL_GAS_MINI("Natural Gas Mini", 250),
    CRUDE_OIL("Crude Oil", 100),
    NATURAL_GAS("Natural Gas", 1250),
    OTHER("Custom / Other", 1);

    companion object {
        fun fromString(value: String): Instrument {
            val trimmed = value.trim()
            entries.find { it.name.equals(trimmed, ignoreCase = true) || it.displayName.equals(trimmed, ignoreCase = true) }?.let { return it }

            if (trimmed.contains("crude", ignoreCase = true) && trimmed.contains("mini", ignoreCase = true)) return CRUDE_OIL_MINI
            if (trimmed.contains("natural", ignoreCase = true) && trimmed.contains("mini", ignoreCase = true)) return NATURAL_GAS_MINI
            if (trimmed.contains("bank", ignoreCase = true)) return BANK_NIFTY
            if (trimmed.contains("fin", ignoreCase = true)) return FINNIFTY
            if (trimmed.contains("mid", ignoreCase = true)) return MIDCPNIFTY
            if (trimmed.contains("sensex", ignoreCase = true)) return SENSEX
            if (trimmed.contains("crude", ignoreCase = true)) return CRUDE_OIL
            if (trimmed.contains("natural", ignoreCase = true) || trimmed.contains("ng", ignoreCase = true)) return NATURAL_GAS
            if (trimmed.contains("nifty", ignoreCase = true)) return NIFTY

            return OTHER
        }
    }
}

enum class OptionType(val displayName: String) {
    CE("Call (CE)"),
    PE("Put (PE)"),
    FUT("Futures"),
    EQ("Equity")
}

enum class TradeDirection(val displayName: String) {
    BUY("BUY / Long"),
    SELL("SELL / Short")
}

enum class TradeStatus(val displayName: String) {
    CLOSED("Closed / Exited"),
    OPEN("Open Position"),
    TARGET_HIT("Target Hit"),
    SL_HIT("Stop Loss Hit"),
    TRAIL_SL_HIT("Trailed SL Hit")
}

enum class Emotion(val displayName: String, val emoji: String) {
    DISCIPLINED("Disciplined", "🧘"),
    CONFIDENT("Confident", "🎯"),
    PATIENT("Patient", "⏳"),
    ANXIOUS("Anxious", "😰"),
    FOMO("FOMO", "⚡"),
    GREEDY("Greedy", "🤑"),
    REVENGE("Revenge Trading", "🔥"),
    HESITANT("Hesitant", "🤔")
}

enum class Mistake(val displayName: String) {
    NONE("None (Followed Plan)"),
    CHASED_ENTRY("Chased Entry"),
    EARLY_EXIT("Early Exit / Panic Exit"),
    MOVED_SL("Moved SL / Widened Risk"),
    NO_SL("No Stop Loss Used"),
    OVERSIZED("Over-leveraged / Oversized"),
    AGAINST_TREND("Traded Against Trend"),
    OVERTRADING("Overtrading"),
    LATE_ENTRY("Late Entry")
}

val POPULAR_INDICATORS = listOf(
    "CPR",
    "EMA (9/21)",
    "EMA (50/200)",
    "VWAP",
    "RSI",
    "MACD",
    "Volume Profile",
    "Support / Resistance",
    "Order Block",
    "Fibonacci"
)

val POPULAR_SETUPS = listOf(
    "CPR Breakout",
    "CPR Virgin Test",
    "VWAP Bounce",
    "EMA Pullback",
    "Opening Range Breakout (ORB)",
    "Breakout & Retest",
    "Double Bottom / Top",
    "Trendline Break",
    "Mean Reversion",
    "Liquidity Sweep",
    "Supply & Demand"
)
