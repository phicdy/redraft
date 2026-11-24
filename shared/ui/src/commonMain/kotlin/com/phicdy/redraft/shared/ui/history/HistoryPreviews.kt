package com.phicdy.redraft.shared.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.phicdy.redraft.shared.data.history.HistoryContext
import com.phicdy.redraft.shared.data.history.HistoryEntry
import com.phicdy.redraft.shared.data.history.HistoryEntrySummary
import com.phicdy.redraft.shared.data.history.HistoryPoliteness
import com.phicdy.redraft.shared.data.history.HistoryPurpose
import com.phicdy.redraft.shared.data.history.HistoryResult
import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.ui.main.SAMPLE_ENGLISH
import com.phicdy.redraft.shared.ui.main.SAMPLE_JAPANESE
import com.phicdy.redraft.shared.ui.main.sampleProofread
import com.phicdy.redraft.shared.ui.main.sampleTranslation
import com.phicdy.redraft.shared.ui.main.sampleUsage
import com.phicdy.redraft.shared.ui.theme.RedraftTheme
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

// Fixed instants and time zone keep the rendered screenshots identical on every run.
private val previewTimeZone: TimeZone = TimeZone.of("Asia/Tokyo")
private val previewNow: Instant = Instant.parse("2026-11-24T01:30:00Z")

private fun summary(
    id: String,
    minutesAgo: Long,
    mode: InputMode,
    input: String,
    counts: List<Int>,
): HistoryEntrySummary = HistoryEntrySummary(
    id = id,
    createdAt = Instant.fromEpochMilliseconds(previewNow.toEpochMilliseconds() - minutesAgo * 60_000),
    mode = mode,
    input = input,
    output = "",
    errorCount = counts[0],
    improvementCount = counts[1],
    grammarPointCount = counts[2],
    expressionCount = counts[3],
)

private val sampleEntries: List<HistoryEntrySummary> = listOf(
    summary("1", 5L, InputMode.Proofread, SAMPLE_ENGLISH, listOf(2, 1, 0, 0)),
    summary("2", 42L, InputMode.Translate, SAMPLE_JAPANESE, listOf(0, 0, 2, 3)),
    summary("3", 60L * 20, InputMode.Proofread, "Thanks for the update. I will check it tomorrow.", listOf(0, 0, 0, 0)),
    summary(
        "4",
        60L * 26,
        InputMode.Translate,
        "お疲れさまです。\n先日の件ですが、仕様の確認が取れましたので、来週から実装に着手します。",
        listOf(0, 0, 1, 2),
    ),
    summary("5", 60L * 24 * 3, InputMode.Proofread, "Could you please review my PR when you have a time?", listOf(1, 2, 0, 0)),
)

private fun entry(input: String, result: HistoryResult): HistoryEntry = HistoryEntry(
    id = "entry",
    deviceId = "device",
    createdAt = previewNow,
    updatedAt = previewNow,
    input = input,
    context = HistoryContext(purpose = HistoryPurpose.Email, politeness = HistoryPoliteness.Formal),
    model = "gpt-5-mini",
    usage = sampleUsage.toHistoryUsage(),
    result = result,
)

@Composable
private fun HistoryContentPreview(uiState: HistoryUiState) {
    RedraftTheme {
        HistoryContent(
            uiState = uiState,
            timeZone = previewTimeZone,
            onBack = {},
            onQueryChange = {},
            onEntrySelect = {},
            onDetailClose = {},
            onDeleteRequest = {},
            onDeleteDismiss = {},
            onDeleteConfirm = {},
        )
    }
}

@Preview
@Composable
internal fun HistoryContentListPreview() {
    HistoryContentPreview(HistoryUiState(entries = sampleEntries, isLoading = false))
}

@Preview
@Composable
internal fun HistoryContentEmptyPreview() {
    HistoryContentPreview(HistoryUiState(isLoading = false))
}

@Preview
@Composable
internal fun HistoryContentNoResultsPreview() {
    HistoryContentPreview(HistoryUiState(query = "deadline", isLoading = false))
}

@Preview
@Composable
internal fun HistoryContentTranslationDetailPreview() {
    val entry = entry(SAMPLE_JAPANESE, HistoryResult.Translation(sampleTranslation))
    HistoryContentPreview(HistoryUiState(isLoading = false, detail = HistoryDetailState.Loaded(entry)))
}

@Preview
@Composable
internal fun HistoryContentProofreadDetailPreview() {
    val entry = entry(SAMPLE_ENGLISH, HistoryResult.Proofread(sampleProofread))
    HistoryContentPreview(HistoryUiState(isLoading = false, detail = HistoryDetailState.Loaded(entry)))
}
