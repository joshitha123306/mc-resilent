package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "seen_packets")
data class SeenPacketEntity(
    @PrimaryKey val packetId: String,
    val seenAt: Long
)
