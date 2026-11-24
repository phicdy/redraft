package com.phicdy.redraft.shared.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phicdy.redraft.shared.data.report.WeeklyReport
import com.phicdy.redraft.shared.model.CountDelta
import com.phicdy.redraft.shared.model.Severity
import com.phicdy.redraft.shared.model.WeeklyStats
import com.phicdy.redraft.shared.ui.history.formatDateTime
import com.phicdy.redraft.shared.ui.history.toTokenUsage
import com.phicdy.redraft.shared.ui.main.displayName
import com.phicdy.redraft.shared.ui.main.result.ExampleText
import com.phicdy.redraft.shared.ui.main.result.ResultItem
import com.phicdy.redraft.shared.ui.main.result.ResultSection
import com.phicdy.redraft.shared.ui.main.result.Tag
import com.phicdy.redraft.shared.ui.main.result.UsageText
import kotlinx.datetime.TimeZone

/** One weekly report: the LLM's narrative around the computed numbers. */
@Composable
internal fun ReportDetail(
    report: WeeklyReport,
    timeZone: TimeZone,
    modifier: Modifier,
) {
    val narrative = report.narrative
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = report.week.titleLabel(), style = MaterialTheme.typography.titleMedium)
            if (report.isPartial(timeZone)) Tag(text = "途中")
            Text(
                text = "${formatDateTime(report.createdAt, timeZone)} 作成",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ResultSection(title = "まとめ") {
            SelectionContainer {
                Text(text = narrative.summary, style = MaterialTheme.typography.bodyLarge)
            }
        }
        StatsSection(report.stats)
        if (narrative.recurringMistakes.isNotEmpty()) {
            ResultSection(title = "繰り返しているミス") {
                narrative.recurringMistakes.forEach { mistake ->
                    ResultItem {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Tag(text = mistake.category.displayName())
                            Text(
                                text = mistake.pattern,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        mistake.examples.forEach { example ->
                            ExampleText("${example.original} → ${example.suggestion}")
                        }
                        Text(text = mistake.advice, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        BulletSection(title = "よくなったこと", items = narrative.improvements)
        BulletSection(title = "来週のフォーカス", items = narrative.focusNextWeek)
        if (narrative.expressionsToReview.isNotEmpty()) {
            ResultSection(title = "復習したい表現") {
                narrative.expressionsToReview.forEach { expression ->
                    ResultItem {
                        Text(text = expression.expression, style = MaterialTheme.typography.titleSmall)
                        Text(text = expression.note, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        UsageText(usage = report.usage.toTokenUsage())
    }
}

@Composable
private fun StatsSection(stats: WeeklyStats) {
    ResultSection(title = "今週の数字（先週比）") {
        stats.entryCounts.forEach { count ->
            StatRow(label = count.mode.displayName(), count = count, higherIsBetter = true)
        }
        val errors = stats.issueCounts.filter { it.severity == Severity.Error }
        val improvements = stats.issueCounts.filter { it.severity == Severity.Improvement }
        StatRow(label = "誤り", count = Total(errors), higherIsBetter = false)
        StatRow(label = "改善の指摘", count = Total(improvements), higherIsBetter = false)
        if (stats.topCategories.isNotEmpty()) {
            SubHeading("指摘の多いカテゴリ")
            stats.topCategories.forEach {
                StatRow(label = it.category.displayName(), count = it, higherIsBetter = false)
            }
        }
        if (stats.grammarPointCounts.any { it.current > 0 }) {
            SubHeading("英訳で学んだ文法")
            stats.grammarPointCounts.filter { it.current > 0 }.forEach {
                StatRow(label = it.category.displayName(), count = it, higherIsBetter = true)
            }
        }
        if (stats.expressions.isNotEmpty()) {
            SubHeading("今週学んだ表現（${stats.expressions.size}）")
            stats.expressions.forEach { expression ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = expression.expression, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = expression.meaning,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Sum of several counts, for the totals per severity. */
private class Total(counts: List<CountDelta>) : CountDelta {
    override val current: Int = counts.sumOf { it.current }
    override val previous: Int = counts.sumOf { it.previous }
}

@Composable
private fun StatRow(label: String, count: CountDelta, higherIsBetter: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(text = "${count.current}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(
            text = "（${deltaLabel(count.delta)}）",
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = deltaColor(count.delta, higherIsBetter),
        )
    }
}

@Composable
private fun deltaColor(delta: Int, higherIsBetter: Boolean): Color = when {
    delta == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
    (delta > 0) == higherIsBetter -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.error
}

@Composable
private fun SubHeading(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BulletSection(title: String, items: List<String>) {
    if (items.isEmpty()) return
    ResultSection(title = title) {
        items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "・", style = MaterialTheme.typography.bodyMedium)
                Text(text = item, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
