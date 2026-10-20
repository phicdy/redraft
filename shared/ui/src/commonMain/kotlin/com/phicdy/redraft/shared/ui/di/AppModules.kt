package com.phicdy.redraft.shared.ui.di

import com.phicdy.redraft.shared.data.ApiKeyStore
import com.phicdy.redraft.shared.data.ConnectionSettings
import com.phicdy.redraft.shared.data.ConnectionSettingsRepository
import com.phicdy.redraft.shared.data.di.dataModule
import com.phicdy.redraft.shared.llm.ApiKeyProvider
import com.phicdy.redraft.shared.llm.ProviderConfig
import com.phicdy.redraft.shared.llm.ProviderConfigProvider
import com.phicdy.redraft.shared.llm.di.llmModule
import com.phicdy.redraft.shared.llm.openai.OpenAiDefaults
import com.phicdy.redraft.shared.ui.main.MainViewModel
import com.phicdy.redraft.shared.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.first
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val uiModule: Module = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::SettingsViewModel)
}

/** Bridges the data layer's key storage to the LLM layer, which must not depend on it. */
val apiKeyBridgeModule: Module = module {
    single<ApiKeyProvider> {
        val store = get<ApiKeyStore>()
        ApiKeyProvider { providerId -> store.get(providerId) }
    }
}

/** Bridges the saved connection settings to the LLM layer; they are read on every request. */
val providerConfigBridgeModule: Module = module {
    single<ProviderConfigProvider> {
        val repository = get<ConnectionSettingsRepository>()
        ProviderConfigProvider { repository.settings.first().toProviderConfig() }
    }
}

/** Applies the user's overrides on top of the OpenAI defaults. */
internal fun ConnectionSettings.toProviderConfig(): ProviderConfig = OpenAiDefaults.config.copy(
    baseUrl = baseUrl ?: OpenAiDefaults.BASE_URL,
    defaultModel = model ?: OpenAiDefaults.MODEL,
)

/** All Koin modules, passed to startKoin by each app entry point. */
val redraftModules: List<Module> =
    listOf(llmModule, dataModule, apiKeyBridgeModule, providerConfigBridgeModule, uiModule)
