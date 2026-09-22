package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeDateTimePickerDialog(
    initialTimestamp: Long = System.currentTimeMillis(),
    title: String = "Set Trade Date & Time",
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val initialCal = remember(initialTimestamp) {
        Calendar.getInstance().apply { timeInMillis = initialTimestamp }
    }

    var selectedYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) } // 0-11
    var selectedDay by remember { mutableIntStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }
    var selectedHour24 by remember { mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY)) } // 0-23
    var selectedMinute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Date, 1 = Time
    var isYearPickerExpanded by remember { mutableStateOf(false) }

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val lastYear = currentYear - 1

    // Build current computed timestamp
    val computedTimestamp = remember(selectedYear, selectedMonth, selectedDay, selectedHour24, selectedMinute) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, selectedDay.coerceIn(1, maxDay))
            set(Calendar.HOUR_OF_DAY, selectedHour24)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val previewDateFormatter = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }
    val previewTimeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val shortMonths = remember { DateFormatSymbols.getInstance(Locale.getDefault()).shortMonths }

    // Quick Year Presets
    val availableYears = remember {
        ((currentYear + 1) downTo (currentYear - 6)).toList()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("dialog_trade_date_time_picker"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Historical & custom trade date logging",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Selected Date & Time Live Display Pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = previewDateFormatter.format(computedTimestamp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = previewTimeFormatter.format(computedTimestamp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Preset Chips for Date & Last Year
                Text(
                    "QUICK JUMP PRESETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        SuggestionChip(
                            onClick = {
                                val now = Calendar.getInstance()
                                selectedYear = now.get(Calendar.YEAR)
                                selectedMonth = now.get(Calendar.MONTH)
                                selectedDay = now.get(Calendar.DAY_OF_MONTH)
                            },
                            label = { Text("Today", fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = {
                                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                                selectedYear = cal.get(Calendar.YEAR)
                                selectedMonth = cal.get(Calendar.MONTH)
                                selectedDay = cal.get(Calendar.DAY_OF_MONTH)
                            },
                            label = { Text("Yesterday", fontSize = 11.sp) }
                        )
                    }
                    item {
                        // Crucial requested shortcut: Last Year!
                        SuggestionChip(
                            onClick = {
                                selectedYear = lastYear
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (selectedYear == lastYear) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (selectedYear == lastYear) SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = MaterialTheme.colorScheme.primary
                            ) else null,
                            label = {
                                Text(
                                    "Last Year ($lastYear)",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedYear == lastYear) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedYear == lastYear) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) }
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = {
                                selectedYear = currentYear - 2
                            },
                            label = { Text("${currentYear - 2}", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Switcher (Date vs Time)
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .height(38.dp)
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("1. Select Date", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("2. Execution Time", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // TAB 0: DATE PICKER
                if (activeTab == 0) {
                    // Month & Year Selector Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (selectedMonth > 0) {
                                    selectedMonth--
                                } else {
                                    selectedMonth = 11
                                    selectedYear--
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month", modifier = Modifier.size(18.dp))
                        }

                        // Year & Month Clickable Dropdown Trigger
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isYearPickerExpanded = !isYearPickerExpanded }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${shortMonths.getOrElse(selectedMonth) { "" }} $selectedYear",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                if (isYearPickerExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                if (selectedMonth < 11) {
                                    selectedMonth++
                                } else {
                                    selectedMonth = 0
                                    selectedYear++
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month", modifier = Modifier.size(18.dp))
                        }
                    }

                    // Expandable Year Grid if user wants to directly jump to 2025, 2024, etc.
                    AnimatedVisibility(visible = isYearPickerExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(8.dp)
                        ) {
                            Text(
                                "CHOOSE YEAR:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(availableYears) { yr ->
                                    val isYrSelected = yr == selectedYear
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isYrSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                            .clickable {
                                                selectedYear = yr
                                                isYearPickerExpanded = false
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = yr.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isYrSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isYrSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Days of Week Header
                    val daysOfWeek = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        daysOfWeek.forEach { dayName ->
                            Text(
                                text = dayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Days of Month Grid
                    val daysInMonth = remember(selectedYear, selectedMonth) {
                        Calendar.getInstance().apply {
                            set(Calendar.YEAR, selectedYear)
                            set(Calendar.MONTH, selectedMonth)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }.getActualMaximum(Calendar.DAY_OF_MONTH)
                    }

                    val firstDayOffset = remember(selectedYear, selectedMonth) {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, selectedYear)
                            set(Calendar.MONTH, selectedMonth)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
                    }

                    val totalGridSlots = firstDayOffset + daysInMonth

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 210.dp)
                    ) {
                        items((0 until totalGridSlots).toList()) { slotIndex ->
                            if (slotIndex < firstDayOffset) {
                                Spacer(modifier = Modifier.size(36.dp))
                            } else {
                                val dayNumber = slotIndex - firstDayOffset + 1
                                val isSelected = dayNumber == selectedDay

                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent
                                        )
                                        .clickable { selectedDay = dayNumber },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // TAB 1: TIME PICKER
                if (activeTab == 1) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "MARKET TRADING SESSIONS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Market Time Shortcuts
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val marketPresets = listOf(
                                "09:15 AM (Open)" to Pair(9, 15),
                                "09:30 AM" to Pair(9, 30),
                                "10:15 AM" to Pair(10, 15),
                                "11:30 AM" to Pair(11, 30),
                                "01:30 PM (Euro)" to Pair(13, 30),
                                "02:30 PM" to Pair(14, 30),
                                "03:15 PM (Close)" to Pair(15, 15),
                                "MCX Evening (07:00 PM)" to Pair(19, 0)
                            )
                            items(marketPresets) { (label, time) ->
                                val (h, m) = time
                                val isChosen = selectedHour24 == h && selectedMinute == m
                                SuggestionChip(
                                    onClick = {
                                        selectedHour24 = h
                                        selectedMinute = m
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isChosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = if (isChosen) SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.primary) else null,
                                    label = {
                                        Text(
                                            label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Digital Clock Display with Steppers
                        val isPm = selectedHour24 >= 12
                        val displayHour12 = when {
                            selectedHour24 == 0 -> 12
                            selectedHour24 > 12 -> selectedHour24 - 12
                            else -> selectedHour24
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Hour Box
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        var newH = selectedHour24 + 1
                                        if (newH > 23) newH = 0
                                        selectedHour24 = newH
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Hour +1")
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "%02d".format(displayHour12),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        var newH = selectedHour24 - 1
                                        if (newH < 0) newH = 23
                                        selectedHour24 = newH
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Hour -1")
                                }
                                Text("HOUR", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Text(
                                ":",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            )

                            // Minute Box
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        var newM = selectedMinute + 5
                                        if (newM >= 60) newM = 0
                                        selectedMinute = newM
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Minute +5")
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "%02d".format(selectedMinute),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        var newM = selectedMinute - 5
                                        if (newM < 0) newM = 55
                                        selectedMinute = newM
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minute -5")
                                }
                                Text("MIN", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // AM/PM Selector
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (!isPm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            if (isPm) selectedHour24 -= 12
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "AM",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isPm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isPm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            if (!isPm) selectedHour24 += 12
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "PM",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(computedTimestamp)
                        },
                        modifier = Modifier.testTag("button_confirm_date_time")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Date & Time", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
