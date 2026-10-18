package com.phicdy.redraft.shared.llm

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject

/**
 * Provider-independent entry point to an LLM.
 *
 * Each provider (OpenAI Responses, Chat Completions compatible, Anthropic Messages)
 * gets its own adapter that only converts [LlmRequest] to the provider's format and
 * the response back to [LlmResult]. Retries and schema validation live outside adapters.
 */
interface LlmClient {
    suspend fun <T> generate(request: LlmRequest, outputSerializer: KSerializer<T>): LlmResult<T>
}

data class LlmRequest(
    /** System prompt. */
    val instructions: String,
    /** User input. */
    val input: String,
    /** JSON schema for structured output. */
    val schema: OutputSchema,
    val model: String,
    val maxOutputTokens: Int,
)

data class OutputSchema(
    val name: String,
    val schema: JsonObject,
)

data class LlmResult<T>(
    val output: T,
    val usage: TokenUsage,
    /** The model that actually answered. */
    val model: String,
)

data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int,
)
