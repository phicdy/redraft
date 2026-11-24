package com.phicdy.redraft.shared.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phicdy.redraft.shared.data.history.HistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lists, searches, opens and deletes history entries. */
class HistoryViewModel(
    private val repository: HistoryRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = mutableUiState.asStateFlow()

    /** Drives the list query; the typed text is also kept in [uiState] right away for the text field. */
    private val query = MutableStateFlow("")
    private var detailJob: Job? = null

    init {
        viewModelScope.launch {
            query.collectLatest { text ->
                repository.observeEntries(text).collect { entries ->
                    mutableUiState.update { it.copy(entries = entries, isLoading = false) }
                }
            }
        }
    }

    fun onQueryChange(text: String) {
        mutableUiState.update { it.copy(query = text) }
        query.value = text
    }

    fun onEntrySelect(id: String) {
        detailJob?.cancel()
        mutableUiState.update { it.copy(detail = HistoryDetailState.Loading) }
        detailJob = viewModelScope.launch {
            val entry = repository.get(id)
            val detail = if (entry == null) HistoryDetailState.NotFound else HistoryDetailState.Loaded(entry)
            mutableUiState.update { it.copy(detail = detail) }
        }
    }

    fun onDetailClose() {
        detailJob?.cancel()
        detailJob = null
        mutableUiState.update { it.copy(detail = null) }
    }

    fun onDeleteRequest() {
        updateLoaded { it.copy(isConfirmingDelete = true, deleteFailed = false) }
    }

    fun onDeleteDismiss() {
        updateLoaded { it.copy(isConfirmingDelete = false) }
    }

    fun onDeleteConfirm() {
        val loaded = mutableUiState.value.detail as? HistoryDetailState.Loaded ?: return
        updateLoaded { it.copy(isConfirmingDelete = false) }
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            try {
                repository.delete(loaded.entry.id)
                mutableUiState.update { it.copy(detail = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateLoaded { it.copy(deleteFailed = true) }
            }
        }
    }

    private fun updateLoaded(transform: (HistoryDetailState.Loaded) -> HistoryDetailState.Loaded) {
        mutableUiState.update { state ->
            val detail = state.detail
            if (detail is HistoryDetailState.Loaded) state.copy(detail = transform(detail)) else state
        }
    }
}
