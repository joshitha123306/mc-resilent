package com.example.domain

import com.example.bluetooth.BluetoothConnectionService
import com.example.bluetooth.BluetoothDiscoveryController
import com.example.bluetooth.ConnectionEvent
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import com.example.data.local.PendingRelayEntity
import com.example.data.local.RelayLogEntity
import com.example.data.local.SeenPacketEntity
import com.example.data.local.UserPreferencesDataStore
import com.example.data.local.UserSettings
import com.example.model.ConversationSummary
import com.example.model.PacketEnvelope
import com.example.model.PacketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MessageRepository(
    private val messageDao: MessageDao,
    private val preferences: UserPreferencesDataStore,
    val connectionService: BluetoothConnectionService,
    val discoveryController: BluetoothDiscoveryController,
    val sosManager: SosManager,
    private val multiHopManager: MultiHopForwardingManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val userSettings: Flow<UserSettings> = preferences.settingsFlow
    val allMessages: Flow<List<MessageEntity>> = messageDao.observeAllMessages()
    val messageCount: Flow<Int> = messageDao.observeMessageCount()
    val seenPacketCount: Flow<Int> = messageDao.observeSeenPacketCount()
    val relayLogs: Flow<List<RelayLogEntity>> = messageDao.observeRelayLogs()
    val activePendingRelays: Flow<List<PendingRelayEntity>> =
        messageDao.observePendingRelays(System.currentTimeMillis())

    val conversations: Flow<List<ConversationSummary>> = allMessages.map { messages ->
        messages.groupBy { it.conversationPeerAddress }
            .map { (address, msgs) ->
                val latest = msgs.maxByOrNull { it.timestamp } ?: msgs.first()
                ConversationSummary(
                    peerAddress = address,
                    peerName = if (address == PacketEnvelope.BROADCAST_ADDRESS) {
                        "Emergency Mesh Channel"
                    } else {
                        latest.conversationPeerName
                    },
                    lastMessage = latest.content,
                    lastTimestamp = latest.timestamp,
                    messageCount = msgs.size,
                    hasActiveSos = msgs.any { it.messageType == PacketType.SOS.name && !it.isAcknowledged }
                )
            }
            .sortedByDescending { it.lastTimestamp }
    }

    init {
        scope.launch {
            preferences.ensureLocalNodeId()
        }
        scope.launch {
            connectionService.incomingPackets.collect { event ->
                multiHopManager.processIncomingPacket(event)
            }
        }
        scope.launch {
            connectionService.connectionEvents.collect { event ->
                if (event is ConnectionEvent.Connected) {
                    multiHopManager.flushPendingRelaysToPeer(event.address)
                }
            }
        }
    }

    fun observeMessagesForPeer(peerAddress: String): Flow<List<MessageEntity>> {
        return messageDao.observeMessagesForPeer(peerAddress)
    }

    suspend fun sendChatMessage(
        targetPeerAddress: String,
        targetPeerName: String,
        text: String
    ): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        val settings = preferences.settingsFlow.first()
        val nodeId = preferences.ensureLocalNodeId()

        val envelope = PacketEnvelope(
            type = PacketType.CHAT,
            originNodeId = nodeId,
            senderName = settings.callsign,
            targetAddress = targetPeerAddress,
            content = trimmed,
            timestamp = System.currentTimeMillis(),
            hopCount = 0,
            maxHops = settings.maxHopLimit,
            routeTrace = settings.callsign
        )

        messageDao.markPacketSeen(SeenPacketEntity(envelope.messageId, envelope.timestamp))

        val delivered: Boolean = if (targetPeerAddress == PacketEnvelope.BROADCAST_ADDRESS) {
            val sentTo = connectionService.broadcastPacket(envelope)
            if (sentTo.isEmpty()) {
                multiHopManager.queueOutgoingForRelay(envelope)
            }
            sentTo.isNotEmpty()
        } else {
            val directSent = connectionService.sendPacketToPeer(targetPeerAddress, envelope)
            if (!directSent) {
                multiHopManager.queueOutgoingForRelay(envelope)
            }
            directSent
        }

        val entity = MessageEntity(
            id = envelope.messageId,
            conversationPeerAddress = targetPeerAddress,
            conversationPeerName = targetPeerName,
            senderName = settings.callsign,
            originNodeId = nodeId,
            content = trimmed,
            timestamp = envelope.timestamp,
            isOutgoing = true,
            messageType = PacketType.CHAT.name,
            hopCount = 0,
            maxHops = settings.maxHopLimit,
            routeTrace = settings.callsign,
            deliveryStatus = if (delivered) "SENT" else "QUEUED"
        )
        messageDao.insertMessage(entity)
        return delivered
    }

    suspend fun sendEmergencySos(emergencyNote: String): Int {
        val settings = preferences.settingsFlow.first()
        val nodeId = preferences.ensureLocalNodeId()
        val content = emergencyNote.trim().ifBlank {
            "EMERGENCY SOS: Immediate assistance needed near ${settings.callsign}."
        }

        val envelope = PacketEnvelope(
            type = PacketType.SOS,
            originNodeId = nodeId,
            senderName = settings.callsign,
            targetAddress = PacketEnvelope.BROADCAST_ADDRESS,
            content = content,
            timestamp = System.currentTimeMillis(),
            hopCount = 0,
            maxHops = settings.maxHopLimit.coerceAtLeast(3),
            routeTrace = settings.callsign
        )

        messageDao.markPacketSeen(SeenPacketEntity(envelope.messageId, envelope.timestamp))
        val deliveredTo = connectionService.broadcastPacket(envelope)
        multiHopManager.queueOutgoingForRelay(envelope)

        val entity = MessageEntity(
            id = envelope.messageId,
            conversationPeerAddress = PacketEnvelope.BROADCAST_ADDRESS,
            conversationPeerName = "Emergency Mesh Channel",
            senderName = settings.callsign,
            originNodeId = nodeId,
            content = content,
            timestamp = envelope.timestamp,
            isOutgoing = true,
            messageType = PacketType.SOS.name,
            hopCount = 0,
            maxHops = envelope.maxHops,
            routeTrace = settings.callsign,
            deliveryStatus = if (deliveredTo.isNotEmpty()) "SENT" else "QUEUED"
        )
        messageDao.insertMessage(entity)
        return deliveredTo.size
    }

    suspend fun acknowledgeIncomingSos(sosMessage: MessageEntity) {
        sosManager.acknowledgeAlert(sosMessage.id)
        val settings = preferences.settingsFlow.first()
        val nodeId = preferences.ensureLocalNodeId()
        val ackEnvelope = PacketEnvelope(
            type = PacketType.SOS_ACK,
            originNodeId = nodeId,
            senderName = settings.callsign,
            targetAddress = PacketEnvelope.BROADCAST_ADDRESS,
            content = "SOS Acknowledged by ${settings.callsign}: \"${sosMessage.content.take(40)}\"",
            timestamp = System.currentTimeMillis(),
            hopCount = 0,
            maxHops = settings.maxHopLimit,
            routeTrace = settings.callsign,
            relatedSosId = sosMessage.id
        )
        messageDao.markPacketSeen(SeenPacketEntity(ackEnvelope.messageId, ackEnvelope.timestamp))
        connectionService.broadcastPacket(ackEnvelope)
    }

    suspend fun cancelOutgoingSos(sosMessage: MessageEntity) {
        sosManager.acknowledgeAlert(sosMessage.id)
        val settings = preferences.settingsFlow.first()
        val nodeId = preferences.ensureLocalNodeId()
        val cancelEnvelope = PacketEnvelope(
            type = PacketType.SOS_CANCEL,
            originNodeId = nodeId,
            senderName = settings.callsign,
            targetAddress = PacketEnvelope.BROADCAST_ADDRESS,
            content = "SOS Cancelled by ${settings.callsign} (Situation Resolved)",
            timestamp = System.currentTimeMillis(),
            hopCount = 0,
            maxHops = settings.maxHopLimit,
            routeTrace = settings.callsign,
            relatedSosId = sosMessage.id
        )
        messageDao.markPacketSeen(SeenPacketEntity(cancelEnvelope.messageId, cancelEnvelope.timestamp))
        val deliveredTo = connectionService.broadcastPacket(cancelEnvelope)
        messageDao.insertMessage(
            MessageEntity(
                id = cancelEnvelope.messageId,
                conversationPeerAddress = PacketEnvelope.BROADCAST_ADDRESS,
                conversationPeerName = "Emergency Mesh Channel",
                senderName = settings.callsign,
                originNodeId = nodeId,
                content = cancelEnvelope.content,
                timestamp = cancelEnvelope.timestamp,
                isOutgoing = true,
                messageType = PacketType.SOS_CANCEL.name,
                hopCount = 0,
                maxHops = cancelEnvelope.maxHops,
                routeTrace = settings.callsign,
                deliveryStatus = if (deliveredTo.isNotEmpty()) "SENT" else "QUEUED",
                isAcknowledged = true
            )
        )
    }

    suspend fun deleteConversation(peerAddress: String) {
        messageDao.deleteConversation(peerAddress)
    }

    suspend fun clearAllHistory() {
        messageDao.clearAllMessages()
        messageDao.clearPendingRelays()
        messageDao.clearSeenPackets()
        messageDao.clearRelayLogs()
    }

    suspend fun updateCallsign(name: String) = preferences.updateCallsign(name)
    suspend fun setAutoAccept(enabled: Boolean) {
        preferences.setAutoAccept(enabled)
        if (enabled) {
            connectionService.startServerIfReady()
        } else {
            connectionService.stopServer()
        }
    }
    suspend fun setAutoRelay(enabled: Boolean) = preferences.setAutoRelay(enabled)
    suspend fun setMaxHopLimit(limit: Int) = preferences.setMaxHopLimit(limit)
    suspend fun setVibrateOnSos(enabled: Boolean) = preferences.setVibrateOnSos(enabled)
}
