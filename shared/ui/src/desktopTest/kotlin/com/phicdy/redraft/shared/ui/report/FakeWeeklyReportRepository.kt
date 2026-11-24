package com.phicdy.redraft.shared.ui.report

import com.phicdy.redraft.shared.data.history.HistoryUsage
import com.phicdy.redraft.shared.data.report.ReportWeek
import com.phicdy.redraft.shared.data.report.WeeklyReport
import com.phicdy.redraft.shared.data.report.WeeklyReportRepository
import com.phicdy.redraft.shared.model.WeeklyReportNarrative
import com.phicdy.redraft.shared.model.WeeklyStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

/** Keeps reports in memory, one per week like the real repository. Set [saveFailure] to make saving fail. */
internal class FakeWeeklyReportRepository : WeeklyReportRepository {
    val reports = MutableStateFlow<List<WeeklyReport>>(emptyList())
    var saveFailure: Exception? = null
    private var nextId = 1

    override fun observeReports(): Flow<List<WeeklyReport>> = reports

    override suspend fun get(id: String): WeeklyReport? = reports.value.firstOrNull { it.id == id }

    override suspend fun save(
        week: ReportWeek,
        stats: WeeklyStats,
        narrative: WeeklyReportNarrative,
        model: String,
        usage: HistoryUsage,
    ): WeeklyReport {
        saveFailure?.let { throw it }
        val createdAt = Instant.fromEpochMilliseconds(1_767_225_600_000L + nextId)
        val report = WeeklyReport(
            id = "report-${nextId++}",
            deviceId = "fake-device",
            week = week,
            createdAt = createdAt,
            updatedAt = createdAt,
            model = model,
            usage = usage,
            stats = stats,
            narrative = narrative,
        )
        reports.update { list ->
            (list.filterNot { it.week == week } + report).sortedByDescending { it.week.start }
        }
        return report
    }
}
