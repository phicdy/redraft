package com.phicdy.redraft.shared.data.report

import com.phicdy.redraft.shared.data.history.GrammarPointCount
import com.phicdy.redraft.shared.data.history.HistoryAggregation
import com.phicdy.redraft.shared.data.history.IssueCount
import com.phicdy.redraft.shared.data.history.LearnedExpression
import com.phicdy.redraft.shared.model.ErrorCategory
import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.model.Severity
import com.phicdy.redraft.shared.model.TranslationResult
import com.phicdy.redraft.shared.model.WeeklyStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class WeeklyStatsCalculatorTest {
    private fun aggregation(
        translate: Int = 0,
        proofread: Int = 0,
        issues: List<IssueCount> = emptyList(),
        grammarPoints: List<GrammarPointCount> = emptyList(),
        expressions: List<LearnedExpression> = emptyList(),
    ): HistoryAggregation = HistoryAggregation(
        entryCounts = mapOf(InputMode.Translate to translate, InputMode.Proofread to proofread),
        issueCounts = issues,
        grammarPointCounts = grammarPoints,
        expressions = expressions,
    )

    private fun expression(text: String, millis: Long = 0): LearnedExpression = LearnedExpression(
        entryId = "entry",
        createdAt = Instant.fromEpochMilliseconds(millis),
        expression = text,
        type = TranslationResult.ExpressionType.Phrase,
        meaning = "意味",
    )

    @Test
    fun entryCountsCompareEveryMode() {
        val stats = WeeklyStatsCalculator.calculate(aggregation(translate = 3, proofread = 5), aggregation(proofread = 6))

        assertEquals(
            listOf(
                WeeklyStats.ModeCount(InputMode.Translate, current = 3, previous = 0),
                WeeklyStats.ModeCount(InputMode.Proofread, current = 5, previous = 6),
            ),
            stats.entryCounts,
        )
        assertEquals(8, stats.totalEntries)
        assertEquals(listOf(3, -1), stats.entryCounts.map { it.delta })
    }

    @Test
    fun issueCountsIncludeCategoriesOfEitherWeekWithDeltas() {
        val current = aggregation(
            issues = listOf(
                IssueCount(ErrorCategory.Article, Severity.Error, 4),
                IssueCount(ErrorCategory.Naturalness, Severity.Improvement, 2),
                IssueCount(ErrorCategory.Article, Severity.Improvement, 1),
            ),
        )
        val previous = aggregation(
            issues = listOf(
                IssueCount(ErrorCategory.Article, Severity.Error, 1),
                IssueCount(ErrorCategory.Tense, Severity.Error, 3),
            ),
        )

        val stats = WeeklyStatsCalculator.calculate(current, previous)

        assertEquals(
            listOf(
                WeeklyStats.IssueCountDelta(ErrorCategory.Article, Severity.Error, current = 4, previous = 1),
                WeeklyStats.IssueCountDelta(ErrorCategory.Naturalness, Severity.Improvement, current = 2, previous = 0),
                WeeklyStats.IssueCountDelta(ErrorCategory.Article, Severity.Improvement, current = 1, previous = 0),
                // Dropped to zero: kept so the improvement is visible.
                WeeklyStats.IssueCountDelta(ErrorCategory.Tense, Severity.Error, current = 0, previous = 3),
            ),
            stats.issueCounts,
        )
        assertEquals(listOf(3, 2, 1, -3), stats.issueCounts.map { it.delta })
    }

    @Test
    fun topCategoriesSumBothSeveritiesAndSkipZero() {
        val current = aggregation(
            issues = listOf(
                IssueCount(ErrorCategory.Naturalness, Severity.Improvement, 3),
                IssueCount(ErrorCategory.Article, Severity.Error, 2),
                IssueCount(ErrorCategory.Article, Severity.Improvement, 2),
                IssueCount(ErrorCategory.Preposition, Severity.Error, 1),
                IssueCount(ErrorCategory.Tense, Severity.Error, 1),
            ),
        )
        val previous = aggregation(
            issues = listOf(
                IssueCount(ErrorCategory.Tense, Severity.Error, 2),
                IssueCount(ErrorCategory.WordChoice, Severity.Error, 9),
            ),
        )

        val stats = WeeklyStatsCalculator.calculate(current, previous)

        assertEquals(
            listOf(
                WeeklyStats.CategoryCountDelta(ErrorCategory.Article, current = 4, previous = 0),
                WeeklyStats.CategoryCountDelta(ErrorCategory.Naturalness, current = 3, previous = 0),
                // Tied with Preposition at 1; the higher previous count comes first.
                WeeklyStats.CategoryCountDelta(ErrorCategory.Tense, current = 1, previous = 2),
            ),
            stats.topCategories,
        )
    }

    @Test
    fun grammarPointsCompareBothWeeks() {
        val current = aggregation(grammarPoints = listOf(GrammarPointCount(ErrorCategory.Tense, 2)))
        val previous = aggregation(grammarPoints = listOf(GrammarPointCount(ErrorCategory.Article, 1)))

        val stats = WeeklyStatsCalculator.calculate(current, previous)

        assertEquals(
            listOf(
                WeeklyStats.CategoryCountDelta(ErrorCategory.Tense, current = 2, previous = 0),
                WeeklyStats.CategoryCountDelta(ErrorCategory.Article, current = 0, previous = 1),
            ),
            stats.grammarPointCounts,
        )
    }

    @Test
    fun expressionsAreThisWeeksWithoutDuplicates() {
        val current = aggregation(
            expressions = listOf(expression("touch base", 1), expression("Touch base ", 2), expression("heads-up", 3)),
        )
        val previous = aggregation(expressions = listOf(expression("old one")))

        val stats = WeeklyStatsCalculator.calculate(current, previous)

        assertEquals(listOf("touch base", "heads-up"), stats.expressions.map { it.expression })
    }

    @Test
    fun emptyWeeksGiveZeroCounts() {
        val stats = WeeklyStatsCalculator.calculate(aggregation(), aggregation())

        assertEquals(0, stats.totalEntries)
        assertTrue(stats.issueCounts.isEmpty())
        assertTrue(stats.topCategories.isEmpty())
        assertTrue(stats.expressions.isEmpty())
    }
}
