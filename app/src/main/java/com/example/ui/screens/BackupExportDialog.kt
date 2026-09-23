package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.LossRed
import com.example.ui.viewmodel.TradeViewModel

@Composable
fun BackupExportDialog(
    viewModel: TradeViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showClearConfirm by remember { mutableStateOf(false) }

    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreBackup(context, uri)
            onDismiss()
        }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importFromCsv(context, uri)
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Data & Backup Tools",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    "Export your journal for Excel or create an offline backup to restore anytime.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // CSV Export
                ToolOption(
                    icon = Icons.Default.FileDownload,
                    title = "Export to CSV (Excel)",
                    subtitle = "Generates a clean .csv spreadsheet with all trade metrics and calculations",
                    onClick = {
                        viewModel.exportToCsv(context)
                        onDismiss()
                    },
                    tag = "dialog_export_csv"
                )

                // CSV Import
                ToolOption(
                    icon = Icons.Default.UploadFile,
                    title = "Import from CSV File",
                    subtitle = "Load trades from CSV with date, time, call/put, strike, prices, and quantities",
                    onClick = {
                        importCsvLauncher.launch("*/*")
                    },
                    tag = "dialog_import_csv"
                )

                // JSON Backup
                ToolOption(
                    icon = Icons.Default.Backup,
                    title = "Create Full Backup (JSON)",
                    subtitle = "Backs up all trades, setups, and notes into an offline file",
                    onClick = {
                        viewModel.exportBackup(context)
                        onDismiss()
                    },
                    tag = "dialog_create_backup"
                )

                // Restore
                ToolOption(
                    icon = Icons.Default.Restore,
                    title = "Restore from Backup File",
                    subtitle = "Import trades from a previously saved JSON backup",
                    onClick = {
                        restoreFileLauncher.launch("application/json")
                    },
                    tag = "dialog_restore_backup"
                )

                // Clear All
                ToolOption(
                    icon = Icons.Default.DeleteForever,
                    title = "Clear All Trade Records",
                    subtitle = "Permanently deletes all logged trades from local database",
                    isDestructive = true,
                    onClick = { showClearConfirm = true },
                    tag = "dialog_clear_all"
                )
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Trades?") },
            text = { Text("Are you sure you want to delete all trades from your local journal database? Make sure you have exported a backup first.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllTrades()
                        showClearConfirm = false
                        onDismiss()
                    }
                ) {
                    Text("Clear Everything", color = LossRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ToolOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDestructive) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isDestructive) LossRed else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDestructive) LossRed else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
