package com.phicdy.redraft.shared.ui.main.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phicdy.redraft.shared.llm.TokenUsage
import com.phicdy.redraft.shared.model.TranslationResult
import com.phicdy.redraft.shared.ui.main.displayName

@Composable
internal fun TranslationResultView(
    result: TranslationResult,
    usage: TokenUsage,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ResultSection(title = "英訳", action = { CopyButton(result.translation) }) {
            SelectionContainer {
                Text(text = result.translation, style = MaterialTheme.typography.bodyLarge)
            }
        }

        if (result.alternatives.isNotEmpty()) {
            ResultSection(title = "言い換え") {
                result.alternatives.forEach { alternative ->
                    ResultItem {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Tag(text = alternative.tone)
                            Spacer(modifier = Modifier.weight(1f))
                            CopyButton(alternative.text)
                        }
                        SelectionContainer {
                            Text(text = alternative.text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        if (result.grammarPoints.isNotEmpty()) {
            ResultSection(title = "文法ポイント") {
                result.grammarPoints.forEach { point ->
                    ResultItem {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Tag(text = point.category.displayName())
                            Text(
                                text = point.title,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        Text(text = point.explanation, style = MaterialTheme.typography.bodyMedium)
                        if (point.example.isNotBlank()) {
                            SelectionContainer { ExampleText("例: ${point.example}") }
                        }
                    }
                }
            }
        }

        if (result.expressions.isNotEmpty()) {
            ResultSection(title = "覚えたい表現") {
                result.expressions.forEach { expression ->
                    ResultItem {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SelectionContainer {
                                Text(
                                    text = expression.expression,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Tag(
                                text = expression.type.displayName(),
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                        Text(text = expression.meaning, style = MaterialTheme.typography.bodyMedium)
                        if (expression.example.isNotBlank()) {
                            SelectionContainer { ExampleText("例: ${expression.example}") }
                        }
                    }
                }
            }
        }

        UsageText(usage)
    }
}
