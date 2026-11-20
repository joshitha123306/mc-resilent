package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ResilientApplication
import com.example.bluetooth.BluetoothPermissions
import com.example.bluetooth.ConnectionEvent
import com.example.data.local.MessageEntity
import com.example.data.local.PendingRelayEntity
import com.example.data.local.RelayLogEntity
import com.example.data.local.UserSettings
import com.example.model.BluetoothPeer
import com.example.model.ConversationSummary
import com.example.model.PeerConnectionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ResilientUiState(
    val hasPermissions: Boolean = false,
    val isBluetoothSupported: Boolean = true,
    val isBluetoothEnabled: Boolean = false,
    val isDiscovering: Boolean = false,
    val isDiscoverable: Boolean = false,
    val isServerListening: Boolean = false,
    val adapterName: String = "Bluetooth Radio",
    val connectedPeers: List<BluetoothPeer> = emptyList(),
    val discoveredPeers: List<BluetoothPeer> = emptyList(),
    val pairedPeers: List<BluetoothPeer> = emptyList(),
    val userSettings: UserSettings = UserSettings(
        callsign = "Responder",
        localNodeId = "",
        autoAcceptConnections = true,
        autoRelayEnabled = true,
        maxHopLimit = 3,
        vibrateOnSos = true
    ),
    val activeIncomingSos: List<MessageEntity> = emptyList(),
    val allSosHistory: List<MessageEntity> = emptyList(),
    val conversations: List<ConversationSummary> = emptyList(),
    val relayLogs: List<RelayLogEntity> = emptyList(),
    val pendingRelays: List<PendingRelayEntity> = emptyList(),
    val totalMessageCount: Int = 0,
    val seenPacketCount: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ResilientApplication
    private val repository = app.repository
    private val discovery = repository.discoveryController
    private val connection = repository.connectionService

    private val _hasPermissions = MutableStateFlow(BluetoothPermissions.hasAllRequiredPermissions(app))

    private val _uiEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    private val bluetoothRadioFlow = combine(
        _hasPermissions,
        discovery.isBluetoothSupported,
        discovery.isBluetoothEnabled,
        discovery.isDiscovering,
        discovery.isDiscoverable
    ) { hasPerm, supported, enabled, discovering, discoverable ->
        RadioSnapshot(hasPerm, supported, enabled, discovering, discoverable)
    }

    private val peerListsFlow = combine(
        connection.connectedPeers,
        connection.connectingAddresses,
        discovery.discoveredPeers,
        discovery.pairedPeers,
        discovery.adapterName
    ) { connectedMap, connectingSet, discovered, paired, adapterName ->
        val connectedList = connectedMap.entries.map { (addr, name) ->
            BluetoothPeer(
                address = addr,
                name = name,
                isPaired = paired.any { it.address == addr },
                status = PeerConnectionStatus.CONNECTED
            )
        }.sortedBy { it.name.lowercase() }

        val enrichedDiscovered = discovered.map { peer ->
            val status = when {
                connectedMap.containsKey(peer.address) -> PeerConnectionStatus.CONNECTED
                connectingSet.contains(peer.address) -> PeerConnectionStatus.CONNECTING
                else -> PeerConnectionStatus.DISCONNECTED
            }
            peer.copy(status = status)
        }

        val enrichedPaired = paired.map { peer ->
            val status = when {
                connectedMap.containsKey(peer.address) -> PeerConnectionStatus.CONNECTED
                connectingSet.contains(peer.address) -> PeerConnectionStatus.CONNECTING
                else -> PeerConnectionStatus.DISCONNECTED
            }
            peer.copy(status = status)
        }

        PeerSnapshot(adapterName, connectedList, enrichedDiscovered, enrichedPaired)
    }

    private val meshDataFlow = combine(
        repository.userSettings,
        repository.sosManager.activeIncomingSos,
        repository.sosManager.allSosHistory,
        repository.conversations,
        repository.relayLogs
    ) { settings, activeSos, sosHistory, conversations, relayLogs ->
        MeshSnapshot(settings, activeSos, sosHistory, conversations, relayLogs)
    }

    private val countsFlow = combine(
        repository.activePendingRelays,
        repository.messageCount,
        repository.seenPacketCount,
        connection.isServerListening
    ) { pending, msgCount, seenCount, listening ->
        CountSnapshot(pending, msgCount, seenCount, listening)
    }

    val uiState: StateFlow<ResilientUiState> = combine(
        bluetoothRadioFlow,
        peerListsFlow,
        meshDataFlow,
        countsFlow
    ) { radio, peers, mesh, counts ->
        ResilientUiState(
            hasPermissions = radio.hasPermissions,
            isBluetoothSupported = radio.isSupported,
            isBluetoothEnabled = radio.isEnabled,
            isDiscovering = radio.isDiscovering,
            isDiscoverable = radio.isDiscoverable,
            isServerListening = counts.isServerListening,
            adapterName = peers.adapterName,
            connectedPeers = peers.connectedPeers,
            discoveredPeers = peers.discoveredPeers,
            pairedPeers = peers.pairedPeers,
            userSettings = mesh.settings,
            activeIncomingSos = mesh.activeSos,
            allSosHistory = mesh.allSos,
            conversations = mesh.conversations,
            relayLogs = mesh.relayLogs,
            pendingRelays = counts.pendingRelays,
            totalMessageCount = counts.messageCount,
            seenPacketCount = counts.seenPacketCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ResilientUiState(hasPermissions = _hasPermissions.value)
    )

    init {
        viewModelScope.launch {
            connection.connectionEvents.collect { event ->
                when (event) {
                    is ConnectionEvent.Connected -> {
                        _uiEvents.emit("Connected to ${event.name}")
                    }
                    is ConnectionEvent.Disconnected -> {
                        _uiEvents.emit("Disconnected from ${event.name}: ${event.reason}")
                    }
                    is ConnectionEvent.ConnectionFailed -> {
                        _uiEvents.emit(event.error)
                    }
                    is ConnectionEvent.ServerStatus -> Unit
                }
            }
        }
    }

    fun refreshPermissionsAndRadio() {
        val granted = BluetoothPermissions.hasAllRequiredPermissions(app)
        _hasPermissions.value = granted
        discovery.syncCurrentState()
        if (granted) {
            connection.startServerIfReady()
        }
    }

    fun startDiscovery() {
        refreshPermissionsAndRadio()
        if (!_hasPermissions.value) {
            _uiEvents.tryEmit("Grant Bluetooth permissions first.")
            return
        }
        val started = discovery.startDiscovery()
        if (!started) {
            _uiEvents.tryEmit("Unable to start discovery. Ensure Bluetooth is turned ON.")
        }
    }

    fun stopDiscovery() {
        discovery.stopDiscovery()
    }

    fun connectPeer(address: String) {
        if (!BluetoothPermissions.hasConnectPermission(app)) {
            _uiEvents.tryEmit("Bluetooth Connect permission is required.")
            return
        }
        connection.connectToDevice(address)
    }

    fun disconnectPeer(address: String) {
        connection.disconnectPeer(address)
    }

    fun observeChatMessages(peerAddress: String): Flow<List<MessageEntity>> {
        return repository.observeMessagesForPeer(peerAddress)
    }

    fun sendChatMessage(peerAddress: String, peerName: String, text: String) {
        viewModelScope.launch {
            val delivered = repository.sendChatMessage(peerAddress, peerName, text)
            if (!delivered) {
                _uiEvents.emit("Saved locally & queued in relay buffer (no direct link).")
            }
        }
    }

    fun sendEmergencySos(note: String) {
        viewModelScope.launch {
            val count = repository.sendEmergencySos(note)
            if (count > 0) {
                _uiEvents.emit("Emergency SOS sent to $count connected device(s).")
            } else {
                _uiEvents.emit("No devices connected right now. SOS saved & queued for relay.")
            }
        }
    }

    fun acknowledgeSos(message: MessageEntity) {
        viewModelScope.launch {
            repository.acknowledgeIncomingSos(message)
            _uiEvents.emit("SOS acknowledged.")
        }
    }

    fun cancelOutgoingSos(message: MessageEntity) {
        viewModelScope.launch {
            repository.cancelOutgoingSos(message)
            _uiEvents.emit("SOS cancellation broadcast sent.")
        }
    }

    fun deleteConversation(peerAddress: String) {
        viewModelScope.launch {
            repository.deleteConversation(peerAddress)
            _uiEvents.emit("Conversation deleted.")
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _uiEvents.emit("All local message history cleared.")
        }
    }

    fun updateCallsign(name: String) {
        viewModelScope.launch {
            repository.updateCallsign(name)
            _uiEvents.emit("Device callsign updated.")
        }
    }

    fun setAutoAccept(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoAccept(enabled) }
    }

    fun setAutoRelay(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoRelay(enabled) }
    }

    fun setMaxHopLimit(limit: Int) {
        viewModelScope.launch { repository.setMaxHopLimit(limit) }
    }

    fun setVibrateOnSos(enabled: Boolean) {
        viewModelScope.launch { repository.setVibrateOnSos(enabled) }
    }

    private data class RadioSnapshot(
        val hasPermissions: Boolean,
        val isSupported: Boolean,
        val isEnabled: Boolean,
        val isDiscovering: Boolean,
        val isDiscoverable: Boolean
    )

    private data class PeerSnapshot(
        val adapterName: String,
        val connectedPeers: List<BluetoothPeer>,
        val discoveredPeers: List<BluetoothPeer>,
        val pairedPeers: List<BluetoothPeer>
    )

    private data class MeshSnapshot(
        val settings: UserSettings,
        val activeSos: List<MessageEntity>,
        val allSos: List<MessageEntity>,
        val conversations: List<ConversationSummary>,
        val relayLogs: List<RelayLogEntity>
    )

    private data class CountSnapshot(
        val pendingRelays: List<PendingRelayEntity>,
        val messageCount: Int,
        val seenPacketCount: Int,
        val isServerListening: Boolean
    )
}
