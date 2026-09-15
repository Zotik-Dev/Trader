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
import com.example.model.SetupPerformance
import com.example.model.TradingSummary
import com.example.util.BackupManager
import com.example.util.CsvExporter
import com.example.util.SampleTradeData
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
    val dateRange: DateFilter = DateFilter.ALL
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

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    // Filtered trades for list screen
    val filteredTrades: StateFlow<List<TradeEntity>> = combine(allTrades, _filters) { trades, filters ->
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

        trades.filter { trade ->
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

            // Date match
            val matchesDate = when (filters.dateRange) {
                DateFilter.TODAY -> trade.entryTimestamp >= startOfToday
                DateFilter.THIS_WEEK -> trade.entryTimestamp >= startOfWeek
                DateFilter.THIS_MONTH -> trade.entryTimestamp >= startOfMonth
                DateFilter.ALL -> true
            }

            matchesQuery && matchesInstrument && matchesOutcome && matchesSetup && matchesDate
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Summary calculation
    val summary: StateFlow<TradingSummary> = combine(allTrades, _filters) { trades, filters ->
        computeSummary(filterTradesByDate(trades, filters.dateRange))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TradingSummary()
    )

    // Daily PnL for chart
    val dailyPnLList: StateFlow<List<DailyPnL>> = combine(allTrades, _filters) { trades, filters ->
        computeDailyPnL(filterTradesByDate(trades, filters.dateRange))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Monthly PnL for chart
    val monthlyPnLList: StateFlow<List<MonthlyPnL>> = combine(allTrades, _filters) { trades, _ ->
        computeMonthlyPnL(trades)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Setup performance
    val setupAnalytics: StateFlow<List<SetupPerformance>> = combine(allTrades, _filters) { trades, filters ->
        computeSetupAnalytics(filterTradesByDate(trades, filters.dateRange))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Instrument performance
    val instrumentAnalytics: StateFlow<List<InstrumentPerformance>> = combine(allTrades, _filters) { trades, filters ->
        computeInstrumentAnalytics(filterTradesByDate(trades, filters.dateRange))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Emotion performance
    val emotionAnalytics: StateFlow<List<EmotionPerformance>> = combine(allTrades, _filters) { trades, filters ->
        computeEmotionAnalytics(filterTradesByDate(trades, filters.dateRange))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun filterTradesByDate(trades: List<TradeEntity>, dateRange: DateFilter): List<TradeEntity> {
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

        return when (dateRange) {
            DateFilter.TODAY -> trades.filter { it.entryTimestamp >= startOfToday }
            DateFilter.THIS_WEEK -> trades.filter { it.entryTimestamp >= startOfWeek }
            DateFilter.THIS_MONTH -> trades.filter { it.entryTimestamp >= startOfMonth }
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

    fun loadSampleData() {
        viewModelScope.launch {
            val samples = SampleTradeData.generateSampleTrades()
            repository.insertTrades(samples)
            _userMessage.value = "Loaded ${samples.size} sample trades!"
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

    private fun computeSummary(trades: List<TradeEntity>): TradingSummary {
        if (trades.isEmpty()) return TradingSummary()

        val closedTrades = trades.filter { it.status != "OPEN" }
        val totalTrades = trades.size
        val winTrades = closedTrades.count { it.netPnL > 0 }
        val lossTrades = closedTrades.count { it.netPnL < 0 }
        val breakEvenTrades = closedTrades.count { it.netPnL == 0.0 }
        val winRate = if (closedTrades.isNotEmpty()) (winTrades.toDouble() / closedTrades.size) * 100.0 else 0.0

        val grossPnL = trades.sumOf { it.grossPnL }
        val totalCharges = trades.sumOf { it.charges }
        val netPnL = trades.sumOf { it.netPnL }

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

        val todayPnL = trades.filter { it.entryTimestamp >= startOfToday }.sumOf { it.netPnL }
        val thisWeekPnL = trades.filter { it.entryTimestamp >= startOfWeek }.sumOf { it.netPnL }
        val thisMonthPnL = trades.filter { it.entryTimestamp >= startOfMonth }.sumOf { it.netPnL }

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
            thisMonthPnL = thisMonthPnL
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
        return grouped.map { (_, mTrades) ->
            val first = mTrades.first()
            val netPnL = mTrades.sumOf { it.netPnL }
            val wins = mTrades.count { it.netPnL > 0 }
            val winRate = if (mTrades.isNotEmpty()) (wins.toDouble() / mTrades.size) * 100.0 else 0.0
            MonthlyPnL(
                monthLabel = monthFormat.format(Date(first.entryTimestamp)),
                netPnL = netPnL,
                tradeCount = mTrades.size,
                winRate = winRate
            )
        }
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
