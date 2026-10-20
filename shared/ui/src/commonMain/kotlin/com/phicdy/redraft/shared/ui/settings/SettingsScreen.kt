package com.phicdy.redraft.shared.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phicdy.redraft.shared.llm.openai.OpenAiDefaults
import com.phicdy.redraft.shared.ui.theme.RedraftTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // The view model outlives this screen, so leaving it drops unsaved edits and the last message.
    DisposableEffect(viewModel) {
        onDispose { viewModel.reload() }
    }
    SettingsContent(
        uiState = uiState,
        onBack = onBack,
        onApiKeyChange = viewModel::onApiKeyChange,
        onBaseUrlChange = viewModel::onBaseUrlChange,
        onModelChange = viewModel::onModelChange,
        onSave = viewModel::onSave,
        onRemoveApiKey = viewModel::onRemoveApiKey,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onSave: () -> Unit,
    onRemoveApiKey: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("戻る") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "OpenAI 接続", style = MaterialTheme.typography.titleMedium)
            ApiKeySection(uiState = uiState, onApiKeyChange = onApiKeyChange)
            HorizontalDivider()
            OutlinedTextField(
                value = uiState.baseUrl,
                onValueChange = onBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                label = { Text("ベース URL") },
                singleLine = true,
                isError = uiState.baseUrlError != null,
                supportingText = {
                    Text(uiState.baseUrlError?.label() ?: "https:// で始まる URL（既定: ${OpenAiDefaults.BASE_URL}）")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            OutlinedTextField(
                value = uiState.model,
                onValueChange = onModelChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                label = { Text("モデル") },
                singleLine = true,
                isError = uiState.modelError != null,
                supportingText = {
                    Text(uiState.modelError?.label() ?: "既定: ${OpenAiDefaults.MODEL}")
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, enabled = uiState.canSave) {
                    Text(if (uiState.isSaving) "保存中…" else "保存")
                }
                OutlinedButton(onClick = onRemoveApiKey, enabled = uiState.canRemoveApiKey) {
                    Text("キーを削除")
                }
            }
            uiState.message?.let { message ->
                Text(
                    text = message.label(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isError) MaterialTheme.colorScheme.error else Color.Unspecified,
                )
            }
        }
    }
}

@Composable
private fun ApiKeySection(
    uiState: SettingsUiState,
    onApiKeyChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (uiState.hasSavedApiKey) "API キー: 保存済み" else "API キー: 未設定",
            style = MaterialTheme.typography.bodyLarge,
            color = if (uiState.hasSavedApiKey) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        OutlinedTextField(
            value = uiState.apiKeyInput,
            onValueChange = onApiKeyChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
            label = { Text(if (uiState.hasSavedApiKey) "新しい API キー" else "API キー") },
            placeholder = { Text("sk-...") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = uiState.apiKeyError != null,
            supportingText = {
                Text(
                    uiState.apiKeyError?.label()
                        ?: if (uiState.hasSavedApiKey) "空欄のまま保存すると現在のキーを使い続けます" else "OpenAI の API キーを入力",
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        Text(
            text = "API キーはこの端末の Keychain（Mac）/ Android Keystore で保護して保存され、" +
                "設定のエクスポートなどで外部に出ることはありません。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun BaseUrlError.label(): String = when (this) {
    BaseUrlError.Blank -> "URL を入力してください"
    BaseUrlError.NotHttps -> "https:// で始まる URL のみ使用できます"
    BaseUrlError.Invalid -> "URL の形式が正しくありません"
}

private fun ModelError.label(): String = when (this) {
    ModelError.Blank -> "モデル名を入力してください"
    ModelError.ContainsWhitespace -> "モデル名に空白は使用できません"
}

private fun ApiKeyError.label(): String = when (this) {
    ApiKeyError.InvalidCharacters -> "API キーに使用できない文字（空白・引用符・全角文字など）が含まれています"
    ApiKeyError.TooLong -> "API キーが長すぎます"
}

private val SettingsMessage.isError: Boolean
    get() = this == SettingsMessage.LoadFailed ||
        this == SettingsMessage.SaveFailed ||
        this == SettingsMessage.RemoveFailed

private fun SettingsMessage.label(): String = when (this) {
    SettingsMessage.Saved -> "保存しました"
    SettingsMessage.ApiKeyRemoved -> "API キーを削除しました"
    SettingsMessage.LoadFailed -> "設定を読み込めませんでした"
    SettingsMessage.SaveFailed -> "保存できませんでした。もう一度お試しください"
    SettingsMessage.RemoveFailed -> "API キーを削除できませんでした"
}

@Preview
@Composable
internal fun SettingsContentEmptyPreview() {
    RedraftTheme {
        SettingsContent(
            uiState = SettingsUiState(isLoading = false),
            onBack = {},
            onApiKeyChange = {},
            onBaseUrlChange = {},
            onModelChange = {},
            onSave = {},
            onRemoveApiKey = {},
        )
    }
}

@Preview
@Composable
internal fun SettingsContentKeySavedPreview() {
    RedraftTheme {
        SettingsContent(
            uiState = SettingsUiState(
                isLoading = false,
                hasSavedApiKey = true,
                baseUrl = "https://llm-proxy.example.com/openai/v1",
                message = SettingsMessage.Saved,
            ),
            onBack = {},
            onApiKeyChange = {},
            onBaseUrlChange = {},
            onModelChange = {},
            onSave = {},
            onRemoveApiKey = {},
        )
    }
}

@Preview
@Composable
internal fun SettingsContentValidationErrorPreview() {
    RedraftTheme {
        SettingsContent(
            uiState = SettingsUiState(
                isLoading = false,
                apiKeyInput = "sk-proj abc",
                baseUrl = "http://llm-proxy.example.com/v1",
                model = "",
                apiKeyError = ApiKeyError.InvalidCharacters,
                baseUrlError = BaseUrlError.NotHttps,
                modelError = ModelError.Blank,
            ),
            onBack = {},
            onApiKeyChange = {},
            onBaseUrlChange = {},
            onModelChange = {},
            onSave = {},
            onRemoveApiKey = {},
        )
    }
}
