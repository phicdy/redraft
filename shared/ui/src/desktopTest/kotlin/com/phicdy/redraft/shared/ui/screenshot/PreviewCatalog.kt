package com.phicdy.redraft.shared.ui.screenshot

import androidx.compose.runtime.Composable
import com.phicdy.redraft.shared.ui.main.MainContentEmptyPreview
import com.phicdy.redraft.shared.ui.main.MainContentErrorPreview
import com.phicdy.redraft.shared.ui.main.MainContentLoadingPreview
import com.phicdy.redraft.shared.ui.main.MainContentProofreadResultPreview
import com.phicdy.redraft.shared.ui.main.MainContentProofreadSelectedPreview
import com.phicdy.redraft.shared.ui.main.MainContentTranslatePreview
import com.phicdy.redraft.shared.ui.main.MainContentTranslationResultPreview
import com.phicdy.redraft.shared.ui.main.ProofreadResultViewPreview
import com.phicdy.redraft.shared.ui.main.TranslationResultViewPreview
import com.phicdy.redraft.shared.ui.settings.SettingsContentEmptyPreview
import com.phicdy.redraft.shared.ui.settings.SettingsContentKeySavedPreview
import com.phicdy.redraft.shared.ui.settings.SettingsContentValidationErrorPreview

internal class PreviewEntry(
    /** Used as the file name, so keep it unique and free of spaces. */
    val name: String,
    val content: @Composable () -> Unit,
)

/**
 * Previews rendered into PNGs and shown on pull requests.
 * Add an entry here whenever a new @Preview is added.
 */
internal val previewCatalog: List<PreviewEntry> = listOf(
    PreviewEntry("MainContentEmpty") { MainContentEmptyPreview() },
    PreviewEntry("MainContentTranslate") { MainContentTranslatePreview() },
    PreviewEntry("MainContentProofreadSelected") { MainContentProofreadSelectedPreview() },
    PreviewEntry("MainContentLoading") { MainContentLoadingPreview() },
    PreviewEntry("MainContentTranslationResult") { MainContentTranslationResultPreview() },
    PreviewEntry("MainContentProofreadResult") { MainContentProofreadResultPreview() },
    PreviewEntry("MainContentError") { MainContentErrorPreview() },
    PreviewEntry("TranslationResultView") { TranslationResultViewPreview() },
    PreviewEntry("ProofreadResultView") { ProofreadResultViewPreview() },
    PreviewEntry("SettingsContentEmpty") { SettingsContentEmptyPreview() },
    PreviewEntry("SettingsContentKeySaved") { SettingsContentKeySavedPreview() },
    PreviewEntry("SettingsContentValidationError") { SettingsContentValidationErrorPreview() },
)
