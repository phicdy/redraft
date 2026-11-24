package com.phicdy.redraft.shared.data.report

import com.phicdy.redraft.shared.data.history.HistoryUsage
import com.phicdy.redraft.shared.model.WeeklyReportNarrative
import com.phicdy.redraft.shared.model.WeeklyStats
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/** A saved weekly report: the computed [stats] and the LLM's [narrative]. */
data class WeeklyReport(
    val id: String,
    /** The install that generated it. */
    val deviceId: String,
    val week: ReportWeek,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** The model that wrote [narrative]. */
    val model: String,
    val usage: HistoryUsage,
    val stats: WeeklyStats,
    val narrative: WeeklyReportNarrative,
) {
    /** True when it was generated before the week ended, so it covers only part of the week. */
    fun isPartial(timeZone: TimeZone): Boolean = week.isInProgressAt(createdAt, timeZone)
}

/** Saved weekly reports. At most one report per week is current; older ones are soft-deleted. */
interface WeeklyReportRepository {
    /** Current reports, newest week first, re-emitted on every change. */
    fun observeReports(): Flow<List<WeeklyReport>>

    /** The report with [id], or null when it does not exist, was replaced or cannot be read. */
    suspend fun get(id: String): WeeklyReport?

    /** Saves a report for [week], replacing the week's previous report, and returns it. */
    suspend fun save(
        week: ReportWeek,
        stats: WeeklyStats,
        narrative: WeeklyReportNarrative,
        model: String,
        usage: HistoryUsage,
    ): WeeklyReport
}
