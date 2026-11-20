package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_relays")
data class PendingRelayEntity(
    @PrimaryKey val packetId: String,
    val payloadJson: String,
    val originNodeId: String,
    val sourcePeerAddress: String,
    val hopCount: Int,
    val maxHops: Int,
    val createdAt: Long,
    val expiresAt: Long
)
