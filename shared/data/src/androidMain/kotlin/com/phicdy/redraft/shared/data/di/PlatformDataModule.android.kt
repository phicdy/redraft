package com.phicdy.redraft.shared.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

/** `Context` is registered by `androidContext(...)` in `RedraftApplication`. */
internal actual val platformDataModule: Module = module {
    single<DataStore<Preferences>> {
        val context = get<Context>()
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { File(context.filesDir, "datastore/$SETTINGS_FILE_NAME").also { it.parentFile?.mkdirs() } },
        )
    }
}
