package com.phicdy.redraft.shared.data.history

import com.phicdy.redraft.shared.data.history.db.HistoryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

class RoomHistoryRepository(
    private val dao: HistoryDao,
    private val deviceIdProvider: DeviceIdProvider,
    private val clock: Clock = Clock.System,
    private val newId: () -> String = { Uuid.random().toString() },
) : HistoryRepository {
    override suspend fun save(
        input: String,
        context: HistoryContext,
        result: HistoryResult,
        model: String,
        usage: HistoryUsage,
    ): HistoryEntry {
        // Millisecond precision, the same as stored, so the returned entry equals what get() reads back.
        val now = Instant.fromEpochMilliseconds(clock.now().toEpochMilliseconds())
        val entry = HistoryEntry(
            id = newId(),
            deviceId = deviceIdProvider.deviceId(),
            createdAt = now,
            updatedAt = now,
            input = input,
            context = context,
            model = model,
            usage = usage,
            result = result,
        )
        dao.insert(HistoryMapper.toEntity(entry), HistoryMapper.toFindings(entry, newId))
        return entry
    }

    override fun observeEntries(query: String?): Flow<List<HistoryEntrySummary>> =
        dao.observeSummaries(HistoryMapper.containsPattern(query))
            .map { rows -> rows.mapNotNull(HistoryMapper::toSummary) }

    override suspend fun get(id: String): HistoryEntry? = dao.getEntry(id)?.let(HistoryMapper::toEntry)

    override suspend fun delete(id: String) {
        dao.markDeleted(id, deletedAt = clock.now().toEpochMilliseconds())
    }
}
