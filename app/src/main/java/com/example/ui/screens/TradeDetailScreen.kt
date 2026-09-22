package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.TradeEntity
import com.example.ui.components.DirectionBadge
import com.example.ui.components.FullImageViewerDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradeViewModel
import com.example.util.TradeCalculations
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TradeDetailScreen(
    tradeId: Long,
    viewModel: TradeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val allTrades by viewModel.allTrades.collectAsState()
    val trade = remember(tradeId, allTrades) { allTrades.find { it.id == tradeId } }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var fullImagePreviewPath by remember { mutableStateOf<String?>(null) }
    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()) }

    if (trade == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Trade not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val isProfit = trade.netPnL >= 0
    val pnlColor = if (isProfit) ProfitGreen else LossRed

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trade #${trade.id}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("button_detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateToEdit(trade.id) },
                        modifier = Modifier.testTag("button_detail_edit")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("button_detail_delete")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LossRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
            // Hero P&L Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(pnlColor.copy(alpha = 0.5f)),
                        width = 1.dp
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = dateFormatter.format(Date(trade.entryTimestamp)),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    DirectionBadge(direction = trade.direction, optionType = trade.optionType)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = trade.strikeOrSymbol,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            StatusBadge(status = trade.status)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text("NET REALIZED P&L", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    TradeCalculations.formatCurrency(trade.netPnL),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = pnlColor
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("POINTS", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    TradeCalculations.formatPoints(trade.points),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (trade.points >= 0) ProfitGreen else LossRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Gross: ${TradeCalculations.formatCurrency(trade.grossPnL)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Charges: ₹${"%.2f".format(trade.charges)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Qty: ${trade.quantity}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // Attached Chart Images Carousel
            if (trade.imageUris.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "ATTACHED CHARTS (${trade.imageUris.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(trade.imageUris) { path ->
                                    Box(
                                        modifier = Modifier
                                            .size(110.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                            .clickable { fullImagePreviewPath = path }
                                    ) {
                                        AsyncImage(
                                            model = File(path),
                                            contentDescription = "Trade Chart",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Tap image to view full screen", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Prices & Execution Details
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "EXECUTION & PRICE TARGETS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        DetailRow("Entry Price", "₹${trade.entryPrice}")
                        DetailRow("Exit Price", if (trade.exitPrice > 0) "₹${trade.exitPrice}" else "Open")
                        DetailRow("Trade Entry Date & Time", dateFormatter.format(Date(trade.entryTimestamp)))
                        if (trade.exitPrice > 0 && trade.exitTimestamp > 0) {
                            DetailRow("Trade Exit Date & Time", dateFormatter.format(Date(trade.exitTimestamp)))
                        }
                        DetailRow("Stop Loss (SL)", if (trade.slPrice > 0) "₹${trade.slPrice}" else "Not set")
                        DetailRow("Target 1", if (trade.target1 > 0) "₹${trade.target1}" else "Not set")
                        if (trade.target2 > 0) DetailRow("Target 2", "₹${trade.target2}")
                        if (trade.target3 > 0) DetailRow("Target 3", "₹${trade.target3}")

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        DetailRow("Risk Amount", "₹${"%.1f".format(trade.riskAmount)}")
                        DetailRow("Planned R:R", TradeCalculations.formatRR(trade.plannedRR))
                        DetailRow("Achieved R:R", TradeCalculations.formatRR(trade.actualRR))
                    }
                }
            }

            // Technical Setup & Indicators
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "TECHNICAL SETUP & INDICATORS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Setup: ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    trade.setup,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (trade.indicators.isNotEmpty()) {
                            Text("Indicators:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                trade.indicators.forEach { ind ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(ind, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Psychology & Learnings
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "PSYCHOLOGY & POST-MORTEM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        DetailRow("Emotion", trade.emotion)
                        DetailRow("Mistake / Deviation", trade.mistake)

                        if (trade.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Notes:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(12.dp)
                            ) {
                                Text(trade.notes, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Trade?") },
            text = { Text("Are you sure you want to permanently delete Trade #${trade.id}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTrade(trade)
                        showDeleteConfirm = false
                        onNavigateBack()
                    }
                ) {
                    Text("Delete", color = LossRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (fullImagePreviewPath != null) {
        FullImageViewerDialog(
            imagePath = fullImagePreviewPath!!,
            onDismiss = { fullImagePreviewPath = null }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
