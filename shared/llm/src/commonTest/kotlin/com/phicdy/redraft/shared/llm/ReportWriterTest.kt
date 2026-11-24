package com.phicdy.redraft.shared.llm

import com.phicdy.redraft.shared.llm.prompt.WeeklyReportInput
import com.phicdy.redraft.shared.llm.schema.OutputSchemas
import com.phicdy.redraft.shared.model.WeeklyReportNarrative
import com.phicdy.redraft.shared.model.WeeklyStats
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ReportWriterTest {
    private val narrative = WeeklyReportNarrative(
        summary = "今週は8件書きました。",
        recurringMistakes = emptyList(),
        improvements = emptyList(),
        focusNextWeek = listOf("冠詞を確認する"),
        expressionsToReview = emptyList(),
    )

    @Test
    fun writesWithTheWeeklyReportSchema(): TestResult = runTest {
        val requests = mutableListOf<LlmRequest>()
        val client = object : LlmClient {
            override suspend fun <T> generate(request: LlmRequest, outputSerializer: KSerializer<T>): LlmResult<T> {
                requests += request
                val json = Json.encodeToString(WeeklyReportNarrative.serializer(), narrative)
                return LlmResult(Json.decodeFromString(outputSerializer, json), TokenUsage(1, 2), "m")
            }
        }
        val stats = WeeklyStats(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

        val result = ReportWriter(client).write(WeeklyReportInput(stats, emptyList(), isInProgress = false))

        assertEquals(narrative, result.output)
        assertEquals(OutputSchemas.weeklyReport, requests.single().schema)
    }
}
