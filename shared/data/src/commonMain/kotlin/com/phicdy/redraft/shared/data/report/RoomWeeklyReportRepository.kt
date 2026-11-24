package com.phicdy.redraft.shared.data.report

import com.phicdy.redraft.shared.data.history.DeviceIdProvider
import com.phicdy.redraft.shared.data.history.HistoryUsage
import com.phicdy.redraft.shared.data.report.db.WeeklyReportDao
import com.phicdy.redraft.shared.model.WeeklyReportNarrative
import com.phicdy.redraft.shared.model.WeeklyStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

class RoomWeeklyReportRepository(
    private val dao: WeeklyReportDao,
    private val deviceIdProvider: DeviceIdProvider,
    private val clock: Clock = Clock.System,
    private val newId: () -> String = { Uuid.random().toString() },
) : WeeklyReportRepository {
    override fun observeReports(): Flow<List<WeeklyReport>> =
        dao.observeReports().map { rows -> rows.mapNotNull(WeeklyReportMapper::toReport) }

    override suspend fun get(id: String): WeeklyReport? = dao.get(id)?.let(WeeklyReportMapper::toReport)

    override suspend fun save(
        week: ReportWeek,
        stats: WeeklyStats,
        narrative: WeeklyReportNarrative,
        model: String,
        usage: HistoryUsage,
    ): WeeklyReport {
        // Millisecond precision, the same as stored, so the returned report equals what get() reads back.
        val now = Instant.fromEpochMilliseconds(clock.now().toEpochMilliseconds())
        val report = WeeklyReport(
            id = newId(),
            deviceId = deviceIdProvider.deviceId(),
            week = week,
            createdAt = now,
            updatedAt = now,
            model = model,
            usage = usage,
            stats = stats,
            narrative = narrative,
        )
        dao.replace(WeeklyReportMapper.toEntity(report))
        return report
    }
}
