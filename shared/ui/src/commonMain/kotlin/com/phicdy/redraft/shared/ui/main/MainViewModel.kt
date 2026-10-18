package com.phicdy.redraft.shared.ui.main

import androidx.lifecycle.ViewModel
import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.model.detectInputMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = mutableUiState.asStateFlow()

    fun onInputChange(text: String) {
        mutableUiState.update { state ->
            state.copy(input = text, detectedMode = detectInputMode(text))
        }
    }

    fun onModeSelect(mode: InputMode?) {
        mutableUiState.update { state -> state.copy(selectedMode = mode) }
    }
}
