package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.TradeEntity
import com.example.model.Instrument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

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
                        lines.add(line ?: "")
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

    data class ParsedRawRow(
        val symbol: String,
        val timestamp: Long,
        val orderId: String,
        val tradeId: String,
        val type: String, // BUY or SELL
        val qty: Int,
        val entryPrice: Double,
        val exitPrice: Double,
        val expiry: String,
        val originalInstrument: String,
        val cleanStrikeOrSymbol: String,
        val optionType: String,
        val slPrice: Double = 0.0,
        val target1: Double = 0.0,
        val target2: Double = 0.0,
        val target3: Double = 0.0,
        val points: Double = 0.0,
        val grossPnL: Double = 0.0,
        val charges: Double = 0.0,
        val netPnL: Double = 0.0,
        val status: String = "",
        val setup: String = "Price Action",
        val indicators: List<String> = emptyList(),
        val emotion: String = "Disciplined",
        val mistake: String = "None (Followed Plan)",
        val notes: String = "",
        val isExplicitJournalExport: Boolean = false,
        val exitTimestamp: Long = 0L
    )

    fun parseCsvLines(lines: List<String>): ImportResult {
        val validLines = lines.filter { it.isNotBlank() }
        if (validLines.isEmpty()) {
            return ImportResult(emptyList(), 0, 0, "No content in CSV")
        }

        // Header detection
        var headerIndex = 0
        val headerMap = mutableMapOf<String, Int>()

        for (i in 0 until minOf(5, validLines.size)) {
            val tokens = parseCsvRow(validLines[i]).map { cleanHeaderName(it) }
            val hasSymbol = tokens.any { it.contains("symbol") || it.contains("instrument") || it.contains("scrip") }
            val hasPrice = tokens.any { it == "price" || it.contains("rate") || it.contains("pnl") || it.contains("entry") }
            val hasQtyOrType = tokens.any { it.contains("qty") || it == "type" || it == "side" || it.contains("quantity") || it == "action" }

            if (hasSymbol && (hasPrice || hasQtyOrType)) {
                headerIndex = i
                tokens.forEachIndexed { colIdx, name ->
                    if (name.isNotEmpty()) {
                        headerMap[name] = colIdx
                    }
                }
                break
            }
        }

        if (headerMap.isEmpty()) {
            val tokens = parseCsvRow(validLines[0]).map { cleanHeaderName(it) }
            tokens.forEachIndexed { colIdx, name ->
                if (name.isNotEmpty()) {
                    headerMap[name] = colIdx
                }
            }
        }

        val rawRows = mutableListOf<ParsedRawRow>()
        var failedRows = 0

        // Strict & smart column getter
        fun findColIndex(vararg candidates: String): Int? {
            // 1. Exact match
            for (candidate in candidates) {
                val clean = cleanHeaderName(candidate)
                headerMap[clean]?.let { return it }
            }
            // 2. Contains match (headerKey contains candidate only if candidate length >= 3)
            for (candidate in candidates) {
                val clean = cleanHeaderName(candidate)
                if (clean.length < 3) continue
                for ((key, idx) in headerMap) {
                    if (key == clean || key.startsWith("$clean ") || key.endsWith(" $clean") || key.contains(" $clean ")) {
                        return idx
                    }
                }
            }
            return null
        }

        for (i in (headerIndex + 1) until validLines.size) {
            val rowLine = validLines[i].trim()
            if (rowLine.isBlank()) continue
            val tokens = parseCsvRow(rowLine)
            if (tokens.isEmpty() || tokens.all { it.isBlank() }) continue

            try {
                fun getField(vararg aliases: String): String {
                    val colIdx = findColIndex(*aliases) ?: return ""
                    return if (colIdx < tokens.size) tokens[colIdx].trim() else ""
                }

                fun getDoubleField(defaultVal: Double, vararg aliases: String): Double {
                    val raw = getField(*aliases)
                        .replace(",", "")
                        .replace("$", "")
                        .replace("₹", "")
                        .replace(" ", "")
                        .trim()
                    return raw.toDoubleOrNull() ?: defaultVal
                }

                fun getIntField(defaultVal: Int, vararg aliases: String): Int {
                    val raw = getField(*aliases)
                        .replace(",", "")
                        .replace(" ", "")
                        .trim()
                    return raw.toIntOrNull() ?: defaultVal
                }

                // 1. Separate Instrument and Symbol extraction
                val rawInstrumentCol = getField("instrument", "index", "segment", "underlying")
                val rawSymbolCol = getField("symbol / strike", "symbol/strike", "symbol", "contract", "scrip")

                val symbolToBreakdown = if (rawSymbolCol.isNotBlank()) rawSymbolCol else rawInstrumentCol
                val cleanSymbol = symbolToBreakdown.replace(Regex("(?i)\\s+(bse|nse|mcx)$"), "").trim()

                val (detectedInstrument, cleanStrike, detectedOptionType) = breakdownSymbol(cleanSymbol)
                val finalInstrument = if (rawInstrumentCol.isNotBlank()) {
                    Instrument.fromString(rawInstrumentCol).name
                } else {
                    detectedInstrument.name
                }

                val optionTypeField = getField("option type", "call/put", "call / put")
                val finalOptionType = if (optionTypeField.isNotBlank()) {
                    parseOptionTypeString(optionTypeField)
                } else {
                    detectedOptionType
                }

                // 2. Date and Time parsing
                val rawTradeTime = getField("trade time", "tradetime", "order time", "execution time", "time", "trade date")
                val explicitDate = getField("date", "trade date", "entry date")
                val explicitTime = getField("time", "entry time")
                val entryTimestamp = parseDateTimeFlexible(rawTradeTime, explicitDate, explicitTime)

                val orderId = getField("order id", "orderid")
                val tradeId = getField("trade id", "tradeid")
                val rawType = getField("type", "order type", "side", "action", "direction", "buy/sell", "trade type")
                val direction = parseDirection(rawType)

                // 3. Prices
                // Entry price: matches "entry price", "buy price", "entry", or standard "price" if it's an execution row
                val entryPrice = getDoubleField(0.0, "entry price", "buy price", "entry", "price", "avg price", "trade price", "rate")
                val exitPrice = getDoubleField(0.0, "exit price", "sell price", "exit", "exit rate", "sell rate")

                val defaultLot = Instrument.fromString(finalInstrument).defaultLotSize
                val qty = getIntField(defaultLot, "qty.", "qty", "quantity", "size", "lotsize", "total qty")
                val expiry = getField("expiry", "expiry date")

                // 4. Financial Calculations / Journal fields
                val hasExplicitJournalFields = headerMap.keys.any {
                    it == "gross pnl" || it == "net pnl" || it == "points" || it == "stop loss" || it == "exit price"
                }

                val slPrice = getDoubleField(0.0, "stop loss", "sl price", "sl", "stoploss")
                val target1 = getDoubleField(0.0, "target 1", "target1", "target", "t1")
                val target2 = getDoubleField(0.0, "target 2", "target2", "t2")
                val target3 = getDoubleField(0.0, "target 3", "target3", "t3")

                val points = getDoubleField(0.0, "points", "pts", "points captured")
                val grossPnL = getDoubleField(0.0, "gross pnl", "gross profit", "gross p&l")
                val charges = getDoubleField(0.0, "charges", "brokerage", "taxes", "total charges")
                val netPnL = getDoubleField(0.0, "net pnl", "net profit", "net p&l", "realized pnl", "realized p&l")
                val status = getField("status", "trade status", "result")
                val setup = getField("setup", "strategy", "setup name").ifBlank { "Price Action" }
                val indicatorsRaw = getField("indicators", "indicator", "signals")
                val indicatorsList = if (indicatorsRaw.isNotBlank()) {
                    indicatorsRaw.split(";", "|", ",").map { it.trim() }.filter { it.isNotEmpty() }
                } else emptyList()

                val emotion = getField("emotion", "psychology", "mental state").ifBlank { "Disciplined" }
                val mistake = getField("mistake", "error", "trading mistake").ifBlank { "None (Followed Plan)" }
                val notes = getField("notes", "comment", "remarks", "trade notes")

                val exitDateStr = getField("exit date")
                val exitTimeStr = getField("exit time")
                val exitTimestamp = if (exitDateStr.isNotBlank() || exitTimeStr.isNotBlank()) {
                    parseDateTimeFlexible(exitDateStr, exitDateStr, exitTimeStr)
                } else {
                    entryTimestamp
                }

                rawRows.add(
                    ParsedRawRow(
                        symbol = cleanSymbol,
                        timestamp = entryTimestamp,
                        orderId = orderId,
                        tradeId = tradeId,
                        type = direction,
                        qty = if (qty > 0) qty else 1,
                        entryPrice = entryPrice,
                        exitPrice = exitPrice,
                        expiry = expiry,
                        originalInstrument = finalInstrument,
                        cleanStrikeOrSymbol = cleanStrike,
                        optionType = finalOptionType,
                        slPrice = slPrice,
                        target1 = target1,
                        target2 = target2,
                        target3 = target3,
                        points = points,
                        grossPnL = grossPnL,
                        charges = charges,
                        netPnL = netPnL,
                        status = status,
                        setup = setup,
                        indicators = indicatorsList,
                        emotion = emotion,
                        mistake = mistake,
                        notes = notes,
                        isExplicitJournalExport = hasExplicitJournalFields,
                        exitTimestamp = exitTimestamp
                    )
                )
            } catch (ex: Exception) {
                failedRows++
            }
        }

        if (rawRows.isEmpty()) {
            return ImportResult(emptyList(), validLines.size - 1, failedRows, "No valid trade records could be parsed from CSV")
        }

        val trades = processRawRowsIntoTrades(rawRows)

        return ImportResult(
            importedTrades = trades,
            totalRowsRead = validLines.size - 1,
            failedRows = failedRows,
            errorMessage = if (trades.isEmpty()) "No trades could be generated from CSV records" else null
        )
    }

    fun processRawRowsIntoTrades(rawRows: List<ParsedRawRow>): List<TradeEntity> {
        val resultTrades = mutableListOf<TradeEntity>()

        // Check if rows are already complete individual trades (either explicit journal export or has distinct non-zero exit price)
        val hasCompletedRows = rawRows.any { it.isExplicitJournalExport || (it.entryPrice > 0 && it.exitPrice > 0) }

        if (hasCompletedRows) {
            for (r in rawRows) {
                val entryP = r.entryPrice
                val exitP = if (r.exitPrice > 0) r.exitPrice else r.entryPrice
                var pts = r.points
                if (pts == 0.0 && entryP > 0.0 && exitP > 0.0) {
                    pts = if (r.type == "BUY") exitP - entryP else entryP - exitP
                }
                var gross = r.grossPnL
                if (gross == 0.0 && pts != 0.0 && r.qty > 0) {
                    gross = pts * r.qty
                }
                var charges = r.charges
                if (charges == 0.0 && (entryP > 0 || exitP > 0)) {
                    charges = 40.0
                }
                var net = r.netPnL
                if (net == 0.0 && gross != 0.0) {
                    net = gross - charges
                }

                val status = if (r.status.isNotBlank()) {
                    parseStatus(r.status, net, exitP)
                } else if (exitP > 0) {
                    "CLOSED"
                } else {
                    "OPEN"
                }

                resultTrades.add(
                    TradeEntity(
                        id = 0,
                        instrument = r.originalInstrument,
                        strikeOrSymbol = r.cleanStrikeOrSymbol,
                        optionType = r.optionType,
                        direction = r.type,
                        entryPrice = entryP,
                        slPrice = r.slPrice,
                        target1 = r.target1,
                        target2 = r.target2,
                        target3 = r.target3,
                        exitPrice = exitP,
                        quantity = r.qty,
                        grossPnL = gross,
                        charges = charges,
                        netPnL = net,
                        points = pts,
                        riskAmount = 0.0,
                        plannedRR = 0.0,
                        actualRR = 0.0,
                        status = status,
                        setup = r.setup,
                        indicators = r.indicators,
                        emotion = r.emotion,
                        mistake = r.mistake,
                        notes = r.notes,
                        imageUris = emptyList(),
                        entryTimestamp = r.timestamp,
                        exitTimestamp = if (r.exitTimestamp > 0) r.exitTimestamp else r.timestamp
                    )
                )
            }
            return resultTrades
        }

        // Broker tradebook order matching (e.g. Zerodha, AngelOne, Groww)
        val sortedRows = rawRows.sortedBy { it.timestamp }
        val openPositions = mutableMapOf<String, MutableList<MutableTradeLeg>>()

        for (row in sortedRows) {
            val key = "${row.cleanStrikeOrSymbol}_${row.optionType}".uppercase()
            val list = openPositions.getOrPut(key) { mutableListOf() }

            val oppositeIdx = list.indexOfFirst { it.direction != row.type && it.remainingQty > 0 }
            if (oppositeIdx != -1) {
                val openLeg = list[oppositeIdx]
                val matchedQty = minOf(openLeg.remainingQty, row.qty)

                val isBuyEntry = openLeg.direction == "BUY"
                val entryPrice = if (isBuyEntry) openLeg.price else row.entryPrice
                val exitPrice = if (isBuyEntry) row.entryPrice else openLeg.price
                val entryTime = minOf(openLeg.timestamp, row.timestamp)
                val exitTime = maxOf(openLeg.timestamp, row.timestamp)
                val direction = openLeg.direction

                val points = if (direction == "BUY") exitPrice - entryPrice else entryPrice - exitPrice
                val grossPnL = points * matchedQty
                val charges = 40.0
                val netPnL = grossPnL - charges

                val tradeNotes = buildString {
                    if (openLeg.orderId.isNotBlank()) append("Entry Order: ${openLeg.orderId} ")
                    if (row.orderId.isNotBlank()) append("Exit Order: ${row.orderId}")
                }.trim()

                resultTrades.add(
                    TradeEntity(
                        id = 0,
                        instrument = row.originalInstrument,
                        strikeOrSymbol = row.cleanStrikeOrSymbol,
                        optionType = row.optionType,
                        direction = direction,
                        entryPrice = entryPrice,
                        slPrice = 0.0,
                        target1 = 0.0,
                        target2 = 0.0,
                        target3 = 0.0,
                        exitPrice = exitPrice,
                        quantity = matchedQty,
                        grossPnL = grossPnL,
                        charges = charges,
                        netPnL = netPnL,
                        points = points,
                        riskAmount = 0.0,
                        plannedRR = 0.0,
                        actualRR = 0.0,
                        status = "CLOSED",
                        setup = "Orderbook Import",
                        indicators = emptyList(),
                        emotion = "Disciplined",
                        mistake = "None (Followed Plan)",
                        notes = tradeNotes,
                        imageUris = emptyList(),
                        entryTimestamp = entryTime,
                        exitTimestamp = exitTime
                    )
                )

                openLeg.remainingQty -= matchedQty
                if (openLeg.remainingQty <= 0) {
                    list.removeAt(oppositeIdx)
                }

                val leftoverQty = row.qty - matchedQty
                if (leftoverQty > 0) {
                    list.add(
                        MutableTradeLeg(
                            symbol = row.cleanStrikeOrSymbol,
                            instrument = row.originalInstrument,
                            optionType = row.optionType,
                            direction = row.type,
                            price = row.entryPrice,
                            remainingQty = leftoverQty,
                            timestamp = row.timestamp,
                            orderId = row.orderId
                        )
                    )
                }
            } else {
                list.add(
                    MutableTradeLeg(
                        symbol = row.cleanStrikeOrSymbol,
                        instrument = row.originalInstrument,
                        optionType = row.optionType,
                        direction = row.type,
                        price = row.entryPrice,
                        remainingQty = row.qty,
                        timestamp = row.timestamp,
                        orderId = row.orderId
                    )
                )
            }
        }

        // Remaining open positions
        for ((_, legs) in openPositions) {
            for (leg in legs) {
                if (leg.remainingQty > 0) {
                    resultTrades.add(
                        TradeEntity(
                            id = 0,
                            instrument = leg.instrument,
                            strikeOrSymbol = leg.symbol,
                            optionType = leg.optionType,
                            direction = leg.direction,
                            entryPrice = leg.price,
                            slPrice = 0.0,
                            target1 = 0.0,
                            target2 = 0.0,
                            target3 = 0.0,
                            exitPrice = 0.0,
                            quantity = leg.remainingQty,
                            grossPnL = 0.0,
                            charges = 20.0,
                            netPnL = -20.0,
                            points = 0.0,
                            riskAmount = 0.0,
                            plannedRR = 0.0,
                            actualRR = 0.0,
                            status = "OPEN",
                            setup = "Orderbook Import",
                            indicators = emptyList(),
                            emotion = "Disciplined",
                            mistake = "None (Followed Plan)",
                            notes = if (leg.orderId.isNotBlank()) "Order ID: ${leg.orderId}" else "",
                            imageUris = emptyList(),
                            entryTimestamp = leg.timestamp,
                            exitTimestamp = leg.timestamp
                        )
                    )
                }
            }
        }

        return resultTrades
    }

    data class MutableTradeLeg(
        val symbol: String,
        val instrument: String,
        val optionType: String,
        val direction: String,
        val price: Double,
        var remainingQty: Int,
        val timestamp: Long,
        val orderId: String
    )

    private fun cleanHeaderName(raw: String): String {
        return raw.lowercase()
            .replace(".", "")
            .replace("_", " ")
            .replace("-", " ")
            .trim()
    }

    /**
     * Resolves exchange symbol names into Instrument, display strike, and Option Type.
     * Handles:
     * - SENSEX2640272600PE -> Sensex 72600 PE
     * - SENSEX 72600 PE -> SENSEX 72600 PE
     * - 25100 CE -> 25100 CE
     * - NIFTY2640225100CE -> NIFTY 25100 CE
     */
    fun breakdownSymbol(rawSymbol: String): Triple<Instrument, String, String> {
        val trimmed = rawSymbol.trim().replace(Regex("(?i)\\s+(bse|nse|mcx)$"), "").trim()
        val instrument = Instrument.fromString(trimmed)

        var optionType = "CE"
        val upper = trimmed.uppercase()
        if (upper.endsWith("PE") || upper.contains(" PE") || upper.contains("PUT")) {
            optionType = "PE"
        } else if (upper.endsWith("CE") || upper.contains(" CE") || upper.contains("CALL")) {
            optionType = "CE"
        } else if (upper.contains("FUT")) {
            optionType = "FUT"
        } else if (upper.contains("EQ")) {
            optionType = "EQ"
        }

        // Already clean or spaced: e.g. "25100 CE" or "SENSEX 72600 PE"
        if (trimmed.contains(" ") && (trimmed.contains("CE", true) || trimmed.contains("PE", true))) {
            return Triple(instrument, trimmed, optionType)
        }

        // Standard Indian contract symbol regex:
        // Examples: SENSEX2640272600PE, BANKNIFTY26APR52500PE, NIFTY2640225100CE
        // Index: SENSEX
        // Expiry code: 26402 (Year 26 + Month 4 + Day 02) or 26APR
        // Strike: 72600
        // Type: PE
        val regex = Pattern.compile("(?i)^([A-Z]+?)(\\d{2}(?:[0-9]|JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\\d{0,2})(\\d{4,6})(CE|PE|FUT)$")
        val matcher = regex.matcher(trimmed)
        if (matcher.find()) {
            val strike = matcher.group(3)
            val type = matcher.group(4)?.uppercase() ?: optionType
            val cleanName = "${instrument.displayName} $strike $type"
            return Triple(instrument, cleanName, type)
        }

        // Fallback for contract ending with [STRIKE][CE/PE]
        val regex2 = Pattern.compile("(?i)([1-9]\\d{3,5})\\s*(CE|PE)$")
        val matcher2 = regex2.matcher(trimmed)
        if (matcher2.find()) {
            val strike = matcher2.group(1)
            val type = matcher2.group(2)?.uppercase() ?: optionType
            val cleanName = "${instrument.displayName} $strike $type"
            return Triple(instrument, cleanName, type)
        }

        return Triple(instrument, trimmed, optionType)
    }

    private fun parseOptionTypeString(raw: String): String {
        val upper = raw.uppercase().trim()
        return when {
            upper.contains("PUT") || upper == "PE" || upper.endsWith("PE") -> "PE"
            upper.contains("CALL") || upper == "CE" || upper.endsWith("CE") -> "CE"
            upper.contains("FUT") -> "FUT"
            upper.contains("EQ") || upper.contains("STOCK") -> "EQ"
            else -> "CE"
        }
    }

    private fun parseDirection(rawDirection: String): String {
        val upper = rawDirection.uppercase().trim()
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

    fun parseDateTimeFlexible(combinedCandidate: String, explicitDate: String, explicitTime: String): Long {
        val candidate = combinedCandidate.replace("\n", " ").replace("\r", " ").trim()
        val expDateClean = explicitDate.replace("\n", " ").trim()
        val expTimeClean = explicitTime.replace("\n", " ").trim()

        val fullString = when {
            candidate.isNotBlank() && candidate.contains(" ") -> candidate
            expDateClean.isNotBlank() && expTimeClean.isNotBlank() -> "$expDateClean $expTimeClean"
            candidate.isNotBlank() -> candidate
            expDateClean.isNotBlank() -> expDateClean
            else -> ""
        }

        if (fullString.isBlank()) {
            return System.currentTimeMillis()
        }

        val formats = listOf(
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
            SimpleDateFormat("dd-MM-yyyy hh:mm:ss a", Locale.ENGLISH),
            SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.ENGLISH),
            SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.ENGLISH),
            SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        )

        for (fmt in formats) {
            try {
                val d = fmt.parse(fullString)
                if (d != null) {
                    return d.time
                }
            } catch (_: Exception) {}
        }

        val match = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})[\\sT]+(\\d{2}:\\d{2}:\\d{2})").matcher(fullString)
        if (match.find()) {
            val dStr = match.group(1)
            val tStr = match.group(2)
            try {
                val d = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse("$dStr $tStr")
                if (d != null) return d.time
            } catch (_: Exception) {}
        }

        return System.currentTimeMillis()
    }

    fun parseCsvRow(row: String): List<String> {
        val result = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < row.length) {
            val c = row[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < row.length && row[i + 1] == '\"') {
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
