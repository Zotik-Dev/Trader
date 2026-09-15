package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trades")
data class TradeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val instrument: String,         // NIFTY, BANK_NIFTY, SENSEX, CRUDE_OIL, NATURAL_GAS, OTHER
    val strikeOrSymbol: String,     // e.g. "25100 CE" or "54000 PE"
    val optionType: String,         // CE, PE, FUT, EQ
    val direction: String,          // BUY, SELL
    val entryPrice: Double,
    val slPrice: Double,
    val target1: Double,
    val target2: Double,
    val target3: Double,
    val exitPrice: Double,
    val quantity: Int,
    val grossPnL: Double,
    val charges: Double,
    val netPnL: Double,
    val points: Double,
    val riskAmount: Double,
    val plannedRR: Double,
    val actualRR: Double,
    val status: String,             // CLOSED, OPEN, TARGET_HIT, SL_HIT, TRAIL_SL_HIT
    val setup: String,
    val indicators: List<String>,
    val emotion: String,
    val mistake: String,
    val notes: String,
    val imageUris: List<String>,    // Local file paths/URIs for multiple attached chart photos
    val entryTimestamp: Long,
    val exitTimestamp: Long
)
