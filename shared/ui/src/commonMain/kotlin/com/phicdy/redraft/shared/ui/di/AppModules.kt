package com.phicdy.redraft.shared.ui.di

import com.phicdy.redraft.shared.data.ApiKeyStore
import com.phicdy.redraft.shared.data.di.dataModule
import com.phicdy.redraft.shared.llm.ApiKeyProvider
import com.phicdy.redraft.shared.llm.di.llmModule
import com.phicdy.redraft.shared.ui.main.MainViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val uiModule: Module = module {
    viewModelOf(::MainViewModel)
}

/**
 * Bridges the data layer's key storage to the LLM layer, which must not depend on it.
 * Until an [ApiKeyStore] implementation is bound, every lookup returns null and requests
 * fail with `LlmError.MissingApiKey`.
 */
val apiKeyBridgeModule: Module = module {
    single<ApiKeyProvider> {
        val store = getOrNull<ApiKeyStore>()
        ApiKeyProvider { providerId -> store?.get(providerId) }
    }
}

/** All Koin modules, passed to startKoin by each app entry point. */
val redraftModules: List<Module> = listOf(llmModule, dataModule, apiKeyBridgeModule, uiModule)
