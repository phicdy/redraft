package com.phicdy.redraft.shared.data.di

import com.phicdy.redraft.shared.data.ConnectionSettingsRepository
import com.phicdy.redraft.shared.data.DataStoreConnectionSettingsRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Platform bindings: an `ApiKeyStore` and the settings `DataStore<Preferences>`.
 * Android needs `androidContext(...)` to be set when Koin starts.
 */
internal expect val platformDataModule: Module

/** History (Room) bindings are added here as they are implemented. */
val dataModule: Module = module {
    includes(platformDataModule)
    single<ConnectionSettingsRepository> { DataStoreConnectionSettingsRepository(dataStore = get()) }
}

/** File name of the settings DataStore. Preferences DataStore requires the `.preferences_pb` extension. */
internal const val SETTINGS_FILE_NAME: String = "connection_settings.preferences_pb"
