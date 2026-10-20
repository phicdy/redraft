package com.phicdy.redraft.shared.llm

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * The provider-independent [LlmClient]: asks the [adapter] for raw output and decodes it.
 *
 * Decoding is strict (unknown keys fail) so it doubles as schema validation.
 * If the output cannot be decoded, the request is sent exactly once more; a second
 * failure is reported as [LlmError.InvalidOutput]. Other errors are not retried here.
 */
class StructuredLlmClient(
    private val adapter: LlmAdapter,
    private val outputJson: Json = Json,
) : LlmClient {
    override suspend fun <T> generate(request: LlmRequest, outputSerializer: KSerializer<T>): LlmResult<T> {
        var inputTokens = 0
        var outputTokens = 0
        var lastFailure: IllegalArgumentException? = null
        repeat(MAX_ATTEMPTS) {
            val raw = adapter.complete(request)
            inputTokens += raw.usage.inputTokens
            outputTokens += raw.usage.outputTokens
            try {
                val output = outputJson.decodeFromString(outputSerializer, raw.output)
                return LlmResult(
                    output = output,
                    usage = TokenUsage(inputTokens = inputTokens, outputTokens = outputTokens),
                    model = raw.model,
                )
            } catch (e: IllegalArgumentException) {
                // SerializationException is a subclass, and enum/required-field failures may surface as either.
                lastFailure = e
            }
        }
        throw LlmException(LlmError.InvalidOutput(lastFailure?.message), lastFailure)
    }

    private companion object {
        /** The first try plus one retry. */
        const val MAX_ATTEMPTS = 2
    }
}
