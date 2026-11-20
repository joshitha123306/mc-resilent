package com.example.domain

import com.example.bluetooth.BluetoothConnectionService
import com.example.bluetooth.IncomingPacketEvent
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import com.example.data.local.PendingRelayEntity
import com.example.data.local.RelayLogEntity
import com.example.data.local.SeenPacketEntity
import com.example.data.local.UserPreferencesDataStore
import com.example.model.PacketEnvelope
import com.example.model.PacketType
import kotlinx.coroutines.flow.first

class MultiHopForwardingManager(
    private val messageDao: MessageDao,
    private val connectionService: BluetoothConnectionService,
    private val preferences: UserPreferencesDataStore,
    private val sosManager: SosManager
) {
    companion object {
        private const val RELAY_TTL_MS = 30 * 60 * 1000L // 30 minutes store-and-forward window
    }

    suspend fun processIncomingPacket(event: IncomingPacketEvent) {
        val envelope = event.envelope
        val settings = preferences.settingsFlow.first()
        val localNodeId = preferences.ensureLocalNodeId()

        // 1. Loop / Duplicate Suppression by unique messageId
        if (envelope.originNodeId == localNodeId || messageDao.isPacketSeen(envelope.messageId)) {
            messageDao.insertRelayLog(
                RelayLogEntity(
                    packetId = envelope.messageId,
                    eventType = "DROPPED_DUPLICATE",
                    summary = "Suppressed duplicate ${envelope.type.name} from ${event.fromPeerName}",
                    routeTrace = envelope.routeTrace,
                    timestamp = System.currentTimeMillis()
                )
            )
            return
        }

        // 2. Mark packet as seen immediately
        messageDao.markPacketSeen(
            SeenPacketEntity(
                packetId = envelope.messageId,
                seenAt = System.currentTimeMillis()
            )
        )

        // 3. Persist received message locally
        val conversationAddress = when {
            envelope.type != PacketType.CHAT -> PacketEnvelope.BROADCAST_ADDRESS
            envelope.targetAddress == PacketEnvelope.BROADCAST_ADDRESS -> PacketEnvelope.BROADCAST_ADDRESS
            else -> event.fromPeerAddress
        }

        val conversationName = if (conversationAddress == PacketEnvelope.BROADCAST_ADDRESS) {
            "Emergency Mesh Channel"
        } else {
            event.fromPeerName
        }

        val entity = MessageEntity(
            id = envelope.messageId,
            conversationPeerAddress = conversationAddress,
            conversationPeerName = conversationName,
            senderName = envelope.senderName,
            originNodeId = envelope.originNodeId,
            content = envelope.content,
            timestamp = envelope.timestamp,
            isOutgoing = false,
            messageType = envelope.type.name,
            hopCount = envelope.hopCount,
            maxHops = envelope.maxHops,
            routeTrace = envelope.routeTrace.ifBlank { envelope.senderName },
            deliveryStatus = if (envelope.hopCount > 0) "RELAYED" else "DELIVERED",
            isAcknowledged = false
        )
        messageDao.insertMessage(entity)

        // 4. Handle SOS alert types
        when (envelope.type) {
            PacketType.SOS -> {
                sosManager.triggerAlertHaptics(settings.vibrateOnSos)
            }
            PacketType.SOS_ACK, PacketType.SOS_CANCEL -> {
                envelope.relatedSosId?.let { sosId ->
                    messageDao.markMessageAcknowledged(sosId)
                }
            }
            PacketType.CHAT -> Unit
        }

        // 5. Multi-Hop Store-and-Forward Relay Logic
        if (!settings.autoRelayEnabled) return

        if (envelope.hopCount >= envelope.maxHops) {
            messageDao.insertRelayLog(
                RelayLogEntity(
                    packetId = envelope.messageId,
                    eventType = "HOP_LIMIT",
                    summary = "Stopped relay at hop ${envelope.hopCount}/${envelope.maxHops}",
                    routeTrace = envelope.routeTrace,
                    timestamp = System.currentTimeMillis()
                )
            )
            return
        }

        val nextEnvelope = envelope.copy(
            hopCount = envelope.hopCount + 1,
            routeTrace = if (envelope.routeTrace.isBlank()) {
                "${envelope.senderName} -> ${settings.callsign}"
            } else {
                "${envelope.routeTrace} -> ${settings.callsign}"
            }
        )

        // Forward immediately to all OTHER currently connected Bluetooth peers
        val forwardedAddresses = connectionService.broadcastPacket(
            envelope = nextEnvelope,
            excludeAddress = event.fromPeerAddress
        )

        if (forwardedAddresses.isNotEmpty()) {
            messageDao.insertRelayLog(
                RelayLogEntity(
                    packetId = envelope.messageId,
                    eventType = "FORWARDED",
                    summary = "Relayed ${envelope.type.name} to ${forwardedAddresses.size} peer(s) (Hop ${nextEnvelope.hopCount}/${nextEnvelope.maxHops})",
                    routeTrace = nextEnvelope.routeTrace,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        // Store-and-forward queue: retain for newly connecting devices (Phone C arriving later)
        val now = System.currentTimeMillis()
        messageDao.insertPendingRelay(
            PendingRelayEntity(
                packetId = nextEnvelope.messageId,
                payloadJson = nextEnvelope.toJsonLine(),
                originNodeId = nextEnvelope.originNodeId,
                sourcePeerAddress = event.fromPeerAddress,
                hopCount = nextEnvelope.hopCount,
                maxHops = nextEnvelope.maxHops,
                createdAt = now,
                expiresAt = now + RELAY_TTL_MS
            )
        )
        messageDao.insertRelayLog(
            RelayLogEntity(
                packetId = nextEnvelope.messageId,
                eventType = "QUEUED",
                summary = "Stored in DTN relay buffer for late-joining peers",
                routeTrace = nextEnvelope.routeTrace,
                timestamp = now
            )
        )
    }

    suspend fun queueOutgoingForRelay(envelope: PacketEnvelope) {
        val settings = preferences.settingsFlow.first()
        if (!settings.autoRelayEnabled) return
        val now = System.currentTimeMillis()
        messageDao.insertPendingRelay(
            PendingRelayEntity(
                packetId = envelope.messageId,
                payloadJson = envelope.toJsonLine(),
                originNodeId = envelope.originNodeId,
                sourcePeerAddress = "LOCAL",
                hopCount = envelope.hopCount,
                maxHops = envelope.maxHops,
                createdAt = now,
                expiresAt = now + RELAY_TTL_MS
            )
        )
    }

    suspend fun flushPendingRelaysToPeer(peerAddress: String) {
        val settings = preferences.settingsFlow.first()
        if (!settings.autoRelayEnabled) return
        val now = System.currentTimeMillis()
        messageDao.deleteExpiredRelays(now)
        val relays = messageDao.getActivePendingRelays(now)
        var flushedCount = 0

        for (relay in relays) {
            if (relay.sourcePeerAddress == peerAddress) continue
            val envelope = PacketEnvelope.fromJsonLine(relay.payloadJson) ?: continue
            if (envelope.hopCount > envelope.maxHops) continue
            val sent = connectionService.sendPacketToPeer(peerAddress, envelope)
            if (sent) {
                flushedCount++
            }
        }

        if (flushedCount > 0) {
            messageDao.insertRelayLog(
                RelayLogEntity(
                    packetId = "FLUSH-${peerAddress.takeLast(5)}",
                    eventType = "FLUSHED",
                    summary = "Delivered $flushedCount buffered relay packet(s) to newly connected peer $peerAddress",
                    routeTrace = settings.callsign,
                    timestamp = now
                )
            )
        }
    }
}
