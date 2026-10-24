package com.phicdy.redraft.shared.ui.di

import com.phicdy.redraft.shared.data.ApiKeyStore
import com.phicdy.redraft.shared.data.di.dataModule
import com.phicdy.redraft.shared.llm.LlmConnection
import com.phicdy.redraft.shared.llm.LlmConnectionSource
import com.phicdy.redraft.shared.llm.di.llmModule
import com.phicdy.redraft.shared.llm.openai.OpenAiDefaults
import com.phicdy.redraft.shared.ui.main.MainViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val uiModule: Module = module {
    viewModelOf(::MainViewModel)
}

/**
 * Supplies the LLM layer, which must not depend on the data layer, with the connection
 * settings and API key, read on every request.
 *
 * Until connection settings are saved in the data layer, the config is always the OpenAI default.
 * Until an [ApiKeyStore] implementation is bound, the key is null and requests fail with
 * `LlmError.MissingApiKey`.
 */
val connectionBridgeModule: Module = module {
    single<LlmConnectionSource> {
        val store = getOrNull<ApiKeyStore>()
        LlmConnectionSource {
            val config = OpenAiDefaults.config
            LlmConnection(config = config, apiKey = store?.get(config.id))
        }
    }
}

/** All Koin modules, passed to startKoin by each app entry point. */
val redraftModules: List<Module> = listOf(llmModule, dataModule, connectionBridgeModule, uiModule)
