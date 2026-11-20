package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.BluetoothPeer
import com.example.model.PeerConnectionStatus
import com.example.ui.ResilientUiState
import com.example.ui.components.BluetoothPermissionCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.SignalGreenContainerLight

@Composable
fun DiscoveryScreen(
    uiState: ResilientUiState,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectPeer: (String) -> Unit,
    onDisconnectPeer: (String) -> Unit,
    onOpenChat: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Bluetooth Device Discovery",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Open Resilient Connect on both smartphones to discover and connect over Bluetooth Classic RFCOMM.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!uiState.isBluetoothSupported) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "This device or emulator does not expose a Bluetooth hardware adapter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            if (!uiState.hasPermissions) {
                item {
                    BluetoothPermissionCard(onRequestPermissions = onRequestPermissions)
                }
            }

            // Radio & Scan Controls
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isBluetoothEnabled) {
                                        Icons.Filled.BluetoothConnected
                                    } else {
                                        Icons.Filled.BluetoothDisabled
                                    },
                                    contentDescription = "Bluetooth state",
                                    tint = if (uiState.isBluetoothEnabled) SignalGreen else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = if (uiState.isBluetoothEnabled) {
                                        if (uiState.isServerListening) "Bluetooth ON · RFCOMM Ready" else "Bluetooth ON"
                                    } else {
                                        "Bluetooth OFF"
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            if (uiState.isDiscoverable) {
                                StatusPill(
                                    label = "Visible",
                                    icon = Icons.Filled.Visibility,
                                    containerColor = SignalGreenContainerLight,
                                    contentColor = Color(0xFF065F46)
                                )
                            }
                        }

                        if (uiState.isDiscovering) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!uiState.isBluetoothEnabled) {
                                Button(
                                    onClick = onEnableBluetooth,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("enable_bluetooth_button")
                                ) {
                                    Icon(Icons.Filled.Bluetooth, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Enable Bluetooth")
                                }
                            } else {
                                Button(
                                    onClick = if (uiState.isDiscovering) onStopScan else onStartScan,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("scan_devices_button")
                                ) {
                                    Icon(Icons.Filled.BluetoothSearching, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (uiState.isDiscovering) "Stop Scan" else "Scan Nearby")
                                }

                                OutlinedButton(
                                    onClick = onMakeDiscoverable,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("make_discoverable_button")
                                ) {
                                    Icon(Icons.Filled.Visibility, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Discoverable")
                                }
                            }
                        }
                    }
                }
            }

            // Connected Peers Section
            if (uiState.connectedPeers.isNotEmpty()) {
                item {
                    Text(
                        text = "Connected Devices (${uiState.connectedPeers.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(uiState.connectedPeers, key = { "conn_${it.address}" }) { peer ->
                    PeerRowCard(
                        peer = peer,
                        onConnect = { onConnectPeer(peer.address) },
                        onDisconnect = { onDisconnectPeer(peer.address) },
                        onOpenChat = { onOpenChat(peer.address, peer.name) }
                    )
                }
            }

            // Discovered Nearby Devices Section
            item {
                Text(
                    text = "Nearby Discovered Devices (${uiState.discoveredPeers.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (uiState.discoveredPeers.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (uiState.isDiscovering) {
                                "Scanning for nearby Bluetooth devices..."
                            } else {
                                "Tap 'Scan Nearby' to discover nearby Android phones. Make sure the other phone tapped 'Discoverable'."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(uiState.discoveredPeers, key = { "disc_${it.address}" }) { peer ->
                    PeerRowCard(
                        peer = peer,
                        onConnect = { onConnectPeer(peer.address) },
                        onDisconnect = { onDisconnectPeer(peer.address) },
                        onOpenChat = { onOpenChat(peer.address, peer.name) }
                    )
                }
            }

            // Paired Devices Section
            item {
                Text(
                    text = "Paired Devices (${uiState.pairedPeers.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (uiState.pairedPeers.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No paired Bluetooth devices found yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(uiState.pairedPeers, key = { "paired_${it.address}" }) { peer ->
                    PeerRowCard(
                        peer = peer,
                        onConnect = { onConnectPeer(peer.address) },
                        onDisconnect = { onDisconnectPeer(peer.address) },
                        onOpenChat = { onOpenChat(peer.address, peer.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PeerRowCard(
    peer: BluetoothPeer,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenChat: () -> Unit
) {
    val isConnected = peer.status == PeerConnectionStatus.CONNECTED
    val isConnecting = peer.status == PeerConnectionStatus.CONNECTING

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (isConnected) SignalGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = peer.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val meta = buildString {
                        append(peer.address)
                        if (peer.isPaired) append(" • Paired")
                        peer.rssi?.let { append(" • $it dBm") }
                    }
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when (peer.status) {
                    PeerConnectionStatus.CONNECTED -> {
                        StatusPill(
                            label = "Connected",
                            icon = Icons.Filled.BluetoothConnected,
                            containerColor = SignalGreenContainerLight,
                            contentColor = Color(0xFF065F46)
                        )
                    }
                    PeerConnectionStatus.CONNECTING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                    PeerConnectionStatus.DISCONNECTED -> Unit
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (peer.status) {
                    PeerConnectionStatus.CONNECTED -> {
                        Button(
                            onClick = onOpenChat,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_chat_peer_${peer.address}")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Open Chat")
                        }
                        OutlinedButton(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.testTag("disconnect_peer_${peer.address}")
                        ) {
                            Icon(Icons.Filled.LinkOff, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Disconnect")
                        }
                    }
                    PeerConnectionStatus.CONNECTING -> {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Establishing RFCOMM Link...")
                        }
                    }
                    PeerConnectionStatus.DISCONNECTED -> {
                        Button(
                            onClick = onConnect,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("connect_peer_${peer.address}")
                        ) {
                            Text("Connect via Bluetooth")
                        }
                    }
                }
            }
        }
    }
}
