package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DateFilter
import com.example.model.Instrument
import com.example.ui.components.DailyPnLBarChart
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.PnLCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradeViewModel
import com.example.util.TradeCalculations
import java.text.DateFormatSymbols

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TradeViewModel,
    onNavigateToAddTrade: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onOpenBackupDialog: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val summary by viewModel.summary.collectAsState()
    val dailyPnLList by viewModel.dailyPnLList.collectAsState()
    val instrumentAnalytics by viewModel.instrumentAnalytics.collectAsState()
    val setupAnalytics by viewModel.setupAnalytics.collectAsState()
    val allTrades by viewModel.allTrades.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    var showMonthYearPicker by remember { mutableStateOf(false) }

    if (showMonthYearPicker) {
        MonthYearPickerDialog(
            initialYear = selectedYear,
            initialMonth = selectedMonth,
            onDismiss = { showMonthYearPicker = false },
            onConfirm = { year, month ->
                viewModel.setCustomMonthYear(year, month)
                showMonthYearPicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Trading Journal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.exportToCsv(context) },
                        modifier = Modifier.testTag("action_export_csv")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("action_settings")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings & Preferences")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddTrade,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Log Trade", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_trade")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Empty state banner if no trades exist
            if (allTrades.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Welcome to Trading Journal!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Log trades across NIFTY, Bank Nifty, Sensex, Crude Oil, & Natural Gas with P&L, risk/reward, chart photos, and setups.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateToAddTrade,
                                modifier = Modifier.testTag("button_start_trading")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Log Your First Trade")
                            }
                        }
                    }
                }
            }

            // Date Range Filter Tabs
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(DateFilter.entries) { filter ->
                        val isSelected = dateFilter == filter
                        val labelText = if (filter == DateFilter.CUSTOM_MONTH_YEAR && isSelected) {
                            val monthName = DateFormatSymbols.getInstance().shortMonths.getOrNull(selectedMonth) ?: ""
                            "$monthName $selectedYear"
                        } else {
                            filter.label
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (filter == DateFilter.CUSTOM_MONTH_YEAR) {
                                    showMonthYearPicker = true
                                } else {
                                    viewModel.setDateFilter(filter)
                                }
                            },
                            leadingIcon = if (filter == DateFilter.CUSTOM_MONTH_YEAR) {
                                {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    labelText,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            // Hero PnL Card
            item {
                PnLCard(
                    netPnL = summary.netPnL,
                    grossPnL = summary.grossPnL,
                    charges = summary.totalCharges,
                    winRate = summary.winRate,
                    totalTrades = summary.totalTrades,
                    winTrades = summary.winTrades,
                    lossTrades = summary.lossTrades
                )
            }

            // Period Performance Metrics: Row 1 (Today, This Week, This Month)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        title = "Today",
                        value = TradeCalculations.formatCurrency(summary.todayPnL),
                        isPositive = summary.todayPnL >= 0,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "This Week",
                        value = TradeCalculations.formatCurrency(summary.thisWeekPnL),
                        isPositive = summary.thisWeekPnL >= 0,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "This Month",
                        value = TradeCalculations.formatCurrency(summary.thisMonthPnL),
                        isPositive = summary.thisMonthPnL >= 0,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Period Performance Metrics: Row 2 (Last Month, This Year, Last Year)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        title = "Last Month",
                        value = TradeCalculations.formatCurrency(summary.lastMonthPnL),
                        isPositive = summary.lastMonthPnL >= 0,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "This Year",
                        value = TradeCalculations.formatCurrency(summary.thisYearPnL),
                        isPositive = summary.thisYearPnL >= 0,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Last Year",
                        value = TradeCalculations.formatCurrency(summary.lastYearPnL),
                        isPositive = summary.lastYearPnL >= 0,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Secondary metrics row: Profit Factor, Avg R:R, Best Trade
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Profit Factor", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "%.2f".format(summary.profitFactor),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.profitFactor >= 1.5) ProfitGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Avg Risk:Reward", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = TradeCalculations.formatRR(summary.avgRiskReward),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Best Trade", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = TradeCalculations.formatCurrency(summary.bestTradePnL),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            )
                        }
                    }
                }
            }

            // Daily P&L Timeline Chart
            item {
                DailyPnLBarChart(dailyList = dailyPnLList)
            }

            // Instruments Performance Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "INSTRUMENT PERFORMANCE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "View Analytics",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { onNavigateToAnalytics() }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (instrumentAnalytics.isEmpty()) {
                            Text("No trades to analyze", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            instrumentAnalytics.forEach { item ->
                                val isProfitable = item.netPnL >= 0
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(item.instrumentName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${item.totalTrades} trades • ${item.winRate.toInt()}% win rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    // Mini progress bar for win rate
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth((item.winRate / 100.0).toFloat().coerceIn(0f, 1f))
                                                .background(if (item.winRate >= 50) ProfitGreen else LossRed)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = TradeCalculations.formatCurrency(item.netPnL),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isProfitable) ProfitGreen else LossRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top Setups Leaderboard
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "TOP SETUPS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (setupAnalytics.isEmpty()) {
                            Text("No setups logged yet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            setupAnalytics.take(4).forEach { setup ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(setup.setupName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${setup.totalTrades} trades • ${setup.winRate.toInt()}% win rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = TradeCalculations.formatCurrency(setup.netPnL),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (setup.netPnL >= 0) ProfitGreen else LossRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Extra padding at bottom for FAB
            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
fun MetricBox(
    title: String,
    value: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) ProfitGreen else LossRed
            )
        }
    }
}
