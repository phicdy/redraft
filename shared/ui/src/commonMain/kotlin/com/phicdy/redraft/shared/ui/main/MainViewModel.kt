package com.phicdy.redraft.shared.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phicdy.redraft.shared.llm.LlmException
import com.phicdy.redraft.shared.llm.WritingAssistant
import com.phicdy.redraft.shared.llm.prompt.Politeness
import com.phicdy.redraft.shared.llm.prompt.WritingContext
import com.phicdy.redraft.shared.llm.prompt.WritingPurpose
import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.model.detectInputMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Runs translation or proofreading for the main screen.
 *
 * Cancellation rules: only one request runs at a time. While it runs, changing the input,
 * the mode or the writing context cancels it and returns to [ResultState.Idle], because the
 * answer would no longer match what is on screen; the user can also cancel explicitly.
 * Editing the input after a result has arrived keeps the result visible, so the user can
 * revise the text while reading the feedback.
 */
class MainViewModel(
    private val writingAssistant: WritingAssistant,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = mutableUiState.asStateFlow()

    private var runningJob: Job? = null

    fun onInputChange(text: String) {
        if (text == mutableUiState.value.input) return
        cancelRunning()
        mutableUiState.update { state ->
            state.copy(input = text, detectedMode = detectInputMode(text))
        }
    }

    fun onModeSelect(mode: InputMode?) {
        cancelRunning()
        mutableUiState.update { state -> state.copy(selectedMode = mode) }
    }

    fun onPurposeSelect(purpose: WritingPurpose) {
        updateWritingContext { context -> context.copy(purpose = purpose) }
    }

    fun onPolitenessSelect(politeness: Politeness) {
        updateWritingContext { context -> context.copy(politeness = politeness) }
    }

    /** Starts translation or proofreading of the current input, depending on [MainUiState.mode]. */
    fun submit() {
        val state = mutableUiState.value
        val mode = state.mode ?: return
        if (!state.canRun) return
        val input = state.input
        val context = state.writingContext

        runningJob?.cancel()
        mutableUiState.update { it.copy(result = ResultState.Loading(mode)) }
        runningJob = viewModelScope.launch {
            val result = request(mode = mode, input = input, context = context)
            ensureActive()
            mutableUiState.update { it.copy(result = result) }
        }
    }

    /** Cancels the running request, if any, and clears the loading state. */
    fun cancel() {
        cancelRunning()
    }

    private suspend fun request(mode: InputMode, input: String, context: WritingContext): ResultState = try {
        when (mode) {
            InputMode.Translate -> {
                val response = writingAssistant.translate(japanese = input, context = context)
                ResultState.Translated(input = input, result = response.output, usage = response.usage)
            }

            InputMode.Proofread -> {
                val response = writingAssistant.proofread(english = input, context = context)
                ResultState.Proofread(input = input, result = response.output, usage = response.usage)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: LlmException) {
        ResultState.Failed(e.error.toUserMessage())
    } catch (e: Exception) {
        // Adapters classify known failures as LlmException; anything else must not crash the app.
        ResultState.Failed(UNEXPECTED_ERROR_MESSAGE)
    }

    private fun updateWritingContext(transform: (WritingContext) -> WritingContext) {
        val current = mutableUiState.value.writingContext
        val updated = transform(current)
        if (updated == current) return
        cancelRunning()
        mutableUiState.update { state -> state.copy(writingContext = updated) }
    }

    private fun cancelRunning() {
        runningJob?.cancel()
        runningJob = null
        mutableUiState.update { state ->
            if (state.result is ResultState.Loading) state.copy(result = ResultState.Idle) else state
        }
    }
}
