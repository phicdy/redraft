package com.phicdy.redraft.shared.data.history

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.Uuid

/**
 * Generates a random id on first use and keeps it in the app's Preferences DataStore, so every
 * history entry from this install carries the same device id. Reinstalling creates a new id.
 */
class DataStoreDeviceIdProvider(
    private val dataStore: DataStore<Preferences>,
    private val newId: () -> String = { Uuid.random().toString() },
) : DeviceIdProvider {
    private val mutex = Mutex()
    private var cached: String? = null

    override suspend fun deviceId(): String = mutex.withLock {
        cached ?: readOrCreate().also { cached = it }
    }

    private suspend fun readOrCreate(): String {
        // edit() is atomic, so the id is generated at most once even with several app windows.
        val preferences = dataStore.edit { current ->
            if (current[DEVICE_ID] == null) current[DEVICE_ID] = newId()
        }
        return checkNotNull(preferences[DEVICE_ID])
    }

    private companion object {
        val DEVICE_ID: Preferences.Key<String> = stringPreferencesKey("device_id")
    }
}
