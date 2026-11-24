package com.phicdy.redraft.shared.ui.history

import com.phicdy.redraft.shared.data.history.HistoryEntry
import com.phicdy.redraft.shared.data.history.HistoryEntrySummary

data class HistoryUiState(
    /** The search text as typed. */
    val query: String = "",
    /** Entries matching [query], newest first. */
    val entries: List<HistoryEntrySummary> = emptyList(),
    /** True until the first list has been read. */
    val isLoading: Boolean = true,
    /** The opened entry, or null while the list is shown. */
    val detail: HistoryDetailState? = null,
)

sealed interface HistoryDetailState {
    data object Loading : HistoryDetailState

    /** The entry was deleted or its stored result cannot be read. */
    data object NotFound : HistoryDetailState

    data class Loaded(
        val entry: HistoryEntry,
        /** The delete confirmation dialog is open. */
        val isConfirmingDelete: Boolean = false,
        /** The last delete attempt failed. */
        val deleteFailed: Boolean = false,
    ) : HistoryDetailState
}
