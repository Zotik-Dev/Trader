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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TradeEntity
import com.example.model.DateFilter
import com.example.model.Instrument
import com.example.ui.components.DirectionBadge
import com.example.ui.components.MonthYearPickerDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradeViewModel
import com.example.util.TradeCalculations
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeListScreen(
    viewModel: TradeViewModel,
    onNavigateToTradeDetail: (Long) -> Unit,
    onNavigateToAddTrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredTrades by viewModel.filteredTrades.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedInstrument by viewModel.selectedInstrumentFilter.collectAsState()
    val selectedOutcome by viewModel.selectedOutcomeFilter.collectAsState()
    val selectedSetup by viewModel.selectedSetupFilter.collectAsState()
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

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Trade History (${filteredTrades.size})",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTrade,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_trade_history")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Trade")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by strike, instrument, setup, notes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("input_search_trades"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Horizontal filters: Date Range (All, Today, Week, Month, Last Month, Year, etc.)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        } else null,
                        label = {
                            Text(
                                labelText,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Horizontal filters: Instruments
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedInstrument == "All",
                        onClick = { viewModel.setInstrumentFilter("All") },
                        label = { Text("All Instruments", fontSize = 11.sp) }
                    )
                }
                items(Instrument.entries) { inst ->
                    FilterChip(
                        selected = selectedInstrument.equals(inst.displayName, ignoreCase = true) ||
                                selectedInstrument.equals(inst.name, ignoreCase = true),
                        onClick = { viewModel.setInstrumentFilter(inst.displayName) },
                        label = { Text(inst.displayName, fontSize = 11.sp) }
                    )
                }
            }

            // Horizontal filters: Outcome
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(listOf("All", "Wins", "Losses", "Open")) { outcome ->
                    FilterChip(
                        selected = selectedOutcome == outcome,
                        onClick = { viewModel.setOutcomeFilter(outcome) },
                        label = { Text(outcome, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Trades list
            if (filteredTrades.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FilterListOff,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No trades match the filters",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Try clearing your search or filter tags.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTrades, key = { it.id }) { trade ->
                        TradeItemCard(
                            trade = trade,
                            dateStr = dateFormatter.format(Date(trade.entryTimestamp)),
                            onClick = { onNavigateToTradeDetail(trade.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TradeItemCard(
    trade: TradeEntity,
    dateStr: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isProfit = trade.netPnL >= 0
    val pnlColor = if (isProfit) ProfitGreen else LossRed

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("trade_card_${trade.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row: Date, Status badge, and Camera attachment indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (trade.imageUris.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Charts attached",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                "${trade.imageUris.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    StatusBadge(status = trade.status)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Instrument, Strike, Direction & Net PnL row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DirectionBadge(direction = trade.direction, optionType = trade.optionType)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = trade.strikeOrSymbol,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${trade.instrument} • Qty: ${trade.quantity}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // P&L
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = TradeCalculations.formatCurrency(trade.netPnL),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = pnlColor
                    )
                    Text(
                        text = TradeCalculations.formatPoints(trade.points),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (trade.points >= 0) ProfitGreen else LossRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Footer row: Entry -> Exit, R:R, Setup tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "₹${trade.entryPrice} → ₹${trade.exitPrice}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (trade.actualRR != 0.0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "• R:R ${TradeCalculations.formatRR(trade.actualRR)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (trade.setup.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = trade.setup,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
