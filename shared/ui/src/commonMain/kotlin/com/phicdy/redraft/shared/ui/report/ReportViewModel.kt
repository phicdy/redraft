package com.phicdy.redraft.shared.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phicdy.redraft.shared.data.report.WeeklyReportRepository
import com.phicdy.redraft.shared.llm.LlmException
import com.phicdy.redraft.shared.ui.main.UNEXPECTED_ERROR_MESSAGE
import com.phicdy.redraft.shared.ui.main.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Lists saved weekly reports, opens one, and generates a new one for last week or this week so far.
 * A generated report is opened right away. Only one generation runs at a time; it keeps running
 * when the user goes back to the list.
 */
class ReportViewModel(
    private val repository: WeeklyReportRepository,
    private val generator: WeeklyReportGenerator,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        ReportUiState(
            lastWeek = generator.weekFor(ReportTarget.LastWeek),
            thisWeek = generator.weekFor(ReportTarget.ThisWeek),
        ),
    )
    val uiState: StateFlow<ReportUiState> = mutableUiState.asStateFlow()

    private var detailJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeReports().collect { reports ->
                mutableUiState.update { it.copy(reports = reports, isLoading = false) }
            }
        }
    }

    fun onTargetSelect(target: ReportTarget) {
        mutableUiState.update { state ->
            // A message about the previous choice no longer applies.
            val generation = if (state.generation is GenerationState.Running) state.generation else GenerationState.Idle
            state.copy(target = target, generation = generation)
        }
    }

    fun onGenerate() {
        val state = mutableUiState.value
        if (state.generation is GenerationState.Running) return
        val target = state.target
        // The weeks move on while the app stays open, so they are read again for every run.
        val week = generator.weekFor(target)
        mutableUiState.update {
            it.copy(
                lastWeek = generator.weekFor(ReportTarget.LastWeek),
                thisWeek = generator.weekFor(ReportTarget.ThisWeek),
                generation = GenerationState.Running(week),
            )
        }
        viewModelScope.launch {
            val generation = try {
                when (val result = generator.generate(target)) {
                    is GenerationResult.Generated -> {
                        detailJob?.cancel()
                        mutableUiState.update { it.copy(detail = ReportDetailState.Loaded(result.report)) }
                        GenerationState.Idle
                    }

                    is GenerationResult.NoEntries -> GenerationState.NoEntries(result.week)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: LlmException) {
                GenerationState.Failed(e.error.toUserMessage())
            } catch (e: Exception) {
                // Known LLM failures are LlmException; storage or anything else must not crash the app.
                GenerationState.Failed(UNEXPECTED_ERROR_MESSAGE)
            }
            mutableUiState.update { it.copy(generation = generation) }
        }
    }

    fun onReportSelect(id: String) {
        detailJob?.cancel()
        mutableUiState.update { it.copy(detail = ReportDetailState.Loading) }
        detailJob = viewModelScope.launch {
            val report = repository.get(id)
            val detail = if (report == null) ReportDetailState.NotFound else ReportDetailState.Loaded(report)
            mutableUiState.update { it.copy(detail = detail) }
        }
    }

    fun onDetailClose() {
        detailJob?.cancel()
        detailJob = null
        mutableUiState.update { it.copy(detail = null) }
    }
}
