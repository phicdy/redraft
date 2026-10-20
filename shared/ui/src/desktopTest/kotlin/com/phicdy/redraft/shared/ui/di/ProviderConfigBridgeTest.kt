package com.phicdy.redraft.shared.ui.di

import com.phicdy.redraft.shared.data.ConnectionSettings
import com.phicdy.redraft.shared.llm.ProviderKind
import com.phicdy.redraft.shared.llm.openai.OpenAiDefaults
import kotlin.test.Test
import kotlin.test.assertEquals

class ProviderConfigBridgeTest {
    @Test
    fun unsetFieldsUseOpenAiDefaults() {
        assertEquals(OpenAiDefaults.config, ConnectionSettings().toProviderConfig())
    }

    @Test
    fun savedFieldsOverrideDefaults() {
        val config = ConnectionSettings(baseUrl = "https://proxy.example.com/v1", model = "m").toProviderConfig()

        assertEquals("https://proxy.example.com/v1", config.baseUrl)
        assertEquals("m", config.defaultModel)
        assertEquals(OpenAiDefaults.CONFIG_ID, config.id)
        assertEquals(ProviderKind.OpenAiResponses, config.kind)
    }
}
