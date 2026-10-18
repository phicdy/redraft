package com.phicdy.redraft.shared.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Connection settings for one provider.
 *
 * API keys are intentionally not part of this class. They are kept in the
 * platform secure storage so that a config can be exported without secrets.
 */
@Serializable
data class ProviderConfig(
    @SerialName("id")
    val id: String,
    @SerialName("kind")
    val kind: ProviderKind,
    @SerialName("base_url")
    val baseUrl: String,
    @SerialName("default_model")
    val defaultModel: String,
    @SerialName("extra_headers")
    val extraHeaders: Map<String, String> = emptyMap(),
)

@Serializable
enum class ProviderKind {
    @SerialName("openai_responses")
    OpenAiResponses,

    @SerialName("chat_completions")
    ChatCompletions,

    @SerialName("anthropic_messages")
    AnthropicMessages,
}
