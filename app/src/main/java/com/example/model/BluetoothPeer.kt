package com.example.model

enum class PeerConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

data class BluetoothPeer(
    val address: String,
    val name: String,
    val isPaired: Boolean,
    val status: PeerConnectionStatus = PeerConnectionStatus.DISCONNECTED,
    val rssi: Int? = null,
    val lastSeenAt: Long = System.currentTimeMillis()
)

data class ConversationSummary(
    val peerAddress: String,
    val peerName: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val messageCount: Int,
    val hasActiveSos: Boolean
)
