package com.phicdy.redraft.shared.data.history.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One item of an entry's result that the weekly report aggregates, copied out of the result JSON
 * so it can be counted with plain SQL.
 *
 * | [kind]          | [category] | [severity] | [expressionType] | [text]     | [detail]   |
 * |-----------------|------------|------------|------------------|------------|------------|
 * | `issue`         | yes        | yes        | null             | original   | suggestion |
 * | `grammar_point` | yes        | null       | null             | title      | null       |
 * | `expression`    | null       | null       | yes              | expression | meaning    |
 *
 * Rows are derived from [HistoryEntryEntity.resultJson] and never edited, so a sync can rebuild them.
 * [createdAt] is copied from the entry so a time range is a simple indexed query.
 */
@Entity(
    tableName = "history_finding",
    foreignKeys = [
        ForeignKey(
            entity = HistoryEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["created_at"]), Index(value = ["entry_id"])],
)
data class HistoryFindingEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "entry_id")
    val entryId: String,
    @ColumnInfo(name = "kind")
    val kind: String,
    /** Order within the entry's list of this kind, starting at 0. */
    @ColumnInfo(name = "position")
    val position: Int,
    /** `ErrorCategory` serial name. */
    @ColumnInfo(name = "category")
    val category: String?,
    /** `Severity` serial name. */
    @ColumnInfo(name = "severity")
    val severity: String?,
    /** `TranslationResult.ExpressionType` serial name. */
    @ColumnInfo(name = "expression_type")
    val expressionType: String?,
    @ColumnInfo(name = "text")
    val text: String,
    @ColumnInfo(name = "detail")
    val detail: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)

/** Values of [HistoryFindingEntity.kind]. Stored in the database, so never rename them. */
object FindingKind {
    const val ISSUE: String = "issue"
    const val GRAMMAR_POINT: String = "grammar_point"
    const val EXPRESSION: String = "expression"
}

/** Values of [HistoryFindingEntity.severity] (the `Severity` serial names) used in queries. */
object FindingSeverity {
    const val ERROR: String = "error"
    const val IMPROVEMENT: String = "improvement"
}
