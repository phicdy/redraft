package com.phicdy.redraft.shared.ui.settings

import com.phicdy.redraft.shared.llm.openai.OpenAiDefaults

data class SettingsUiState(
    val isLoading: Boolean = true,
    /** What the user is typing. The stored key is never loaded into the UI. */
    val apiKeyInput: String = "",
    val hasSavedApiKey: Boolean = false,
    val baseUrl: String = OpenAiDefaults.BASE_URL,
    val model: String = OpenAiDefaults.MODEL,
    val apiKeyError: ApiKeyError? = null,
    val baseUrlError: BaseUrlError? = null,
    val modelError: ModelError? = null,
    val isSaving: Boolean = false,
    val message: SettingsMessage? = null,
) {
    val canSave: Boolean
        get() = !isLoading && !isSaving

    val canRemoveApiKey: Boolean
        get() = !isLoading && !isSaving && hasSavedApiKey
}

/** Result of the last action, shown below the buttons. */
enum class SettingsMessage {
    Saved,
    ApiKeyRemoved,
    LoadFailed,
    SaveFailed,
    RemoveFailed,
}
