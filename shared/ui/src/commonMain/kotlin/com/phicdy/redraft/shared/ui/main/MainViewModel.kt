package com.phicdy.redraft.shared.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phicdy.redraft.shared.data.history.HistoryRepository
import com.phicdy.redraft.shared.data.history.HistoryResult
import com.phicdy.redraft.shared.llm.LlmException
import com.phicdy.redraft.shared.llm.TokenUsage
import com.phicdy.redraft.shared.llm.WritingAssistant
import com.phicdy.redraft.shared.llm.prompt.Politeness
import com.phicdy.redraft.shared.llm.prompt.WritingContext
import com.phicdy.redraft.shared.llm.prompt.WritingPurpose
import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.model.detectInputMode
import com.phicdy.redraft.shared.ui.history.toHistoryContext
import com.phicdy.redraft.shared.ui.history.toHistoryUsage
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
 *
 * Every successful result is saved to history in the background. Failed and cancelled requests are
 * not saved, and a failed save only shows a small notice; the result stays visible.
 */
class MainViewModel(
    private val writingAssistant: WritingAssistant,
    private val historyRepository: HistoryRepository,
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
        mutableUiState.update { it.copy(result = ResultState.Loading(mode), historySaveFailed = false) }
        runningJob = viewModelScope.launch {
            val outcome = request(mode = mode, input = input, context = context)
            ensureActive()
            mutableUiState.update { it.copy(result = outcome.state) }
            // Saved only once the result is shown, so a request cancelled at the last moment is not saved.
            outcome.completed?.let(::saveToHistory)
        }
    }

    /** Cancels the running request, if any, and clears the loading state. */
    fun cancel() {
        cancelRunning()
    }

    private suspend fun request(mode: InputMode, input: String, context: WritingContext): RequestOutcome = try {
        when (mode) {
            InputMode.Translate -> {
                val response = writingAssistant.translate(japanese = input, context = context)
                RequestOutcome(
                    state = ResultState.Translated(input = input, result = response.output, usage = response.usage),
                    completed = CompletedRequest(
                        input = input,
                        context = context,
                        result = HistoryResult.Translation(response.output),
                        model = response.model,
                        usage = response.usage,
                    ),
                )
            }

            InputMode.Proofread -> {
                val response = writingAssistant.proofread(english = input, context = context)
                RequestOutcome(
                    state = ResultState.Proofread(input = input, result = response.output, usage = response.usage),
                    completed = CompletedRequest(
                        input = input,
                        context = context,
                        result = HistoryResult.Proofread(response.output),
                        model = response.model,
                        usage = response.usage,
                    ),
                )
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: LlmException) {
        RequestOutcome(state = ResultState.Failed(e.error.toUserMessage()), completed = null)
    } catch (e: Exception) {
        // Adapters classify known failures as LlmException; anything else must not crash the app.
        RequestOutcome(state = ResultState.Failed(UNEXPECTED_ERROR_MESSAGE), completed = null)
    }

    /** Runs apart from [runningJob], so editing the input right after the result does not cancel the save. */
    private fun saveToHistory(completed: CompletedRequest) {
        viewModelScope.launch {
            try {
                historyRepository.save(
                    input = completed.input,
                    context = completed.context.toHistoryContext(),
                    result = completed.result,
                    model = completed.model,
                    usage = completed.usage.toHistoryUsage(),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutableUiState.update { it.copy(historySaveFailed = true) }
            }
        }
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

private class RequestOutcome(
    val state: ResultState,
    /** Set only for a successful request, which is saved to history. */
    val completed: CompletedRequest?,
)

private class CompletedRequest(
    val input: String,
    val context: WritingContext,
    val result: HistoryResult,
    val model: String,
    val usage: TokenUsage,
)
