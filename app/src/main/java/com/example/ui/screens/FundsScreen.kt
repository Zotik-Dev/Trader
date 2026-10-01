package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.FundTransactionEntity
import com.example.model.PlatformFundSummary
import com.example.model.TradingPlatform
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.TradeViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundsScreen(
    viewModel: TradeViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val totalFundsSummary by viewModel.totalFundsSummary.collectAsState()
    val filteredTransactions by viewModel.filteredFundTransactions.collectAsState()
    val platformFilter by viewModel.fundPlatformFilter.collectAsState()
    val typeFilter by viewModel.fundTypeFilter.collectAsState()
    val searchQuery by viewModel.fundSearchQuery.collectAsState()
    val allTrades by viewModel.allTrades.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<FundTransactionEntity?>(null) }
    var presetPlatformForNew by remember { mutableStateOf<String?>(null) }
    var transactionToDelete by remember { mutableStateOf<FundTransactionEntity?>(null) }

    val currencyFormatter = remember {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 0
        format
    }

    val totalTradingPnL = remember(allTrades) {
        allTrades.sumOf { it.netPnL }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Broker Funds & Ledger",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            "Credited & Debited Amounts",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.exportFundsToCsv(context) },
                        modifier = Modifier.testTag("button_export_funds_csv")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export CSV")
                    }
                    IconButton(
                        onClick = {
                            editingTransaction = null
                            presetPlatformForNew = null
                            showAddEditDialog = true
                        },
                        modifier = Modifier.testTag("button_add_fund_top")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add Funds Record", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingTransaction = null
                    presetPlatformForNew = null
                    showAddEditDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Credit / Debit", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("fab_add_fund_transaction")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
        ) {
            // Section 1: Grand Capital Overview Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_funds_total_overview"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "NET TRADING CAPITAL",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        "Across all connected brokers",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    "${totalFundsSummary.totalTransactions} Records",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Large Net Capital Value
                        Text(
                            text = currencyFormatter.format(totalFundsSummary.netCapital),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (totalFundsSummary.netCapital >= 0) MaterialTheme.colorScheme.onSurface else LossRed
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Inflow & Outflow Side by Side
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Total Credited (Deposits)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = ProfitGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Total Credited",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "+ ${currencyFormatter.format(totalFundsSummary.totalCredited)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
                                )
                                Text(
                                    text = "Deposited to brokers",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Total Debited (Withdrawals)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = LossRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Total Debited",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "- ${currencyFormatter.format(totalFundsSummary.totalDebited)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LossRed
                                )
                                Text(
                                    text = "Withdrawn to bank",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Journal P&L Insight Link
                        if (allTrades.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Journal Realized Trading P&L",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            (if (totalTradingPnL >= 0) "+ " else "") + currencyFormatter.format(totalTradingPnL),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (totalTradingPnL >= 0) ProfitGreen else LossRed
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "Current Portfolio Value",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            currencyFormatter.format(totalFundsSummary.netCapital + totalTradingPnL),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Platform Breakdown Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "TRADING PLATFORMS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Zerodha • Groww • Sahi • Dhan",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 3: Platform Cards (Zerodha, Groww, Sahi, Dhan, etc.)
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(totalFundsSummary.platformSummaries) { summary ->
                        PlatformSummaryCard(
                            summary = summary,
                            currencyFormatter = currencyFormatter,
                            isSelected = platformFilter.equals(summary.platformName, ignoreCase = true),
                            onCardClick = {
                                if (platformFilter.equals(summary.platformName, ignoreCase = true)) {
                                    viewModel.setFundPlatformFilter("All")
                                } else {
                                    viewModel.setFundPlatformFilter(summary.platformName)
                                }
                            },
                            onAddClick = {
                                editingTransaction = null
                                presetPlatformForNew = summary.platformName
                                showAddEditDialog = true
                            }
                        )
                    }
                }
            }

            // Section 4: Filters & Search Bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setFundSearchQuery(it) },
                        placeholder = { Text("Search by remark, UTR, or amount...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setFundSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_search_funds"),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Platform Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        val platformOptions = listOf("All", "Zerodha", "Groww", "Sahi", "Dhan") +
                            totalFundsSummary.platformSummaries
                                .map { it.platformName }
                                .filter { it !in listOf("Zerodha", "Groww", "Sahi", "Dhan") }

                        items(platformOptions.distinct()) { platform ->
                            val isSelected = platformFilter.equals(platform, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setFundPlatformFilter(platform) },
                                label = { Text(platform, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                modifier = Modifier.testTag("filter_platform_${platform.lowercase()}")
                            )
                        }
                    }

                    // Transaction Type Chips (All, Credit, Debit)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("All", "All Types", null),
                            Triple("CREDIT", "Credited (Deposits) 🟢", ProfitGreen),
                            Triple("DEBIT", "Debited (Withdrawals) 🔴", LossRed)
                        ).forEach { (typeCode, label, tintColor) ->
                            val isSelected = typeFilter.equals(typeCode, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setFundTypeFilter(typeCode) },
                                label = {
                                    Text(
                                        label,
                                        fontSize = 11.sp,
                                        color = if (isSelected && tintColor != null) tintColor else Color.Unspecified
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("filter_type_${typeCode.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Section 5: Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "PASSBOOK & LEDGER HISTORY",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "${filteredTransactions.size} transactions",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 6: Transactions List or Empty State
            if (filteredTransactions.isEmpty()) {
                item {
                    EmptyFundsCard(
                        hasFilter = platformFilter != "All" || typeFilter != "All" || searchQuery.isNotEmpty(),
                        onResetFilters = {
                            viewModel.setFundPlatformFilter("All")
                            viewModel.setFundTypeFilter("All")
                            viewModel.setFundSearchQuery("")
                        },
                        onQuickAdd = { platform ->
                            editingTransaction = null
                            presetPlatformForNew = platform
                            showAddEditDialog = true
                        }
                    )
                }
            } else {
                items(filteredTransactions, key = { it.id }) { transaction ->
                    FundTransactionItem(
                        transaction = transaction,
                        currencyFormatter = currencyFormatter,
                        onEdit = {
                            editingTransaction = transaction
                            showAddEditDialog = true
                        },
                        onDelete = {
                            transactionToDelete = transaction
                        }
                    )
                }
            }
        }
    }

    // Add / Edit Transaction Dialog
    if (showAddEditDialog) {
        AddEditFundTransactionDialog(
            existing = editingTransaction,
            presetPlatform = presetPlatformForNew,
            onDismiss = { showAddEditDialog = false },
            onSave = { platform, type, amount, timestamp, mode, ref, notes ->
                viewModel.saveFundTransaction(
                    id = editingTransaction?.id ?: 0L,
                    platform = platform,
                    type = type,
                    amount = amount,
                    timestamp = timestamp,
                    paymentMode = mode,
                    referenceNumber = ref,
                    notes = notes
                )
                showAddEditDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction Record?") },
            text = {
                Text(
                    "Are you sure you want to delete this ${tx.type.lowercase()} record of ₹${tx.amount.toLong()} for ${tx.platform}?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFundTransaction(tx.id)
                        transactionToDelete = null
                    }
                ) {
                    Text("Delete", color = LossRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PlatformSummaryCard(
    summary: PlatformFundSummary,
    currencyFormatter: NumberFormat,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val platformInfo = summary.tradingPlatform
    Card(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCardClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) platformInfo.primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("card_platform_${summary.platformName.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) platformInfo.primaryColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(platformInfo.primaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = platformInfo.shortCode,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            summary.platformName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            "${summary.transactionCount} transactions",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = "Add Funds",
                        tint = platformInfo.primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Net Broker Capital",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                currencyFormatter.format(summary.netCapital),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (summary.netCapital >= 0) MaterialTheme.colorScheme.onSurface else LossRed
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Credited", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "+${currencyFormatter.format(summary.totalCredited)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfitGreen
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Debited", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "-${currencyFormatter.format(summary.totalDebited)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LossRed
                    )
                }
            }
        }
    }
}

@Composable
private fun FundTransactionItem(
    transaction: FundTransactionEntity,
    currencyFormatter: NumberFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isCredit = transaction.type.equals("CREDIT", ignoreCase = true)
    val platformInfo = TradingPlatform.fromString(transaction.platform)
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fund_tx_item_${transaction.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Platform Indicator Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(platformInfo.primaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = platformInfo.shortCode,
                        color = platformInfo.primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Date
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = transaction.platform,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCredit) ProfitGreen.copy(alpha = 0.15f) else LossRed.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isCredit) ProfitGreen else LossRed,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (isCredit) "CREDITED" else "DEBITED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isCredit) ProfitGreen else LossRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateFormatter.format(Date(transaction.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Amount
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = (if (isCredit) "+ " else "- ") + currencyFormatter.format(transaction.amount),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (isCredit) ProfitGreen else LossRed
                    )
                    Text(
                        text = transaction.paymentMode.ifBlank { "UPI" },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Reference or Notes if present
            if (transaction.referenceNumber.isNotBlank() || transaction.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (transaction.referenceNumber.isNotBlank()) {
                            Text(
                                text = "Ref / UTR: ${transaction.referenceNumber}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (transaction.notes.isNotBlank()) {
                            Text(
                                text = "Note: ${transaction.notes}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Action Icons
                    Row {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = LossRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = LossRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFundsCard(
    hasFilter: Boolean,
    onResetFilters: () -> Unit,
    onQuickAdd: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                if (hasFilter) "No Transactions Found" else "No Broker Ledger Records Yet",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                if (hasFilter) "Try adjusting your platform or type filter to view other records."
                else "Track the exact amount credited (deposits) and debited (withdrawals) for Zerodha, Groww, Sahi, and Dhan.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (hasFilter) {
                Button(
                    onClick = onResetFilters,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Clear All Filters")
                }
            } else {
                Text(
                    "Quick Start by adding an initial deposit:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Zerodha", "Groww", "Sahi", "Dhan").forEach { platform ->
                        OutlinedButton(
                            onClick = { onQuickAdd(platform) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(platform, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditFundTransactionDialog(
    existing: FundTransactionEntity?,
    presetPlatform: String?,
    onDismiss: () -> Unit,
    onSave: (platform: String, type: String, amount: Double, timestamp: Long, mode: String, ref: String, notes: String) -> Unit
) {
    val platforms = listOf("Zerodha", "Groww", "Sahi", "Dhan", "Angel One", "Upstox", "Other")

    var selectedPlatform by remember {
        mutableStateOf(existing?.platform ?: presetPlatform ?: "Zerodha")
    }
    var customPlatformName by remember {
        mutableStateOf(if (existing != null && existing.platform !in platforms) existing.platform else "")
    }
    var transactionType by remember {
        mutableStateOf(existing?.type ?: "CREDIT")
    }
    var amountText by remember {
        mutableStateOf(existing?.amount?.let { if (it % 1 == 0.0) it.toLong().toString() else it.toString() } ?: "")
    }
    var paymentMode by remember {
        mutableStateOf(existing?.paymentMode ?: "UPI")
    }
    var referenceNumber by remember {
        mutableStateOf(existing?.referenceNumber ?: "")
    }
    var notes by remember {
        mutableStateOf(existing?.notes ?: "")
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (existing == null) "Log Broker Fund Transaction" else "Edit Fund Transaction",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                // 1. Transaction Type Toggle (Credit vs Debit)
                item {
                    Text("TRANSACTION TYPE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Credited Button
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { transactionType = "CREDIT" }
                                .border(
                                    width = if (transactionType == "CREDIT") 2.dp else 1.dp,
                                    color = if (transactionType == "CREDIT") ProfitGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("radio_type_credit"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (transactionType == "CREDIT") ProfitGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = ProfitGreen)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Credited (Deposit)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                                Text("Add to Broker", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Debited Button
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { transactionType = "DEBIT" }
                                .border(
                                    width = if (transactionType == "DEBIT") 2.dp else 1.dp,
                                    color = if (transactionType == "DEBIT") LossRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("radio_type_debit"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (transactionType == "DEBIT") LossRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = LossRed)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Debited (Withdrawal)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LossRed)
                                Text("Payout to Bank", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // 2. Select Platform (Zerodha, Groww, Sahi, Dhan, etc.)
                item {
                    Text("TRADING PLATFORM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(platforms) { platform ->
                            val isSelected = selectedPlatform.equals(platform, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPlatform = platform },
                                label = { Text(platform, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.testTag("dialog_select_platform_${platform.lowercase()}")
                            )
                        }
                    }

                    if (selectedPlatform == "Other") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customPlatformName,
                            onValueChange = { customPlatformName = it },
                            label = { Text("Enter Platform Name") },
                            placeholder = { Text("e.g. Kotak Neo, Fyers") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 3. Amount Input & Quick Boosters
                item {
                    Text("AMOUNT (₹)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) {
                                amountText = input
                            }
                        },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp)) },
                        placeholder = { Text("e.g. 50000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_fund_amount")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Boosters
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val boosters = listOf(5000L, 10000L, 25000L, 50000L, 100000L)
                        items(boosters) { booster ->
                            AssistChip(
                                onClick = {
                                    val current = amountText.toDoubleOrNull() ?: 0.0
                                    val updated = current + booster
                                    amountText = if (updated % 1 == 0.0) updated.toLong().toString() else updated.toString()
                                },
                                label = { Text("+₹${booster / 1000}k", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // 4. Payment Mode
                item {
                    Text("PAYMENT MODE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf("UPI", "Net Banking", "IMPS / NEFT", "Bank Payout", "Cheque")
                        items(modes) { mode ->
                            val isSelected = paymentMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { paymentMode = mode },
                                label = { Text(mode, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // 5. Reference / UTR
                item {
                    OutlinedTextField(
                        value = referenceNumber,
                        onValueChange = { referenceNumber = it },
                        label = { Text("Reference / UTR / Order ID (Optional)") },
                        placeholder = { Text("e.g. UPI/123456789 or TXN-8901") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 6. Notes / Purpose
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Purpose (Optional)") },
                        placeholder = { Text("e.g. Expiry margin addition, monthly profit withdrawal") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Error text
                errorMessage?.let { error ->
                    item {
                        Text(error, color = LossRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // 7. Save / Cancel Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val amount = amountText.toDoubleOrNull()
                                if (amount == null || amount <= 0.0) {
                                    errorMessage = "Please enter a valid amount greater than 0"
                                    return@Button
                                }
                                val finalPlatform = if (selectedPlatform == "Other") {
                                    if (customPlatformName.isBlank()) {
                                        errorMessage = "Please enter the platform name"
                                        return@Button
                                    }
                                    customPlatformName.trim()
                                } else {
                                    selectedPlatform
                                }

                                onSave(
                                    finalPlatform,
                                    transactionType,
                                    amount,
                                    existing?.timestamp ?: System.currentTimeMillis(),
                                    paymentMode,
                                    referenceNumber,
                                    notes
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("button_save_fund_transaction"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (transactionType == "CREDIT") ProfitGreen else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                if (existing == null) {
                                    if (transactionType == "CREDIT") "Record Deposit" else "Record Withdrawal"
                                } else "Update Record",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
