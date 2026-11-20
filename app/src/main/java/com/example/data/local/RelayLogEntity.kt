package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "relay_logs")
data class RelayLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packetId: String,
    val eventType: String, // FORWARDED, QUEUED, DROPPED_DUPLICATE, HOP_LIMIT, FLUSHED
    val summary: String,
    val routeTrace: String,
    val timestamp: Long
)
