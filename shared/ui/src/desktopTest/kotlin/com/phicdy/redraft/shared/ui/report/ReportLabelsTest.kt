package com.phicdy.redraft.shared.ui.report

import com.phicdy.redraft.shared.data.report.ReportWeek
import com.phicdy.redraft.shared.model.ErrorCategory
import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.model.Severity
import com.phicdy.redraft.shared.model.WeeklyStats
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ReportLabelsTest {
    @Test
    fun weekLabelsShowMondayToSunday() {
        val week = ReportWeek(LocalDate(2026, 12, 28))

        assertEquals("12/28〜1/3", week.rangeLabel())
        assertEquals("2026/12/28〜1/3", week.titleLabel())
    }

    @Test
    fun deltaLabelsHaveASign() {
        assertEquals("+3", deltaLabel(3))
        assertEquals("−2", deltaLabel(-2))
        assertEquals("±0", deltaLabel(0))
    }

    @Test
    fun countsLabelListsModesAndIssues() {
        val stats = WeeklyStats(
            entryCounts = listOf(
                WeeklyStats.ModeCount(InputMode.Translate, current = 3, previous = 0),
                WeeklyStats.ModeCount(InputMode.Proofread, current = 5, previous = 0),
            ),
            issueCounts = listOf(
                WeeklyStats.IssueCountDelta(ErrorCategory.Article, Severity.Error, current = 4, previous = 9),
                WeeklyStats.IssueCountDelta(ErrorCategory.Clarity, Severity.Improvement, current = 3, previous = 0),
            ),
            topCategories = emptyList(),
            grammarPointCounts = emptyList(),
            expressions = emptyList(),
        )

        assertEquals("英訳3・校正5・指摘7", stats.countsLabel())
    }
}
