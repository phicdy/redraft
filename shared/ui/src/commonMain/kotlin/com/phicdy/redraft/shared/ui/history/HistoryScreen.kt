package com.phicdy.redraft.shared.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.phicdy.redraft.shared.data.history.HistoryEntry
import com.phicdy.redraft.shared.data.history.HistoryEntrySummary
import com.phicdy.redraft.shared.data.history.HistoryResult
import com.phicdy.redraft.shared.ui.PlatformBackHandler
import com.phicdy.redraft.shared.ui.main.displayName
import com.phicdy.redraft.shared.ui.main.result.ProofreadResultView
import com.phicdy.redraft.shared.ui.main.result.ResultSection
import com.phicdy.redraft.shared.ui.main.result.Tag
import com.phicdy.redraft.shared.ui.main.result.TranslationResultView
import kotlinx.datetime.TimeZone
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // System back closes the detail first, then leaves the history screen.
    PlatformBackHandler(onBack = { if (uiState.detail != null) viewModel.onDetailClose() else onBack() })
    // The view model outlives this screen; reopening history starts at the list.
    DisposableEffect(viewModel) {
        onDispose { viewModel.onDetailClose() }
    }
    val timeZone = remember { TimeZone.currentSystemDefault() }
    HistoryContent(
        uiState = uiState,
        timeZone = timeZone,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onEntrySelect = viewModel::onEntrySelect,
        onDetailClose = viewModel::onDetailClose,
        onDeleteRequest = viewModel::onDeleteRequest,
        onDeleteDismiss = viewModel::onDeleteDismiss,
        onDeleteConfirm = viewModel::onDeleteConfirm,
    )
}

/** Stateless history screen. Dates are shown in [timeZone], so previews can fix it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryContent(
    uiState: HistoryUiState,
    timeZone: TimeZone,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onEntrySelect: (String) -> Unit,
    onDetailClose: () -> Unit,
    onDeleteRequest: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onDeleteConfirm: () -> Unit,
) {
    val detail = uiState.detail
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (detail == null) "履歴" else "履歴の詳細") },
                navigationIcon = {
                    TextButton(onClick = if (detail == null) onBack else onDetailClose) { Text("戻る") }
                },
                actions = {
                    if (detail is HistoryDetailState.Loaded) {
                        TextButton(onClick = onDeleteRequest) {
                            Text(text = "削除", color = MaterialTheme.colorScheme.error)
                        }
                    }
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
                null -> HistoryList(uiState, timeZone, onQueryChange, onEntrySelect, contentModifier)
                HistoryDetailState.Loading -> CenteredProgress()
                HistoryDetailState.NotFound -> CenteredMessage("この履歴は見つかりませんでした")
                is HistoryDetailState.Loaded -> HistoryDetail(detail, timeZone, contentModifier)
            }
        }
    }
    if (detail is HistoryDetailState.Loaded && detail.isConfirmingDelete) {
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = { Text("この履歴を削除しますか？") },
            text = { Text("削除した履歴は一覧と週次レポートに表示されなくなります。") },
            confirmButton = {
                TextButton(onClick = onDeleteConfirm) {
                    Text(text = "削除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDismiss) { Text("キャンセル") }
            },
        )
    }
}

@Composable
private fun HistoryList(
    uiState: HistoryUiState,
    timeZone: TimeZone,
    onQueryChange: (String) -> Unit,
    onEntrySelect: (String) -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
            placeholder = { Text("入力文や結果を検索") },
            singleLine = true,
        )
        when {
            uiState.isLoading -> CenteredProgress()
            uiState.entries.isEmpty() && uiState.query.isBlank() ->
                CenteredMessage("まだ履歴がありません\n英訳・校正の結果がここに保存されます")

            uiState.entries.isEmpty() -> CenteredMessage("「${uiState.query.trim()}」に一致する履歴はありません")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.entries, key = { it.id }) { entry ->
                    HistoryRow(entry = entry, timeZone = timeZone, onClick = { onEntrySelect(entry.id) })
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntrySummary,
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
                Tag(text = entry.mode.displayName())
                Text(
                    text = formatDateTime(entry.createdAt, timeZone),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = entry.countsLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = firstLine(entry.input),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HistoryDetail(
    detail: HistoryDetailState.Loaded,
    timeZone: TimeZone,
    modifier: Modifier,
) {
    val entry = detail.entry
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (detail.deleteFailed) {
            Text(
                text = "削除できませんでした。もう一度お試しください",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        EntryHeader(entry = entry, timeZone = timeZone)
        val usage = entry.usage.toTokenUsage()
        when (val result = entry.result) {
            is HistoryResult.Translation -> {
                ResultSection(title = "入力") {
                    SelectionContainer {
                        Text(text = entry.input, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                TranslationResultView(result = result.result, usage = usage)
            }

            // The proofread view already shows the input, with the issues highlighted.
            is HistoryResult.Proofread -> ProofreadResultView(input = entry.input, result = result.result, usage = usage)
        }
    }
}

@Composable
private fun EntryHeader(entry: HistoryEntry, timeZone: TimeZone) {
    val context = entry.context.toWritingContext()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Tag(text = entry.result.mode.displayName())
        Tag(text = context.purpose.displayName())
        Tag(text = context.politeness.displayName())
        Text(
            text = formatDateTime(entry.createdAt, timeZone),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CenteredProgress() {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.TopCenter) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.TopCenter) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
