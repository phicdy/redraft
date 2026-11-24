package com.phicdy.redraft.shared.ui.report

import com.phicdy.redraft.shared.llm.LlmError
import com.phicdy.redraft.shared.llm.LlmException
import com.phicdy.redraft.shared.llm.TokenUsage
import com.phicdy.redraft.shared.model.ErrorCategory
import com.phicdy.redraft.shared.model.Severity
import com.phicdy.redraft.shared.model.WeeklyStats
import com.phicdy.redraft.shared.ui.history.FakeHistoryRepository
import com.phicdy.redraft.shared.ui.main.FakeLlmClient
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WeeklyReportGeneratorTest {
    private val history = FakeHistoryRepository()
    private val reports = FakeWeeklyReportRepository()
    private val llm = FakeLlmClient()
    private val generator = generator(history, reports, llm)

    @Test
    fun weeksFollowTheClockAndTimeZone() {
        assertEquals(LAST_WEEK, generator.weekFor(ReportTarget.LastWeek))
        assertEquals(THIS_WEEK, generator.weekFor(ReportTarget.ThisWeek))
    }

    @Test
    fun lastWeekIsComparedWithTheWeekBeforeAndSaved(): TestResult = runTest {
        history.aggregations[LAST_WEEK.startInstant(TOKYO)] = aggregation(translate = 2, proofread = 3, articleErrors = 4)
        history.aggregations[LAST_WEEK.previous().startInstant(TOKYO)] = aggregation(translate = 1, proofread = 1, articleErrors = 1)
        history.issues = listOf(ISSUE)
        llm.usage = TokenUsage(inputTokens = 900, outputTokens = 300)

        val result = assertIs<GenerationResult.Generated>(generator.generate(ReportTarget.LastWeek))

        val report = result.report
        assertEquals(LAST_WEEK, report.week)
        assertEquals(llm.weeklyReport, report.narrative)
        assertEquals("fake-model", report.model)
        assertEquals(900, report.usage.inputTokens)
        assertEquals(
            listOf(WeeklyStats.IssueCountDelta(ErrorCategory.Article, Severity.Error, current = 4, previous = 1)),
            report.stats.issueCounts,
        )
        assertEquals(listOf(report), reports.reports.value)
        assertEquals(
            listOf(
                LAST_WEEK.startInstant(TOKYO) to LAST_WEEK.endInstant(TOKYO),
                LAST_WEEK.previous().startInstant(TOKYO) to LAST_WEEK.previous().endInstant(TOKYO),
            ),
            history.aggregatedRanges,
        )
        val input = Json.parseToJsonElement(llm.requests.single().input).jsonObject
        assertEquals("completed", input.getValue("week_status").jsonPrimitive.content)
        assertTrue("I sent email" in llm.requests.single().input)
    }

    @Test
    fun thisWeekIsMarkedInProgress(): TestResult = runTest {
        history.aggregations[THIS_WEEK.startInstant(TOKYO)] = aggregation(translate = 1, proofread = 0)

        val result = assertIs<GenerationResult.Generated>(generator.generate(ReportTarget.ThisWeek))

        assertEquals(THIS_WEEK, result.report.week)
        val input = Json.parseToJsonElement(llm.requests.single().input).jsonObject
        assertEquals("in_progress", input.getValue("week_status").jsonPrimitive.content)
    }

    @Test
    fun emptyWeekDoesNotCallTheLlm(): TestResult = runTest {
        val result = generator.generate(ReportTarget.LastWeek)

        assertEquals(GenerationResult.NoEntries(LAST_WEEK), result)
        assertTrue(llm.requests.isEmpty())
        assertTrue(reports.reports.value.isEmpty())
    }

    @Test
    fun llmFailureSavesNothing(): TestResult = runTest {
        history.aggregations[LAST_WEEK.startInstant(TOKYO)] = aggregation(translate = 1, proofread = 1)
        llm.llmError = LlmError.Timeout

        val error = assertFailsWith<LlmException> { generator.generate(ReportTarget.LastWeek) }

        assertEquals(LlmError.Timeout, error.error)
        assertTrue(reports.reports.value.isEmpty())
    }
}
