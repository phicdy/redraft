package com.phicdy.redraft.shared.ui.main

import com.phicdy.redraft.shared.llm.LlmClient
import com.phicdy.redraft.shared.llm.LlmError
import com.phicdy.redraft.shared.llm.LlmException
import com.phicdy.redraft.shared.llm.LlmRequest
import com.phicdy.redraft.shared.llm.LlmResult
import com.phicdy.redraft.shared.llm.TokenUsage
import com.phicdy.redraft.shared.model.ProofreadResult
import com.phicdy.redraft.shared.model.TranslationResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Answers with [translation] or [proofread] depending on the requested output type.
 * Set [gate] to hold the answer until the test completes it, or [llmError] to fail.
 */
internal class FakeLlmClient : LlmClient {
    var translation: TranslationResult = TranslationResult(
        translation = "Could we move the meeting to Friday?",
        alternatives = emptyList(),
        grammarPoints = emptyList(),
        expressions = emptyList(),
    )
    var proofread: ProofreadResult = ProofreadResult(
        correctedText = "I look forward to seeing you.",
        issues = emptyList(),
        naturalAlternatives = emptyList(),
        overallComment = "",
    )
    var usage: TokenUsage = TokenUsage(inputTokens = 10, outputTokens = 20)
    var llmError: LlmError? = null
    var unexpectedFailure: Exception? = null
    var gate: CompletableDeferred<Unit>? = null

    /** Serial names of the requested output types, in call order. */
    val requestedOutputs = mutableListOf<String>()
    val requests = mutableListOf<LlmRequest>()

    override suspend fun <T> generate(request: LlmRequest, outputSerializer: KSerializer<T>): LlmResult<T> {
        requests += request
        requestedOutputs += outputSerializer.descriptor.serialName
        gate?.await()
        llmError?.let { throw LlmException(it) }
        unexpectedFailure?.let { throw it }
        val json = when (outputSerializer.descriptor.serialName) {
            TRANSLATION -> Json.encodeToString(TranslationResult.serializer(), translation)
            PROOFREAD -> Json.encodeToString(ProofreadResult.serializer(), proofread)
            else -> error("Unexpected output type ${outputSerializer.descriptor.serialName}")
        }
        return LlmResult(
            output = Json.decodeFromString(outputSerializer, json),
            usage = usage,
            model = "fake-model",
        )
    }

    companion object {
        val TRANSLATION: String = TranslationResult.serializer().descriptor.serialName
        val PROOFREAD: String = ProofreadResult.serializer().descriptor.serialName
    }
}
