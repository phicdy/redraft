package com.phicdy.redraft.shared.ui.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phicdy.redraft.shared.data.report.WeeklyReport
import com.phicdy.redraft.shared.ui.PlatformBackHandler
import com.phicdy.redraft.shared.ui.main.result.Tag
import kotlinx.datetime.TimeZone
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReportScreen(
    onBack: () -> Unit,
    viewModel: ReportViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // System back closes the report first, then leaves the report screen.
    PlatformBackHandler(onBack = { if (uiState.detail != null) viewModel.onDetailClose() else onBack() })
    // The view model outlives this screen; coming back starts at the list.
    DisposableEffect(viewModel) {
        onDispose { viewModel.onDetailClose() }
    }
    val timeZone = remember { TimeZone.currentSystemDefault() }
    ReportContent(
        uiState = uiState,
        timeZone = timeZone,
        onBack = onBack,
        onTargetSelect = viewModel::onTargetSelect,
        onGenerate = viewModel::onGenerate,
        onReportSelect = viewModel::onReportSelect,
        onDetailClose = viewModel::onDetailClose,
    )
}

/** Stateless weekly report screen: the list with the generate card, or one report. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportContent(
    uiState: ReportUiState,
    timeZone: TimeZone,
    onBack: () -> Unit,
    onTargetSelect: (ReportTarget) -> Unit,
    onGenerate: () -> Unit,
    onReportSelect: (String) -> Unit,
    onDetailClose: () -> Unit,
) {
    val detail = uiState.detail
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (detail == null) "週次レポート" else "レポート") },
                navigationIcon = {
                    TextButton(onClick = if (detail == null) onBack else onDetailClose) { Text("戻る") }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val contentModifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()
            when (detail) {
                null -> ReportList(uiState, timeZone, onTargetSelect, onGenerate, onReportSelect, contentModifier)
                ReportDetailState.Loading -> CenteredProgress()
                ReportDetailState.NotFound -> CenteredMessage("このレポートは見つかりませんでした")
                is ReportDetailState.Loaded -> ReportDetail(detail.report, timeZone, contentModifier)
            }
        }
    }
}

@Composable
private fun ReportList(
    uiState: ReportUiState,
    timeZone: TimeZone,
    onTargetSelect: (ReportTarget) -> Unit,
    onGenerate: () -> Unit,
    onReportSelect: (String) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "generate") {
            GenerateCard(uiState = uiState, onTargetSelect = onTargetSelect, onGenerate = onGenerate)
        }
        when {
            uiState.isLoading -> item(key = "loading") { CenteredProgress() }
            uiState.reports.isEmpty() -> item(key = "empty") {
                CenteredMessage("まだレポートがありません\n1週間の英訳・校正の履歴から、よくあるミスと改善点をまとめます")
            }

            else -> items(uiState.reports, key = { it.id }) { report ->
                ReportRow(report = report, timeZone = timeZone, onClick = { onReportSelect(report.id) })
            }
        }
    }
}

@Composable
private fun GenerateCard(
    uiState: ReportUiState,
    onTargetSelect: (ReportTarget) -> Unit,
    onGenerate: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "対象の週", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportTarget.entries.forEach { target ->
                    FilterChip(
                        selected = uiState.target == target,
                        onClick = { onTargetSelect(target) },
                        label = { Text(target.displayName()) },
                    )
                }
            }
            Text(
                text = when (uiState.target) {
                    ReportTarget.LastWeek -> "期間: ${uiState.lastWeek.rangeLabel()}"
                    ReportTarget.ThisWeek -> "期間: ${uiState.thisWeek.rangeLabel()}（今日までの分）"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val generation = uiState.generation
            Button(onClick = onGenerate, enabled = generation !is GenerationState.Running) {
                Text("レポートを作成")
            }
            when (generation) {
                is GenerationState.Running -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        text = "${generation.week.rangeLabel()} のレポートを作成中…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is GenerationState.NoEntries -> Text(
                    text = "${generation.week.rangeLabel()} の履歴がないため、レポートを作成できません。英訳や校正を使うと作成できるようになります",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                is GenerationState.Failed -> Text(
                    text = generation.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )

                GenerationState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun ReportRow(
    report: WeeklyReport,
    timeZone: TimeZone,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = report.week.titleLabel(),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (report.isPartial(timeZone)) Tag(text = "途中")
                Text(
                    text = report.stats.countsLabel(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }
            Text(
                text = report.narrative.summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun CenteredProgress() {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.TopCenter) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun CenteredMessage(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.TopCenter) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
