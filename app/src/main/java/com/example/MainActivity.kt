package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TradeViewModel

sealed class Screen {
    object Dashboard : Screen()
    object History : Screen()
    object Analytics : Screen()
    data class AddEdit(val tradeId: Long? = null) : Screen()
    data class Detail(val tradeId: Long) : Screen()
}

enum class NavigationTab(val label: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "tab_dashboard"),
    HISTORY("Trades", Icons.Default.FormatListBulleted, "tab_trades"),
    ANALYTICS("Analytics", Icons.Default.BarChart, "tab_analytics")
}

class MainActivity : ComponentActivity() {
    private val viewModel: TradeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TradingJournalApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TradingJournalApp(viewModel: TradeViewModel) {
    var backStack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Dashboard)) }
    val currentScreen = backStack.lastOrNull() ?: Screen.Dashboard

    var currentTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }
    var showBackupDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage by viewModel.userMessage.collectAsState()

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Handle back press
    BackHandler(enabled = backStack.size > 1) {
        backStack = backStack.dropLast(1)
        when (val top = backStack.lastOrNull()) {
            is Screen.Dashboard -> currentTab = NavigationTab.DASHBOARD
            is Screen.History -> currentTab = NavigationTab.HISTORY
            is Screen.Analytics -> currentTab = NavigationTab.ANALYTICS
            else -> {}
        }
    }

    fun navigateTo(screen: Screen) {
        backStack = backStack + screen
        when (screen) {
            is Screen.Dashboard -> currentTab = NavigationTab.DASHBOARD
            is Screen.History -> currentTab = NavigationTab.HISTORY
            is Screen.Analytics -> currentTab = NavigationTab.ANALYTICS
            else -> {}
        }
    }

    fun switchTab(tab: NavigationTab) {
        currentTab = tab
        val screen = when (tab) {
            NavigationTab.DASHBOARD -> Screen.Dashboard
            NavigationTab.HISTORY -> Screen.History
            NavigationTab.ANALYTICS -> Screen.Analytics
        }
        backStack = listOf(screen)
    }

    val isRootTab = currentScreen is Screen.Dashboard || currentScreen is Screen.History || currentScreen is Screen.Analytics

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (isRootTab) {
                NavigationBar(
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { switchTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val screen = currentScreen) {
                is Screen.Dashboard -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToAddTrade = { navigateTo(Screen.AddEdit()) },
                        onNavigateToHistory = { switchTab(NavigationTab.HISTORY) },
                        onNavigateToAnalytics = { switchTab(NavigationTab.ANALYTICS) },
                        onOpenBackupDialog = { showBackupDialog = true }
                    )
                }
                is Screen.History -> {
                    TradeListScreen(
                        viewModel = viewModel,
                        onNavigateToTradeDetail = { tradeId -> navigateTo(Screen.Detail(tradeId)) },
                        onNavigateToAddTrade = { navigateTo(Screen.AddEdit()) }
                    )
                }
                is Screen.Analytics -> {
                    AnalyticsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { switchTab(NavigationTab.DASHBOARD) }
                    )
                }
                is Screen.AddEdit -> {
                    AddEditTradeScreen(
                        viewModel = viewModel,
                        existingTradeId = screen.tradeId,
                        onNavigateBack = {
                            if (backStack.size > 1) {
                                backStack = backStack.dropLast(1)
                            } else {
                                backStack = listOf(Screen.Dashboard)
                            }
                        }
                    )
                }
                is Screen.Detail -> {
                    TradeDetailScreen(
                        tradeId = screen.tradeId,
                        viewModel = viewModel,
                        onNavigateBack = {
                            if (backStack.size > 1) {
                                backStack = backStack.dropLast(1)
                            } else {
                                backStack = listOf(Screen.Dashboard)
                            }
                        },
                        onNavigateToEdit = { id -> navigateTo(Screen.AddEdit(id)) }
                    )
                }
            }
        }
    }

    if (showBackupDialog) {
        BackupExportDialog(
            viewModel = viewModel,
            onDismiss = { showBackupDialog = false }
        )
    }
}
