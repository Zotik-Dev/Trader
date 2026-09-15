package com.example.model

enum class Instrument(val displayName: String, val defaultLotSize: Int) {
    NIFTY("NIFTY 50", 25),
    BANK_NIFTY("Bank Nifty", 15),
    SENSEX("Sensex", 10),
    CRUDE_OIL("Crude Oil", 100),
    NATURAL_GAS("Natural Gas", 1250),
    OTHER("Custom / Other", 1);

    companion object {
        fun fromString(value: String): Instrument {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) } ?: OTHER
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
