package com.phicdy.redraft.shared.data.history.db

import androidx.room.ColumnInfo

/** A history list row with finding counts. */
data class HistorySummaryRow(
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "mode")
    val mode: String,
    @ColumnInfo(name = "input_text")
    val inputText: String,
    @ColumnInfo(name = "result_text")
    val resultText: String,
    @ColumnInfo(name = "error_count")
    val errorCount: Int,
    @ColumnInfo(name = "improvement_count")
    val improvementCount: Int,
    @ColumnInfo(name = "grammar_point_count")
    val grammarPointCount: Int,
    @ColumnInfo(name = "expression_count")
    val expressionCount: Int,
)

data class FindingCountRow(
    @ColumnInfo(name = "category")
    val category: String?,
    @ColumnInfo(name = "severity")
    val severity: String?,
    @ColumnInfo(name = "item_count")
    val count: Int,
)

data class ModeCountRow(
    @ColumnInfo(name = "mode")
    val mode: String,
    @ColumnInfo(name = "item_count")
    val count: Int,
)

/** Everything the weekly report needs, read in one transaction. */
data class AggregationRows(
    val modeCounts: List<ModeCountRow>,
    val issueCounts: List<FindingCountRow>,
    val grammarPointCounts: List<FindingCountRow>,
    val expressions: List<HistoryFindingEntity>,
)
