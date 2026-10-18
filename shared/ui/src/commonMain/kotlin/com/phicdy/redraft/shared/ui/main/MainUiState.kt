package com.phicdy.redraft.shared.ui.main

import com.phicdy.redraft.shared.model.InputMode

data class MainUiState(
    val input: String = "",
    /** Mode detected from [input]; null while it cannot be decided. */
    val detectedMode: InputMode? = null,
    /** Mode chosen by the user, which wins over [detectedMode]. */
    val selectedMode: InputMode? = null,
) {
    val mode: InputMode?
        get() = selectedMode ?: detectedMode
}
