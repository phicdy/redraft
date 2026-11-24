package com.phicdy.redraft.shared.ui.report

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.phicdy.redraft.shared.ui.theme.RedraftTheme

private val baseState = ReportUiState(isLoading = false, lastWeek = previewLastWeek, thisWeek = previewThisWeek)

@Composable
private fun ReportContentPreview(uiState: ReportUiState) {
    RedraftTheme {
        ReportContent(
            uiState = uiState,
            timeZone = previewTimeZone,
            onBack = {},
            onTargetSelect = {},
            onGenerate = {},
            onReportSelect = {},
            onDetailClose = {},
        )
    }
}

@Preview
@Composable
internal fun ReportContentListPreview() {
    ReportContentPreview(baseState.copy(reports = sampleReports))
}

@Preview
@Composable
internal fun ReportContentEmptyPreview() {
    ReportContentPreview(baseState)
}

@Preview
@Composable
internal fun ReportContentGeneratingPreview() {
    ReportContentPreview(
        baseState.copy(
            reports = sampleReports.drop(1),
            target = ReportTarget.ThisWeek,
            generation = GenerationState.Running(previewThisWeek),
        ),
    )
}

@Preview
@Composable
internal fun ReportContentNoEntriesPreview() {
    ReportContentPreview(baseState.copy(generation = GenerationState.NoEntries(previewLastWeek)))
}

@Preview
@Composable
internal fun ReportContentErrorPreview() {
    ReportContentPreview(
        baseState.copy(
            reports = sampleReports,
            generation = GenerationState.Failed("APIキーが設定されていません。設定画面から登録してください"),
        ),
    )
}

@Preview
@Composable
internal fun ReportContentDetailPreview() {
    ReportContentPreview(baseState.copy(reports = sampleReports, detail = ReportDetailState.Loaded(sampleReports[1])))
}
