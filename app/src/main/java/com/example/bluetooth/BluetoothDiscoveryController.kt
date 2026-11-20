package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.model.BluetoothPeer
import com.example.model.PeerConnectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BluetoothDiscoveryController(
    private val context: Context,
    private val connectionService: BluetoothConnectionService
) {
    private val adapter: BluetoothAdapter?
        get() = connectionService.bluetoothAdapter

    private val _isBluetoothSupported = MutableStateFlow(adapter != null)
    val isBluetoothSupported: StateFlow<Boolean> = _isBluetoothSupported.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(adapter?.isEnabled == true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _isDiscoverable = MutableStateFlow(false)
    val isDiscoverable: StateFlow<Boolean> = _isDiscoverable.asStateFlow()

    private val _adapterName = MutableStateFlow("Bluetooth Radio")
    val adapterName: StateFlow<String> = _adapterName.asStateFlow()

    private val _pairedPeers = MutableStateFlow<List<BluetoothPeer>>(emptyList())
    val pairedPeers: StateFlow<List<BluetoothPeer>> = _pairedPeers.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<BluetoothPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<BluetoothPeer>> = _discoveredPeers.asStateFlow()

    private var isReceiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    val enabled = state == BluetoothAdapter.STATE_ON
                    _isBluetoothEnabled.value = enabled
                    if (enabled) {
                        refreshAdapterMetadata()
                        refreshPairedDevices()
                        connectionService.startServerIfReady()
                    } else {
                        _isDiscovering.value = false
                        _isDiscoverable.value = false
                        connectionService.stopServer()
                        connectionService.disconnectAll()
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _isDiscovering.value = true
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isDiscovering.value = false
                }

                BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> {
                    val mode = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_SCAN_MODE,
                        BluetoothAdapter.SCAN_MODE_NONE
                    )
                    _isDiscoverable.value = mode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
                }

                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                        .takeIf { it != Short.MIN_VALUE }?.toInt()
                    device?.let { addOrUpdateDiscovered(it, rssi) }
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    refreshPairedDevices()
                }
            }
        }
    }

    fun register() {
        if (isReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        isReceiverRegistered = true
        syncCurrentState()
    }

    fun unregister() {
        if (!isReceiverRegistered) return
        runCatching { context.unregisterReceiver(receiver) }
        isReceiverRegistered = false
    }

    @SuppressLint("MissingPermission")
    fun syncCurrentState() {
        val currentAdapter = adapter
        _isBluetoothSupported.value = currentAdapter != null
        _isBluetoothEnabled.value = currentAdapter?.isEnabled == true
        if (currentAdapter != null && currentAdapter.isEnabled) {
            refreshAdapterMetadata()
            refreshPairedDevices()
            if (BluetoothPermissions.hasConnectPermission(context)) {
                _isDiscoverable.value =
                    currentAdapter.scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
                connectionService.startServerIfReady()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun refreshAdapterMetadata() {
        val currentAdapter = adapter ?: return
        if (BluetoothPermissions.hasConnectPermission(context)) {
            _adapterName.value = runCatching { currentAdapter.name }.getOrNull()
                ?.takeIf { it.isNotBlank() } ?: Build.MODEL
        } else {
            _adapterName.value = Build.MODEL
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        val currentAdapter = adapter ?: return
        if (!BluetoothPermissions.hasConnectPermission(context) || !currentAdapter.isEnabled) {
            _pairedPeers.value = emptyList()
            return
        }
        val bonded = runCatching { currentAdapter.bondedDevices }.getOrNull().orEmpty()
        _pairedPeers.value = bonded.map { device ->
            val name = runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: "Paired Device"
            BluetoothPeer(
                address = device.address,
                name = name,
                isPaired = true,
                status = PeerConnectionStatus.DISCONNECTED
            )
        }.sortedBy { it.name.lowercase() }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {
        val currentAdapter = adapter ?: return false
        if (!currentAdapter.isEnabled || !BluetoothPermissions.hasScanPermission(context)) {
            return false
        }
        return try {
            if (currentAdapter.isDiscovering) {
                currentAdapter.cancelDiscovery()
            }
            _discoveredPeers.value = emptyList()
            refreshPairedDevices()
            val started = currentAdapter.startDiscovery()
            _isDiscovering.value = started
            started
        } catch (_: SecurityException) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        val currentAdapter = adapter ?: return
        if (!BluetoothPermissions.hasScanPermission(context)) return
        runCatching {
            if (currentAdapter.isDiscovering) {
                currentAdapter.cancelDiscovery()
            }
        }
        _isDiscovering.value = false
    }

    @SuppressLint("MissingPermission")
    private fun addOrUpdateDiscovered(device: BluetoothDevice, rssi: Int?) {
        val name = if (BluetoothPermissions.hasConnectPermission(context)) {
            runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Unknown Bluetooth Device"
        } else {
            "Bluetooth Device"
        }
        val isPaired = if (BluetoothPermissions.hasConnectPermission(context)) {
            runCatching { device.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false)
        } else {
            false
        }

        val peer = BluetoothPeer(
            address = device.address,
            name = name,
            isPaired = isPaired,
            rssi = rssi,
            lastSeenAt = System.currentTimeMillis()
        )
        _discoveredPeers.update { current ->
            val filtered = current.filterNot { it.address == peer.address }
            listOf(peer) + filtered
        }
    }
}
