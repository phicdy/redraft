package com.phicdy.redraft.shared.llm

import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class WritingAssistantTest {
    private val requests = mutableListOf<LlmRequest>()

    /** Answers with a minimal valid output for whichever schema was requested. */
    private val client = object : LlmClient {
        override suspend fun <T> generate(request: LlmRequest, outputSerializer: KSerializer<T>): LlmResult<T> {
            requests += request
            val output = when (request.schema.name) {
                "translation_result" ->
                    """{"translation":"x","alternatives":[],"grammar_points":[],"expressions":[]}"""
                else -> """{"corrected_text":"x","issues":[],"natural_alternatives":[],"overall_comment":"x"}"""
            }
            return LlmResult(
                output = Json.decodeFromString(outputSerializer, output),
                usage = TokenUsage(inputTokens = 0, outputTokens = 0),
                model = request.model,
            )
        }
    }

    private var config = ProviderConfig(
        id = "openai",
        kind = ProviderKind.OpenAiResponses,
        baseUrl = "https://api.openai.com/v1",
        defaultModel = "model-a",
    )

    private val assistant = WritingAssistant(client, ProviderConfigProvider { config })

    @Test
    fun usesTheModelOfTheCurrentConfigForEachRequest(): TestResult = runTest {
        assistant.translate("会議を延期したいです")
        config = config.copy(defaultModel = "model-b")
        assistant.proofread("I look forward to see you.")

        assertEquals(listOf("model-a", "model-b"), requests.map { it.model })
        assertEquals("会議を延期したいです", requests[0].input)
        assertEquals("I look forward to see you.", requests[1].input)
    }
}
