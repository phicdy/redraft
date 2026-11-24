package com.phicdy.redraft.shared.ui.report

import com.phicdy.redraft.shared.data.history.HistoryAggregation
import com.phicdy.redraft.shared.data.history.IssueCount
import com.phicdy.redraft.shared.data.report.ReportWeek
import com.phicdy.redraft.shared.llm.ReportWriter
import com.phicdy.redraft.shared.model.ErrorCategory
import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.model.ProofreadResult
import com.phicdy.redraft.shared.model.Severity
import com.phicdy.redraft.shared.ui.history.FakeHistoryRepository
import com.phicdy.redraft.shared.ui.main.FakeLlmClient
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

/** Wednesday 2026-11-25 12:00 in Tokyo: last week is 11/16–11/22 and this week started on 11/23. */
internal val NOW: Instant = Instant.parse("2026-11-25T03:00:00Z")
internal val TOKYO: TimeZone = TimeZone.of("Asia/Tokyo")
internal val LAST_WEEK: ReportWeek = ReportWeek(LocalDate(2026, 11, 16))
internal val THIS_WEEK: ReportWeek = ReportWeek(LocalDate(2026, 11, 23))

internal val ISSUE: ProofreadResult.Issue = ProofreadResult.Issue(
    original = "I sent email",
    suggestion = "I sent an email",
    category = ErrorCategory.Article,
    severity = Severity.Error,
    explanation = "email は可算名詞です。",
)

internal fun aggregation(translate: Int, proofread: Int, articleErrors: Int = 0): HistoryAggregation =
    HistoryAggregation(
        entryCounts = mapOf(InputMode.Translate to translate, InputMode.Proofread to proofread),
        issueCounts = if (articleErrors > 0) listOf(IssueCount(ErrorCategory.Article, Severity.Error, articleErrors)) else emptyList(),
        grammarPointCounts = emptyList(),
        expressions = emptyList(),
    )

internal fun generator(
    history: FakeHistoryRepository,
    reports: FakeWeeklyReportRepository,
    llm: FakeLlmClient,
    now: () -> Instant = { NOW },
): WeeklyReportGenerator = WeeklyReportGenerator(
    historyRepository = history,
    reportRepository = reports,
    reportWriter = ReportWriter(llm),
    clock = object : Clock {
        override fun now(): Instant = now()
    },
    timeZone = { TOKYO },
)
