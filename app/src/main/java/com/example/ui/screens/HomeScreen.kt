package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.PacketEnvelope
import com.example.ui.ResilientUiState
import com.example.ui.components.BluetoothPermissionCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CobaltBright
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RelayAmber
import com.example.ui.theme.RelayAmberContainerLight
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.SignalGreenContainerLight

@Composable
fun HomeScreen(
    uiState: ResilientUiState,
    onRequestPermissions: () -> Unit,
    onOpenDiscovery: () -> Unit,
    onOpenChat: (String, String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMesh: () -> Unit,
    onTriggerSosClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nearbyCount = (uiState.discoveredPeers.map { it.address } +
        uiState.pairedPeers.map { it.address } +
        uiState.connectedPeers.map { it.address }).distinct().size

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Tactical Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = NavyDeep),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_mesh_hero),
                            contentDescription = "Offline peer-to-peer mesh network banner",
                            contentScale = ContentScale.Crop,
                            alpha = 0.42f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(206.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(206.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            NavyDeep.copy(alpha = 0.55f),
                                            NavyDeep.copy(alpha = 0.92f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_launcher_logo),
                                    contentDescription = "Resilient Connect Logo",
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = PureWhite
                                    )
                                    Text(
                                        text = stringResource(R.string.no_internet_badge),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CobaltBright
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatusPill(
                                    label = if (uiState.isBluetoothEnabled) "BT Active" else "BT Off",
                                    icon = if (uiState.isBluetoothEnabled) {
                                        Icons.Filled.BluetoothConnected
                                    } else {
                                        Icons.Filled.BluetoothDisabled
                                    },
                                    containerColor = if (uiState.isBluetoothEnabled) {
                                        SignalGreenContainerLight
                                    } else {
                                        RelayAmberContainerLight
                                    },
                                    contentColor = if (uiState.isBluetoothEnabled) {
                                        Color(0xFF065F46)
                                    } else {
                                        Color(0xFF92400E)
                                    }
                                )
                                StatusPill(
                                    label = "${uiState.connectedPeers.size} Connected",
                                    icon = Icons.Filled.Devices,
                                    containerColor = NavyElevated,
                                    contentColor = PureWhite
                                )
                            }

                            Text(
                                text = "Node: ${uiState.userSettings.callsign} (${uiState.userSettings.localNodeId.ifBlank { "INIT" }})",
                                style = MaterialTheme.typography.labelMedium,
                                color = PureWhite.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Permissions reminder if missing
            if (!uiState.hasPermissions) {
                item {
                    BluetoothPermissionCard(onRequestPermissions = onRequestPermissions)
                }
            }

            // 2. Prominent Emergency SOS Button Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    border = BorderStroke(1.5.dp, EmergencyRed.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EmergencyRed,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = "Emergency SOS icon",
                                        tint = PureWhite
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Emergency SOS Broadcast",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = stringResource(R.string.sos_disclaimer),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Button(
                            onClick = onTriggerSosClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmergencyRed,
                                contentColor = PureWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("home_sos_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SEND EMERGENCY SOS",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. Live Radio & Network Metrics Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Network Status",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "Bluetooth Radio",
                            value = if (uiState.isBluetoothEnabled) {
                                if (uiState.isServerListening) "Listening" else "ON"
                            } else {
                                "OFF"
                            },
                            subtitle = uiState.adapterName,
                            accent = if (uiState.isBluetoothEnabled) SignalGreen else RelayAmber,
                            icon = Icons.Filled.Bluetooth,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Nearby Devices",
                            value = "$nearbyCount",
                            subtitle = "${uiState.connectedPeers.size} active link(s)",
                            accent = MaterialTheme.colorScheme.secondary,
                            icon = Icons.Filled.Radar,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "Saved Messages",
                            value = "${uiState.totalMessageCount}",
                            subtitle = "Stored offline in Room",
                            accent = MaterialTheme.colorScheme.primary,
                            icon = Icons.Filled.Security,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Multi-Hop Relay",
                            value = if (uiState.userSettings.autoRelayEnabled) "Enabled" else "Paused",
                            subtitle = "${uiState.pendingRelays.size} buffered packet(s)",
                            accent = MaterialTheme.colorScheme.secondary,
                            icon = Icons.Filled.Hub,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Active Connected Devices Quick Jump
            if (uiState.connectedPeers.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Connected Peers",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        uiState.connectedPeers.forEach { peer ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenChat(peer.address, peer.name) }
                                    .testTag("home_connected_peer_${peer.address}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, SignalGreen.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = peer.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = peer.address,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusPill(
                                        label = "Open Chat",
                                        icon = Icons.AutoMirrored.Filled.Chat,
                                        containerColor = SignalGreenContainerLight,
                                        contentColor = Color(0xFF065F46)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Quick Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Quick Navigation",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    QuickNavTile(
                        title = "Discover Nearby Devices",
                        subtitle = "Scan Bluetooth Classic peers & establish RFCOMM link",
                        icon = Icons.Filled.Radar,
                        onClick = onOpenDiscovery,
                        tag = "quick_nav_discover"
                    )
                    QuickNavTile(
                        title = "Emergency Mesh Channel",
                        subtitle = "Open broadcast & SOS room across all connected peers",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        onClick = {
                            onOpenChat(PacketEnvelope.BROADCAST_ADDRESS, "Emergency Mesh Channel")
                        },
                        tag = "quick_nav_mesh_chat"
                    )
                    QuickNavTile(
                        title = "Local Message History",
                        subtitle = "View offline conversations stored on this smartphone",
                        icon = Icons.Filled.History,
                        onClick = onOpenHistory,
                        tag = "quick_nav_history"
                    )
                    QuickNavTile(
                        title = "Multi-Hop Store & Forward",
                        subtitle = "Inspect packet forwarding queue, hop limits & test plan",
                        icon = Icons.Filled.Hub,
                        onClick = onOpenMesh,
                        tag = "quick_nav_mesh"
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickNavTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
