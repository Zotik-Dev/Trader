package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradeViewModel
import com.example.util.TradeCalculations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: TradeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val instrumentAnalytics by viewModel.instrumentAnalytics.collectAsState()
    val setupAnalytics by viewModel.setupAnalytics.collectAsState()
    val emotionAnalytics by viewModel.emotionAnalytics.collectAsState()
    val monthlyPnLList by viewModel.monthlyPnLList.collectAsState()
    val yearlyPnLList by viewModel.yearlyPnLList.collectAsState()
    val summary by viewModel.summary.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // High-Level Win/Loss Ratio Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "WIN / LOSS RATIO & EXPECTANCY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Visual Win / Loss Bar
                        val total = (summary.winTrades + summary.lossTrades).coerceAtLeast(1)
                        val winRatio = summary.winTrades.toFloat() / total

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(winRatio.coerceAtLeast(0.01f))
                                    .background(ProfitGreen)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight((1f - winRatio).coerceAtLeast(0.01f))
                                    .background(LossRed)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Wins: ${summary.winTrades} (${"%.1f".format(summary.winRate)}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            )
                            Text(
                                "Losses: ${summary.lossTrades}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = LossRed
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Avg Win", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(TradeCalculations.formatCurrency(summary.averageWin), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                            }
                            Column {
                                Text("Avg Loss", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(TradeCalculations.formatCurrency(-summary.averageLoss), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LossRed)
                            }
                            Column {
                                Text("Profit Factor", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("%.2f".format(summary.profitFactor), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Instrument Analytics
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "INSTRUMENT ANALYTICS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (instrumentAnalytics.isEmpty()) {
                            Text("No trades recorded", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            instrumentAnalytics.forEach { item ->
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item.instrumentName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            TradeCalculations.formatCurrency(item.netPnL),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.netPnL >= 0) ProfitGreen else LossRed
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
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
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            "${item.totalTrades} trades • ${item.winRate.toInt()}% Win",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            }
                        }
                    }
                }
            }

            // Setup Analytics
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "SETUP ANALYTICS & WIN RATES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (setupAnalytics.isEmpty()) {
                            Text("No setups recorded", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            setupAnalytics.forEach { setup ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.4f)) {
                                        Text(setup.setupName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${setup.totalTrades} trades • ${setup.winRate.toInt()}% win rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        TradeCalculations.formatCurrency(setup.netPnL),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (setup.netPnL >= 0) ProfitGreen else LossRed
                                    )
                                }
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            }
                        }
                    }
                }
            }

            // Psychology & Emotion Impact
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "EMOTION & PSYCHOLOGY IMPACT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (emotionAnalytics.isEmpty()) {
                            Text("No emotion data recorded", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            emotionAnalytics.forEach { emo ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(emo.emoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(emo.emotionName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text("${emo.totalTrades} trades • ${emo.winRate.toInt()}% win", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Text(
                                        TradeCalculations.formatCurrency(emo.netPnL),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (emo.netPnL >= 0) ProfitGreen else LossRed
                                    )
                                }
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            }
                        }
                    }
                }
            }

            // Yearly P&L Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "YEARLY P&L PERFORMANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (yearlyPnLList.isEmpty()) {
                            Text("No yearly data yet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            yearlyPnLList.forEach { y ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Year ${y.yearLabel}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("${y.tradeCount} trades • ${y.winRate.toInt()}% Win Rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        TradeCalculations.formatCurrency(y.netPnL),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (y.netPnL >= 0) ProfitGreen else LossRed
                                    )
                                }
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            }
                        }
                    }
                }
            }

            // Monthly P&L Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "MONTHLY P&L PERFORMANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (monthlyPnLList.isEmpty()) {
                            Text("No monthly data yet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            monthlyPnLList.forEach { m ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(m.monthLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${m.tradeCount} trades • ${m.winRate.toInt()}% Win", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        TradeCalculations.formatCurrency(m.netPnL),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (m.netPnL >= 0) ProfitGreen else LossRed
                                    )
                                }
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
}
