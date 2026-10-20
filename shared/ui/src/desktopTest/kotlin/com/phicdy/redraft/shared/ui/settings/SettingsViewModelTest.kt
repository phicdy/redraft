package com.phicdy.redraft.shared.ui.settings

import com.phicdy.redraft.shared.data.ApiKeyStore
import com.phicdy.redraft.shared.data.ConnectionSettings
import com.phicdy.redraft.shared.data.ConnectionSettingsRepository
import com.phicdy.redraft.shared.data.InMemoryApiKeyStore
import com.phicdy.redraft.shared.llm.openai.OpenAiDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private class FakeSettingsRepository(initial: ConnectionSettings = ConnectionSettings()) :
        ConnectionSettingsRepository {
        val state = MutableStateFlow(initial)
        var failSave = false
        override val settings: Flow<ConnectionSettings> = state

        override suspend fun save(settings: ConnectionSettings) {
            if (failSave) throw IllegalStateException("disk full")
            state.value = settings
        }
    }

    private class FailingKeyStore : ApiKeyStore {
        override suspend fun get(providerId: String): String? = null
        override suspend fun contains(providerId: String): Boolean = false
        override suspend fun put(providerId: String, apiKey: String): Unit = throw IllegalStateException("locked")
        override suspend fun remove(providerId: String): Unit = throw IllegalStateException("locked")
    }

    private val keyStore = InMemoryApiKeyStore()
    private val repository = FakeSettingsRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(store: ApiKeyStore = keyStore): SettingsViewModel = SettingsViewModel(store, repository)

    @Test
    fun loadsDefaultsWhenNothingIsSaved(): TestResult = runTest {
        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.hasSavedApiKey)
        assertEquals(OpenAiDefaults.BASE_URL, state.baseUrl)
        assertEquals(OpenAiDefaults.MODEL, state.model)
        assertEquals("", state.apiKeyInput)
    }

    @Test
    fun loadsSavedValuesWithoutExposingTheKey(): TestResult = runTest {
        keyStore.put(OpenAiDefaults.CONFIG_ID, "sk-secret")
        repository.state.value = ConnectionSettings(baseUrl = "https://proxy.example.com/v1", model = "m")

        val state = viewModel().uiState.value

        assertTrue(state.hasSavedApiKey)
        assertEquals("", state.apiKeyInput)
        assertFalse("sk-secret" in state.toString())
        assertEquals("https://proxy.example.com/v1", state.baseUrl)
        assertEquals("m", state.model)
    }

    @Test
    fun savesKeyAndNormalizedSettings(): TestResult = runTest {
        val viewModel = viewModel()
        viewModel.onApiKeyChange("  sk-new  ")
        viewModel.onBaseUrlChange(" https://proxy.example.com/v1/ ")
        viewModel.onModelChange(" test-model ")

        viewModel.onSave()

        val state = viewModel.uiState.value
        assertEquals("sk-new", keyStore.get(OpenAiDefaults.CONFIG_ID))
        assertEquals(
            ConnectionSettings(baseUrl = "https://proxy.example.com/v1", model = "test-model"),
            repository.state.value,
        )
        assertEquals("", state.apiKeyInput)
        assertTrue(state.hasSavedApiKey)
        assertEquals(SettingsMessage.Saved, state.message)
        assertFalse(state.isSaving)
    }

    @Test
    fun emptyKeyInputKeepsTheSavedKey(): TestResult = runTest {
        keyStore.put(OpenAiDefaults.CONFIG_ID, "sk-old")
        val viewModel = viewModel()
        viewModel.onModelChange("other-model")

        viewModel.onSave()

        assertEquals("sk-old", keyStore.get(OpenAiDefaults.CONFIG_ID))
        assertEquals("other-model", repository.state.value.model)
        assertTrue(viewModel.uiState.value.hasSavedApiKey)
    }

    @Test
    fun invalidInputIsNotSaved(): TestResult = runTest {
        val viewModel = viewModel()
        viewModel.onApiKeyChange("sk with spaces")
        viewModel.onBaseUrlChange("http://proxy.example.com/v1")
        viewModel.onModelChange("")

        viewModel.onSave()

        val state = viewModel.uiState.value
        assertEquals(ApiKeyError.InvalidCharacters, state.apiKeyError)
        assertEquals(BaseUrlError.NotHttps, state.baseUrlError)
        assertEquals(ModelError.Blank, state.modelError)
        assertNull(state.message)
        assertNull(keyStore.get(OpenAiDefaults.CONFIG_ID))
        assertEquals(ConnectionSettings(), repository.state.value)
    }

    @Test
    fun editingAFieldClearsItsError(): TestResult = runTest {
        val viewModel = viewModel()
        viewModel.onBaseUrlChange("http://x")
        viewModel.onSave()

        viewModel.onBaseUrlChange("https://x")

        assertNull(viewModel.uiState.value.baseUrlError)
    }

    @Test
    fun removesTheKey(): TestResult = runTest {
        keyStore.put(OpenAiDefaults.CONFIG_ID, "sk-old")
        val viewModel = viewModel()

        viewModel.onRemoveApiKey()

        assertNull(keyStore.get(OpenAiDefaults.CONFIG_ID))
        assertFalse(viewModel.uiState.value.hasSavedApiKey)
        assertEquals(SettingsMessage.ApiKeyRemoved, viewModel.uiState.value.message)
    }

    @Test
    fun storageFailuresAreReportedNotThrown(): TestResult = runTest {
        val viewModel = viewModel(store = FailingKeyStore())
        viewModel.onApiKeyChange("sk-new")

        viewModel.onSave()

        assertEquals(SettingsMessage.SaveFailed, viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals("sk-new", viewModel.uiState.value.apiKeyInput)
    }

    @Test
    fun settingsSaveFailureIsReported(): TestResult = runTest {
        repository.failSave = true
        val viewModel = viewModel()

        viewModel.onSave()

        assertEquals(SettingsMessage.SaveFailed, viewModel.uiState.value.message)
    }

    @Test
    fun reloadDiscardsUnsavedEdits(): TestResult = runTest {
        val viewModel = viewModel()
        viewModel.onApiKeyChange("sk-typed")
        viewModel.onModelChange("unsaved")

        viewModel.reload()

        assertEquals("", viewModel.uiState.value.apiKeyInput)
        assertEquals(OpenAiDefaults.MODEL, viewModel.uiState.value.model)
    }
}
