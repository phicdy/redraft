package com.phicdy.redraft.shared.data.report.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WeeklyReportDao {
    @Insert
    abstract suspend fun insert(report: WeeklyReportEntity)

    /** Soft-deletes the current report of the week, if any. */
    @Query(
        """
        UPDATE weekly_report SET deleted_at = :deletedAt, updated_at = :deletedAt
        WHERE week_start = :weekStart AND deleted_at IS NULL
        """,
    )
    abstract suspend fun markWeekDeleted(weekStart: String, deletedAt: Long): Int

    /** Saves [report] as the only current report of its week. */
    @Transaction
    open suspend fun replace(report: WeeklyReportEntity) {
        markWeekDeleted(report.weekStart, deletedAt = report.createdAt)
        insert(report)
    }

    /** Non-deleted reports, newest week first. */
    @Query("SELECT * FROM weekly_report WHERE deleted_at IS NULL ORDER BY week_start DESC, created_at DESC")
    abstract fun observeReports(): Flow<List<WeeklyReportEntity>>

    @Query("SELECT * FROM weekly_report WHERE id = :id AND deleted_at IS NULL")
    abstract suspend fun get(id: String): WeeklyReportEntity?
}
