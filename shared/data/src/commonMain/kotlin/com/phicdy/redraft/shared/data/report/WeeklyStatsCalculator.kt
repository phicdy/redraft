package com.phicdy.redraft.shared.data.report

import com.phicdy.redraft.shared.data.history.HistoryAggregation
import com.phicdy.redraft.shared.model.ErrorCategory
import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.model.Severity
import com.phicdy.redraft.shared.model.WeeklyStats

/**
 * Compares a week's history with the week before. Pure and deterministic, so the numbers in a
 * report never depend on the LLM.
 *
 * Categories that appear in either week are listed, so a category that dropped to zero still shows
 * its improvement. Ties are ordered by the previous count and then by declaration order, so the
 * result is stable.
 */
object WeeklyStatsCalculator {
    const val MAX_TOP_CATEGORIES: Int = 3

    fun calculate(current: HistoryAggregation, previous: HistoryAggregation): WeeklyStats {
        val issueKeys = (current.issueCounts + previous.issueCounts).map { it.category to it.severity }.distinct()
        val issueCounts = issueKeys
            .map { (category, severity) ->
                WeeklyStats.IssueCountDelta(
                    category = category,
                    severity = severity,
                    current = current.issueCount(category, severity),
                    previous = previous.issueCount(category, severity),
                )
            }
            .sortedWith(
                compareByDescending<WeeklyStats.IssueCountDelta> { it.current }
                    .thenByDescending { it.previous }
                    .thenBy { it.category }
                    .thenBy { it.severity },
            )
        val categoryTotals = issueCounts.groupBy { it.category }.map { (category, counts) ->
            WeeklyStats.CategoryCountDelta(
                category = category,
                current = counts.sumOf { it.current },
                previous = counts.sumOf { it.previous },
            )
        }
        val grammarCategories = (current.grammarPointCounts + previous.grammarPointCounts).map { it.category }.distinct()
        return WeeklyStats(
            entryCounts = InputMode.entries.map { mode ->
                WeeklyStats.ModeCount(
                    mode = mode,
                    current = current.entryCounts[mode] ?: 0,
                    previous = previous.entryCounts[mode] ?: 0,
                )
            },
            issueCounts = issueCounts,
            topCategories = categoryTotals
                .filter { it.current > 0 }
                .sortedWith(categoryOrder)
                .take(MAX_TOP_CATEGORIES),
            grammarPointCounts = grammarCategories
                .map { category ->
                    WeeklyStats.CategoryCountDelta(
                        category = category,
                        current = current.grammarPointCounts.filter { it.category == category }.sumOf { it.count },
                        previous = previous.grammarPointCounts.filter { it.category == category }.sumOf { it.count },
                    )
                }
                .sortedWith(categoryOrder),
            expressions = current.expressions
                .distinctBy { it.expression.trim().lowercase() }
                .map { WeeklyStats.WeeklyExpression(expression = it.expression, type = it.type, meaning = it.meaning) },
        )
    }

    private val categoryOrder: Comparator<WeeklyStats.CategoryCountDelta> =
        compareByDescending<WeeklyStats.CategoryCountDelta> { it.current }
            .thenByDescending { it.previous }
            .thenBy { it.category }

    private fun HistoryAggregation.issueCount(category: ErrorCategory, severity: Severity): Int =
        issueCounts.filter { it.category == category && it.severity == severity }.sumOf { it.count }
}
