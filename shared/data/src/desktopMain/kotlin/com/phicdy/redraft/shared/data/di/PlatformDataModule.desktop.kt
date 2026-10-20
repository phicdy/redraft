package com.phicdy.redraft.shared.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.phicdy.redraft.shared.data.ApiKeyStore
import com.phicdy.redraft.shared.data.InMemoryApiKeyStore
import com.phicdy.redraft.shared.data.MacKeychainApiKeyStore
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

internal actual val platformDataModule: Module = module {
    single<ApiKeyStore> {
        // Redraft ships for macOS. Elsewhere (Linux CI, local runs) keys are kept in memory only,
        // so the app works for the session without writing a plaintext key to disk.
        if (isMacOs()) MacKeychainApiKeyStore() else InMemoryApiKeyStore()
    }
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
