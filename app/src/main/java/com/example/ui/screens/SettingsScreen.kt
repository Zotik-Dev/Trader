package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppColorTheme
import com.example.model.ThemeMode
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.TradeViewModel
import com.example.util.BackupManager
import com.example.util.SettingsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TradeViewModel,
    settingsManager: SettingsManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val colorTheme by settingsManager.colorTheme.collectAsState()
    val themeMode by settingsManager.themeMode.collectAsState()
    val isAppLockEnabled by settingsManager.isAppLockEnabled.collectAsState()
    val hasPinSet by settingsManager.hasPinSet.collectAsState()
    val allTrades by viewModel.allTrades.collectAsState()

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.restoreBackup(context, it) }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importFromCsv(context, it) }
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings & Preferences",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("button_settings_back")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Section 1: Color Customization
            item {
                SettingsSectionCard(title = "APP COLOR THEME", icon = Icons.Default.Palette) {
                    Text(
                        "Choose your primary trading accent color:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(AppColorTheme.entries) { theme ->
                            val isSelected = colorTheme == theme
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { settingsManager.setColorTheme(theme) }
                                    .padding(4.dp)
                                    .testTag("color_theme_${theme.name.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(theme.primaryColor)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    theme.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Theme Appearance Mode
            item {
                SettingsSectionCard(title = "APPEARANCE MODE", icon = Icons.Default.Brightness4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val isSelected = themeMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { settingsManager.setThemeMode(mode) },
                                label = { Text(mode.displayName.split(" ").first(), fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Section 4: App Lock (PIN Security)
            item {
                SettingsSectionCard(title = "SECURITY & APP LOCK", icon = Icons.Default.Security) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "App Lock Protection",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (isAppLockEnabled) "App requires 4-digit passcode upon entry"
                                else "Protect your trading records with a 4-digit PIN",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (hasPinSet) {
                                        settingsManager.setAppLockEnabled(true)
                                    } else {
                                        showPinSetupDialog = true
                                    }
                                } else {
                                    settingsManager.setAppLockEnabled(false)
                                    Toast.makeText(context, "App lock disabled", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("switch_app_lock")
                        )
                    }

                    if (isAppLockEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showChangePinDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("button_change_pin")
                            ) {
                                Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change PIN", fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = {
                                    settingsManager.lockApp()
                                    Toast.makeText(context, "App locked!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("button_lock_now")
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lock Now", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Section 5: Offline Export & Tools
            item {
                SettingsSectionCard(title = "OFFLINE BACKUP & DATA EXPORT", icon = Icons.Default.SaveAlt) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.exportBackup(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("button_export_json")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Offline JSON Backup File")
                        }

                        OutlinedButton(
                            onClick = { restoreFileLauncher.launch("application/json") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("button_restore_json")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore from JSON Backup File")
                        }

                        OutlinedButton(
                            onClick = { importCsvLauncher.launch("*/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("button_import_csv")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import Trades from CSV File")
                        }

                        OutlinedButton(
                            onClick = { viewModel.exportToCsv(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("button_export_csv_settings")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Trades to CSV / Excel")
                        }

                        OutlinedButton(
                            onClick = { viewModel.exportFundsToCsv(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("button_export_funds_csv_settings")
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Broker Funds Ledger to CSV")
                        }

                        TextButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("button_clear_all_data")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = LossRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear All Trade Records", color = LossRed)
                        }
                    }
                }
            }
        }
    }

    // PIN Setup Dialog
    if (showPinSetupDialog) {
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            title = { Text("Set 4-Digit Passcode") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter a 4-digit PIN to secure your Trading Journal records:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                        label = { Text("4-Digit PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                        label = { Text("Confirm 4-Digit PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    pinError?.let {
                        Text(it, color = LossRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length != 4) {
                            pinError = "PIN must be exactly 4 digits."
                        } else if (newPin != confirmPin) {
                            pinError = "PINs do not match."
                        } else {
                            settingsManager.setPin(newPin)
                            showPinSetupDialog = false
                            Toast.makeText(context, "App Lock enabled with 4-digit PIN!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Set & Enable Lock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        var currentPin by remember { mutableStateOf("") }
        var updatedPin by remember { mutableStateOf("") }
        var changePinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change Passcode") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) currentPin = it },
                        label = { Text("Current 4-Digit PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = updatedPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) updatedPin = it },
                        label = { Text("New 4-Digit PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    changePinError?.let {
                        Text(it, color = LossRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (updatedPin.length != 4) {
                            changePinError = "New PIN must be exactly 4 digits."
                        } else if (!settingsManager.changePin(currentPin, updatedPin)) {
                            changePinError = "Current PIN is incorrect."
                        } else {
                            showChangePinDialog = false
                            Toast.makeText(context, "Passcode updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Update Passcode")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Trades?") },
            text = {
                Text(
                    "Are you sure you want to delete all trades from your local journal database? This action cannot be undone unless you have a backup.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllTrades()
                        showClearConfirm = false
                        Toast.makeText(context, "All trade records cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("Delete All Trades")
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
fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }
            content()
        }
    }
}
