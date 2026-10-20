package com.phicdy.redraft.shared.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phicdy.redraft.shared.data.ApiKeyStore
import com.phicdy.redraft.shared.data.ConnectionSettings
import com.phicdy.redraft.shared.data.ConnectionSettingsRepository
import com.phicdy.redraft.shared.llm.openai.OpenAiDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class SettingsViewModel(
    private val apiKeyStore: ApiKeyStore,
    private val settingsRepository: ConnectionSettingsRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        reload()
    }

    /** Loads the saved values and discards unsaved edits, errors and messages. */
    fun reload() {
        mutableUiState.value = SettingsUiState(isLoading = true)
        viewModelScope.launch {
            val loaded = runCatchingNonCancellation {
                val settings = settingsRepository.settings.first()
                val hasKey = apiKeyStore.contains(PROVIDER_ID)
                SettingsUiState(
                    isLoading = false,
                    hasSavedApiKey = hasKey,
                    baseUrl = settings.baseUrl ?: OpenAiDefaults.BASE_URL,
                    model = settings.model ?: OpenAiDefaults.MODEL,
                )
            }
            mutableUiState.value = loaded ?: SettingsUiState(isLoading = false, message = SettingsMessage.LoadFailed)
        }
    }

    fun onApiKeyChange(value: String) {
        mutableUiState.update { it.copy(apiKeyInput = value, apiKeyError = null, message = null) }
    }

    fun onBaseUrlChange(value: String) {
        mutableUiState.update { it.copy(baseUrl = value, baseUrlError = null, message = null) }
    }

    fun onModelChange(value: String) {
        mutableUiState.update { it.copy(model = value, modelError = null, message = null) }
    }

    fun onSave() {
        val state = uiState.value
        if (!state.canSave) return
        val newKey = state.apiKeyInput.trim()
        val apiKeyError = if (newKey.isEmpty()) null else validateApiKey(newKey)
        val baseUrlError = validateBaseUrl(state.baseUrl)
        val modelError = validateModel(state.model)
        if (apiKeyError != null || baseUrlError != null || modelError != null) {
            mutableUiState.update {
                it.copy(
                    apiKeyError = apiKeyError,
                    baseUrlError = baseUrlError,
                    modelError = modelError,
                    message = null,
                )
            }
            return
        }

        val baseUrl = normalizeBaseUrl(state.baseUrl)
        val model = state.model.trim()
        mutableUiState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            val saved = runCatchingNonCancellation {
                if (newKey.isNotEmpty()) apiKeyStore.put(PROVIDER_ID, newKey)
                settingsRepository.save(ConnectionSettings(baseUrl = baseUrl, model = model))
            }
            mutableUiState.update {
                if (saved != null) {
                    it.copy(
                        isSaving = false,
                        apiKeyInput = "",
                        hasSavedApiKey = it.hasSavedApiKey || newKey.isNotEmpty(),
                        baseUrl = baseUrl,
                        model = model,
                        message = SettingsMessage.Saved,
                    )
                } else {
                    it.copy(isSaving = false, message = SettingsMessage.SaveFailed)
                }
            }
        }
    }

    fun onRemoveApiKey() {
        if (!uiState.value.canRemoveApiKey) return
        mutableUiState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            val removed = runCatchingNonCancellation { apiKeyStore.remove(PROVIDER_ID) }
            mutableUiState.update {
                if (removed != null) {
                    it.copy(
                        isSaving = false,
                        apiKeyInput = "",
                        apiKeyError = null,
                        hasSavedApiKey = false,
                        message = SettingsMessage.ApiKeyRemoved,
                    )
                } else {
                    it.copy(isSaving = false, message = SettingsMessage.RemoveFailed)
                }
            }
        }
    }

    private companion object {
        /** The settings screen edits the single OpenAI connection for now. */
        const val PROVIDER_ID: String = OpenAiDefaults.CONFIG_ID
    }
}

/** Returns null on failure. Storage errors are shown as a message instead of crashing the app. */
private inline fun <T : Any> runCatchingNonCancellation(block: () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}
