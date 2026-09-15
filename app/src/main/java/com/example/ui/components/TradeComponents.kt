package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.model.DailyPnL
import com.example.ui.theme.*
import com.example.util.TradeCalculations
import java.io.File
import kotlin.math.abs
import kotlin.math.max

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "TARGET_HIT" -> Triple(ProfitGreenBg, ProfitGreen, "Target Hit")
        "SL_HIT" -> Triple(LossRedBg, LossRed, "SL Hit")
        "TRAIL_SL_HIT" -> Triple(AmberGoldBg, AmberGold, "Trail SL")
        "OPEN" -> Triple(ElectricBlueBg, ElectricBlue, "Open")
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Closed")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun DirectionBadge(direction: String, optionType: String = "") {
    val isBuy = direction.contains("BUY", ignoreCase = true)
    val color = if (isBuy) ProfitGreen else LossRed
    val bg = if (isBuy) ProfitGreenBg else LossRedBg

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (isBuy) "BUY" else "SELL",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        if (optionType.isNotBlank()) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = optionType,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PnLCard(
    netPnL: Double,
    grossPnL: Double,
    charges: Double,
    winRate: Double,
    totalTrades: Int,
    winTrades: Int,
    lossTrades: Int,
    modifier: Modifier = Modifier
) {
    val isProfit = netPnL >= 0
    val pnlColor = if (isProfit) ProfitGreen else LossRed

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("summary_pnl_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(pnlColor.copy(alpha = 0.35f)),
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
                        text = "TOTAL NET P&L",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = TradeCalculations.formatCurrency(netPnL),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = pnlColor
                    )
                }

                // Win rate badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (winRate >= 50) ProfitGreenBg else LossRedBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${"%.1f".format(winRate)}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (winRate >= 50) ProfitGreen else LossRed
                        )
                        Text(
                            text = "WIN RATE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(12.dp))

            // Sub metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Gross P&L", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        TradeCalculations.formatCurrency(grossPnL),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (grossPnL >= 0) ProfitGreen else LossRed
                    )
                }
                Column {
                    Text("Est. Charges", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "₹${"%.2f".format(charges)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text("Total Trades", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$totalTrades ($winTrades W / $lossTrades L)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun DailyPnLBarChart(
    dailyList: List<DailyPnL>,
    modifier: Modifier = Modifier
) {
    if (dailyList.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No trading days recorded yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    val maxAbs = max(dailyList.maxOfOrNull { abs(it.netPnL) } ?: 1.0, 100.0)

    Card(
        modifier = modifier.fillMaxWidth(),
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
                    text = "DAILY P&L TIMELINE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${dailyList.size} trading sessions",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas drawing positive and negative bars around centerline
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val width = size.width
                val height = size.height
                val midY = height / 2f
                val count = dailyList.size
                val barSlotWidth = width / count.toFloat()
                val barWidth = (barSlotWidth * 0.55f).coerceAtLeast(6f).coerceAtMost(36f)

                // Baseline
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(0f, midY),
                    end = Offset(width, midY),
                    strokeWidth = 1.5f
                )

                dailyList.forEachIndexed { index, day ->
                    val x = index * barSlotWidth + (barSlotWidth - barWidth) / 2f
                    val normalizedHeight = ((abs(day.netPnL) / maxAbs) * (height * 0.42f)).toFloat()
                    val isPos = day.netPnL >= 0
                    val barColor = if (isPos) ProfitGreen else LossRed

                    val top = if (isPos) midY - normalizedHeight else midY
                    val barH = normalizedHeight.coerceAtLeast(3f)

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, top),
                        size = Size(barWidth, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Date labels row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dailyList.take(6).forEach {
                    Text(
                        text = it.dateLabel,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun FullImageViewerDialog(
    imagePath: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TradeBgDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Chart Preview",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AsyncImage(
                    model = File(imagePath),
                    contentDescription = "Full Chart Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 450.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}
