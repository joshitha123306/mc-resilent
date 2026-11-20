package com.example.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.MainViewModel
import com.example.ui.components.IncomingSosBanner
import com.example.ui.components.SosEmergencyDialog
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DiscoveryScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MeshScreen
import com.example.ui.screens.SettingsScreen

sealed class TopTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : TopTab("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Discover : TopTab(
        "discover",
        "Discover",
        Icons.Filled.BluetoothSearching,
        Icons.Outlined.BluetoothSearching
    )
    data object Chats : TopTab(
        "chats",
        "Chats",
        Icons.AutoMirrored.Filled.Chat,
        Icons.AutoMirrored.Outlined.Chat
    )
    data object Mesh : TopTab("mesh", "Mesh", Icons.Filled.Hub, Icons.Outlined.Hub)
    data object Settings : TopTab(
        "settings",
        "Settings",
        Icons.Filled.Settings,
        Icons.Outlined.Settings
    )
}

private const val ROUTE_CHAT = "chat/{peerAddress}/{peerName}"

private fun chatRoute(peerAddress: String, peerName: String): String {
    return "chat/${Uri.encode(peerAddress)}/${Uri.encode(peerName)}"
}

@Composable
fun ResilientNavHost(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSosDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val tabs = remember {
        listOf(TopTab.Home, TopTab.Discover, TopTab.Chats, TopTab.Mesh, TopTab.Settings)
    }
    val showTopLevelNav = currentRoute != null && tabs.any { it.route == currentRoute }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (showSosDialog) {
        SosEmergencyDialog(
            connectedCount = uiState.connectedPeers.size,
            onDismiss = { showSosDialog = false },
            onSendSos = { note ->
                showSosDialog = false
                viewModel.sendEmergencySos(note)
            }
        )
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openChat: (String, String) -> Unit = { address, name ->
        navController.navigate(chatRoute(address, name))
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp && showTopLevelNav

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showTopLevelNav && !useRail) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    val iconVector = if (selected) tab.selectedIcon else tab.unselectedIcon
                                    if (tab == TopTab.Discover && uiState.connectedPeers.isNotEmpty()) {
                                        BadgedBox(badge = {
                                            Badge { Text("${uiState.connectedPeers.size}") }
                                        }) {
                                            Icon(iconVector, contentDescription = tab.label)
                                        }
                                    } else {
                                        Icon(iconVector, contentDescription = tab.label)
                                    }
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    AnimatedVisibility(visible = uiState.activeIncomingSos.isNotEmpty()) {
                        IncomingSosBanner(
                            activeAlerts = uiState.activeIncomingSos,
                            onAcknowledge = { viewModel.acknowledgeSos(it) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = TopTab.Home.route
                        ) {
                            composable(TopTab.Home.route) {
                                HomeScreen(
                                    uiState = uiState,
                                    onRequestPermissions = onRequestPermissions,
                                    onOpenDiscovery = { navigateToTab(TopTab.Discover.route) },
                                    onOpenChat = openChat,
                                    onOpenHistory = { navigateToTab(TopTab.Chats.route) },
                                    onOpenMesh = { navigateToTab(TopTab.Mesh.route) },
                                    onTriggerSosClick = { showSosDialog = true }
                                )
                            }
                            composable(TopTab.Discover.route) {
                                DiscoveryScreen(
                                    uiState = uiState,
                                    onRequestPermissions = onRequestPermissions,
                                    onEnableBluetooth = onEnableBluetooth,
                                    onMakeDiscoverable = onMakeDiscoverable,
                                    onStartScan = { viewModel.startDiscovery() },
                                    onStopScan = { viewModel.stopDiscovery() },
                                    onConnectPeer = { viewModel.connectPeer(it) },
                                    onDisconnectPeer = { viewModel.disconnectPeer(it) },
                                    onOpenChat = openChat
                                )
                            }
                            composable(TopTab.Chats.route) {
                                HistoryScreen(
                                    uiState = uiState,
                                    onOpenChat = openChat,
                                    onDeleteConversation = { viewModel.deleteConversation(it) },
                                    onClearAllHistory = { viewModel.clearAllHistory() }
                                )
                            }
                            composable(TopTab.Mesh.route) {
                                MeshScreen(
                                    uiState = uiState,
                                    onToggleAutoRelay = { viewModel.setAutoRelay(it) },
                                    onChangeMaxHops = { viewModel.setMaxHopLimit(it) }
                                )
                            }
                            composable(TopTab.Settings.route) {
                                SettingsScreen(
                                    uiState = uiState,
                                    onUpdateCallsign = { viewModel.updateCallsign(it) },
                                    onToggleAutoAccept = { viewModel.setAutoAccept(it) },
                                    onToggleVibrateSos = { viewModel.setVibrateOnSos(it) },
                                    onClearAllHistory = { viewModel.clearAllHistory() }
                                )
                            }
                            composable(
                                route = ROUTE_CHAT,
                                arguments = listOf(
                                    navArgument("peerAddress") { type = NavType.StringType },
                                    navArgument("peerName") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val addr = Uri.decode(backStackEntry.arguments?.getString("peerAddress").orEmpty())
                                val name = Uri.decode(backStackEntry.arguments?.getString("peerName").orEmpty())
                                val messages by viewModel.observeChatMessages(addr)
                                    .collectAsStateWithLifecycle(initialValue = emptyList())

                                ChatScreen(
                                    peerAddress = addr,
                                    peerName = name,
                                    messages = messages,
                                    uiState = uiState,
                                    onBack = { navController.popBackStack() },
                                    onReconnect = { viewModel.connectPeer(it) },
                                    onSendMessage = { text ->
                                        viewModel.sendChatMessage(addr, name, text)
                                    },
                                    onTriggerSos = { showSosDialog = true },
                                    onAcknowledgeSos = { viewModel.acknowledgeSos(it) },
                                    onCancelOutgoingSos = { viewModel.cancelOutgoingSos(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
