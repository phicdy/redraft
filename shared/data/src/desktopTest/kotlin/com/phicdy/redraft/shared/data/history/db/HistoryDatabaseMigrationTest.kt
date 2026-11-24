package com.phicdy.redraft.shared.data.history.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.phicdy.redraft.shared.data.report.db.WeeklyReportEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Creates a version 1 database exactly as the exported `1.json` describes, then opens it with the
 * current [HistoryDatabase]. Room validates the migrated schema against the entities when it opens,
 * so a wrong migration fails here.
 */
class HistoryDatabaseMigrationTest {
    private val directory = File(System.getProperty("java.io.tmpdir"), "redraft-migration-${System.nanoTime()}")
    private val file = File(directory, "history.db")

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun migratesFromVersion1KeepingHistory(): TestResult = runTest {
        directory.mkdirs()
        createVersion1Database()

        val database = Room.databaseBuilder<HistoryDatabase>(name = file.absolutePath)
            .buildHistoryDatabase(queryContext = Dispatchers.IO)
        try {
            assertEquals("kept", database.historyDao().getEntry("entry-1")?.inputText)
            val report = WeeklyReportEntity(
                id = "report-1",
                deviceId = "device",
                weekStart = "2026-11-16",
                createdAt = 1L,
                updatedAt = 1L,
                deletedAt = null,
                model = "m",
                inputTokens = 1,
                outputTokens = 2,
                statsJson = "{}",
                narrativeJson = "{}",
                schemaVersion = 1,
            )
            database.weeklyReportDao().replace(report)
            assertEquals(listOf(report), database.weeklyReportDao().observeReports().first())
        } finally {
            database.close()
        }
    }

    private fun createVersion1Database() {
        val schema = Json.parseToJsonElement(File(SCHEMA_DIRECTORY, "1.json").readText())
            .jsonObject.getValue("database").jsonObject
        val statements = buildList {
            schema.getValue("entities").jsonArray.map { it.jsonObject }.forEach { entity ->
                val table = entity.string("tableName")
                add(entity.string("createSql").replace("\${TABLE_NAME}", table))
                entity["indices"]?.jsonArray?.forEach { index ->
                    add(index.jsonObject.string("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            schema.getValue("setupQueries").jsonArray.forEach { add(it.jsonPrimitive.content) }
            add(
                """
                INSERT INTO history_entry (id, device_id, created_at, updated_at, deleted_at, mode, input_text,
                    result_text, purpose, politeness, model, input_tokens, output_tokens, result_schema_version,
                    result_json)
                VALUES ('entry-1', 'device', 1, 1, NULL, 'proofread', 'kept', 'kept', 'chat', 'neutral', 'm', 1, 2, 1, '{}')
                """.trimIndent(),
            )
            add("PRAGMA user_version = 1")
        }
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            statements.forEach(connection::execSQL)
        } finally {
            connection.close()
        }
    }

    private fun JsonObject.string(name: String): String = getValue(name).jsonPrimitive.content

    private companion object {
        /** Tests run in the module directory. */
        const val SCHEMA_DIRECTORY = "schemas/com.phicdy.redraft.shared.data.history.db.HistoryDatabase"
    }
}
