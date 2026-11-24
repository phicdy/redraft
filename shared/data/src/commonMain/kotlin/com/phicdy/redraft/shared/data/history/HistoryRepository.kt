package com.phicdy.redraft.shared.data.history

import com.phicdy.redraft.shared.model.ProofreadResult
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** Local history of completed translations and proofreading. */
interface HistoryRepository {
    /** Saves a completed request and returns the stored entry. */
    suspend fun save(
        input: String,
        context: HistoryContext,
        result: HistoryResult,
        model: String,
        usage: HistoryUsage,
    ): HistoryEntry

    /**
     * Entries that are not deleted, newest first, re-emitted on every change. A non-blank [query]
     * keeps entries whose input or output contains it (case-insensitive for ASCII letters).
     */
    fun observeEntries(query: String? = null): Flow<List<HistoryEntrySummary>>

    /** The entry with [id], or null when it does not exist, is deleted or cannot be read. */
    suspend fun get(id: String): HistoryEntry?

    /** Soft-deletes the entry: it disappears from lists, lookups and aggregation. */
    suspend fun delete(id: String)

    /** Totals for entries created in [from] (inclusive) until [until] (exclusive). */
    suspend fun aggregate(from: Instant, until: Instant): HistoryAggregation

    /**
     * Proofread issues of the newest [maxEntries] proofreading entries created in [from] (inclusive)
     * until [until] (exclusive), newest entry first, for the weekly report to quote.
     */
    suspend fun recentIssues(from: Instant, until: Instant, maxEntries: Int): List<ProofreadResult.Issue>
}

/** The id of this install, generated once and kept. */
fun interface DeviceIdProvider {
    suspend fun deviceId(): String
}
