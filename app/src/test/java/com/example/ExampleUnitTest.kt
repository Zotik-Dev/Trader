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

    @Test
    fun testCsvImporterParsing() {
        val csvData = """
            Trade ID,Date,Time,Instrument,Symbol / Strike,Option Type,Direction,Entry Price,Exit Price,Quantity,Gross PnL,Charges,Net PnL,Points,Stop Loss,Target 1,Target 2,Target 3,Risk Amount,Planned RR,Actual RR,Status,Setup,Indicators,Emotion,Mistake,Notes,Images Count
            1,2026-09-20,09:30:00,NIFTY,25100 CE,CE,BUY,120.0,150.0,65,1950.0,40.0,1910.0,30.0,100.0,150.0,180.0,200.0,1300.0,1.5,1.5,CLOSED,CPR Breakout,VWAP; RSI,Disciplined,None (Followed Plan),Clean trade,0
            2,2026-09-21,14:15:30,BANK_NIFTY,52500 PE,PE,BUY,200.0,170.0,30,-900.0,40.0,-940.0,-30.0,170.0,260.0,300.0,0.0,900.0,2.0,-1.0,SL_HIT,EMA Pullback,EMA (9/21),Anxious,Chased Entry,Took trade late,0
        """.trimIndent()

        val lines = csvData.lines()
        val result = com.example.util.CsvImporter.parseCsvLines(lines)

        assertEquals(2, result.importedTrades.size)
        assertEquals(0, result.failedRows)

        val trade1 = result.importedTrades[0]
        assertEquals("NIFTY", trade1.instrument)
        assertEquals("25100 CE", trade1.strikeOrSymbol)
        assertEquals("CE", trade1.optionType)
        assertEquals("BUY", trade1.direction)
        assertEquals(120.0, trade1.entryPrice, 0.001)
        assertEquals(150.0, trade1.exitPrice, 0.001)
        assertEquals(65, trade1.quantity)
        assertEquals(1910.0, trade1.netPnL, 0.001)
        assertTrue(trade1.indicators.contains("VWAP"))

        val trade2 = result.importedTrades[1]
        assertEquals("BANK_NIFTY", trade2.instrument)
        assertEquals("52500 PE", trade2.strikeOrSymbol)
        assertEquals("PE", trade2.optionType)
        assertEquals(-940.0, trade2.netPnL, 0.001)
        assertEquals("SL_HIT", trade2.status)
        assertEquals("Chased Entry", trade2.mistake)
    }

    @Test
    fun testCsvImporterGenericFormats() {
        val genericCsv = """
            Date,Time,Symbol,Type,Action,Price,Exit,Qty
            22/09/2026,10:00 AM,NIFTY 25000 CE,Call,Buy,100,120,65
            22/09/2026,11:30 AM,BANKNIFTY 52000 PE,Put,Buy,250,220,30
        """.trimIndent()

        val result = com.example.util.CsvImporter.parseCsvLines(genericCsv.lines())
        assertEquals(2, result.importedTrades.size)

        val t1 = result.importedTrades[0]
        assertEquals("CE", t1.optionType)
        assertEquals("BUY", t1.direction)
        assertEquals(100.0, t1.entryPrice, 0.001)
        assertEquals(120.0, t1.exitPrice, 0.001)
        assertEquals(65, t1.quantity)
        assertEquals(20.0, t1.points, 0.001)
        assertEquals(1300.0, t1.grossPnL, 0.001)

        val t2 = result.importedTrades[1]
        assertEquals("PE", t2.optionType)
        assertEquals("BUY", t2.direction)
        assertEquals(250.0, t2.entryPrice, 0.001)
        assertEquals(220.0, t2.exitPrice, 0.001)
    }
}
