package com.phicdy.redraft.shared.ui.di

import com.phicdy.redraft.shared.data.ApiKeyStore
import com.phicdy.redraft.shared.data.ConnectionSettings
import com.phicdy.redraft.shared.data.ConnectionSettingsRepository
import com.phicdy.redraft.shared.data.di.dataModule
import com.phicdy.redraft.shared.llm.LlmConnection
import com.phicdy.redraft.shared.llm.LlmConnectionSource
import com.phicdy.redraft.shared.llm.ProviderConfig
import com.phicdy.redraft.shared.llm.di.llmModule
import com.phicdy.redraft.shared.llm.openai.OpenAiDefaults
import com.phicdy.redraft.shared.ui.main.MainViewModel
import kotlinx.coroutines.flow.first
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val uiModule: Module = module {
    viewModelOf(::MainViewModel)
}

/**
 * Supplies the LLM layer, which must not depend on the data layer, with the saved connection
 * settings and API key, read on every request.
 *
 * Until an [ApiKeyStore] implementation is bound, the key is null and requests fail with
 * `LlmError.MissingApiKey`.
 */
val connectionBridgeModule: Module = module {
    single<LlmConnectionSource> {
        val repository = get<ConnectionSettingsRepository>()
        val store = getOrNull<ApiKeyStore>()
        LlmConnectionSource {
            val config = repository.settings.first().toProviderConfig()
            LlmConnection(config = config, apiKey = store?.get(config.id))
        }
    }
}

/** Applies the user's overrides on top of the OpenAI defaults. */
internal fun ConnectionSettings.toProviderConfig(): ProviderConfig = OpenAiDefaults.config.copy(
    baseUrl = baseUrl ?: OpenAiDefaults.BASE_URL,
    defaultModel = model ?: OpenAiDefaults.MODEL,
)

/** All Koin modules, passed to startKoin by each app entry point. */
val redraftModules: List<Module> = listOf(llmModule, dataModule, connectionBridgeModule, uiModule)
