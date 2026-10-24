package com.phicdy.redraft.shared.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

internal actual val platformDataModule: Module = module {
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { File(settingsDirectory(), SETTINGS_FILE_NAME).also { it.parentFile?.mkdirs() } },
        )
    }
}

private fun isMacOs(): Boolean = System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)

/** `~/Library/Application Support/Redraft` on macOS, `~/.config/redraft` elsewhere. */
private fun settingsDirectory(): File {
    val home = File(System.getProperty("user.home"))
    return if (isMacOs()) {
        File(home, "Library/Application Support/Redraft")
    } else {
        File(home, ".config/redraft")
    }
}
