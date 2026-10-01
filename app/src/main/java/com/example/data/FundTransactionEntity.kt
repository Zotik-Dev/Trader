package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fund_transactions")
data class FundTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val platform: String,          // Zerodha, Groww, Sahi, Dhan, Angel One, Upstox, Other
    val type: String,              // CREDIT (Added/Deposited), DEBIT (Withdrawn/Debited)
    val amount: Double,
    val timestamp: Long,
    val paymentMode: String = "UPI", // UPI, Net Banking, Bank Payout, IMPS/NEFT, Other
    val referenceNumber: String = "", // UTR, Transaction ID, Bank Ref
    val notes: String = ""         // Reason or note
)
