package com.phicdy.redraft.shared.llm.openai

import com.phicdy.redraft.shared.llm.ProviderConfig
import com.phicdy.redraft.shared.llm.ProviderKind

/** Defaults for a direct OpenAI connection. A company proxy overrides [BASE_URL] via [ProviderConfig]. */
object OpenAiDefaults {
    const val BASE_URL: String = "https://api.openai.com/v1"
    const val MODEL: String = "gpt-6.1-sol"
    const val CONFIG_ID: String = "openai"

    val config: ProviderConfig = ProviderConfig(
        id = CONFIG_ID,
        kind = ProviderKind.OpenAiResponses,
        baseUrl = BASE_URL,
        defaultModel = MODEL,
    )
}
