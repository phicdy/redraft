package com.phicdy.redraft.shared.data.history.db

import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryDaoTest {
    private val database = inMemoryHistoryDatabase()
    private val dao = database.historyDao()

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertsEntryWithFindings(): TestResult = runTest {
        val findings = listOf(
            finding(id = "f2", kind = FindingKind.ISSUE, position = 1),
            finding(id = "f1", kind = FindingKind.ISSUE, position = 0),
        )

        dao.insert(ENTRY, findings)

        assertEquals(ENTRY, dao.getEntry(ENTRY.id))
        assertEquals(listOf("f1", "f2"), dao.getFindings(ENTRY.id).map { it.id })
    }

    @Test
    fun failedFindingInsertRollsBackTheEntry(): TestResult = runTest {
        val duplicate = finding(id = "same", kind = FindingKind.ISSUE, position = 0)

        assertFailsWith<Exception> { dao.insert(ENTRY, listOf(duplicate, duplicate)) }

        assertNull(dao.getEntry(ENTRY.id))
        assertTrue(dao.getFindings(ENTRY.id).isEmpty())
    }

    @Test
    fun softDeleteHidesTheEntryButKeepsTheRow(): TestResult = runTest {
        dao.insert(ENTRY, emptyList())

        assertEquals(1, dao.markDeleted(ENTRY.id, deletedAt = 2_000L))

        assertNull(dao.getEntry(ENTRY.id))
        // A second delete changes nothing, so the first deletion time is kept.
        assertEquals(0, dao.markDeleted(ENTRY.id, deletedAt = 3_000L))
    }

    private fun finding(id: String, kind: String, position: Int): HistoryFindingEntity = HistoryFindingEntity(
        id = id,
        entryId = ENTRY.id,
        kind = kind,
        position = position,
        category = "article",
        severity = "error",
        expressionType = null,
        text = "a apple",
        detail = "an apple",
        createdAt = ENTRY.createdAt,
    )

    private companion object {
        val ENTRY = HistoryEntryEntity(
            id = "0b5c6f9e-2d4b-4a63-9c43-2a7c1c2f9d10",
            deviceId = "device",
            createdAt = 1_000L,
            updatedAt = 1_000L,
            deletedAt = null,
            mode = "proofread",
            inputText = "I ate a apple.",
            resultText = "I ate an apple.",
            purpose = "chat",
            politeness = "neutral",
            model = "test-model",
            inputTokens = 10,
            outputTokens = 20,
            resultSchemaVersion = 1,
            resultJson = "{}",
        )
    }
}
