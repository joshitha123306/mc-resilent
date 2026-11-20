package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.MessageEntity
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainerLight
import com.example.ui.theme.PureWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusPill(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BluetoothPermissionCard(
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bluetooth,
                    contentDescription = "Bluetooth permissions",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Bluetooth Permissions Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = "Resilient Connect uses Bluetooth Classic RFCOMM to discover nearby smartphones and exchange messages offline without internet or cellular service.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.testTag("grant_permissions_button")
            ) {
                Text("Grant Bluetooth Permissions")
            }
        }
    }
}

@Composable
fun SosEmergencyDialog(
    connectedCount: Int,
    onDismiss: () -> Unit,
    onSendSos: (String) -> Unit
) {
    val presets = listOf(
        "Medical assistance needed",
        "Trapped / need rescue",
        "Danger / hazard nearby",
        "Need water or supplies"
    )
    var selectedPreset by remember { mutableStateOf(presets.first()) }
    var customNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Emergency SOS",
                tint = EmergencyRed,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Broadcast Emergency SOS",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = EmergencyRedContainerLight,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = stringResource(R.string.sos_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7F1D1D),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Text(
                    text = if (connectedCount > 0) {
                        "Will transmit immediately to $connectedCount connected device(s) and relay via multi-hop."
                    } else {
                        "No devices connected right now. SOS will be saved locally and auto-relayed when a peer connects."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset && customNote.isBlank(),
                            onClick = {
                                selectedPreset = preset
                                customNote = ""
                            },
                            label = { Text(preset, style = MaterialTheme.typography.bodySmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmergencyRedContainerLight,
                                selectedLabelColor = Color(0xFF7F1D1D)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                OutlinedTextField(
                    value = customNote,
                    onValueChange = { customNote = it },
                    label = { Text("Or enter custom emergency note") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sos_custom_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val payload = customNote.trim().ifBlank { selectedPreset }
                    onSendSos("SOS ALERT: $payload")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmergencyRed,
                    contentColor = PureWhite
                ),
                modifier = Modifier.testTag("confirm_send_sos_button")
            ) {
                Text("Send SOS Now")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_sos_dialog_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun IncomingSosBanner(
    activeAlerts: List<MessageEntity>,
    onAcknowledge: (MessageEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val latest = activeAlerts.firstOrNull() ?: return
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EmergencyRed,
            contentColor = PureWhite
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Incoming SOS alert",
                        tint = PureWhite
                    )
                    Text(
                        text = "INCOMING EMERGENCY SOS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
                if (activeAlerts.size > 1) {
                    Text(
                        text = "+${activeAlerts.size - 1} more",
                        style = MaterialTheme.typography.labelSmall,
                        color = PureWhite.copy(alpha = 0.9f)
                    )
                }
            }

            Text(
                text = latest.content,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = PureWhite
            )

            Text(
                text = "From ${latest.senderName} • ${formatTimestamp(latest.timestamp)} • Hop ${latest.hopCount}",
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(2.dp))

            OutlinedButton(
                onClick = { onAcknowledge(latest) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                border = BorderStroke(1.dp, PureWhite),
                modifier = Modifier.testTag("acknowledge_sos_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Acknowledge Alert")
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatTimeShort(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
