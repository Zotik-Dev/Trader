package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.TradeEntity
import com.example.model.Instrument
import com.example.model.OptionType
import com.example.model.TradeDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CsvImporter {

    data class ImportResult(
        val importedTrades: List<TradeEntity>,
        val totalRowsRead: Int,
        val failedRows: Int,
        val errorMessage: String? = null
    )

    private val supportedDateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
        SimpleDateFormat("d MMM yyyy", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    )

    private val supportedTimeFormats = listOf(
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("HH:mm", Locale.getDefault()),
        SimpleDateFormat("hh:mm:ss a", Locale.ENGLISH),
        SimpleDateFormat("hh:mm a", Locale.ENGLISH),
        SimpleDateFormat("h:mm a", Locale.ENGLISH)
    )

    suspend fun importTradesFromCsvUri(context: Context, uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val lines = mutableListOf<String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        if (!line.isNullOrBlank()) {
                            lines.add(line!!)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return@withContext ImportResult(
                importedTrades = emptyList(),
                totalRowsRead = 0,
                failedRows = 0,
                errorMessage = "Failed to read CSV file: ${e.localizedMessage}"
            )
        }

        if (lines.isEmpty()) {
            return@withContext ImportResult(
                importedTrades = emptyList(),
                totalRowsRead = 0,
                failedRows = 0,
                errorMessage = "CSV file is empty"
            )
        }

        parseCsvLines(lines)
    }

    fun parseCsvLines(lines: List<String>): ImportResult {
        if (lines.isEmpty()) {
            return ImportResult(emptyList(), 0, 0, "No content in CSV")
        }

        val headerTokens = parseCsvRow(lines.first()).map { it.trim().lowercase() }
        val headerMap = mutableMapOf<String, Int>()
        headerTokens.forEachIndexed { index, name ->
            headerMap[name] = index
        }

        val parsedTrades = mutableListOf<TradeEntity>()
        var failedRows = 0

        for (i in 1 until lines.size) {
            val rowLine = lines[i].trim()
            if (rowLine.isBlank()) continue
            val tokens = parseCsvRow(rowLine)
            if (tokens.isEmpty() || tokens.all { it.isBlank() }) continue

            try {
                fun getField(vararg aliases: String): String {
                    for (alias in aliases) {
                        val key = alias.lowercase()
                        val colIdx = headerMap[key]
                        if (colIdx != null && colIdx < tokens.size) {
                            return tokens[colIdx].trim()
                        }
                    }
                    return ""
                }

                fun getDoubleField(defaultVal: Double, vararg aliases: String): Double {
                    val raw = getField(*aliases).replace(",", "").replace("$", "").replace("₹", "").trim()
                    return raw.toDoubleOrNull() ?: defaultVal
                }

                fun getIntField(defaultVal: Int, vararg aliases: String): Int {
                    val raw = getField(*aliases).replace(",", "").trim()
                    return raw.toIntOrNull() ?: defaultVal
                }

                // 1. Date and Time parsing
                val dateStr = getField("date", "trade date", "entry date", "order date")
                val timeStr = getField("time", "trade time", "entry time", "order time")

                val entryTimestamp = parseDateTime(dateStr, timeStr)
                val exitDateStr = getField("exit date")
                val exitTimeStr = getField("exit time")
                val exitTimestamp = if (exitDateStr.isNotBlank() || exitTimeStr.isNotBlank()) {
                    parseDateTime(exitDateStr.ifBlank { dateStr }, exitTimeStr.ifBlank { timeStr })
                } else {
                    entryTimestamp
                }

                // 2. Instrument & Symbol/Strike
                val rawInstrument = getField("instrument", "index", "segment", "symbol", "underlying")
                val strikeOrSymbolField = getField("symbol / strike", "symbol/strike", "strike", "contract", "scrip")
                val finalInstrumentEnum = Instrument.fromString(rawInstrument.ifBlank { strikeOrSymbolField })
                val finalInstrument = finalInstrumentEnum.name

                // 3. Option Type (Call or Put etc.)
                val rawOptionType = getField("option type", "call/put", "call / put", "type", "instrument type")
                val finalOptionType = parseOptionType(rawOptionType, strikeOrSymbolField)

                // 4. Strike / Symbol name cleanup
                val finalStrikeOrSymbol = if (strikeOrSymbolField.isNotBlank()) {
                    strikeOrSymbolField
                } else if (rawInstrument.isNotBlank()) {
                    rawInstrument
                } else {
                    finalInstrumentEnum.displayName
                }

                // 5. Direction (BUY / SELL)
                val rawDirection = getField("direction", "action", "side", "trade type", "buy/sell", "order type")
                val finalDirection = parseDirection(rawDirection)

                // 6. Prices & Quantity
                val entryPrice = getDoubleField(0.0, "entry price", "buy price", "entry", "avg price", "price", "entry rate")
                val exitPrice = getDoubleField(0.0, "exit price", "sell price", "exit", "exit rate", "sell rate")
                val defaultLot = finalInstrumentEnum.defaultLotSize
                val quantity = getIntField(defaultLot, "quantity", "qty", "size", "lotsize", "total qty")

                // 7. SL and Targets
                val slPrice = getDoubleField(0.0, "stop loss", "sl price", "sl", "stoploss")
                val target1 = getDoubleField(0.0, "target 1", "target1", "target", "t1")
                val target2 = getDoubleField(0.0, "target 2", "target2", "t2")
                val target3 = getDoubleField(0.0, "target 3", "target3", "t3")

                // 8. Financial Calculations
                var points = getDoubleField(0.0, "points", "pts", "points captured")
                var grossPnL = getDoubleField(0.0, "gross pnl", "gross profit", "gross p&l", "pnl", "p&l")
                var charges = getDoubleField(0.0, "charges", "brokerage", "taxes", "total charges", "brokerage & charges")
                var netPnL = getDoubleField(0.0, "net pnl", "net profit", "net p&l", "realized pnl", "realized p&l")

                if (points == 0.0 && entryPrice > 0.0 && exitPrice > 0.0) {
                    points = if (finalDirection == "BUY") exitPrice - entryPrice else entryPrice - exitPrice
                }

                if (grossPnL == 0.0 && points != 0.0 && quantity > 0) {
                    grossPnL = points * quantity
                } else if (grossPnL == 0.0 && entryPrice > 0.0 && exitPrice > 0.0 && quantity > 0) {
                    val pts = if (finalDirection == "BUY") exitPrice - entryPrice else entryPrice - exitPrice
                    grossPnL = pts * quantity
                }

                if (charges == 0.0 && (entryPrice > 0.0 || exitPrice > 0.0)) {
                    charges = 40.0 // Standard minimal estimate if omitted
                }

                if (netPnL == 0.0 && grossPnL != 0.0) {
                    netPnL = grossPnL - charges
                }

                val riskAmount = getDoubleField(0.0, "risk amount", "risk", "planned risk")
                val plannedRR = getDoubleField(0.0, "planned rr", "planned r:r", "r:r", "risk reward")
                val actualRR = getDoubleField(0.0, "actual rr", "actual r:r")

                // 9. Meta / Journaling fields
                val statusRaw = getField("status", "trade status", "result")
                val finalStatus = parseStatus(statusRaw, netPnL, exitPrice)

                val setup = getField("setup", "strategy", "setup name").ifBlank { "Price Action" }
                val indicatorsRaw = getField("indicators", "indicator", "signals")
                val indicatorsList = if (indicatorsRaw.isNotBlank()) {
                    indicatorsRaw.split(";", "|", ",").map { it.trim() }.filter { it.isNotEmpty() }
                } else {
                    emptyList()
                }

                val emotion = getField("emotion", "psychology", "mental state").ifBlank { "Disciplined" }
                val mistake = getField("mistake", "error", "trading mistake").ifBlank { "None (Followed Plan)" }
                val notes = getField("notes", "comment", "remarks", "trade notes")

                val trade = TradeEntity(
                    id = 0,
                    instrument = finalInstrument,
                    strikeOrSymbol = finalStrikeOrSymbol,
                    optionType = finalOptionType,
                    direction = finalDirection,
                    entryPrice = entryPrice,
                    slPrice = slPrice,
                    target1 = target1,
                    target2 = target2,
                    target3 = target3,
                    exitPrice = exitPrice,
                    quantity = if (quantity > 0) quantity else 1,
                    grossPnL = grossPnL,
                    charges = charges,
                    netPnL = netPnL,
                    points = points,
                    riskAmount = riskAmount,
                    plannedRR = plannedRR,
                    actualRR = actualRR,
                    status = finalStatus,
                    setup = setup,
                    indicators = indicatorsList,
                    emotion = emotion,
                    mistake = mistake,
                    notes = notes,
                    imageUris = emptyList(),
                    entryTimestamp = entryTimestamp,
                    exitTimestamp = exitTimestamp
                )
                parsedTrades.add(trade)
            } catch (ex: Exception) {
                failedRows++
            }
        }

        return ImportResult(
            importedTrades = parsedTrades,
            totalRowsRead = lines.size - 1,
            failedRows = failedRows,
            errorMessage = if (parsedTrades.isEmpty()) "No valid trade records could be parsed from CSV" else null
        )
    }

    private fun parseOptionType(rawOption: String, strikeOrSymbol: String): String {
        val upper = (rawOption + " " + strikeOrSymbol).uppercase()
        return when {
            upper.contains("CALL") || upper.contains(" CE") || upper.endsWith("CE") -> "CE"
            upper.contains("PUT") || upper.contains(" PE") || upper.endsWith("PE") -> "PE"
            upper.contains("FUT") || upper.contains("FUTURE") -> "FUT"
            upper.contains("EQ") || upper.contains("EQUITY") || upper.contains("STOCK") -> "EQ"
            rawOption.equals("PE", ignoreCase = true) -> "PE"
            rawOption.equals("CE", ignoreCase = true) -> "CE"
            else -> "CE"
        }
    }

    private fun parseDirection(rawDirection: String): String {
        val upper = rawDirection.uppercase()
        return if (upper.contains("SELL") || upper.contains("SHORT") || upper == "S") {
            "SELL"
        } else {
            "BUY"
        }
    }

    private fun parseStatus(rawStatus: String, netPnL: Double, exitPrice: Double): String {
        val upper = rawStatus.uppercase()
        return when {
            upper.contains("TARGET") -> "TARGET_HIT"
            upper.contains("TRAIL") -> "TRAIL_SL_HIT"
            upper.contains("SL") || upper.contains("STOP") -> "SL_HIT"
            upper.contains("OPEN") -> "OPEN"
            upper.contains("CLOSED") || upper.contains("EXIT") -> "CLOSED"
            exitPrice > 0.0 -> "CLOSED"
            netPnL != 0.0 -> "CLOSED"
            else -> "CLOSED"
        }
    }

    private fun parseDateTime(dateStr: String, timeStr: String): Long {
        if (dateStr.isBlank() && timeStr.isBlank()) {
            return System.currentTimeMillis()
        }

        // Try combined date + time
        if (dateStr.isNotBlank() && timeStr.isNotBlank()) {
            val combined = "$dateStr $timeStr".trim()
            val combinedFormats = listOf(
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
                SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()),
                SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()),
                SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.ENGLISH),
                SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.ENGLISH),
                SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.ENGLISH),
                SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
            )

            for (fmt in combinedFormats) {
                try {
                    val date = fmt.parse(combined)
                    if (date != null) return date.time
                } catch (_: Exception) {}
            }
        }

        // Parse date component
        var calendar = Calendar.getInstance()
        var dateParsed = false

        if (dateStr.isNotBlank()) {
            for (fmt in supportedDateFormats) {
                try {
                    val d = fmt.parse(dateStr)
                    if (d != null) {
                        calendar.time = d
                        dateParsed = true
                        break
                    }
                } catch (_: Exception) {}
            }
        }

        // Parse time component
        if (timeStr.isNotBlank()) {
            for (tFmt in supportedTimeFormats) {
                try {
                    val t = tFmt.parse(timeStr)
                    if (t != null) {
                        val timeCal = Calendar.getInstance().apply { time = t }
                        calendar.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                        calendar.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                        calendar.set(Calendar.SECOND, timeCal.get(Calendar.SECOND))
                        break
                    }
                } catch (_: Exception) {}
            }
        }

        return if (dateParsed || timeStr.isNotBlank()) calendar.timeInMillis else System.currentTimeMillis()
    }

    /**
     * Splits a CSV row handling quoted values and escaped quotes.
     */
    fun parseCsvRow(row: String): List<String> {
        val result = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < row.length) {
            val c = row[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < row.length && row[i + 1] == '\"') {
                    // Escaped quote
                    cur.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(cur.toString().trim())
                cur.setLength(0)
            } else {
                cur.append(c)
            }
            i++
        }
        result.add(cur.toString().trim())
        return result
    }
}
