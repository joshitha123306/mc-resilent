package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.model.PacketEnvelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class IncomingPacketEvent(
    val envelope: PacketEnvelope,
    val fromPeerAddress: String,
    val fromPeerName: String
)

sealed interface ConnectionEvent {
    data class Connected(val address: String, val name: String) : ConnectionEvent
    data class Disconnected(val address: String, val name: String, val reason: String) : ConnectionEvent
    data class ConnectionFailed(val address: String, val name: String, val error: String) : ConnectionEvent
    data class ServerStatus(val isListening: Boolean, val detail: String) : ConnectionEvent
}

class BluetoothConnectionService(private val context: Context) {

    companion object {
        private const val TAG = "BtConnectionService"
        const val SERVICE_NAME = "ResilientConnectRFCOMM"
        val APP_UUID: UUID = UUID.fromString("8a4f2c10-9b3d-4e7a-a1c2-5f8e9d0b1a23")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(BluetoothManager::class.java)

    val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private class PeerSession(
        val address: String,
        val name: String,
        val socket: BluetoothSocket,
        val outputStream: OutputStream,
        val writeMutex: Mutex = Mutex(),
        var readerJob: Job? = null
    )

    private val sessions = ConcurrentHashMap<String, PeerSession>()

    private var serverJob: Job? = null
    private var serverSocket: BluetoothServerSocket? = null

    private val _connectedPeers = MutableStateFlow<Map<String, String>>(emptyMap())
    val connectedPeers: StateFlow<Map<String, String>> = _connectedPeers.asStateFlow()

    private val _connectingAddresses = MutableStateFlow<Set<String>>(emptySet())
    val connectingAddresses: StateFlow<Set<String>> = _connectingAddresses.asStateFlow()

    private val _isServerListening = MutableStateFlow(false)
    val isServerListening: StateFlow<Boolean> = _isServerListening.asStateFlow()

    private val _incomingPackets = MutableSharedFlow<IncomingPacketEvent>(extraBufferCapacity = 64)
    val incomingPackets: SharedFlow<IncomingPacketEvent> = _incomingPackets.asSharedFlow()

    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 32)
    val connectionEvents: SharedFlow<ConnectionEvent> = _connectionEvents.asSharedFlow()

    @SuppressLint("MissingPermission")
    fun startServerIfReady() {
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled || !BluetoothPermissions.hasConnectPermission(context)) {
            _isServerListening.value = false
            return
        }
        if (serverJob?.isActive == true) return

