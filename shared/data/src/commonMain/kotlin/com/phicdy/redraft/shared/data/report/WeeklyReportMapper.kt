package com.phicdy.redraft.shared.data.report

import com.phicdy.redraft.shared.data.history.HistoryUsage
import com.phicdy.redraft.shared.data.report.db.WeeklyReportEntity
import com.phicdy.redraft.shared.model.WeeklyReportNarrative
import com.phicdy.redraft.shared.model.WeeklyStats
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/** Converts between [WeeklyReport] and its row. Rows this version cannot read are skipped. */
internal object WeeklyReportMapper {
    /** Version of the JSON in `stats_json` and `narrative_json`. Bump it when those types change incompatibly. */
    const val SCHEMA_VERSION: Int = 1

    private val json = Json { ignoreUnknownKeys = true }

    fun toEntity(report: WeeklyReport): WeeklyReportEntity = WeeklyReportEntity(
        id = report.id,
        deviceId = report.deviceId,
        weekStart = report.week.start.toString(),
        createdAt = report.createdAt.toEpochMilliseconds(),
        updatedAt = report.updatedAt.toEpochMilliseconds(),
        deletedAt = null,
        model = report.model,
        inputTokens = report.usage.inputTokens,
        outputTokens = report.usage.outputTokens,
        statsJson = json.encodeToString(WeeklyStats.serializer(), report.stats),
        narrativeJson = json.encodeToString(WeeklyReportNarrative.serializer(), report.narrative),
        schemaVersion = SCHEMA_VERSION,
    )

    /** Null when the week or the stored JSON cannot be read by this version. */
    fun toReport(entity: WeeklyReportEntity): WeeklyReport? = try {
        WeeklyReport(
            id = entity.id,
            deviceId = entity.deviceId,
            week = ReportWeek(LocalDate.parse(entity.weekStart)),
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            updatedAt = Instant.fromEpochMilliseconds(entity.updatedAt),
            model = entity.model,
            usage = HistoryUsage(inputTokens = entity.inputTokens, outputTokens = entity.outputTokens),
            stats = json.decodeFromString(WeeklyStats.serializer(), entity.statsJson),
            narrative = json.decodeFromString(WeeklyReportNarrative.serializer(), entity.narrativeJson),
        )
    } catch (e: IllegalArgumentException) {
        // A bad date, a week not starting on Monday, or a SerializationException (an IllegalArgumentException).
        null
    }
}
