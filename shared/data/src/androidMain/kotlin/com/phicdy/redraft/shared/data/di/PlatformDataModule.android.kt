package com.phicdy.redraft.shared.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room.Room
import com.phicdy.redraft.shared.data.AndroidKeystoreApiKeyStore
import com.phicdy.redraft.shared.data.ApiKeyStore
import com.phicdy.redraft.shared.data.history.db.HISTORY_DATABASE_FILE_NAME
import com.phicdy.redraft.shared.data.history.db.HistoryDatabase
import com.phicdy.redraft.shared.data.history.db.buildHistoryDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

/** `Context` is registered by `androidContext(...)` in `RedraftApplication`. */
internal actual val platformDataModule: Module = module {
    single<ApiKeyStore> { AndroidKeystoreApiKeyStore(context = get<Context>()) }
    single<DataStore<Preferences>> {
        val context = get<Context>()
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { File(context.filesDir, "datastore/$SETTINGS_FILE_NAME").also { it.parentFile?.mkdirs() } },
        )
    }
    single<HistoryDatabase> {
        // The app's standard database directory, which is private to the app and excluded from backups
        // (allowBackup is false).
        val context = get<Context>().applicationContext
        Room.databaseBuilder<HistoryDatabase>(
            context = context,
            name = context.getDatabasePath(HISTORY_DATABASE_FILE_NAME).absolutePath,
        ).buildHistoryDatabase(queryContext = Dispatchers.IO)
    }
}
