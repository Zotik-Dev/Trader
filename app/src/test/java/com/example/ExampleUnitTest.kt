package com.example

import com.example.model.Instrument
import com.example.model.TradeDirection
import com.example.util.TradeCalculations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testBuyPointsAndGrossPnL() {
        val points = TradeCalculations.calculatePoints(TradeDirection.BUY, 24500.0, 24650.0)
        assertEquals(150.0, points, 0.001)

        val grossPnL = TradeCalculations.calculateGrossPnL(TradeDirection.BUY, 24500.0, 24650.0, 25)
        assertEquals(3750.0, grossPnL, 0.001)
    }

    @Test
    fun testSellPointsAndGrossPnL() {
        val points = TradeCalculations.calculatePoints(TradeDirection.SELL, 52000.0, 51700.0)
        assertEquals(300.0, points, 0.001)

        val grossPnL = TradeCalculations.calculateGrossPnL(TradeDirection.SELL, 52000.0, 51700.0, 15)
        assertEquals(4500.0, grossPnL, 0.001)
    }

    @Test
    fun testRiskAndRewardCalculation() {
        // Entry: 24500, SL: 24450 (Risk = 50 pts), Target: 24650 (Target = 150 pts)
        val riskPoints = TradeCalculations.calculateRiskPoints(TradeDirection.BUY, 24500.0, 24450.0)
        assertEquals(50.0, riskPoints, 0.001)

        val plannedRR = TradeCalculations.calculatePlannedRR(TradeDirection.BUY, 24500.0, 24450.0, 24650.0)
        assertEquals(3.0, plannedRR, 0.001)

        val actualRR = TradeCalculations.calculateActualRR(TradeDirection.BUY, 24500.0, 24450.0, 24600.0)
        assertEquals(2.0, actualRR, 0.001)
    }

    @Test
    fun testChargesCalculation() {
        val charges = TradeCalculations.estimateCharges(24500.0, 24650.0, 25)
        assertTrue("Charges should be at least standard ₹40", charges >= 40.0)
    }

    @Test
    fun testRewardPointsAndBreakeven() {
        val reward = TradeCalculations.calculateRewardPoints(TradeDirection.BUY, 24500.0, 24700.0)
        assertEquals(200.0, reward, 0.001)

        val breakeven = TradeCalculations.calculateBreakevenPrice(TradeDirection.BUY, 24500.0, 50.0, 25)
        assertEquals(24502.0, breakeven, 0.001)
    }

    @Test
    fun testInstrumentLotSizes() {
        assertEquals(65, Instrument.NIFTY.defaultLotSize)
        assertEquals(20, Instrument.SENSEX.defaultLotSize)
        assertEquals(30, Instrument.BANK_NIFTY.defaultLotSize)
        assertEquals(120, Instrument.MIDCPNIFTY.defaultLotSize)
        assertEquals(60, Instrument.FINNIFTY.defaultLotSize)
        assertEquals(10, Instrument.CRUDE_OIL_MINI.defaultLotSize)
        assertEquals(250, Instrument.NATURAL_GAS_MINI.defaultLotSize)

        // Verify fromString lookups
        assertEquals(Instrument.NIFTY, Instrument.fromString("Nifty"))
        assertEquals(Instrument.SENSEX, Instrument.fromString("Sensex"))
        assertEquals(Instrument.BANK_NIFTY, Instrument.fromString("Banknifty"))
        assertEquals(Instrument.MIDCPNIFTY, Instrument.fromString("Midcpnifty"))
        assertEquals(Instrument.FINNIFTY, Instrument.fromString("Finnifty"))
        assertEquals(Instrument.CRUDE_OIL_MINI, Instrument.fromString("Crudeoil mini"))
        assertEquals(Instrument.NATURAL_GAS_MINI, Instrument.fromString("Naturalgas mini"))
    }
}
