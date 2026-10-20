package com.phicdy.redraft.shared.ui.main.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.phicdy.redraft.shared.llm.TokenUsage
import com.phicdy.redraft.shared.model.ProofreadResult
import com.phicdy.redraft.shared.model.Severity
import com.phicdy.redraft.shared.ui.main.displayName

@Composable
internal fun ProofreadResultView(
    input: String,
    result: ProofreadResult,
    usage: TokenUsage,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val highlights = remember(input, result.issues) { locateIssues(input, result.issues) }
    val locatedIssues = remember(highlights) { highlights.map { it.issueIndex }.toSet() }
    val diff = remember(input, result.correctedText) { wordDiff(input, result.correctedText) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ResultSection(title = "修正後", action = { CopyButton(result.correctedText) }) {
            SelectionContainer {
                Text(text = result.correctedText, style = MaterialTheme.typography.bodyLarge)
            }
        }

        if (diff != null && diff.any { it.kind != DiffKind.Unchanged }) {
            ResultSection(title = "変更点") {
                SelectionContainer {
                    Text(
                        text = diffText(diff, colorScheme),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        if (result.issues.isNotEmpty()) {
            ResultSection(title = "原文") {
                SelectionContainer {
                    Text(
                        text = highlightedText(input, highlights, result.issues, colorScheme),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                SeverityLegend()
            }

            ResultSection(title = "指摘 (${result.issues.size}件)") {
                result.issues.forEachIndexed { index, issue ->
                    IssueItem(issue = issue, isLocated = index in locatedIssues)
                }
            }
        }

        if (result.naturalAlternatives.isNotEmpty()) {
            ResultSection(title = "より自然な言い方") {
                result.naturalAlternatives.forEach { alternative ->
                    ResultItem {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SelectionContainer(modifier = Modifier.weight(1f)) {
                                Text(text = alternative.text, style = MaterialTheme.typography.bodyLarge)
                            }
                            CopyButton(alternative.text)
                        }
                        ExampleText(alternative.nuance)
                    }
                }
            }
        }

        if (result.overallComment.isNotBlank()) {
            ResultSection(title = "総評") {
                Text(text = result.overallComment, style = MaterialTheme.typography.bodyMedium)
            }
        }

        UsageText(usage)
    }
}

@Composable
private fun IssueItem(issue: ProofreadResult.Issue, isLocated: Boolean) {
    val colors = severityColors(issue.severity, MaterialTheme.colorScheme)
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    ResultItem {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tag(
                text = issue.severity.displayName(),
                containerColor = colors.tag,
                contentColor = colors.onTag,
            )
            Tag(text = issue.category.displayName())
        }
        SelectionContainer {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            textDecoration = TextDecoration.LineThrough,
                            color = mutedColor,
                        ),
                    ) {
                        append(issue.original)
                    }
                    append("  →  ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(issue.suggestion)
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(text = issue.explanation, style = MaterialTheme.typography.bodyMedium)
        if (!isLocated) {
            Text(
                text = "原文中の該当箇所が見つかりませんでした",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SeverityLegend() {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Severity.entries.forEach { severity ->
            val colors = severityColors(severity, colorScheme)
            Tag(
                text = severity.displayName(),
                containerColor = colors.highlight,
                contentColor = colors.onHighlight,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

private class SeverityColors(
    val tag: Color,
    val onTag: Color,
    val highlight: Color,
    val onHighlight: Color,
)

private fun severityColors(severity: Severity, colorScheme: ColorScheme): SeverityColors = when (severity) {
    Severity.Error -> SeverityColors(
        tag = colorScheme.error,
        onTag = colorScheme.onError,
        highlight = colorScheme.errorContainer,
        onHighlight = colorScheme.onErrorContainer,
    )

    Severity.Improvement -> SeverityColors(
        tag = colorScheme.tertiary,
        onTag = colorScheme.onTertiary,
        highlight = colorScheme.tertiaryContainer,
        onHighlight = colorScheme.onTertiaryContainer,
    )
}

private fun highlightedText(
    input: String,
    highlights: List<IssueHighlight>,
    issues: List<ProofreadResult.Issue>,
    colorScheme: ColorScheme,
): AnnotatedString = buildAnnotatedString {
    append(input)
    highlights.forEach { highlight ->
        val colors = severityColors(issues[highlight.issueIndex].severity, colorScheme)
        addStyle(
            style = SpanStyle(background = colors.highlight, color = colors.onHighlight),
            start = highlight.start,
            end = highlight.end,
        )
    }
}

private fun diffText(segments: List<DiffSegment>, colorScheme: ColorScheme): AnnotatedString = buildAnnotatedString {
    segments.forEach { segment ->
        when (segment.kind) {
            DiffKind.Unchanged -> append(segment.text)
            DiffKind.Removed -> withStyle(
                SpanStyle(color = colorScheme.error, textDecoration = TextDecoration.LineThrough),
            ) {
                append(segment.text)
            }

            DiffKind.Added -> withStyle(
                SpanStyle(
                    color = colorScheme.onPrimaryContainer,
                    background = colorScheme.primaryContainer,
                    fontWeight = FontWeight.SemiBold,
                ),
            ) {
                append(segment.text)
            }
        }
    }
}
