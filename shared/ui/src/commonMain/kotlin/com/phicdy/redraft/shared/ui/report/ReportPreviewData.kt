package com.phicdy.redraft.shared.ui.report

import com.phicdy.redraft.shared.data.history.HistoryUsage
import com.phicdy.redraft.shared.data.report.ReportWeek
import com.phicdy.redraft.shared.data.report.WeeklyReport
import com.phicdy.redraft.shared.model.ErrorCategory
import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.model.Severity
import com.phicdy.redraft.shared.model.TranslationResult
import com.phicdy.redraft.shared.model.WeeklyReportNarrative
import com.phicdy.redraft.shared.model.WeeklyStats
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

// Sample data shared by the report previews. Fixed weeks, instants and time zone keep the
// rendered screenshots identical on every run.
internal val previewTimeZone: TimeZone = TimeZone.of("Asia/Tokyo")
internal val previewLastWeek: ReportWeek = ReportWeek(LocalDate(2026, 11, 16))
internal val previewThisWeek: ReportWeek = ReportWeek(LocalDate(2026, 11, 23))

internal val sampleStats = WeeklyStats(
    entryCounts = listOf(
        WeeklyStats.ModeCount(InputMode.Translate, current = 6, previous = 4),
        WeeklyStats.ModeCount(InputMode.Proofread, current = 9, previous = 11),
    ),
    issueCounts = listOf(
        WeeklyStats.IssueCountDelta(ErrorCategory.Article, Severity.Error, current = 5, previous = 3),
        WeeklyStats.IssueCountDelta(ErrorCategory.Naturalness, Severity.Improvement, current = 3, previous = 4),
        WeeklyStats.IssueCountDelta(ErrorCategory.Preposition, Severity.Error, current = 2, previous = 2),
        WeeklyStats.IssueCountDelta(ErrorCategory.Tense, Severity.Error, current = 0, previous = 4),
    ),
    topCategories = listOf(
        WeeklyStats.CategoryCountDelta(ErrorCategory.Article, current = 5, previous = 3),
        WeeklyStats.CategoryCountDelta(ErrorCategory.Naturalness, current = 3, previous = 4),
        WeeklyStats.CategoryCountDelta(ErrorCategory.Preposition, current = 2, previous = 2),
    ),
    grammarPointCounts = listOf(WeeklyStats.CategoryCountDelta(ErrorCategory.VerbForm, current = 2, previous = 1)),
    expressions = listOf(
        WeeklyStats.WeeklyExpression("touch base", TranslationResult.ExpressionType.Idiom, "軽く連絡を取り合う"),
        WeeklyStats.WeeklyExpression("heads-up", TranslationResult.ExpressionType.Word, "事前のお知らせ"),
    ),
)

internal val sampleNarrative = WeeklyReportNarrative(
    summary = "今週は英訳6件・校正9件で、先週より英訳が増えました。時制の誤りがなくなった一方、冠詞の誤りが5件と増えています。",
    recurringMistakes = listOf(
        WeeklyReportNarrative.RecurringMistake(
            category = ErrorCategory.Article,
            pattern = "数えられる名詞の単数形に a / the が抜けています。",
            examples = listOf(
                WeeklyReportNarrative.MistakeExample("I sent email to him", "I sent an email to him"),
                WeeklyReportNarrative.MistakeExample("Please check document", "Please check the document"),
            ),
            advice = "名詞を書いたら、数えられるか・特定のものかを確認して a か the を付けましょう。",
        ),
        WeeklyReportNarrative.RecurringMistake(
            category = ErrorCategory.Preposition,
            pattern = "日付・曜日の前置詞が in になっています。",
            examples = listOf(WeeklyReportNarrative.MistakeExample("in Monday", "on Monday")),
            advice = "曜日と日付には on を使います。",
        ),
    ),
    improvements = listOf("先週4件あった時制の誤りが今週はありませんでした。"),
    focusNextWeek = listOf("送信前に名詞ごとに冠詞を確認する", "曜日・日付の前置詞 on を意識する"),
    expressionsToReview = listOf(
        WeeklyReportNarrative.ExpressionToReview("touch base", "短い打ち合わせや連絡を提案するときに使えます。"),
    ),
)

private fun report(id: String, week: ReportWeek, createdAt: String, summary: String = sampleNarrative.summary): WeeklyReport =
    WeeklyReport(
        id = id,
        deviceId = "device",
        week = week,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(createdAt),
        model = "gpt-5-mini",
        usage = HistoryUsage(inputTokens = 2_345, outputTokens = 876),
        stats = sampleStats,
        narrative = sampleNarrative.copy(summary = summary),
    )

internal val sampleReports: List<WeeklyReport> = listOf(
    report("3", previewThisWeek, "2026-11-25T03:00:00Z", "今週はここまで英訳3件・校正4件です。冠詞の誤りが続いています。"),
    report("2", previewLastWeek, "2026-11-23T01:00:00Z"),
    report("1", previewLastWeek.previous(), "2026-11-16T01:00:00Z", "先週より校正の件数が増え、前置詞の誤りが減りました。"),
)
