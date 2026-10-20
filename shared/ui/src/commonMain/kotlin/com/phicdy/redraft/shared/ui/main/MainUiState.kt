package com.phicdy.redraft.shared.ui.main

import com.phicdy.redraft.shared.llm.TokenUsage
import com.phicdy.redraft.shared.llm.prompt.WritingContext
import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.model.ProofreadResult
import com.phicdy.redraft.shared.model.TranslationResult

data class MainUiState(
    val input: String = "",
    /** Mode detected from [input]; null while it cannot be decided. */
    val detectedMode: InputMode? = null,
    /** Mode chosen by the user, which wins over [detectedMode]. */
    val selectedMode: InputMode? = null,
    /** Purpose and politeness sent with the request. */
    val writingContext: WritingContext = WritingContext(),
    val result: ResultState = ResultState.Idle,
) {
    val mode: InputMode?
        get() = selectedMode ?: detectedMode

    val isRunning: Boolean
        get() = result is ResultState.Loading

    /** Whether the primary button is enabled. */
    val canRun: Boolean
        get() = input.isNotBlank() && mode != null && !isRunning
}

/** What the area below the input shows. */
sealed interface ResultState {
    /** Nothing requested yet, or the last request was cancelled. */
    data object Idle : ResultState

    data class Loading(val mode: InputMode) : ResultState

    /**
     * [input] is the text the result was made from. It can differ from the current input
     * once the user edits it, and the proofread view highlights issues in this text.
     */
    data class Translated(
        val input: String,
        val result: TranslationResult,
        val usage: TokenUsage,
    ) : ResultState

    data class Proofread(
        val input: String,
        val result: ProofreadResult,
        val usage: TokenUsage,
    ) : ResultState

    data class Failed(val message: String) : ResultState
}
