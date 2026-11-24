package com.phicdy.redraft.shared.data.history.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class HistoryDao {
    @Insert
    abstract suspend fun insertEntry(entry: HistoryEntryEntity)

    @Insert
    abstract suspend fun insertFindings(findings: List<HistoryFindingEntity>)

    /** Saves an entry together with its findings, or nothing if either insert fails. */
    @Transaction
    open suspend fun insert(entry: HistoryEntryEntity, findings: List<HistoryFindingEntity>) {
        insertEntry(entry)
        insertFindings(findings)
    }

    /** The entry with [id] unless it was deleted. */
    @Query("SELECT * FROM history_entry WHERE id = :id AND deleted_at IS NULL")
    abstract suspend fun getEntry(id: String): HistoryEntryEntity?

    @Query("SELECT * FROM history_finding WHERE entry_id = :entryId ORDER BY kind, position")
    abstract suspend fun getFindings(entryId: String): List<HistoryFindingEntity>

    /** Soft delete: the row stays (so a later sync can propagate the deletion) but is hidden everywhere. */
    @Query("UPDATE history_entry SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id AND deleted_at IS NULL")
    abstract suspend fun markDeleted(id: String, deletedAt: Long): Int

    /**
     * Non-deleted entries, newest first. A non-null [pattern] is a LIKE pattern with `\` as the
     * escape character, matched against the input and the output.
     */
    @Query(
        """
        SELECT e.id, e.created_at, e.mode, e.input_text, e.result_text,
            (SELECT COUNT(*) FROM history_finding f WHERE f.entry_id = e.id
                AND f.kind = '${FindingKind.ISSUE}' AND f.severity = '${FindingSeverity.ERROR}') AS error_count,
            (SELECT COUNT(*) FROM history_finding f WHERE f.entry_id = e.id
                AND f.kind = '${FindingKind.ISSUE}' AND f.severity = '${FindingSeverity.IMPROVEMENT}') AS improvement_count,
            (SELECT COUNT(*) FROM history_finding f WHERE f.entry_id = e.id
                AND f.kind = '${FindingKind.GRAMMAR_POINT}') AS grammar_point_count,
            (SELECT COUNT(*) FROM history_finding f WHERE f.entry_id = e.id
                AND f.kind = '${FindingKind.EXPRESSION}') AS expression_count
        FROM history_entry e
        WHERE e.deleted_at IS NULL
            AND (:pattern IS NULL OR e.input_text LIKE :pattern ESCAPE '\' OR e.result_text LIKE :pattern ESCAPE '\')
        ORDER BY e.created_at DESC, e.id DESC
        """,
    )
    abstract fun observeSummaries(pattern: String?): Flow<List<HistorySummaryRow>>
}
