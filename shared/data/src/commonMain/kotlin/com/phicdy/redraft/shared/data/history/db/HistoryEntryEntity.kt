package com.phicdy.redraft.shared.data.history.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One completed translation or proofreading.
 *
 * Designed for a later sync between devices: [id] is a random UUID string instead of an
 * autoincrement number, every row carries [deviceId] and [createdAt]/[updatedAt], and rows are
 * never removed but soft deleted through [deletedAt]. Timestamps are epoch milliseconds (UTC).
 * Enum-like columns hold stable strings that must never be renamed.
 */
@Entity(
    tableName = "history_entry",
    indices = [Index(value = ["created_at"])],
)
data class HistoryEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "device_id")
    val deviceId: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long?,
    /** `translate` or `proofread`. */
    @ColumnInfo(name = "mode")
    val mode: String,
    @ColumnInfo(name = "input_text")
    val inputText: String,
    /** The main output (translation or corrected text), kept as a column so it can be searched and listed. */
    @ColumnInfo(name = "result_text")
    val resultText: String,
    /** `email`, `chat` or `document`. */
    @ColumnInfo(name = "purpose")
    val purpose: String,
    /** `casual`, `neutral` or `formal`. */
    @ColumnInfo(name = "politeness")
    val politeness: String,
    @ColumnInfo(name = "model")
    val model: String,
    @ColumnInfo(name = "input_tokens")
    val inputTokens: Int,
    @ColumnInfo(name = "output_tokens")
    val outputTokens: Int,
    /** Version of the JSON format in [resultJson], so old entries can still be read after the result types change. */
    @ColumnInfo(name = "result_schema_version")
    val resultSchemaVersion: Int,
    /** The whole `TranslationResult` / `ProofreadResult`, serialized with kotlinx.serialization. */
    @ColumnInfo(name = "result_json")
    val resultJson: String,
)
