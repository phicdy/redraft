package com.phicdy.redraft.shared.data.history

import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.model.ProofreadResult
import com.phicdy.redraft.shared.model.Severity
import com.phicdy.redraft.shared.model.TranslationResult
import kotlin.time.Instant

/**
 * What the text was written for. Mirrors the LLM layer's `WritingPurpose`, which the data layer
 * cannot depend on; the UI maps between them. [storageValue] is stored and must never change.
 */
enum class HistoryPurpose(val storageValue: String) {
    Email("email"),
    Chat("chat"),
    Document("document"),
}

/** Mirrors the LLM layer's `Politeness`. [storageValue] is stored and must never change. */
enum class HistoryPoliteness(val storageValue: String) {
    Casual("casual"),
    Neutral("neutral"),
    Formal("formal"),
}

data class HistoryContext(
    val purpose: HistoryPurpose,
    val politeness: HistoryPoliteness,
)

data class HistoryUsage(
    val inputTokens: Int,
    val outputTokens: Int,
)

/** The structured result of one request, kept exactly so it can be shown again. */
sealed interface HistoryResult {
    val mode: InputMode

    data class Translation(val result: TranslationResult) : HistoryResult {
        override val mode: InputMode
            get() = InputMode.Translate
    }

    data class Proofread(val result: ProofreadResult) : HistoryResult {
        override val mode: InputMode
            get() = InputMode.Proofread
    }
}

/** One saved translation or proofreading. */
data class HistoryEntry(
    val id: String,
    /** The install that made this entry. */
    val deviceId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val input: String,
    val context: HistoryContext,
    /** The model that actually answered. */
    val model: String,
    val usage: HistoryUsage,
    val result: HistoryResult,
)

/** What a history list row needs, without decoding the stored result. */
data class HistoryEntrySummary(
    val id: String,
    val createdAt: Instant,
    val mode: InputMode,
    val input: String,
    /** The translation or the corrected text. */
    val output: String,
    /** Proofread issues with [Severity.Error]. */
    val errorCount: Int,
    /** Proofread issues with [Severity.Improvement]. */
    val improvementCount: Int,
    val grammarPointCount: Int,
    val expressionCount: Int,
)
