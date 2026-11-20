package com.example

import android.app.Application
import com.example.bluetooth.BluetoothConnectionService
import com.example.bluetooth.BluetoothDiscoveryController
import com.example.data.local.ResilientDatabase
import com.example.data.local.UserPreferencesDataStore
import com.example.domain.MessageRepository
import com.example.domain.MultiHopForwardingManager
import com.example.domain.SosManager

class ResilientApplication : Application() {

    val database by lazy { ResilientDatabase.getInstance(this) }
    val preferences by lazy { UserPreferencesDataStore(this) }
    val connectionService by lazy { BluetoothConnectionService(this) }
    val discoveryController by lazy { BluetoothDiscoveryController(this, connectionService) }
    val sosManager by lazy { SosManager(this, database.messageDao()) }
    val multiHopManager by lazy {
        MultiHopForwardingManager(
            messageDao = database.messageDao(),
            connectionService = connectionService,
            preferences = preferences,
            sosManager = sosManager
        )
    }
    val repository by lazy {
        MessageRepository(
            messageDao = database.messageDao(),
            preferences = preferences,
            connectionService = connectionService,
            discoveryController = discoveryController,
            sosManager = sosManager,
            multiHopManager = multiHopManager
        )
    }

    override fun onCreate() {
        super.onCreate()
        discoveryController.register()
    }
}
