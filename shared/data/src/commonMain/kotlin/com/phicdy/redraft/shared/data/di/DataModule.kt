package com.phicdy.redraft.shared.data.di

import com.phicdy.redraft.shared.data.ConnectionSettingsRepository
import com.phicdy.redraft.shared.data.DataStoreConnectionSettingsRepository
import com.phicdy.redraft.shared.data.history.DataStoreDeviceIdProvider
import com.phicdy.redraft.shared.data.history.DeviceIdProvider
import com.phicdy.redraft.shared.data.history.HistoryRepository
import com.phicdy.redraft.shared.data.history.RoomHistoryRepository
import com.phicdy.redraft.shared.data.history.db.HistoryDatabase
import com.phicdy.redraft.shared.data.report.RoomWeeklyReportRepository
import com.phicdy.redraft.shared.data.report.WeeklyReportRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Platform bindings: an `ApiKeyStore`, the settings `DataStore<Preferences>` and the `HistoryDatabase`.
 * Android needs `androidContext(...)` to be set when Koin starts.
 */
internal expect val platformDataModule: Module

val dataModule: Module = module {
    includes(platformDataModule)
    single<ConnectionSettingsRepository> { DataStoreConnectionSettingsRepository(dataStore = get()) }
    // The device id shares the settings DataStore; it is a single small value.
    single<DeviceIdProvider> { DataStoreDeviceIdProvider(dataStore = get()) }
    single<HistoryRepository> {
        RoomHistoryRepository(dao = get<HistoryDatabase>().historyDao(), deviceIdProvider = get())
    }
    single<WeeklyReportRepository> {
        RoomWeeklyReportRepository(dao = get<HistoryDatabase>().weeklyReportDao(), deviceIdProvider = get())
    }
}

/** File name of the settings DataStore. Preferences DataStore requires the `.preferences_pb` extension. */
internal const val SETTINGS_FILE_NAME: String = "connection_settings.preferences_pb"
