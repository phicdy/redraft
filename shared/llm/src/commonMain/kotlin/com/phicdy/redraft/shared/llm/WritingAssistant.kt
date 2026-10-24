package com.phicdy.redraft.shared.llm

import com.phicdy.redraft.shared.llm.prompt.Prompts
import com.phicdy.redraft.shared.llm.prompt.WritingContext
import com.phicdy.redraft.shared.model.ProofreadResult
import com.phicdy.redraft.shared.model.TranslationResult

/**
 * The two operations the app needs, built on any [LlmClient].
 * Callers pick the operation from `detectInputMode`; the model never decides the mode.
 */
class WritingAssistant(
    private val client: LlmClient,
) {
    suspend fun translate(
        japanese: String,
        context: WritingContext = WritingContext(),
    ): LlmResult<TranslationResult> = client.generate(
        Prompts.translation(input = japanese, context = context),
        TranslationResult.serializer(),
    )

    suspend fun proofread(
        english: String,
        context: WritingContext = WritingContext(),
    ): LlmResult<ProofreadResult> = client.generate(
        Prompts.proofread(input = english, context = context),
        ProofreadResult.serializer(),
    )
}
