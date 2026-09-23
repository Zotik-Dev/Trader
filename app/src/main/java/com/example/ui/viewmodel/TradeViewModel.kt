package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.TradeDatabase
import com.example.data.TradeEntity
import com.example.data.TradeRepository
import com.example.model.DateFilter
import com.example.model.DailyPnL
import com.example.model.Emotion
import com.example.model.EmotionPerformance
import com.example.model.InstrumentPerformance
import com.example.model.MonthlyPnL
import com.example.model.YearlyPnL
import com.example.model.SetupPerformance
import com.example.model.TradingSummary
import com.example.util.BackupManager
import com.example.util.CsvExporter
import com.example.util.CsvImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TradeFilterState(
    val query: String = "",
    val instrument: String = "All",
    val outcome: String = "All",
    val setup: String = "All",
    val dateRange: DateFilter = DateFilter.ALL,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
)

class TradeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TradeRepository

    init {
        val database = TradeDatabase.getInstance(application)
        repository = TradeRepository(database.tradeDao())
    }

    val allTrades: StateFlow<List<TradeEntity>> = repository.allTrades
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _filters = MutableStateFlow(TradeFilterState())

    val searchQuery: StateFlow<String> = _filters
        .map { it.query }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val selectedInstrumentFilter: StateFlow<String> = _filters
        .map { it.instrument }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "All")

    val selectedOutcomeFilter: StateFlow<String> = _filters
        .map { it.outcome }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "All")

    val selectedSetupFilter: StateFlow<String> = _filters
        .map { it.setup }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "All")

    val dateFilter: StateFlow<DateFilter> = _filters
        .map { it.dateRange }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DateFilter.ALL)

    val selectedYear: StateFlow<Int> = _filters
        .map { it.selectedYear }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Calendar.getInstance().get(Calendar.YEAR))

    val selectedMonth: StateFlow<Int> = _filters
        .map { it.selectedMonth }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Calendar.getInstance().get(Calendar.MONTH))

    val availableYears: StateFlow<List<Int>> = allTrades.map { trades ->
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val tradeYears = trades.map {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.entryTimestamp
            cal.get(Calendar.YEAR)
        }.distinct()
        (tradeYears + currentYear + (currentYear - 1)).distinct().sortedDescending()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(Calendar.getInstance().get(Calendar.YEAR)))

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    // Filtered trades for list screen
    val filteredTrades: StateFlow<List<TradeEntity>> = combine(allTrades, _filters) { trades, filters ->
        val dateMatched = filterTradesByDate(trades, filters)
        dateMatched.filter { trade ->
            // Query match
            val matchesQuery = filters.query.isBlank() ||
                    trade.instrument.contains(filters.query, ignoreCase = true) ||
                    trade.strikeOrSymbol.contains(filters.query, ignoreCase = true) ||
                    trade.setup.contains(filters.query, ignoreCase = true) ||
                    trade.notes.contains(filters.query, ignoreCase = true)

            // Instrument match
            val matchesInstrument = filters.instrument == "All" || trade.instrument.equals(filters.instrument, ignoreCase = true)

            // Outcome match
            val matchesOutcome = when (filters.outcome) {
                "Wins" -> trade.netPnL > 0 && trade.status != "OPEN"
                "Losses" -> trade.netPnL < 0 && trade.status != "OPEN"
                "Open" -> trade.status == "OPEN"
                else -> true
            }

            // Setup match
            val matchesSetup = filters.setup == "All" || trade.setup.equals(filters.setup, ignoreCase = true)

            matchesQuery && matchesInstrument && matchesOutcome && matchesSetup
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Summary calculation
    val summary: StateFlow<TradingSummary> = combine(allTrades, _filters) { trades, filters ->
        computeSummary(filterTradesByDate(trades, filters), trades)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TradingSummary()
    )

    // Daily PnL for chart
    val dailyPnLList: StateFlow<List<DailyPnL>> = combine(allTrades, _filters) { trades, filters ->
        computeDailyPnL(filterTradesByDate(trades, filters))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Monthly PnL for chart & breakdown
    val monthlyPnLList: StateFlow<List<MonthlyPnL>> = combine(allTrades, _filters) { trades, _ ->
        computeMonthlyPnL(trades)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Yearly PnL for breakdown
    val yearlyPnLList: StateFlow<List<YearlyPnL>> = combine(allTrades, _filters) { trades, _ ->
        computeYearlyPnL(trades)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Setup performance
    val setupAnalytics: StateFlow<List<SetupPerformance>> = combine(allTrades, _filters) { trades, filters ->
        computeSetupAnalytics(filterTradesByDate(trades, filters))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Instrument performance
    val instrumentAnalytics: StateFlow<List<InstrumentPerformance>> = combine(allTrades, _filters) { trades, filters ->
        computeInstrumentAnalytics(filterTradesByDate(trades, filters))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Emotion performance
    val emotionAnalytics: StateFlow<List<EmotionPerformance>> = combine(allTrades, _filters) { trades, filters ->
        computeEmotionAnalytics(filterTradesByDate(trades, filters))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun filterTradesByDate(trades: List<TradeEntity>, filters: TradeFilterState): List<TradeEntity> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val startOfWeek = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = calendar.timeInMillis

        // Last month bounds
        val calLastMonthStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val startOfLastMonth = calLastMonthStart.timeInMillis

        val calLastMonthEnd = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfLastMonth = calLastMonthEnd.timeInMillis

        // This year bounds
        val calThisYearStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val startOfThisYear = calThisYearStart.timeInMillis

        // Last year bounds
        val calLastYearStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.YEAR, -1)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val calLastYearEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
            add(Calendar.YEAR, -1)
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 31)
        }

        return when (filters.dateRange) {
            DateFilter.TODAY -> trades.filter { it.entryTimestamp >= startOfToday }
            DateFilter.THIS_WEEK -> trades.filter { it.entryTimestamp >= startOfWeek }
            DateFilter.THIS_MONTH -> trades.filter { it.entryTimestamp >= startOfMonth }
            DateFilter.LAST_MONTH -> trades.filter { it.entryTimestamp in startOfLastMonth..endOfLastMonth }
            DateFilter.THIS_YEAR -> trades.filter { it.entryTimestamp >= startOfThisYear }
            DateFilter.LAST_YEAR -> trades.filter { it.entryTimestamp in calLastYearStart.timeInMillis..calLastYearEnd.timeInMillis }
            DateFilter.CUSTOM_MONTH_YEAR -> {
                val calCustomStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    set(Calendar.YEAR, filters.selectedYear)
                    set(Calendar.MONTH, filters.selectedMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val calCustomEnd = Calendar.getInstance().apply {
                    set(Calendar.YEAR, filters.selectedYear)
                    set(Calendar.MONTH, filters.selectedMonth)
                    set(Calendar.DAY_OF_MONTH, calCustomStart.getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                trades.filter { it.entryTimestamp in calCustomStart.timeInMillis..calCustomEnd.timeInMillis }
            }
            DateFilter.ALL -> trades
        }
    }

    fun setSearchQuery(query: String) {
        _filters.update { it.copy(query = query) }
    }

    fun setInstrumentFilter(instrument: String) {
        _filters.update { it.copy(instrument = instrument) }
    }

    fun setOutcomeFilter(outcome: String) {
        _filters.update { it.copy(outcome = outcome) }
    }

    fun setSetupFilter(setup: String) {
        _filters.update { it.copy(setup = setup) }
    }

    fun setDateFilter(filter: DateFilter) {
        _filters.update { it.copy(dateRange = filter) }
    }

    fun setCustomMonthYear(year: Int, month: Int) {
        _filters.update {
            it.copy(
                dateRange = DateFilter.CUSTOM_MONTH_YEAR,
                selectedYear = year,
                selectedMonth = month
            )
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun saveTrade(trade: TradeEntity, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            if (trade.id == 0L) {
                val newId = repository.insertTrade(trade)
                _userMessage.value = "Trade saved successfully!"
                onComplete(newId)
            } else {
                repository.updateTrade(trade)
                _userMessage.value = "Trade updated successfully!"
                onComplete(trade.id)
            }
        }
    }

    fun deleteTrade(trade: TradeEntity) {
        viewModelScope.launch {
            repository.deleteTrade(trade)
            _userMessage.value = "Trade deleted"
        }
    }

    fun insertRestoredTrades(trades: List<TradeEntity>) {
        viewModelScope.launch {
            repository.insertTrades(trades)
            _userMessage.value = "Restored ${trades.size} trades!"
        }
    }

    fun clearAllTrades() {
        viewModelScope.launch {
            repository.deleteAllTrades()
            _userMessage.value = "All trade records cleared"
        }
    }

    fun exportToCsv(context: Context) {
        viewModelScope.launch {
            val trades = allTrades.value
            if (trades.isEmpty()) {
                _userMessage.value = "No trades to export"
                return@launch
            }
            try {
                val file = CsvExporter.exportTradesToCsv(context, trades)
                CsvExporter.shareCsvFile(context, file)
                _userMessage.value = "CSV exported successfully!"
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.value = "Failed to export CSV: ${e.localizedMessage}"
            }
        }
    }

    fun exportBackup(context: Context) {
        viewModelScope.launch {
            val trades = allTrades.value
            if (trades.isEmpty()) {
                _userMessage.value = "No trades to backup"
                return@launch
            }
            try {
                val file = BackupManager.createJsonBackup(context, trades)
                BackupManager.shareBackupFile(context, file)
                _userMessage.value = "Backup created successfully!"
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.value = "Backup failed: ${e.localizedMessage}"
            }
        }
    }

    fun restoreBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val restored = BackupManager.restoreTradesFromJsonUri(context, uri)
                if (restored.isNotEmpty()) {
                    repository.insertTrades(restored)
                    _userMessage.value = "Successfully restored ${restored.size} trades!"
                } else {
                    _userMessage.value = "No valid trades found in backup file"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.value = "Failed to restore backup: ${e.localizedMessage}"
            }
        }
    }

    fun importFromCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val result = CsvImporter.importTradesFromCsvUri(context, uri)
                if (result.importedTrades.isNotEmpty()) {
                    repository.insertTrades(result.importedTrades)
                    val msg = if (result.failedRows > 0) {
                        "Imported ${result.importedTrades.size} trades successfully (${result.failedRows} skipped rows)."
                    } else {
                        "Successfully imported ${result.importedTrades.size} trades from CSV!"
                    }
                    _userMessage.value = msg
                } else {
                    _userMessage.value = result.errorMessage ?: "No trades could be imported from CSV."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.value = "Failed to import CSV: ${e.localizedMessage}"
            }
        }
    }

    private fun computeSummary(filteredTrades: List<TradeEntity>, allTrades: List<TradeEntity>): TradingSummary {
        val closedTrades = filteredTrades.filter { it.status != "OPEN" }
        val totalTrades = filteredTrades.size
        val winTrades = closedTrades.count { it.netPnL > 0 }
        val lossTrades = closedTrades.count { it.netPnL < 0 }
        val breakEvenTrades = closedTrades.count { it.netPnL == 0.0 }
        val winRate = if (closedTrades.isNotEmpty()) (winTrades.toDouble() / closedTrades.size) * 100.0 else 0.0

        val grossPnL = filteredTrades.sumOf { it.grossPnL }
        val totalCharges = filteredTrades.sumOf { it.charges }
        val netPnL = filteredTrades.sumOf { it.netPnL }

        val totalGains = closedTrades.filter { it.netPnL > 0 }.sumOf { it.netPnL }
        val totalLosses = Math.abs(closedTrades.filter { it.netPnL < 0 }.sumOf { it.netPnL })
        val profitFactor = if (totalLosses > 0) totalGains / totalLosses else if (totalGains > 0) 99.0 else 0.0

        val averageWin = if (winTrades > 0) totalGains / winTrades else 0.0
        val averageLoss = if (lossTrades > 0) totalLosses / lossTrades else 0.0

        val closedWithRR = closedTrades.filter { it.actualRR != 0.0 }
        val avgRiskReward = if (closedWithRR.isNotEmpty()) closedWithRR.map { it.actualRR }.average() else 0.0

        val bestTrade = closedTrades.maxByOrNull { it.netPnL }?.netPnL ?: 0.0
        val worstTrade = closedTrades.minByOrNull { it.netPnL }?.netPnL ?: 0.0

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val startOfWeek = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = cal.timeInMillis

        // Last month bounds
        val calLastMonthStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val startOfLastMonth = calLastMonthStart.timeInMillis

        val calLastMonthEnd = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfLastMonth = calLastMonthEnd.timeInMillis

        // This year bounds
        val calThisYearStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val startOfThisYear = calThisYearStart.timeInMillis

        // Last year bounds
        val calLastYearStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.YEAR, -1)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val calLastYearEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
            add(Calendar.YEAR, -1)
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 31)
        }

        val todayPnL = allTrades.filter { it.entryTimestamp >= startOfToday }.sumOf { it.netPnL }
        val thisWeekPnL = allTrades.filter { it.entryTimestamp >= startOfWeek }.sumOf { it.netPnL }
        val thisMonthPnL = allTrades.filter { it.entryTimestamp >= startOfMonth }.sumOf { it.netPnL }
        val lastMonthPnL = allTrades.filter { it.entryTimestamp in startOfLastMonth..endOfLastMonth }.sumOf { it.netPnL }
        val thisYearPnL = allTrades.filter { it.entryTimestamp >= startOfThisYear }.sumOf { it.netPnL }
        val lastYearPnL = allTrades.filter { it.entryTimestamp in calLastYearStart.timeInMillis..calLastYearEnd.timeInMillis }.sumOf { it.netPnL }

        return TradingSummary(
            totalTrades = totalTrades,
            winTrades = winTrades,
            lossTrades = lossTrades,
            breakEvenTrades = breakEvenTrades,
            winRate = winRate,
            grossPnL = grossPnL,
            totalCharges = totalCharges,
            netPnL = netPnL,
            profitFactor = profitFactor,
            averageWin = averageWin,
            averageLoss = averageLoss,
            avgRiskReward = avgRiskReward,
            bestTradePnL = bestTrade,
            worstTradePnL = worstTrade,
            todayPnL = todayPnL,
            thisWeekPnL = thisWeekPnL,
            thisMonthPnL = thisMonthPnL,
            lastMonthPnL = lastMonthPnL,
            thisYearPnL = thisYearPnL,
            lastYearPnL = lastYearPnL
        )
    }

    private fun computeDailyPnL(trades: List<TradeEntity>): List<DailyPnL> {
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        val grouped = trades.groupBy { dayKeyFormat.format(Date(it.entryTimestamp)) }
        return grouped.map { (_, dayTrades) ->
            val first = dayTrades.first()
            val netPnL = dayTrades.sumOf { it.netPnL }
            val wins = dayTrades.count { it.netPnL > 0 }
            DailyPnL(
                dateLabel = dateFormat.format(Date(first.entryTimestamp)),
                timestamp = first.entryTimestamp,
                netPnL = netPnL,
                tradeCount = dayTrades.size,
                winCount = wins
            )
        }.sortedBy { it.timestamp }
    }

    private fun computeMonthlyPnL(trades: List<TradeEntity>): List<MonthlyPnL> {
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val monthKeyFormat = SimpleDateFormat("yyyyMM", Locale.getDefault())

        val grouped = trades.groupBy { monthKeyFormat.format(Date(it.entryTimestamp)) }
        return grouped.map { (key, mTrades) ->
            val first = mTrades.first()
            val netPnL = mTrades.sumOf { it.netPnL }
            val wins = mTrades.count { it.netPnL > 0 }
            val winRate = if (mTrades.isNotEmpty()) (wins.toDouble() / mTrades.size) * 100.0 else 0.0
            Pair(
                key,
                MonthlyPnL(
                    monthLabel = monthFormat.format(Date(first.entryTimestamp)),
                    netPnL = netPnL,
                    tradeCount = mTrades.size,
                    winRate = winRate
                )
            )
        }.sortedByDescending { it.first }.map { it.second }
    }

    private fun computeYearlyPnL(trades: List<TradeEntity>): List<YearlyPnL> {
        val yearKeyFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        val grouped = trades.groupBy { yearKeyFormat.format(Date(it.entryTimestamp)) }
        return grouped.map { (yearKey, yTrades) ->
            val netPnL = yTrades.sumOf { it.netPnL }
            val wins = yTrades.count { it.netPnL > 0 }
            val winRate = if (yTrades.isNotEmpty()) (wins.toDouble() / yTrades.size) * 100.0 else 0.0
            YearlyPnL(
                yearLabel = yearKey,
                netPnL = netPnL,
                tradeCount = yTrades.size,
                winRate = winRate
            )
        }.sortedByDescending { it.yearLabel }
    }

    private fun computeSetupAnalytics(trades: List<TradeEntity>): List<SetupPerformance> {
        return trades
            .filter { it.setup.isNotBlank() }
            .groupBy { it.setup }
            .map { (setup, sTrades) ->
                val wins = sTrades.count { it.netPnL > 0 }
                val rate = if (sTrades.isNotEmpty()) (wins.toDouble() / sTrades.size) * 100.0 else 0.0
                SetupPerformance(
                    setupName = setup,
                    totalTrades = sTrades.size,
                    winCount = wins,
                    winRate = rate,
                    netPnL = sTrades.sumOf { it.netPnL }
                )
            }.sortedByDescending { it.netPnL }
    }

    private fun computeInstrumentAnalytics(trades: List<TradeEntity>): List<InstrumentPerformance> {
        return trades
            .groupBy { it.instrument }
            .map { (instrument, iTrades) ->
                val wins = iTrades.count { it.netPnL > 0 }
                val rate = if (iTrades.isNotEmpty()) (wins.toDouble() / iTrades.size) * 100.0 else 0.0
                InstrumentPerformance(
                    instrumentName = instrument,
                    totalTrades = iTrades.size,
                    winCount = wins,
                    winRate = rate,
                    netPnL = iTrades.sumOf { it.netPnL }
                )
            }.sortedByDescending { it.netPnL }
    }

    private fun computeEmotionAnalytics(trades: List<TradeEntity>): List<EmotionPerformance> {
        return trades
            .filter { it.emotion.isNotBlank() }
            .groupBy { it.emotion }
            .map { (emotionName, eTrades) ->
                val wins = eTrades.count { it.netPnL > 0 }
                val rate = if (eTrades.isNotEmpty()) (wins.toDouble() / eTrades.size) * 100.0 else 0.0
                val matched = Emotion.entries.find { it.displayName.equals(emotionName, ignoreCase = true) }
                EmotionPerformance(
                    emotionName = emotionName,
                    emoji = matched?.emoji ?: "📊",
                    totalTrades = eTrades.size,
                    netPnL = eTrades.sumOf { it.netPnL },
                    winRate = rate
                )
            }.sortedByDescending { it.netPnL }
    }
}
