package com.phicdy.redraft.shared.ui.report

import com.phicdy.redraft.shared.data.history.HistoryRepository
import com.phicdy.redraft.shared.data.report.ReportWeek
import com.phicdy.redraft.shared.data.report.WeeklyReport
import com.phicdy.redraft.shared.data.report.WeeklyReportRepository
import com.phicdy.redraft.shared.data.report.WeeklyStatsCalculator
import com.phicdy.redraft.shared.llm.LlmException
import com.phicdy.redraft.shared.llm.ReportWriter
import com.phicdy.redraft.shared.llm.prompt.WeeklyReportInput
import com.phicdy.redraft.shared.ui.history.toHistoryUsage
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

/** Which week to report on. */
enum class ReportTarget {
    /** The most recent week that has ended. */
    LastWeek,

    /** The current week so far. */
    ThisWeek,
}

sealed interface GenerationResult {
    data class Generated(val report: WeeklyReport) : GenerationResult

    /** The week has no history, so the LLM was not called and nothing was saved. */
    data class NoEntries(val week: ReportWeek) : GenerationResult
}

/**
 * Generates and saves a weekly report: the numbers come from history via [WeeklyStatsCalculator],
 * and only the narrative is written by the LLM. Lives in the UI layer because it joins the data and
 * LLM layers, which must not depend on each other.
 */
class WeeklyReportGenerator(
    private val historyRepository: HistoryRepository,
    private val reportRepository: WeeklyReportRepository,
    private val reportWriter: ReportWriter,
    private val clock: Clock = Clock.System,
    /** Read on every call, so a change of the system time zone is picked up. */
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    /** The week [target] refers to right now. */
    fun weekFor(target: ReportTarget): ReportWeek = weekAt(target, clock.now(), timeZone())

    /** @throws LlmException when the narrative cannot be written; nothing is saved then. */
    suspend fun generate(target: ReportTarget): GenerationResult {
        val now = clock.now()
        val zone = timeZone()
        val week = weekAt(target, now, zone)
        val from = week.startInstant(zone)
        val until = week.endInstant(zone)
        val current = historyRepository.aggregate(from, until)
        if (current.entryCounts.values.sum() == 0) return GenerationResult.NoEntries(week)

        val previousWeek = week.previous()
        val previous = historyRepository.aggregate(previousWeek.startInstant(zone), previousWeek.endInstant(zone))
        val stats = WeeklyStatsCalculator.calculate(current, previous)
        val issues = historyRepository.recentIssues(from, until, maxEntries = MAX_ISSUE_ENTRIES)
        val written = reportWriter.write(
            WeeklyReportInput(stats = stats, issues = issues, isInProgress = week.isInProgressAt(now, zone)),
        )
        val report = reportRepository.save(
            week = week,
            stats = stats,
            narrative = written.output,
            model = written.model,
            usage = written.usage.toHistoryUsage(),
        )
        return GenerationResult.Generated(report)
    }

    private fun weekAt(target: ReportTarget, now: Instant, zone: TimeZone): ReportWeek = when (target) {
        ReportTarget.LastWeek -> ReportWeek.lastCompleted(now, zone)
        ReportTarget.ThisWeek -> ReportWeek.containing(now, zone)
    }

    private companion object {
        /** Proofreading entries whose issues are read; the prompt samples and caps them further. */
        const val MAX_ISSUE_ENTRIES: Int = 50
    }
}
