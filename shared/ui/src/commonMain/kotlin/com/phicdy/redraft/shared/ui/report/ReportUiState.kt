package com.phicdy.redraft.shared.ui.report

import com.phicdy.redraft.shared.data.report.ReportWeek
import com.phicdy.redraft.shared.data.report.WeeklyReport

data class ReportUiState(
    /** Saved reports, newest week first. */
    val reports: List<WeeklyReport> = emptyList(),
    /** True until the first list has been read. */
    val isLoading: Boolean = true,
    /** The week chosen for the next report. */
    val target: ReportTarget = ReportTarget.LastWeek,
    /** The week each choice refers to, for the labels. */
    val lastWeek: ReportWeek,
    val thisWeek: ReportWeek,
    val generation: GenerationState = GenerationState.Idle,
    /** The opened report, or null while the list is shown. */
    val detail: ReportDetailState? = null,
)

sealed interface GenerationState {
    data object Idle : GenerationState

    data class Running(val week: ReportWeek) : GenerationState

    /** The chosen week has no history; nothing was generated. */
    data class NoEntries(val week: ReportWeek) : GenerationState

    data class Failed(val message: String) : GenerationState
}

sealed interface ReportDetailState {
    data object Loading : ReportDetailState

    /** The report was replaced or cannot be read. */
    data object NotFound : ReportDetailState

    data class Loaded(val report: WeeklyReport) : ReportDetailState
}
