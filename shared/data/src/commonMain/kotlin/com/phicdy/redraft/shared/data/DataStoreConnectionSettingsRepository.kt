package com.phicdy.redraft.shared.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException

/** Stores [ConnectionSettings] in a Preferences DataStore. */
class DataStoreConnectionSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : ConnectionSettingsRepository {
    override val settings: Flow<ConnectionSettings> = dataStore.data
        .catch { e ->
            // An unreadable file (I/O error) falls back to the defaults instead of breaking every request.
            // Corrupted content is handled by the DataStore's corruption handler. Saving still reports errors.
            if (e is CancellationException) throw e
            emit(emptyPreferences())
        }
        .map { preferences ->
            ConnectionSettings(
                baseUrl = preferences[BASE_URL],
                model = preferences[MODEL],
            )
        }
        .distinctUntilChanged()

    override suspend fun save(settings: ConnectionSettings) {
        dataStore.edit { preferences ->
            preferences.setOrRemove(BASE_URL, settings.baseUrl)
            preferences.setOrRemove(MODEL, settings.model)
        }
    }

    private fun MutablePreferences.setOrRemove(key: Preferences.Key<String>, value: String?) {
        if (value == null) remove(key) else set(key, value)
    }

    private companion object {
        val BASE_URL: Preferences.Key<String> = stringPreferencesKey("base_url")
        val MODEL: Preferences.Key<String> = stringPreferencesKey("model")
    }
}
