package com.phicdy.redraft.shared.ui.history

import com.phicdy.redraft.shared.data.history.HistoryAggregation
import com.phicdy.redraft.shared.data.history.HistoryContext
import com.phicdy.redraft.shared.data.history.HistoryEntry
import com.phicdy.redraft.shared.data.history.HistoryEntrySummary
import com.phicdy.redraft.shared.data.history.HistoryRepository
import com.phicdy.redraft.shared.data.history.HistoryResult
import com.phicdy.redraft.shared.data.history.HistoryUsage
import com.phicdy.redraft.shared.model.Severity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

/** Keeps entries in memory. Set [saveFailure] or [deleteFailure] to make those calls fail. */
internal class FakeHistoryRepository : HistoryRepository {
    val entries = MutableStateFlow<List<HistoryEntry>>(emptyList())
    var saveFailure: Exception? = null
    var deleteFailure: Exception? = null
    val queries = mutableListOf<String?>()
    val deletedIds = mutableListOf<String>()
    private var nextId = 1

    override suspend fun save(
        input: String,
        context: HistoryContext,
        result: HistoryResult,
        model: String,
        usage: HistoryUsage,
    ): HistoryEntry {
        saveFailure?.let { throw it }
        val createdAt = Instant.fromEpochMilliseconds(1_767_225_600_000L + nextId)
        val entry = HistoryEntry(
            id = "entry-${nextId++}",
            deviceId = "fake-device",
            createdAt = createdAt,
            updatedAt = createdAt,
            input = input,
            context = context,
            model = model,
            usage = usage,
            result = result,
        )
        entries.update { listOf(entry) + it }
        return entry
    }

    override fun observeEntries(query: String?): Flow<List<HistoryEntrySummary>> {
        queries += query
        return entries.map { list ->
            list.filter { query.isNullOrBlank() || query.trim() in it.input }.map { it.toSummary() }
        }
    }

    override suspend fun get(id: String): HistoryEntry? = entries.value.firstOrNull { it.id == id }

    override suspend fun delete(id: String) {
        deleteFailure?.let { throw it }
        deletedIds += id
        entries.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun aggregate(from: Instant, until: Instant): HistoryAggregation =
        error("Not used by the UI")

    private fun HistoryEntry.toSummary(): HistoryEntrySummary {
        val issues = (result as? HistoryResult.Proofread)?.result?.issues.orEmpty()
        val translation = (result as? HistoryResult.Translation)?.result
        return HistoryEntrySummary(
            id = id,
            createdAt = createdAt,
            mode = result.mode,
            input = input,
            output = translation?.translation ?: (result as HistoryResult.Proofread).result.correctedText,
            errorCount = issues.count { it.severity == Severity.Error },
            improvementCount = issues.count { it.severity == Severity.Improvement },
            grammarPointCount = translation?.grammarPoints?.size ?: 0,
            expressionCount = translation?.expressions?.size ?: 0,
        )
    }
}
