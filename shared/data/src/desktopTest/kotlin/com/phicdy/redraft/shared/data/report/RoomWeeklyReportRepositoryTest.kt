package com.phicdy.redraft.shared.data.report

import com.phicdy.redraft.shared.data.history.HistoryUsage
import com.phicdy.redraft.shared.data.history.db.inMemoryHistoryDatabase
import com.phicdy.redraft.shared.model.ErrorCategory
import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.model.Severity
import com.phicdy.redraft.shared.model.TranslationResult
import com.phicdy.redraft.shared.model.WeeklyReportNarrative
import com.phicdy.redraft.shared.model.WeeklyStats
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

class RoomWeeklyReportRepositoryTest {
    private val database = inMemoryHistoryDatabase()
    private var now: Instant = Instant.parse("2026-11-24T00:00:00Z")
    private val clock = object : Clock {
        override fun now(): Instant = now
    }
    private val repository = RoomWeeklyReportRepository(
        dao = database.weeklyReportDao(),
        deviceIdProvider = { DEVICE_ID },
        clock = clock,
    )

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private suspend fun saveAt(instant: String, week: ReportWeek, summary: String = "summary"): WeeklyReport {
        now = Instant.parse(instant)
        return repository.save(
            week = week,
            stats = stats,
            narrative = narrative.copy(summary = summary),
            model = "test-model",
            usage = HistoryUsage(inputTokens = 900, outputTokens = 400),
        )
    }

    @Test
    fun savedReportReadsBackExactly(): TestResult = runTest {
        val saved = saveAt("2026-11-24T00:00:00.123Z", WEEK_16)

        assertEquals(saved, repository.get(saved.id))
        assertEquals(DEVICE_ID, saved.deviceId)
        assertEquals(saved.id, Uuid.parse(saved.id).toString())
        assertEquals(stats, saved.stats)
    }

    @Test
    fun listShowsNewestWeekFirst(): TestResult = runTest {
        val newer = saveAt("2026-11-24T00:00:00Z", WEEK_16)
        val older = saveAt("2026-11-24T00:01:00Z", WEEK_16.previous())

        assertEquals(listOf(newer.id, older.id), repository.observeReports().first().map { it.id })
    }

    @Test
    fun regeneratingAWeekReplacesItsReport(): TestResult = runTest {
        val other = saveAt("2026-11-24T00:00:00Z", WEEK_16.previous())
        val first = saveAt("2026-11-24T00:01:00Z", WEEK_16, summary = "first")
        val second = saveAt("2026-11-24T00:02:00Z", WEEK_16, summary = "second")

        assertNull(repository.get(first.id))
        assertEquals("second", repository.get(second.id)?.narrative?.summary)
        assertEquals(listOf(second.id, other.id), repository.observeReports().first().map { it.id })
    }

    @Test
    fun partialWhenGeneratedBeforeTheWeekEnded(): TestResult = runTest {
        val tokyo = TimeZone.of("Asia/Tokyo")
        val partial = saveAt("2026-11-22T14:59:00Z", WEEK_16)
        val complete = saveAt("2026-11-22T15:00:00Z", WEEK_16)

        assertTrue(partial.isPartial(tokyo))
        assertFalse(complete.isPartial(tokyo))
    }

    @Test
    fun unreadableRowsAreSkipped(): TestResult = runTest {
        val saved = saveAt("2026-11-24T00:00:00Z", WEEK_16)
        val unreadable = WeeklyReportMapper.toEntity(saved).copy(
            id = "unreadable",
            weekStart = "2026-11-02",
            narrativeJson = "{\"summary\": 1}",
        )
        database.weeklyReportDao().insert(unreadable)

        assertNull(repository.get("unreadable"))
        assertEquals(listOf(saved.id), repository.observeReports().first().map { it.id })
    }

    private companion object {
        const val DEVICE_ID = "3f1c9a52-7d0e-4b8f-9a61-0c2d5e7b8a94"
        val WEEK_16 = ReportWeek(LocalDate(2026, 11, 16))

        val stats = WeeklyStats(
            entryCounts = listOf(
                WeeklyStats.ModeCount(InputMode.Translate, current = 2, previous = 1),
                WeeklyStats.ModeCount(InputMode.Proofread, current = 4, previous = 5),
            ),
            issueCounts = listOf(WeeklyStats.IssueCountDelta(ErrorCategory.Article, Severity.Error, current = 3, previous = 1)),
            topCategories = listOf(WeeklyStats.CategoryCountDelta(ErrorCategory.Article, current = 3, previous = 1)),
            grammarPointCounts = listOf(WeeklyStats.CategoryCountDelta(ErrorCategory.Tense, current = 1, previous = 0)),
            expressions = listOf(
                WeeklyStats.WeeklyExpression("touch base", TranslationResult.ExpressionType.Idiom, "連絡を取り合う"),
            ),
        )

        val narrative = WeeklyReportNarrative(
            summary = "summary",
            recurringMistakes = listOf(
                WeeklyReportNarrative.RecurringMistake(
                    category = ErrorCategory.Article,
                    pattern = "可算名詞の単数形に冠詞がありません。",
                    examples = listOf(WeeklyReportNarrative.MistakeExample("I sent email", "I sent an email")),
                    advice = "名詞を書いたら a / the を確認しましょう。",
                ),
            ),
            improvements = listOf("時制の誤りが減りました。"),
            focusNextWeek = listOf("冠詞を確認する"),
            expressionsToReview = listOf(WeeklyReportNarrative.ExpressionToReview("touch base", "軽い連絡に使います。")),
        )
    }
}
