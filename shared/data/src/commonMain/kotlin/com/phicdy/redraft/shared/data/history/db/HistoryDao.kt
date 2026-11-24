package com.phicdy.redraft.shared.data.history.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

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
}
