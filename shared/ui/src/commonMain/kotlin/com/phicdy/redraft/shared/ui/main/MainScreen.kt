package com.phicdy.redraft.shared.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.ui.theme.RedraftTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainScreen(viewModel: MainViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MainContent(
        uiState = uiState,
        onInputChange = viewModel::onInputChange,
        onModeSelect = viewModel::onModeSelect,
    )
}

@Composable
private fun MainContent(
    uiState: MainUiState,
    onInputChange: (String) -> Unit,
    onModeSelect: (InputMode?) -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Redraft", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = uiState.input,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                placeholder = { Text("日本語を入力すると英訳、英語を入力すると校正します") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeChip(
                    label = "英訳",
                    mode = InputMode.Translate,
                    uiState = uiState,
                    onModeSelect = onModeSelect,
                )
                ModeChip(
                    label = "校正",
                    mode = InputMode.Proofread,
                    uiState = uiState,
                    onModeSelect = onModeSelect,
                )
            }
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    mode: InputMode,
    uiState: MainUiState,
    onModeSelect: (InputMode?) -> Unit,
) {
    val isSelectedByUser = uiState.selectedMode == mode
    FilterChip(
        selected = uiState.mode == mode,
        onClick = { onModeSelect(if (isSelectedByUser) null else mode) },
        label = { Text(label) },
    )
}

@Preview
@Composable
internal fun MainContentEmptyPreview() {
    RedraftTheme {
        MainContent(uiState = MainUiState(), onInputChange = {}, onModeSelect = {})
    }
}

@Preview
@Composable
internal fun MainContentTranslatePreview() {
    RedraftTheme {
        MainContent(
            uiState = MainUiState(
                input = "来週の会議を金曜日に変更できますか？",
                detectedMode = InputMode.Translate,
            ),
            onInputChange = {},
            onModeSelect = {},
        )
    }
}

@Preview
@Composable
internal fun MainContentProofreadSelectedPreview() {
    RedraftTheme {
        MainContent(
            uiState = MainUiState(
                input = "I look forward to see you next week.",
                detectedMode = InputMode.Proofread,
                selectedMode = InputMode.Proofread,
            ),
            onInputChange = {},
            onModeSelect = {},
        )
    }
}
