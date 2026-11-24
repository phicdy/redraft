package com.phicdy.redraft.shared.data.history

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.phicdy.redraft.shared.data.ConnectionSettings
import com.phicdy.redraft.shared.data.DataStoreConnectionSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DataStoreDeviceIdProviderTest {
    private val directory: File = Files.createTempDirectory("redraft-device-id-test").toFile()
    private var generated = 0
    private val newId: () -> String = { "device-${++generated}" }

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun openDataStore(): Pair<DataStore<Preferences>, Job> {
        val job = Job()
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { File(directory, "settings.preferences_pb") },
        )
        return dataStore to job
    }

    @Test
    fun idIsGeneratedOnceAndKept(): TestResult = runTest {
        val (first, firstJob) = openDataStore()
        val provider = DataStoreDeviceIdProvider(first, newId)
        assertEquals("device-1", provider.deviceId())
        assertEquals("device-1", provider.deviceId())
        firstJob.cancelAndJoin()

        val (second, secondJob) = openDataStore()
        assertEquals("device-1", DataStoreDeviceIdProvider(second, newId).deviceId())
        assertEquals(1, generated)
        secondJob.cancelAndJoin()
    }

    @Test
    fun sharesTheFileWithConnectionSettings(): TestResult = runTest {
        val (dataStore, job) = openDataStore()
        val settings = DataStoreConnectionSettingsRepository(dataStore)
        settings.save(ConnectionSettings(baseUrl = null, model = "test-model"))

        DataStoreDeviceIdProvider(dataStore, newId).deviceId()

        assertEquals(ConnectionSettings(baseUrl = null, model = "test-model"), settings.settings.first())
        job.cancelAndJoin()
    }
}
