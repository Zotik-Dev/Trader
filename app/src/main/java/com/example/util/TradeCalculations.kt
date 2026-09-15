package com.example.util

import com.example.model.TradeDirection
import java.text.DecimalFormat
import kotlin.math.abs

object TradeCalculations {
    private val currencyFormatter = DecimalFormat("#,##,##0.00")
    private val pointsFormatter = DecimalFormat("+#,##0.00;-#,##0.00")
    private val ratioFormatter = DecimalFormat("0.00")

    fun calculateGrossPnL(
        direction: TradeDirection,
        entryPrice: Double,
        exitPrice: Double,
        quantity: Int
    ): Double {
        if (entryPrice <= 0 || exitPrice <= 0 || quantity <= 0) return 0.0
        return when (direction) {
            TradeDirection.BUY -> (exitPrice - entryPrice) * quantity
            TradeDirection.SELL -> (entryPrice - exitPrice) * quantity
        }
    }

    fun calculatePoints(
        direction: TradeDirection,
        entryPrice: Double,
        exitPrice: Double
    ): Double {
        if (entryPrice <= 0 || exitPrice <= 0) return 0.0
        return when (direction) {
            TradeDirection.BUY -> exitPrice - entryPrice
            TradeDirection.SELL -> entryPrice - exitPrice
        }
    }

    /**
     * Estimated standard brokerage + taxes (STT, GST, Exchange charges, Stamp Duty)
     * For Indian F&O: ₹20/order flat buy + ₹20 sell = ₹40, plus approx 0.05% turnover fee
     */
    fun estimateCharges(
        entryPrice: Double,
        exitPrice: Double,
        quantity: Int
    ): Double {
        if (entryPrice <= 0 || quantity <= 0) return 0.0
        val effectiveExit = if (exitPrice > 0) exitPrice else entryPrice
        val totalTurnover = (entryPrice + effectiveExit) * quantity
        val baseBrokerage = 40.0 // Flat buy + sell
        val statutoryLevies = totalTurnover * 0.0006
        return (baseBrokerage + statutoryLevies).coerceAtLeast(40.0)
    }

    fun calculateNetPnL(
        grossPnL: Double,
        charges: Double
    ): Double {
        return grossPnL - charges
    }

    fun calculateRiskPoints(
        direction: TradeDirection,
        entryPrice: Double,
        slPrice: Double
    ): Double {
        if (entryPrice <= 0 || slPrice <= 0) return 0.0
        return when (direction) {
            TradeDirection.BUY -> (entryPrice - slPrice).coerceAtLeast(0.0)
            TradeDirection.SELL -> (slPrice - entryPrice).coerceAtLeast(0.0)
        }
    }

    fun calculateRiskAmount(
        direction: TradeDirection,
        entryPrice: Double,
        slPrice: Double,
        quantity: Int
    ): Double {
        return calculateRiskPoints(direction, entryPrice, slPrice) * quantity
    }

    fun calculatePlannedRR(
        direction: TradeDirection,
        entryPrice: Double,
        slPrice: Double,
        targetPrice: Double
    ): Double {
        val risk = calculateRiskPoints(direction, entryPrice, slPrice)
        if (risk <= 0 || targetPrice <= 0) return 0.0
        val reward = when (direction) {
            TradeDirection.BUY -> (targetPrice - entryPrice).coerceAtLeast(0.0)
            TradeDirection.SELL -> (entryPrice - targetPrice).coerceAtLeast(0.0)
        }
        return if (risk > 0) reward / risk else 0.0
    }

    fun calculateActualRR(
        direction: TradeDirection,
        entryPrice: Double,
        slPrice: Double,
        exitPrice: Double
    ): Double {
        val risk = calculateRiskPoints(direction, entryPrice, slPrice)
        if (risk <= 0 || exitPrice <= 0) return 0.0
        val points = calculatePoints(direction, entryPrice, exitPrice)
        return points / risk
    }

    fun formatCurrency(amount: Double, prefix: String = "₹"): String {
        val sign = if (amount < 0) "-" else if (amount > 0) "+" else ""
        return "$sign$prefix${currencyFormatter.format(abs(amount))}"
    }

    fun formatPoints(points: Double): String {
        return pointsFormatter.format(points) + " pts"
    }

    fun formatRR(rr: Double): String {
        if (rr <= 0) return "1:0.0"
        return "1:${ratioFormatter.format(rr)}"
    }
}
