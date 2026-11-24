package com.phicdy.redraft.shared.data.report.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One generated weekly report. Follows the sync-ready conventions of `history_entry`: a random
 * UUID [id], [deviceId], [createdAt]/[updatedAt] in epoch milliseconds and soft delete through
 * [deletedAt]. Regenerating a week soft-deletes the previous report of that week.
 */
@Entity(
    tableName = "weekly_report",
    indices = [Index(value = ["week_start"])],
)
data class WeeklyReportEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "device_id")
    val deviceId: String,
    /** The Monday the week starts on, as an ISO date such as `2026-11-16`; independent of the time zone. */
    @ColumnInfo(name = "week_start")
    val weekStart: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long?,
    /** The model that wrote the narrative. */
    @ColumnInfo(name = "model")
    val model: String,
    @ColumnInfo(name = "input_tokens")
    val inputTokens: Int,
    @ColumnInfo(name = "output_tokens")
    val outputTokens: Int,
    /** `WeeklyStats`, serialized with kotlinx.serialization. */
    @ColumnInfo(name = "stats_json")
    val statsJson: String,
    /** `WeeklyReportNarrative`, serialized with kotlinx.serialization. */
    @ColumnInfo(name = "narrative_json")
    val narrativeJson: String,
    /** Version of the JSON formats in [statsJson] and [narrativeJson]. */
    @ColumnInfo(name = "schema_version")
    val schemaVersion: Int,
)
