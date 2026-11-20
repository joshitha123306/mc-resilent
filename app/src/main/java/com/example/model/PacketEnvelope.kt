package com.example.model

import org.json.JSONObject
import java.util.UUID

enum class PacketType {
    CHAT,
    SOS,
    SOS_ACK,
    SOS_CANCEL
}

data class PacketEnvelope(
    val messageId: String = UUID.randomUUID().toString(),
    val type: PacketType = PacketType.CHAT,
    val originNodeId: String,
    val senderName: String,
    val targetAddress: String = BROADCAST_ADDRESS,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val hopCount: Int = 0,
    val maxHops: Int = 3,
    val routeTrace: String = senderName,
    val relatedSosId: String? = null
) {
    fun toJsonLine(): String {
        val json = JSONObject().apply {
            put("messageId", messageId)
            put("type", type.name)
            put("originNodeId", originNodeId)
            put("senderName", senderName)
            put("targetAddress", targetAddress)
            put("content", content)
            put("timestamp", timestamp)
            put("hopCount", hopCount)
            put("maxHops", maxHops)
            put("routeTrace", routeTrace)
            if (relatedSosId != null) {
                put("relatedSosId", relatedSosId)
            }
        }
        return json.toString()
    }

    companion object {
        const val BROADCAST_ADDRESS = "BROADCAST"

        fun fromJsonLine(line: String): PacketEnvelope? {
            return try {
                val json = JSONObject(line)
                val typeStr = json.optString("type", PacketType.CHAT.name)
                val parsedType = runCatching { PacketType.valueOf(typeStr) }
                    .getOrDefault(PacketType.CHAT)
                PacketEnvelope(
                    messageId = json.getString("messageId"),
                    type = parsedType,
                    originNodeId = json.optString("originNodeId", "unknown"),
                    senderName = json.optString("senderName", "Nearby Peer"),
                    targetAddress = json.optString("targetAddress", BROADCAST_ADDRESS),
                    content = json.optString("content", ""),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                    hopCount = json.optInt("hopCount", 0),
                    maxHops = json.optInt("maxHops", 3),
                    routeTrace = json.optString("routeTrace", ""),
                    relatedSosId = json.optString("relatedSosId", "").ifBlank { null }
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
