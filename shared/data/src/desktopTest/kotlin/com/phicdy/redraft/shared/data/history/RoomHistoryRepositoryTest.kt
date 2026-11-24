package com.phicdy.redraft.shared.data.history

import com.phicdy.redraft.shared.data.history.db.inMemoryHistoryDatabase
import com.phicdy.redraft.shared.model.InputMode
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

class RoomHistoryRepositoryTest {
    private val database = inMemoryHistoryDatabase()
    private var now: Instant = Instant.fromEpochMilliseconds(DAY_1)
    private val clock = object : Clock {
        override fun now(): Instant = now
    }
    private val repository = RoomHistoryRepository(
        dao = database.historyDao(),
        deviceIdProvider = { DEVICE_ID },
        clock = clock,
    )

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private suspend fun saveAt(millis: Long, result: HistoryResult, input: String = "input"): HistoryEntry {
        now = Instant.fromEpochMilliseconds(millis)
        return repository.save(
            input = input,
            context = HistorySamples.context,
            result = result,
            model = "test-model",
            usage = HistorySamples.usage,
        )
    }

    @Test
    fun savedEntriesReadBackExactly(): TestResult = runTest {
        val translated = saveAt(DAY_1, HistorySamples.translation, input = "来週の会議を金曜日に変更できますか？")
        val proofread = saveAt(DAY_1 + 1, HistorySamples.proofread, input = "I look forward to see you.")

        assertEquals(translated, repository.get(translated.id))
        assertEquals(proofread, repository.get(proofread.id))
        assertEquals(DEVICE_ID, translated.deviceId)
        assertEquals(Instant.fromEpochMilliseconds(DAY_1), translated.createdAt)
        assertEquals(translated.createdAt, translated.updatedAt)
        // Random UUIDs, not sequence numbers, so entries from several devices never collide.
        assertEquals(translated.id, Uuid.parse(translated.id).toString())
        assertNotEquals(translated.id, proofread.id)
    }

    @Test
    fun listShowsNewestFirstWithFindingCounts(): TestResult = runTest {
        val translated = saveAt(DAY_1, HistorySamples.translation)
        val proofread = saveAt(DAY_2, HistorySamples.proofread)

        val entries = repository.observeEntries().first()

        assertEquals(listOf(proofread.id, translated.id), entries.map { it.id })
        val proofreadRow = entries[0]
        assertEquals(InputMode.Proofread, proofreadRow.mode)
        assertEquals("I look forward to seeing you.", proofreadRow.output)
        assertEquals(listOf(1, 1, 0, 0), proofreadRow.counts())
        assertEquals(listOf(0, 0, 2, 1), entries[1].counts())
        assertEquals(Instant.fromEpochMilliseconds(DAY_1), entries[1].createdAt)
    }

    @Test
    fun searchMatchesInputAndOutput(): TestResult = runTest {
        val meeting = saveAt(DAY_1, HistorySamples.translation, input = "会議を変更したい")
        val seeing = saveAt(DAY_2, HistorySamples.proofread, input = "I look forward to see you.")
        saveAt(DAY_3, HistorySamples.proofread, input = "100% done")

        suspend fun search(query: String?): List<String> = repository.observeEntries(query).first().map { it.id }

        assertEquals(listOf(meeting.id), search("会議"))
        // Output text (translation), case-insensitive for ASCII.
        assertEquals(listOf(meeting.id), search("FRIDAY"))
        assertEquals(listOf(seeing.id), search("to see you"))
        assertEquals(1, search("100%").size)
        assertTrue(search("%%").isEmpty())
        assertEquals(3, search("  ").size)
        assertEquals(3, search(null).size)
    }

    @Test
    fun listUpdatesWhenEntriesChange(): TestResult = runTest {
        assertTrue(repository.observeEntries().first().isEmpty())
        val update = async { repository.observeEntries().first { it.isNotEmpty() } }

        val saved = saveAt(DAY_1, HistorySamples.proofread)

        assertEquals(listOf(saved.id), update.await().map { it.id })
    }

    @Test
    fun deletedEntriesAreHiddenEverywhere(): TestResult = runTest {
        val kept = saveAt(DAY_1, HistorySamples.proofread)
        val deleted = saveAt(DAY_1 + 1, HistorySamples.proofread)
        val deletedTranslation = saveAt(DAY_1 + 2, HistorySamples.translation)

        repository.delete(deleted.id)
        repository.delete(deletedTranslation.id)

        assertNull(repository.get(deleted.id))
        assertEquals(listOf(kept.id), repository.observeEntries().first().map { it.id })
    }

    private fun HistoryEntrySummary.counts(): List<Int> =
        listOf(errorCount, improvementCount, grammarPointCount, expressionCount)

    private companion object {
        const val DEVICE_ID = "3f1c9a52-7d0e-4b8f-9a61-0c2d5e7b8a94"
        const val DAY_1 = 1_767_225_600_000L // 2026-01-01T00:00:00Z
        const val DAY_2 = DAY_1 + 86_400_000L
        const val DAY_3 = DAY_2 + 86_400_000L
    }
}
