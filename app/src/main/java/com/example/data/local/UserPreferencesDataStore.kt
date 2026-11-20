package com.example.data.local

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "resilient_prefs")

data class UserSettings(
    val callsign: String,
    val localNodeId: String,
    val autoAcceptConnections: Boolean,
    val autoRelayEnabled: Boolean,
    val maxHopLimit: Int,
    val vibrateOnSos: Boolean
)

class UserPreferencesDataStore(private val context: Context) {

    private object Keys {
        val CALLSIGN = stringPreferencesKey("callsign")
        val LOCAL_NODE_ID = stringPreferencesKey("local_node_id")
        val AUTO_ACCEPT = booleanPreferencesKey("auto_accept_connections")
        val AUTO_RELAY = booleanPreferencesKey("auto_relay_enabled")
        val MAX_HOP_LIMIT = intPreferencesKey("max_hop_limit")
        val VIBRATE_ON_SOS = booleanPreferencesKey("vibrate_on_sos")
    }

    private fun defaultCallsign(): String {
        val model = (Build.MODEL ?: "Android").replace(" ", "")
        return "Node-${model.take(10)}"
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            callsign = prefs[Keys.CALLSIGN] ?: defaultCallsign(),
            localNodeId = prefs[Keys.LOCAL_NODE_ID] ?: "",
            autoAcceptConnections = prefs[Keys.AUTO_ACCEPT] ?: true,
            autoRelayEnabled = prefs[Keys.AUTO_RELAY] ?: true,
            maxHopLimit = prefs[Keys.MAX_HOP_LIMIT] ?: 3,
            vibrateOnSos = prefs[Keys.VIBRATE_ON_SOS] ?: true
        )
    }

    suspend fun ensureLocalNodeId(): String {
        val current = context.dataStore.data.first()[Keys.LOCAL_NODE_ID]
        if (!current.isNullOrBlank()) return current
        val generated = UUID.randomUUID().toString().take(8).uppercase()
        context.dataStore.edit { prefs ->
            prefs[Keys.LOCAL_NODE_ID] = generated
            if (prefs[Keys.CALLSIGN].isNullOrBlank()) {
                prefs[Keys.CALLSIGN] = defaultCallsign()
            }
        }
        return generated
    }

    suspend fun updateCallsign(callsign: String) {
        val cleaned = callsign.trim().ifBlank { defaultCallsign() }
        context.dataStore.edit { it[Keys.CALLSIGN] = cleaned }
    }

    suspend fun setAutoAccept(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_ACCEPT] = enabled }
    }

    suspend fun setAutoRelay(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_RELAY] = enabled }
    }

    suspend fun setMaxHopLimit(limit: Int) {
        context.dataStore.edit { it[Keys.MAX_HOP_LIMIT] = limit.coerceIn(1, 5) }
    }

    suspend fun setVibrateOnSos(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATE_ON_SOS] = enabled }
    }
}
