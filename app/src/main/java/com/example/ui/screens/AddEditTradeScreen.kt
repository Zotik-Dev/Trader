package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TradeEntity
import com.example.model.*
import com.example.ui.components.DirectionBadge
import com.example.ui.components.ImageAttachmentSection
import com.example.ui.components.StatusBadge
import com.example.ui.components.TradeDateTimePickerDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradeViewModel
import com.example.util.TradeCalculations
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTradeScreen(
    viewModel: TradeViewModel,
    existingTradeId: Long? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allTrades by viewModel.allTrades.collectAsState()
    val existingTrade = remember(existingTradeId, allTrades) {
        if (existingTradeId != null && existingTradeId > 0) {
            allTrades.find { it.id == existingTradeId }
        } else null
    }

    // State fields
    var selectedInstrument by remember { mutableStateOf(existingTrade?.instrument ?: "NIFTY") }
    var strikeOrSymbol by remember { mutableStateOf(existingTrade?.strikeOrSymbol ?: "") }
    var selectedOptionType by remember { mutableStateOf(existingTrade?.optionType ?: "CE") }
    var selectedDirection by remember { mutableStateOf(existingTrade?.direction ?: "BUY") }

    var instrumentExpanded by remember { mutableStateOf(false) }
    var optionTypeExpanded by remember { mutableStateOf(false) }

    var entryPriceStr by remember { mutableStateOf(existingTrade?.entryPrice?.toString()?.removeSuffix(".0") ?: "") }
    var slPriceStr by remember { mutableStateOf(existingTrade?.slPrice?.takeIf { it > 0 }?.toString()?.removeSuffix(".0") ?: "") }
    var target1Str by remember { mutableStateOf(existingTrade?.target1?.takeIf { it > 0 }?.toString()?.removeSuffix(".0") ?: "") }
    var target2Str by remember { mutableStateOf(existingTrade?.target2?.takeIf { it > 0 }?.toString()?.removeSuffix(".0") ?: "") }
    var target3Str by remember { mutableStateOf(existingTrade?.target3?.takeIf { it > 0 }?.toString()?.removeSuffix(".0") ?: "") }
    var exitPriceStr by remember { mutableStateOf(existingTrade?.exitPrice?.takeIf { it > 0 }?.toString()?.removeSuffix(".0") ?: "") }
    var quantityStr by remember { mutableStateOf(existingTrade?.quantity?.toString() ?: "65") }
    var customChargesStr by remember { mutableStateOf(existingTrade?.charges?.takeIf { it > 0 }?.toString() ?: "") }

    var selectedStatus by remember { mutableStateOf(existingTrade?.status ?: "TARGET_HIT") }
    var selectedSetup by remember { mutableStateOf(existingTrade?.setup ?: "CPR Breakout") }
    var selectedIndicators by remember { mutableStateOf(existingTrade?.indicators ?: listOf("CPR", "VWAP")) }
    var selectedEmotion by remember { mutableStateOf(existingTrade?.emotion ?: "Disciplined") }
    var selectedMistake by remember { mutableStateOf(existingTrade?.mistake ?: "None (Followed Plan)") }
    var notes by remember { mutableStateOf(existingTrade?.notes ?: "") }
    var attachedImages by remember { mutableStateOf(existingTrade?.imageUris ?: emptyList()) }

    // Trade execution date & time (supports custom dates from last year or any date)
    var entryTimestamp by remember { mutableLongStateOf(existingTrade?.entryTimestamp ?: System.currentTimeMillis()) }
    var exitTimestamp by remember { mutableLongStateOf(existingTrade?.exitTimestamp ?: System.currentTimeMillis()) }
    var showEntryDateTimePicker by remember { mutableStateOf(false) }
    var showExitDateTimePicker by remember { mutableStateOf(false) }

    var isAdvancedTargetsVisible by remember { mutableStateOf(target2Str.isNotBlank() || target3Str.isNotBlank()) }

    // Live calculations
    val entryPrice = entryPriceStr.toDoubleOrNull() ?: 0.0
    val exitPrice = exitPriceStr.toDoubleOrNull() ?: 0.0
    val slPrice = slPriceStr.toDoubleOrNull() ?: 0.0
    val target1 = target1Str.toDoubleOrNull() ?: 0.0
    val target2 = target2Str.toDoubleOrNull() ?: 0.0
    val target3 = target3Str.toDoubleOrNull() ?: 0.0
    val quantity = quantityStr.toIntOrNull() ?: 0

    val directionEnum = if (selectedDirection.contains("SELL", ignoreCase = true)) TradeDirection.SELL else TradeDirection.BUY

    val points = TradeCalculations.calculatePoints(directionEnum, entryPrice, exitPrice)
    val grossPnL = TradeCalculations.calculateGrossPnL(directionEnum, entryPrice, exitPrice, quantity)
    val estimatedCharges = customChargesStr.toDoubleOrNull() ?: TradeCalculations.estimateCharges(entryPrice, exitPrice, quantity)
    val netPnL = TradeCalculations.calculateNetPnL(grossPnL, estimatedCharges)
    val riskAmount = TradeCalculations.calculateRiskAmount(directionEnum, entryPrice, slPrice, quantity)
    val plannedRR = TradeCalculations.calculatePlannedRR(directionEnum, entryPrice, slPrice, target1)
    val actualRR = TradeCalculations.calculateActualRR(directionEnum, entryPrice, slPrice, exitPrice)

    // Summary Section Metrics
    val target1Points = TradeCalculations.calculateRewardPoints(directionEnum, entryPrice, target1)
    val potentialTargetGross = TradeCalculations.calculateGrossPnL(directionEnum, entryPrice, target1, quantity)
    val targetCharges = TradeCalculations.estimateCharges(entryPrice, target1, quantity)
    val potentialTargetNet = potentialTargetGross - targetCharges

    val slRiskPoints = TradeCalculations.calculateRiskPoints(directionEnum, entryPrice, slPrice)
    val potentialRiskGross = TradeCalculations.calculateRiskAmount(directionEnum, entryPrice, slPrice, quantity)
    val slCharges = TradeCalculations.estimateCharges(entryPrice, slPrice, quantity)
    val potentialTotalRisk = potentialRiskGross + slCharges

    val breakevenPrice = TradeCalculations.calculateBreakevenPrice(directionEnum, entryPrice, estimatedCharges, quantity)
    val totalCapital = entryPrice * quantity

    // Sync default lot size when instrument changes (if creating fresh trade)
    fun onInstrumentSelected(inst: Instrument) {
        selectedInstrument = inst.displayName
        if (existingTrade == null) {
            quantityStr = inst.defaultLotSize.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existingTrade != null) "Edit Trade #${existingTrade.id}" else "Log New Trade",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("button_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live P&L Calculation Banner (sticky visual indicator of trade math)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (netPnL >= 0) ProfitGreenBg else LossRedBg
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (netPnL >= 0) ProfitGreen else LossRed),
                        width = 1.dp
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "LIVE ESTIMATED NET P&L",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    TradeCalculations.formatCurrency(netPnL),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (netPnL >= 0) ProfitGreen else LossRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    TradeCalculations.formatPoints(points),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (points >= 0) ProfitGreen else LossRed
                                )
                                Text(
                                    "Charges: ₹${"%.1f".format(estimatedCharges)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Risk: ₹${"%.0f".format(riskAmount)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LossRed
                            )
                            Text(
                                "Planned R:R: ${TradeCalculations.formatRR(plannedRR)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Actual R:R: ${TradeCalculations.formatRR(actualRR)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 0. Trade Date & Execution Time (Supports historical entries, last year, or custom date)
            item {
                val dateDisplayFormatter = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }
                val timeDisplayFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
                val entryCal = remember(entryTimestamp) { Calendar.getInstance().apply { timeInMillis = entryTimestamp } }
                val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
                val entryYear = entryCal.get(Calendar.YEAR)
                val isHistorical = entryYear < currentYear
                val lastYear = currentYear - 1

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_trade_date_time"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "TRADE EXECUTION DATE & TIME",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                            }

                            if (isHistorical) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Historical Entry ($entryYear)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Interactive Date Box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    1.dp,
                                    if (isHistorical) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { showEntryDateTimePicker = true }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Entry Date & Time:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = dateDisplayFormatter.format(entryTimestamp),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "• ${timeDisplayFormatter.format(entryTimestamp)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { showEntryDateTimePicker = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("button_change_trade_date")
                            ) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Change", fontSize = 12.sp)
                            }
                        }

                        // Quick Presets Row: Today, Yesterday, Last Year
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Today Preset
                            item {
                                FilterChip(
                                    selected = !isHistorical && entryCal.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR) && entryCal.get(Calendar.YEAR) == currentYear,
                                    onClick = {
                                        val now = Calendar.getInstance()
                                        val updated = Calendar.getInstance().apply {
                                            timeInMillis = entryTimestamp
                                            set(Calendar.YEAR, now.get(Calendar.YEAR))
                                            set(Calendar.MONTH, now.get(Calendar.MONTH))
                                            set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
                                        }
                                        entryTimestamp = updated.timeInMillis
                                    },
                                    label = { Text("Today", fontSize = 11.sp) }
                                )
                            }

                            // Yesterday Preset
                            item {
                                FilterChip(
                                    selected = !isHistorical && entryCal.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.get(Calendar.DAY_OF_YEAR),
                                    onClick = {
                                        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                                        val updated = Calendar.getInstance().apply {
                                            timeInMillis = entryTimestamp
                                            set(Calendar.YEAR, yesterday.get(Calendar.YEAR))
                                            set(Calendar.MONTH, yesterday.get(Calendar.MONTH))
                                            set(Calendar.DAY_OF_MONTH, yesterday.get(Calendar.DAY_OF_MONTH))
                                        }
                                        entryTimestamp = updated.timeInMillis
                                    },
                                    label = { Text("Yesterday", fontSize = 11.sp) }
                                )
                            }

                            // Last Year Preset
                            item {
                                FilterChip(
                                    selected = entryYear == lastYear,
                                    onClick = {
                                        val updated = Calendar.getInstance().apply {
                                            timeInMillis = entryTimestamp
                                            set(Calendar.YEAR, lastYear)
                                        }
                                        entryTimestamp = updated.timeInMillis
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    label = { Text("Last Year ($lastYear)", fontSize = 11.sp) }
                                )
                            }

                            // Custom Pick
                            item {
                                FilterChip(
                                    selected = showEntryDateTimePicker,
                                    onClick = { showEntryDateTimePicker = true },
                                    leadingIcon = {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    label = { Text("Pick Custom Date...", fontSize = 11.sp) }
                                )
                            }
                        }

                        // Optional Exit Date & Time for closed / exited trades
                        val hasExitPrice = exitPriceStr.toDoubleOrNull()?.let { it > 0 } == true
                        if (hasExitPrice || selectedStatus != "OPEN") {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showExitDateTimePicker = true }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Exit Date & Time:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${dateDisplayFormatter.format(exitTimestamp)} • ${timeDisplayFormatter.format(exitTimestamp)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                TextButton(onClick = { showExitDateTimePicker = true }) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Set Exit Time", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 1. Instrument & Trade Type Dropdowns
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "INSTRUMENT & CONTRACT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        // Instrument Dropdown
                        ExposedDropdownMenuBox(
                            expanded = instrumentExpanded,
                            onExpandedChange = { instrumentExpanded = !instrumentExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedInstrument,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Instrument *") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = instrumentExpanded)
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("dropdown_instrument")
                            )

                            ExposedDropdownMenu(
                                expanded = instrumentExpanded,
                                onDismissRequest = { instrumentExpanded = false }
                            ) {
                                Instrument.entries.forEach { inst ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    inst.displayName,
                                                    fontWeight = if (selectedInstrument == inst.displayName || selectedInstrument == inst.name) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    "${inst.defaultLotSize} qty/lot",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            onInstrumentSelected(inst)
                                            instrumentExpanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }

                        // Trade Type (CE/PE) Dropdown and Direction (BUY/SELL)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Trade Type (CE/PE) Dropdown
                            ExposedDropdownMenuBox(
                                expanded = optionTypeExpanded,
                                onExpandedChange = { optionTypeExpanded = !optionTypeExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = when (selectedOptionType) {
                                        "CE" -> "CE (Call)"
                                        "PE" -> "PE (Put)"
                                        "FUT" -> "FUT (Futures)"
                                        "EQ" -> "EQ (Equity)"
                                        else -> selectedOptionType
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Type (CE/PE) *") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = optionTypeExpanded)
                                    },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                        .testTag("dropdown_trade_type")
                                )

                                ExposedDropdownMenu(
                                    expanded = optionTypeExpanded,
                                    onDismissRequest = { optionTypeExpanded = false }
                                ) {
                                    listOf(
                                        "CE" to "CE - Call Option",
                                        "PE" to "PE - Put Option",
                                        "FUT" to "FUT - Futures Contract",
                                        "EQ" to "EQ - Cash Equity"
                                    ).forEach { (code, title) ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(code, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        title,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedOptionType = code
                                                optionTypeExpanded = false
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                        )
                                    }
                                }
                            }

                            // Direction Buttons: BUY vs SELL
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Direction",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = { selectedDirection = "BUY" },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("button_direction_buy"),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selectedDirection == "BUY") ProfitGreen else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (selectedDirection == "BUY") TradeBgDark else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Text("BUY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { selectedDirection = "SELL" },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("button_direction_sell"),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selectedDirection == "SELL") LossRed else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (selectedDirection == "SELL") TextPrimaryDark else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Text("SELL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Strike / Contract Symbol
                        OutlinedTextField(
                            value = strikeOrSymbol,
                            onValueChange = { strikeOrSymbol = it },
                            label = { Text("Strike / Contract Symbol") },
                            placeholder = { Text("e.g. 24800 CE, 52000 PE, Aug Future") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_strike_symbol"),
                            singleLine = true
                        )
                    }
                }
            }

            // 2. Entry and Exit Prices & Position Size
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "ENTRY, EXIT & EXECUTION PRICES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        // Entry Price & Exit Price text fields
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = entryPriceStr,
                                onValueChange = { entryPriceStr = it },
                                label = { Text("Entry Price *") },
                                placeholder = { Text("0.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_entry_price"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = exitPriceStr,
                                onValueChange = { exitPriceStr = it },
                                label = { Text("Exit Price") },
                                placeholder = { Text("0.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_exit_price"),
                                singleLine = true
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = slPriceStr,
                                onValueChange = { slPriceStr = it },
                                label = { Text("Stop Loss (SL)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_sl_price"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = target1Str,
                                onValueChange = { target1Str = it },
                                label = { Text("Target 1") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_target1"),
                                singleLine = true
                            )
                        }

                        // Advanced targets (Target 2 and Target 3)
                        AnimatedVisibility(visible = isAdvancedTargetsVisible) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = target2Str,
                                    onValueChange = { target2Str = it },
                                    label = { Text("Target 2") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = target3Str,
                                    onValueChange = { target3Str = it },
                                    label = { Text("Target 3") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }

                        TextButton(
                            onClick = { isAdvancedTargetsVisible = !isAdvancedTargetsVisible },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(if (isAdvancedTargetsVisible) "- Hide Targets 2 & 3" else "+ Add Target 2 & 3")
                        }

                        // Quantity & Lot Quick Adders
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = { Text("Quantity / Units *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_quantity"),
                            singleLine = true
                        )

                        // Quick lot size increment row
                        val step = Instrument.fromString(selectedInstrument).defaultLotSize.coerceAtLeast(1)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Lots:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            listOf(1, 2, 3, 4).forEach { multiplier ->
                                val qty = step * multiplier
                                SuggestionChip(
                                    onClick = { quantityStr = qty.toString() },
                                    label = { Text("${multiplier}L ($qty)", fontSize = 11.sp) }
                                )
                            }
                        }

                        // Optional custom charges override
                        OutlinedTextField(
                            value = customChargesStr,
                            onValueChange = { customChargesStr = it },
                            label = { Text("Custom Charges (₹) [Leave blank for auto ₹${"%.1f".format(estimatedCharges)}]") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // 3. Trade Summary & Risk:Reward Analysis Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_trade_summary"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (plannedRR >= 2.0) ProfitGreen.copy(alpha = 0.5f)
                        else if (plannedRR >= 1.0) ElectricBlue.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Section Header with dynamic evaluation badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ElectricBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = null,
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        "TRADE SUMMARY & RISK:REWARD",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Auto-calculated potential P&L & expectancy",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Dynamic Evaluation Pill
                            val (badgeText, badgeBg, badgeTextColor) = when {
                                plannedRR >= 2.0 -> Triple("Excellent R:R (≥ 1:2)", ProfitGreenBg, ProfitGreen)
                                plannedRR >= 1.5 -> Triple("Favorable R:R (≥ 1:1.5)", ElectricBlueBg, ElectricBlue)
                                plannedRR >= 1.0 -> Triple("Moderate R:R (1:1)", AmberGoldBg, AmberGold)
                                plannedRR > 0.0 -> Triple("High Risk (< 1:1)", LossRedBg, LossRed)
                                else -> Triple("Awaiting Levels", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = badgeBg,
                                border = BorderStroke(1.dp, badgeTextColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    badgeText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeTextColor
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // Side-by-side Potential Reward vs Potential Risk Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Target 1 Potential Reward Box
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("summary_potential_profit"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = ProfitGreenBg),
                                border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "POTENTIAL PROFIT",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ProfitGreen
                                        )
                                        Icon(
                                            Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = ProfitGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        if (target1 > 0 && entryPrice > 0 && quantity > 0)
                                            TradeCalculations.formatCurrency(potentialTargetNet)
                                        else "—",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ProfitGreen
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        if (target1 > 0 && entryPrice > 0)
                                            "+${"%.1f".format(target1Points)} pts @ ₹$target1"
                                        else "Set Target 1 price",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (target1 > 0 && entryPrice > 0 && quantity > 0) {
                                        Text(
                                            "Gross: ₹${"%,.0f".format(potentialTargetGross)}",
                                            fontSize = 9.sp,
                                            color = ProfitGreen.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            // Stop Loss Potential Risk Box
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("summary_potential_risk"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = LossRedBg),
                                border = BorderStroke(1.dp, LossRed.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "MAX RISK / LOSS",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LossRed
                                        )
                                        Icon(
                                            Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = LossRed,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        if (slPrice > 0 && entryPrice > 0 && quantity > 0)
                                            "-₹${"%,.0f".format(potentialTotalRisk)}"
                                        else "—",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = LossRed
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        if (slPrice > 0 && entryPrice > 0)
                                            "-${"%.1f".format(slRiskPoints)} pts @ ₹$slPrice"
                                        else "Set Stop Loss price",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (slPrice > 0 && entryPrice > 0 && quantity > 0) {
                                        Text(
                                            "Risk: ₹${"%,.0f".format(potentialRiskGross)} + fees",
                                            fontSize = 9.sp,
                                            color = LossRed.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        // Risk : Reward Ratio Highlight Card & Visual Gauge
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("summary_risk_reward_gauge"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "PLANNED RISK : REWARD",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            TradeCalculations.formatRR(plannedRR),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (plannedRR >= 2.0) ProfitGreen else if (plannedRR >= 1.0) ElectricBlue else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (exitPrice > 0 && entryPrice > 0) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "REALIZED R:R",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                TradeCalculations.formatRR(actualRR),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (actualRR >= 1.0) ProfitGreen else if (actualRR > 0) ElectricBlue else LossRed
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Proportional Visual Risk vs Reward Bar
                                val totalPointsSpan = (slRiskPoints + target1Points).coerceAtLeast(0.001)
                                val riskFraction = (slRiskPoints / totalPointsSpan).toFloat().coerceIn(0.05f, 0.95f)
                                val rewardFraction = 1f - riskFraction

                                if (slRiskPoints > 0 && target1Points > 0) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .weight(riskFraction)
                                                    .background(LossRed)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .weight(rewardFraction)
                                                    .background(ProfitGreen)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Risk: ${"%.0f".format(slRiskPoints)} pts (${(riskFraction * 100).toInt()}%)",
                                                fontSize = 10.sp,
                                                color = LossRed,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                "Reward: ${"%.0f".format(target1Points)} pts (${(rewardFraction * 100).toInt()}%)",
                                                fontSize = 10.sp,
                                                color = ProfitGreen,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        "Input Stop Loss and Target 1 to view the proportional Risk vs Reward visualizer.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Realized Outcome Banner (if Exit Price entered)
                        if (exitPrice > 0 && entryPrice > 0 && quantity > 0) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("summary_realized_outcome"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (netPnL >= 0) ProfitGreenBg else LossRedBg
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (netPnL >= 0) ProfitGreen.copy(alpha = 0.5f) else LossRed.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "CURRENT REALIZED P&L (EXIT @ ₹$exitPrice)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (netPnL >= 0) ProfitGreen else LossRed
                                        )
                                        Text(
                                            TradeCalculations.formatCurrency(netPnL),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (netPnL >= 0) ProfitGreen else LossRed
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            TradeCalculations.formatPoints(points),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (points >= 0) ProfitGreen else LossRed
                                        )
                                        Text(
                                            "Gross: ₹${"%,.0f".format(grossPnL)} | Fees: ₹${"%.1f".format(estimatedCharges)}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Key Execution Metrics Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Contract Value",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    if (totalCapital > 0) "₹${"%,.0f".format(totalCapital)}" else "—",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Breakeven Price",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    if (breakevenPrice > 0) "₹${"%.2f".format(breakevenPrice)}" else "—",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Est. Charges",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    if (estimatedCharges > 0) "₹${"%.1f".format(estimatedCharges)}" else "—",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 4. Trade Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "TRADE STATUS / OUTCOME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(TradeStatus.entries) { status ->
                                FilterChip(
                                    selected = selectedStatus == status.name,
                                    onClick = { selectedStatus = status.name },
                                    label = { Text(status.displayName, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // 5. 📸 Chart Screenshots (Camera & Gallery attachment)
            item {
                ImageAttachmentSection(
                    imagePaths = attachedImages,
                    onImagesChanged = { attachedImages = it }
                )
            }

            // 6. Setup & Indicators
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "SETUP & INDICATORS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        OutlinedTextField(
                            value = selectedSetup,
                            onValueChange = { selectedSetup = it },
                            label = { Text("Primary Trading Setup") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_setup"),
                            singleLine = true
                        )

                        // Popular setup quick-picker chips
                        Text("Popular Setups:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(POPULAR_SETUPS) { setup ->
                                SuggestionChip(
                                    onClick = { selectedSetup = setup },
                                    label = { Text(setup, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Indicators Used (CPR, EMA, VWAP, RSI, MACD, Volume...):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        // Multi-select chips for indicators
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            POPULAR_INDICATORS.forEach { indicator ->
                                val isSelected = selectedIndicators.contains(indicator)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedIndicators = if (isSelected) {
                                            selectedIndicators.filter { it != indicator }
                                        } else {
                                            selectedIndicators + indicator
                                        }
                                    },
                                    label = { Text(indicator, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // 7. Psychology & Notes
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "PSYCHOLOGY & REVIEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        Text("Trading Emotion:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(Emotion.entries) { emotion ->
                                FilterChip(
                                    selected = selectedEmotion.equals(emotion.displayName, ignoreCase = true),
                                    onClick = { selectedEmotion = emotion.displayName },
                                    label = { Text("${emotion.emoji} ${emotion.displayName}", fontSize = 11.sp) }
                                )
                            }
                        }

                        Text("Mistake / Deviation:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(Mistake.entries) { mistake ->
                                FilterChip(
                                    selected = selectedMistake.equals(mistake.displayName, ignoreCase = true),
                                    onClick = { selectedMistake = mistake.displayName },
                                    label = { Text(mistake.displayName, fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Trade Notes & Post-Mortem Analysis") },
                            placeholder = { Text("Why did you take this trade? Market context, price action at entry, exit reasoning, learnings...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 90.dp)
                                .testTag("input_notes"),
                            minLines = 3
                        )
                    }
                }
            }

            // Save Trade Action Button
            item {
                Button(
                    onClick = {
                        val parsedEntry = entryPriceStr.toDoubleOrNull() ?: 0.0
                        val parsedExit = exitPriceStr.toDoubleOrNull() ?: 0.0
                        val parsedQty = quantityStr.toIntOrNull() ?: 1

                        val tradeToSave = TradeEntity(
                            id = existingTrade?.id ?: 0L,
                            instrument = selectedInstrument,
                            strikeOrSymbol = strikeOrSymbol.ifBlank { "$selectedInstrument $selectedOptionType" },
                            optionType = selectedOptionType,
                            direction = selectedDirection,
                            entryPrice = parsedEntry,
                            slPrice = slPriceStr.toDoubleOrNull() ?: 0.0,
                            target1 = target1Str.toDoubleOrNull() ?: 0.0,
                            target2 = target2Str.toDoubleOrNull() ?: 0.0,
                            target3 = target3Str.toDoubleOrNull() ?: 0.0,
                            exitPrice = parsedExit,
                            quantity = parsedQty,
                            grossPnL = grossPnL,
                            charges = estimatedCharges,
                            netPnL = netPnL,
                            points = points,
                            riskAmount = riskAmount,
                            plannedRR = plannedRR,
                            actualRR = actualRR,
                            status = selectedStatus,
                            setup = selectedSetup.ifBlank { "Price Action" },
                            indicators = selectedIndicators,
                            emotion = selectedEmotion,
                            mistake = selectedMistake,
                            notes = notes,
                            imageUris = attachedImages,
                            entryTimestamp = entryTimestamp,
                            exitTimestamp = if (parsedExit > 0) exitTimestamp else (existingTrade?.exitTimestamp ?: entryTimestamp)
                        )

                        viewModel.saveTrade(tradeToSave) {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("button_save_trade"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (existingTrade != null) "Update Trade Record" else "Save Trade to Journal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Interactive Date & Time Picker Dialogs
    if (showEntryDateTimePicker) {
        TradeDateTimePickerDialog(
            initialTimestamp = entryTimestamp,
            title = "Set Trade Entry Date & Time",
            onDismiss = { showEntryDateTimePicker = false },
            onConfirm = { chosenTimestamp ->
                entryTimestamp = chosenTimestamp
                if (exitTimestamp < chosenTimestamp) {
                    exitTimestamp = chosenTimestamp + (30 * 60 * 1000)
                }
                showEntryDateTimePicker = false
            }
        )
    }

    if (showExitDateTimePicker) {
        TradeDateTimePickerDialog(
            initialTimestamp = exitTimestamp,
            title = "Set Trade Exit Date & Time",
            onDismiss = { showExitDateTimePicker = false },
            onConfirm = { chosenTimestamp ->
                exitTimestamp = chosenTimestamp
                showExitDateTimePicker = false
            }
        )
    }
}
