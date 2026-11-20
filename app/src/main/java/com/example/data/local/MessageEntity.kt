package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationPeerAddress: String,
    val conversationPeerName: String,
    val senderName: String,
    val originNodeId: String,
    val content: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val messageType: String, // CHAT, SOS, SOS_ACK, SOS_CANCEL
    val hopCount: Int,
    val maxHops: Int,
    val routeTrace: String,
    val deliveryStatus: String, // SENT, DELIVERED, RELAYED, QUEUED
    val isAcknowledged: Boolean = false
)
