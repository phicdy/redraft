package com.phicdy.redraft.shared.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
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

class DataStoreConnectionSettingsRepositoryTest {
    private val directory: File = Files.createTempDirectory("redraft-settings-test").toFile()
    private val file = File(directory, "connection_settings.preferences_pb")

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    /** Each test cancels the scope before reopening, because one file may have only one active DataStore. */
    private fun openDataStore(): Pair<DataStore<Preferences>, Job> {
        val job = Job()
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { file },
        )
        return dataStore to job
    }

    @Test
    fun emptyStoreHasNoOverrides(): TestResult = runTest {
        val (dataStore, job) = openDataStore()
        val repository = DataStoreConnectionSettingsRepository(dataStore)

        assertEquals(ConnectionSettings(baseUrl = null, model = null), repository.settings.first())
        job.cancelAndJoin()
    }

    @Test
    fun savedSettingsSurviveReopening(): TestResult = runTest {
        val saved = ConnectionSettings(baseUrl = "https://proxy.example.com/openai/v1", model = "test-model")
        val (first, firstJob) = openDataStore()
        DataStoreConnectionSettingsRepository(first).save(saved)
        firstJob.cancelAndJoin()

        val (second, secondJob) = openDataStore()
        val reopened = DataStoreConnectionSettingsRepository(second)

        assertEquals(saved, reopened.settings.first())
        secondJob.cancelAndJoin()
    }

    @Test
    fun nullFieldsClearPreviousValues(): TestResult = runTest {
        val (dataStore, job) = openDataStore()
        val repository = DataStoreConnectionSettingsRepository(dataStore)
        repository.save(ConnectionSettings(baseUrl = "https://proxy.example.com/v1", model = "test-model"))

        repository.save(ConnectionSettings(baseUrl = null, model = "other-model"))

        assertEquals(ConnectionSettings(baseUrl = null, model = "other-model"), repository.settings.first())
        job.cancelAndJoin()
    }
}