        serverJob = scope.launch {
            while (isActive && adapter.isEnabled && BluetoothPermissions.hasConnectPermission(context)) {
                try {
                    val srv = adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, APP_UUID)
                    serverSocket = srv
                    _isServerListening.value = true
                    _connectionEvents.tryEmit(
                        ConnectionEvent.ServerStatus(true, "Listening for RFCOMM connections")
                    )

                    while (isActive) {
                        val acceptedSocket = srv.accept() ?: break
                        attachSession(acceptedSocket)
                    }
                } catch (e: IOException) {
                    Log.w(TAG, "RFCOMM accept loop ended: ${e.message}")
                    _isServerListening.value = false
                    break
                } catch (se: SecurityException) {
                    Log.w(TAG, "Missing Bluetooth connect permission for server", se)
                    _isServerListening.value = false
                    break
                }
            }
            closeServerSocketQuietly()
            _isServerListening.value = false
        }
    }

    fun stopServer() {
        serverJob?.cancel()
        serverJob = null
        closeServerSocketQuietly()
        _isServerListening.value = false
    }

    private fun closeServerSocketQuietly() {
        try {
            serverSocket?.close()
        } catch (_: IOException) {
        } finally {
            serverSocket = null
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String) {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionEvents.tryEmit(
                ConnectionEvent.ConnectionFailed(address, address, "Bluetooth is turned off.")
            )
            return
        }
        if (!BluetoothPermissions.hasConnectPermission(context)) {
            _connectionEvents.tryEmit(
                ConnectionEvent.ConnectionFailed(address, address, "Missing BLUETOOTH_CONNECT permission.")
            )
            return
        }
        if (sessions.containsKey(address)) {
            return
        }

        scope.launch {
            _connectingAddresses.update { it + address }
            val device: BluetoothDevice = try {
                adapter.getRemoteDevice(address)
            } catch (e: Exception) {
                _connectingAddresses.update { it - address }
                _connectionEvents.tryEmit(
                    ConnectionEvent.ConnectionFailed(address, address, "Invalid Bluetooth address.")
                )
                return@launch
            }

            val peerName = runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: address

            try {
                if (BluetoothPermissions.hasScanPermission(context) && adapter.isDiscovering) {
                    adapter.cancelDiscovery()
                }
            } catch (_: SecurityException) {
            }

            var socket: BluetoothSocket? = null
            try {
                socket = device.createRfcommSocketToServiceRecord(APP_UUID)
                socket.connect()
                attachSession(socket)
            } catch (primaryEx: IOException) {
                Log.w(TAG, "Primary RFCOMM connect failed for $address, trying channel 1 fallback", primaryEx)
                runCatching { socket?.close() }
                try {
                    val fallbackMethod = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    val fallbackSocket = fallbackMethod.invoke(device, 1) as BluetoothSocket
                    fallbackSocket.connect()
                    attachSession(fallbackSocket)
                } catch (fallbackEx: Exception) {
                    Log.e(TAG, "Fallback RFCOMM connect failed for $address", fallbackEx)
                    _connectionEvents.tryEmit(
                        ConnectionEvent.ConnectionFailed(
                            address = address,
                            name = peerName,
                            error = "Could not connect to $peerName. Make sure Resilient Connect is open on the other phone."
                        )
                    )
                }
            } catch (se: SecurityException) {
                _connectionEvents.tryEmit(
                    ConnectionEvent.ConnectionFailed(address, peerName, "Bluetooth permission denied.")
                )
            } finally {
                _connectingAddresses.update { it - address }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun attachSession(socket: BluetoothSocket) {
        val device = socket.remoteDevice
        val address = device.address
        val name = if (BluetoothPermissions.hasConnectPermission(context)) {
            runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: address
        } else {
            address
        }

        // Replace stale session if any
        sessions.remove(address)?.let { existing ->
            existing.readerJob?.cancel()
            runCatching { existing.socket.close() }
        }

        val session = PeerSession(
            address = address,
            name = name,
            socket = socket,
            outputStream = socket.outputStream
        )

        sessions[address] = session
        refreshConnectedPeers()
        _connectionEvents.tryEmit(ConnectionEvent.Connected(address, name))

        session.readerJob = scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue
                    val envelope = PacketEnvelope.fromJsonLine(line)
                    if (envelope != null) {
                        _incomingPackets.emit(
                            IncomingPacketEvent(
                                envelope = envelope,
                                fromPeerAddress = address,
                                fromPeerName = name
                            )
                        )
                    }
                }
            } catch (e: IOException) {
                Log.i(TAG, "Socket reader closed for $address: ${e.message}")
            } finally {
                removeSession(address, "Connection closed")
            }
        }
    }

    suspend fun sendPacketToPeer(address: String, envelope: PacketEnvelope): Boolean {
        val session = sessions[address] ?: return false
        return writeEnvelope(session, envelope)
    }

    suspend fun broadcastPacket(
        envelope: PacketEnvelope,
        excludeAddress: String? = null
    ): List<String> {
        val deliveredTo = mutableListOf<String>()
        val activeSessions = sessions.values.toList()
        for (session in activeSessions) {
            if (excludeAddress != null && session.address == excludeAddress) continue
            if (writeEnvelope(session, envelope)) {
                deliveredTo.add(session.address)
            }
        }
        return deliveredTo
    }

    private suspend fun writeEnvelope(session: PeerSession, envelope: PacketEnvelope): Boolean {
        return withContext(Dispatchers.IO) {
            session.writeMutex.withLock {
                try {
                    val bytes = (envelope.toJsonLine() + "\n").toByteArray(Charsets.UTF_8)
                    session.outputStream.write(bytes)
                    session.outputStream.flush()
                    true
                } catch (e: IOException) {
                    Log.e(TAG, "Write failed to ${session.address}", e)
                    removeSession(session.address, "Write error / link lost")
                    false
                }
            }
        }
    }

    fun disconnectPeer(address: String) {
        removeSession(address, "Disconnected by user")
    }

    fun disconnectAll() {
        val keys = sessions.keys.toList()
        keys.forEach { removeSession(it, "Disconnected") }
    }

    private fun removeSession(address: String, reason: String) {
        val removed = sessions.remove(address) ?: return
        removed.readerJob?.cancel()
        runCatching { removed.socket.close() }
        refreshConnectedPeers()
        _connectionEvents.tryEmit(ConnectionEvent.Disconnected(address, removed.name, reason))
    }

    private fun refreshConnectedPeers() {
        _connectedPeers.value = sessions.mapValues { it.value.name }
    }
}
