package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradeViewModel
import com.example.util.TradeCalculations

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

    var entryPriceStr by remember { mutableStateOf(existingTrade?.entryPrice?.toString()?.removeSuffix(".0") ?: "") }
    var slPriceStr by remember { mutableStateOf(existingTrade?.slPrice?.takeIf { it > 0 }?.toString()?.removeSuffix(".0") ?: "") }
    var target1Str by remember { mutableStateOf(existingTrade?.target1?.takeIf { it > 0 }?.toString()?.removeSuffix(".0") ?: "") }
    var target2Str by remember { mutableStateOf(existingTrade?.target2?.takeIf { it > 0 }?.toString()?.removeSuffix(".0") ?: "") }
    var target3Str by remember { mutableStateOf(existingTrade?.target3?.takeIf { it > 0 }?.toString()?.removeSuffix(".0") ?: "") }
    var exitPriceStr by remember { mutableStateOf(existingTrade?.exitPrice?.takeIf { it > 0 }?.toString()?.removeSuffix(".0") ?: "") }
    var quantityStr by remember { mutableStateOf(existingTrade?.quantity?.toString() ?: "25") }
    var customChargesStr by remember { mutableStateOf(existingTrade?.charges?.takeIf { it > 0 }?.toString() ?: "") }

    var selectedStatus by remember { mutableStateOf(existingTrade?.status ?: "TARGET_HIT") }
    var selectedSetup by remember { mutableStateOf(existingTrade?.setup ?: "CPR Breakout") }
    var selectedIndicators by remember { mutableStateOf(existingTrade?.indicators ?: listOf("CPR", "VWAP")) }
    var selectedEmotion by remember { mutableStateOf(existingTrade?.emotion ?: "Disciplined") }
    var selectedMistake by remember { mutableStateOf(existingTrade?.mistake ?: "None (Followed Plan)") }
    var notes by remember { mutableStateOf(existingTrade?.notes ?: "") }
    var attachedImages by remember { mutableStateOf(existingTrade?.imageUris ?: emptyList()) }

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

            // 1. Instrument Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "SELECT INSTRUMENT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(Instrument.entries) { inst ->
                                FilterChip(
                                    selected = selectedInstrument.equals(inst.displayName, ignoreCase = true) ||
                                            selectedInstrument.equals(inst.name, ignoreCase = true),
                                    onClick = { onInstrumentSelected(inst) },
                                    label = { Text(inst.displayName, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Strike/Symbol & Option Type & Direction
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = strikeOrSymbol,
                            onValueChange = { strikeOrSymbol = it },
                            label = { Text("Symbol / Strike (e.g. 24800 CE, 52000 PE, FUT)") },
                            placeholder = { Text("e.g. 24800 CE") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_strike_symbol"),
                            singleLine = true
                        )

                        // Option Type row (CE, PE, FUT, EQ)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("CE", "PE", "FUT", "EQ").forEach { type ->
                                val isSelected = selectedOptionType == type
                                OutlinedButton(
                                    onClick = { selectedOptionType = type },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    )
                                ) {
                                    Text(
                                        type,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Direction row (BUY vs SELL)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { selectedDirection = "BUY" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedDirection == "BUY") ProfitGreen else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selectedDirection == "BUY") TradeBgDark else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("BUY / LONG", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { selectedDirection = "SELL" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedDirection == "SELL") LossRed else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selectedDirection == "SELL") TextPrimaryDark else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("SELL / SHORT", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Trade Prices & Quantity
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "PRICES & POSITION SIZE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = entryPriceStr,
                                onValueChange = { entryPriceStr = it },
                                label = { Text("Entry Price *") },
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
                        val step = when {
                            selectedInstrument.contains("Bank", ignoreCase = true) -> 15
                            selectedInstrument.contains("Nifty", ignoreCase = true) -> 25
                            selectedInstrument.contains("Sensex", ignoreCase = true) -> 10
                            selectedInstrument.contains("Crude", ignoreCase = true) -> 100
                            selectedInstrument.contains("Natural", ignoreCase = true) -> 1250
                            else -> 10
                        }

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
                            entryTimestamp = existingTrade?.entryTimestamp ?: System.currentTimeMillis(),
                            exitTimestamp = if (parsedExit > 0) System.currentTimeMillis() else (existingTrade?.exitTimestamp ?: System.currentTimeMillis())
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
}
