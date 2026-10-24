package com.phicdy.redraft.shared.ui.main

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phicdy.redraft.shared.llm.prompt.Politeness
import com.phicdy.redraft.shared.llm.prompt.WritingPurpose
import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.ui.main.result.ProofreadResultView
import com.phicdy.redraft.shared.ui.main.result.TranslationResultView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainScreen(viewModel: MainViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MainContent(
        uiState = uiState,
        onInputChange = viewModel::onInputChange,
        onModeSelect = viewModel::onModeSelect,
        onPurposeSelect = viewModel::onPurposeSelect,
        onPolitenessSelect = viewModel::onPolitenessSelect,
        onSubmit = viewModel::submit,
        onCancel = viewModel::cancel,
    )
}

/** Stateless main screen. Previews use this directly so they need no Koin. */
@Composable
internal fun MainContent(
    uiState: MainUiState,
    onInputChange: (String) -> Unit,
    onModeSelect: (InputMode?) -> Unit,
    onPurposeSelect: (WritingPurpose) -> Unit,
    onPolitenessSelect: (Politeness) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            // One scrolling column: long results scroll while the input stays at the top.
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = uiState.input,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 240.dp),
                    placeholder = { Text("日本語を入力すると英訳、英語を入力すると校正します") },
                )
                OptionRow(label = "モード") {
                    InputMode.entries.forEach { mode ->
                        val isSelectedByUser = uiState.selectedMode == mode
                        FilterChip(
                            selected = uiState.mode == mode,
                            onClick = { onModeSelect(if (isSelectedByUser) null else mode) },
                            label = { Text(mode.displayName()) },
                        )
                    }
                    if (uiState.selectedMode == null && uiState.detectedMode != null) {
                        Text(
                            text = "自動判定",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                OptionRow(label = "用途") {
                    WritingPurpose.entries.forEach { purpose ->
                        FilterChip(
                            selected = uiState.writingContext.purpose == purpose,
                            onClick = { onPurposeSelect(purpose) },
                            label = { Text(purpose.displayName()) },
                        )
                    }
                }
                OptionRow(label = "トーン") {
                    Politeness.entries.forEach { politeness ->
                        FilterChip(
                            selected = uiState.writingContext.politeness == politeness,
                            onClick = { onPolitenessSelect(politeness) },
                            label = { Text(politeness.displayName()) },
                        )
                    }
                }
                RunRow(uiState = uiState, onSubmit = onSubmit, onCancel = onCancel)
                ResultArea(result = uiState.result)
            }
        }
    }
}

@Composable
private fun OptionRow(
    label: String,
    content: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.widthIn(min = 48.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun RunRow(
    uiState: MainUiState,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = onSubmit, enabled = uiState.canRun) {
            Text(uiState.mode.runLabel())
        }
        val result = uiState.result
        if (result is ResultState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
            Text(
                text = result.mode.runningLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onCancel) {
                Text("キャンセル")
            }
        }
    }
}

@Composable
private fun ResultArea(result: ResultState) {
    when (result) {
        ResultState.Idle, is ResultState.Loading -> Unit
        is ResultState.Failed -> ErrorMessage(result.message)
        is ResultState.Translated -> TranslationResultView(result = result.result, usage = result.usage)
        is ResultState.Proofread -> ProofreadResultView(
            input = result.input,
            result = result.result,
            usage = result.usage,
        )
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
