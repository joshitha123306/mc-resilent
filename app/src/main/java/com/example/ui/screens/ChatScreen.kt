package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.MessageEntity
import com.example.model.PacketEnvelope
import com.example.model.PacketType
import com.example.ui.ResilientUiState
import com.example.ui.components.formatTimeShort
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainerLight
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RelayAmberContainerLight
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.SignalGreenContainerLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    peerAddress: String,
    peerName: String,
    messages: List<MessageEntity>,
    uiState: ResilientUiState,
    onBack: () -> Unit,
    onReconnect: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onTriggerSos: () -> Unit,
    onAcknowledgeSos: (MessageEntity) -> Unit,
    onCancelOutgoingSos: (MessageEntity) -> Unit
) {
    BackHandler(onBack = onBack)

    val isBroadcast = peerAddress == PacketEnvelope.BROADCAST_ADDRESS
    val isPeerConnected = if (isBroadcast) {
        uiState.connectedPeers.isNotEmpty()
    } else {
        uiState.connectedPeers.any { it.address == peerAddress }
    }

    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyDeep,
                    titleContentColor = PureWhite,
                    navigationIconContentColor = PureWhite,
                    actionIconContentColor = PureWhite
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = peerName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isPeerConnected) {
                                    Icons.Filled.BluetoothConnected
                                } else {
                                    Icons.Filled.BluetoothDisabled
                                },
                                contentDescription = null,
                                tint = if (isPeerConnected) Color(0xFF34D399) else Color(0xFFFBBF24),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isBroadcast) {
                                    "${uiState.connectedPeers.size} peer(s) active"
                                } else if (isPeerConnected) {
                                    "Connected • $peerAddress"
                                } else {
                                    "Disconnected • $peerAddress"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = PureWhite.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (!isBroadcast && !isPeerConnected) {
                        IconButton(
                            onClick = { onReconnect(peerAddress) },
                            modifier = Modifier.testTag("chat_reconnect_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Reconnect Bluetooth"
                            )
                        }
                    }
                    IconButton(
                        onClick = onTriggerSos,
                        modifier = Modifier.testTag("chat_sos_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Send Emergency SOS",
                            tint = Color(0xFFFCA5A5)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                if (isPeerConnected) "Message over Bluetooth..."
                                else "Message (will queue locally)..."
                            )
                        },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )
                    FilledIconButton(
                        onClick = {
                            val toSend = inputText.trim()
                            if (toSend.isNotEmpty()) {
                                onSendMessage(toSend)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = NavyPrimary,
                            contentColor = PureWhite
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isPeerConnected) {
                Surface(
                    color = RelayAmberContainerLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isBroadcast) {
                                "No active Bluetooth peers. Messages will queue for relay."
                            } else {
                                "Device disconnected. Tap Reconnect when nearby."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF78350F),
                            modifier = Modifier.weight(1f)
                        )
                        if (!isBroadcast) {
                            TextButton(onClick = { onReconnect(peerAddress) }) {
                                Text("Reconnect")
                            }
                        }
                    }
                }
            }

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No messages yet. Send an offline message over Bluetooth Classic RFCOMM.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        MessageBubble(
                            message = msg,
                            onAcknowledgeSos = { onAcknowledgeSos(msg) },
                            onCancelOutgoingSos = { onCancelOutgoingSos(msg) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageEntity,
    onAcknowledgeSos: () -> Unit,
    onCancelOutgoingSos: () -> Unit
) {
    val isSos = message.messageType == PacketType.SOS.name
    val isSosControl = message.messageType == PacketType.SOS_ACK.name ||
        message.messageType == PacketType.SOS_CANCEL.name

    val horizontalAlign = if (message.isOutgoing) Alignment.End else Alignment.Start

    val containerColor = when {
        isSos -> EmergencyRedContainerLight
        isSosControl -> SignalGreenContainerLight
        message.isOutgoing -> NavyPrimary
        else -> MaterialTheme.colorScheme.surface
    }

    val textColor = when {
        isSos -> Color(0xFF7F1D1D)
        isSosControl -> Color(0xFF064E3B)
        message.isOutgoing -> PureWhite
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderStroke = when {
        isSos -> BorderStroke(1.5.dp, EmergencyRed)
        isSosControl -> BorderStroke(1.dp, SignalGreen)
        message.isOutgoing -> null
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlign
    ) {
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isOutgoing) 18.dp else 4.dp,
                bottomEnd = if (message.isOutgoing) 4.dp else 18.dp
            ),
            border = borderStroke,
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!message.isOutgoing || isSos) {
                    Text(
                        text = if (isSos) "SOS • ${message.senderName}" else message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSos) EmergencyRed else textColor.copy(alpha = 0.8f)
                    )
                }

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hopLabel = if (message.hopCount > 0) {
                        "Hop ${message.hopCount} · ${message.routeTrace}"
                    } else if (message.isOutgoing) {
                        message.deliveryStatus
                    } else {
                        "Direct"
                    }
                    Text(
                        text = hopLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatTimeShort(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.72f)
                    )
                }

                if (isSos && !message.isAcknowledged) {
                    Spacer(modifier = Modifier.padding(top = 2.dp))
                    if (!message.isOutgoing) {
                        OutlinedButton(
                            onClick = onAcknowledgeSos,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed),
                            border = BorderStroke(1.dp, EmergencyRed),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Acknowledge SOS", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onCancelOutgoingSos,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed),
                            border = BorderStroke(1.dp, EmergencyRed),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Cancel My SOS", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
