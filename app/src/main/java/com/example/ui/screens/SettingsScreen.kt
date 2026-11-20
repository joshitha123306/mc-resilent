package com.example.ui.screens

import android.os.Build
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.bluetooth.BluetoothConnectionService
import com.example.ui.ResilientUiState
import com.example.ui.theme.EmergencyRed

@Composable
fun SettingsScreen(
    uiState: ResilientUiState,
    onUpdateCallsign: (String) -> Unit,
    onToggleAutoAccept: (Boolean) -> Unit,
    onToggleVibrateSos: (Boolean) -> Unit,
    onClearAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var callsignDraft by remember(uiState.userSettings.callsign) {
        mutableStateOf(uiState.userSettings.callsign)
    }
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Delete All Local Messages?") },
            text = { Text("All offline chat messages, SOS alerts, and relay logs will be permanently erased.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        onClearAllHistory()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = EmergencyRed)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                Text(
                    text = "Settings & Diagnostics",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Callsign Card
            item {
                SettingsSectionCard(
                    title = "Responder Identity",
                    icon = Icons.Filled.PhoneAndroid
                ) {
                    OutlinedTextField(
                        value = callsignDraft,
                        onValueChange = { callsignDraft = it },
                        label = { Text("Device Callsign / Display Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("callsign_input")
                    )
                    Button(
                        onClick = { onUpdateCallsign(callsignDraft) },
                        enabled = callsignDraft.isNotBlank() && callsignDraft != uiState.userSettings.callsign,
                        modifier = Modifier.testTag("save_callsign_button")
                    ) {
                        Text("Save Callsign")
                    }
                    InfoRow("Local Node ID", uiState.userSettings.localNodeId.ifBlank { "Generating..." })
                    InfoRow("Device Model", "${Build.MANUFACTURER} ${Build.MODEL}")
                    InfoRow("Android OS", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                }
            }

            // Bluetooth & Connection Info
            item {
                SettingsSectionCard(
                    title = "Bluetooth & Connection Info",
                    icon = Icons.Filled.Bluetooth
                ) {
                    InfoRow("Bluetooth Radio", if (uiState.isBluetoothEnabled) "Enabled (ON)" else "Disabled (OFF)")
                    InfoRow("Adapter Name", uiState.adapterName)
                    InfoRow("RFCOMM Server", if (uiState.isServerListening) "Listening for peers" else "Stopped")
                    InfoRow("Active Peer Links", "${uiState.connectedPeers.size} device(s)")
                    InfoRow("Service UUID", BluetoothConnectionService.APP_UUID.toString())

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Accept Incoming Connections", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Keep RFCOMM server socket open while Bluetooth is ON",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.userSettings.autoAcceptConnections,
                            onCheckedChange = onToggleAutoAccept,
                            modifier = Modifier.testTag("auto_accept_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Vibrate on Incoming SOS", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Trigger emergency haptic pattern for incoming alerts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.userSettings.vibrateOnSos,
                            onCheckedChange = onToggleVibrateSos,
                            modifier = Modifier.testTag("vibrate_sos_switch")
                        )
                    }
                }
            }

            // Message History Management
            item {
                SettingsSectionCard(
                    title = "Message History Management",
                    icon = Icons.Filled.Storage
                ) {
                    InfoRow("Stored Messages", "${uiState.totalMessageCount}")
                    InfoRow("Active Conversations", "${uiState.conversations.size}")
                    InfoRow("Buffered Relays", "${uiState.pendingRelays.size}")
                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_clear_history_button")
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Clear All Local Message History")
                    }
                }
            }

            // Application Information
            item {
                SettingsSectionCard(
                    title = "Application Information",
                    icon = Icons.Filled.Info
                ) {
                    InfoRow("App Name", "Resilient Connect")
                    InfoRow("Version", "1.0.0 (Offline Mesh Edition)")
                    InfoRow("Transport", "Bluetooth Classic RFCOMM")
                    InfoRow("Local Storage", "Android Room SQLite + DataStore")
                    Text(
                        text = "Resilient Connect operates entirely without Internet, Wi-Fi, or cellular networks. Emergency SOS broadcasts notify connected nearby devices only and do not contact official emergency services.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
