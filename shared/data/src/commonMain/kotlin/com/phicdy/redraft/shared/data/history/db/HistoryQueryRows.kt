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
