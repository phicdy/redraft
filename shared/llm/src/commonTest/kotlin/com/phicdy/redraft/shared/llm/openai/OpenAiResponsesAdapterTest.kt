package com.phicdy.redraft.shared.llm.openai

import com.phicdy.redraft.shared.llm.LlmError
import com.phicdy.redraft.shared.llm.LlmException
import com.phicdy.redraft.shared.llm.LlmConnection
import com.phicdy.redraft.shared.llm.LlmConnectionSource
import com.phicdy.redraft.shared.llm.LlmRequest
import com.phicdy.redraft.shared.llm.ProviderConfig
import com.phicdy.redraft.shared.llm.ProviderKind
import com.phicdy.redraft.shared.llm.StructuredLlmClient
import com.phicdy.redraft.shared.llm.TokenUsage
import com.phicdy.redraft.shared.llm.prompt.Prompts
import com.phicdy.redraft.shared.llm.prompt.WritingContext
import com.phicdy.redraft.shared.model.ErrorCategory
import com.phicdy.redraft.shared.model.ProofreadResult
import com.phicdy.redraft.shared.model.Severity
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OpenAiResponsesAdapterTest {
    private val config = ProviderConfig(
        id = "work",
        kind = ProviderKind.OpenAiResponses,
        baseUrl = "https://proxy.example.com/openai/v1/",
        defaultModel = "test-model",
        extraHeaders = mapOf("X-Team" to "redraft", "OpenAI-Project" to "proj_123"),
    )

    private val request: LlmRequest = Prompts.proofread(
        input = "I look forward to see you.",
        context = WritingContext(),
    )

    private val validOutput = """
        {"corrected_text":"I look forward to seeing you.",
         "issues":[{"original":"to see","suggestion":"to seeing","category":"verb_form",
                    "severity":"error","explanation":"look forward to の後は動名詞です。"}],
         "natural_alternatives":[],
         "overall_comment":"全体的に伝わります。"}
    """.trimIndent()

    private val requests = mutableListOf<HttpRequestData>()

    private fun adapter(
        apiKey: String? = "sk-test",
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): OpenAiResponsesAdapter {
        val engine = MockEngine { data ->
            requests += data
            handler(this, data)
        }
        val client = HttpClient(engine) {
            install(HttpTimeout)
        }
        return OpenAiResponsesAdapter(client, LlmConnectionSource { LlmConnection(config, apiKey) })
    }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun sendsResponsesRequestWithStoreFalseAndStrictSchema(): TestResult = runTest {
        var body: JsonObject? = null
        val adapter = adapter { data ->
            body = Json.parseToJsonElement(data.body.toByteArray().decodeToString()).jsonObject
            json(successResponse(validOutput))
        }

        adapter.complete(request)

        val sent = requests.single()
        assertEquals(HttpMethod.Post, sent.method)
        assertEquals("https://proxy.example.com/openai/v1/responses", sent.url.toString())
        val sentBody = checkNotNull(body)
        assertEquals("test-model", sentBody["model"]?.jsonPrimitive?.content)
        assertEquals(request.instructions, sentBody["instructions"]?.jsonPrimitive?.content)
        assertEquals("I look forward to see you.", sentBody["input"]?.jsonPrimitive?.content)
        assertEquals(request.maxOutputTokens, sentBody["max_output_tokens"]?.jsonPrimitive?.int)
        assertFalse(checkNotNull(sentBody["store"]).jsonPrimitive.boolean)
        val format = checkNotNull(sentBody["text"]).jsonObject["format"]!!.jsonObject
        assertEquals("json_schema", format["type"]?.jsonPrimitive?.content)
        assertEquals("proofread_result", format["name"]?.jsonPrimitive?.content)
        assertTrue(checkNotNull(format["strict"]).jsonPrimitive.boolean)
        assertEquals(request.schema.schema, format["schema"])
    }

    @Test
    fun sendsBearerKeyAndExtraHeaders(): TestResult = runTest {
        val adapter = adapter { json(successResponse(validOutput)) }

        adapter.complete(request)

        val headers = requests.single().headers
        assertEquals("Bearer sk-test", headers[HttpHeaders.Authorization])
        assertEquals("redraft", headers["X-Team"])
        assertEquals("proj_123", headers["OpenAI-Project"])
    }

    @Test
    fun parsesSuccessResponse(): TestResult = runTest {
        val adapter = adapter { json(successResponse(validOutput)) }

        val result = adapter.complete(request)

        assertEquals(validOutput, result.output)
        assertEquals(TokenUsage(inputTokens = 812, outputTokens = 154), result.usage)
        assertEquals("test-model-2026-08-01", result.model)
    }

    @Test
    fun requestModelOverridesTheConnectionDefault(): TestResult = runTest {
        var body: JsonObject? = null
        val adapter = adapter { data ->
            body = Json.parseToJsonElement(data.body.toByteArray().decodeToString()).jsonObject
            json(successResponse(validOutput))
        }

        adapter.complete(request.copy(model = "override-model"))

        assertEquals("override-model", checkNotNull(body)["model"]?.jsonPrimitive?.content)
    }

    @Test
    fun readsTheConnectionOnEveryRequest(): TestResult = runTest {
        var connection = LlmConnection(config, "sk-first")
        val engine = MockEngine { data ->
            requests += data
            json(successResponse(validOutput))
        }
        val adapter = OpenAiResponsesAdapter(HttpClient(engine), LlmConnectionSource { connection })

        adapter.complete(request)
        connection = LlmConnection(
            config.copy(baseUrl = "https://api.example.com/v1", extraHeaders = emptyMap()),
            "sk-second",
        )
        adapter.complete(request)

        assertEquals("https://proxy.example.com/openai/v1/responses", requests[0].url.toString())
        assertEquals("Bearer sk-first", requests[0].headers[HttpHeaders.Authorization])
        assertEquals("https://api.example.com/v1/responses", requests[1].url.toString())
        assertEquals("Bearer sk-second", requests[1].headers[HttpHeaders.Authorization])
        assertEquals(null, requests[1].headers["X-Team"])
    }

    @Test
    fun commonLayerDecodesSuccessIntoModel(): TestResult = runTest {
        val client = StructuredLlmClient(adapter { json(successResponse(validOutput)) })

        val result = client.generate(request, ProofreadResult.serializer())

        val issue = result.output.issues.single()
        assertEquals("I look forward to seeing you.", result.output.correctedText)
        assertEquals(ErrorCategory.VerbForm, issue.category)
        assertEquals(Severity.Error, issue.severity)
        assertEquals(1, requests.size)
    }

    @Test
    fun refusalIsTypedError(): TestResult = runTest {
        val adapter = adapter {
            json(
                """
                {"id":"resp_1","object":"response","status":"completed","model":"test-model",
                 "output":[{"type":"message","id":"msg_1","status":"completed","role":"assistant",
                   "content":[{"type":"refusal","refusal":"I can't help with that."}]}],
                 "usage":{"input_tokens":10,"output_tokens":5,"total_tokens":15}}
                """.trimIndent(),
            )
        }

        val error = assertFailsWith<LlmException> { adapter.complete(request) }.error

        assertEquals(LlmError.Refused("I can't help with that."), error)
    }

    @Test
    fun incompleteIsTypedError(): TestResult = runTest {
        val adapter = adapter {
            json(
                """
                {"id":"resp_1","object":"response","status":"incomplete","model":"test-model",
                 "incomplete_details":{"reason":"max_output_tokens"},
                 "output":[{"type":"message","id":"msg_1","status":"incomplete","role":"assistant",
                   "content":[{"type":"output_text","text":"{\"corrected_text\":\"I look","annotations":[]}]}],
                 "usage":{"input_tokens":10,"output_tokens":8000}}
                """.trimIndent(),
            )
        }

        val error = assertFailsWith<LlmException> { adapter.complete(request) }.error

        assertEquals(LlmError.Incomplete("max_output_tokens"), error)
    }

    @Test
    fun failedStatusIsTypedError(): TestResult = runTest {
        val adapter = adapter {
            json(
                """
                {"id":"resp_1","status":"failed","model":"test-model","output":[],
                 "error":{"code":"server_error","message":"Something went wrong."}}
                """.trimIndent(),
            )
        }

        val error = assertFailsWith<LlmException> { adapter.complete(request) }.error

        assertEquals(LlmError.GenerationFailed("server_error", "Something went wrong."), error)
    }

    @Test
    fun retriesOnceWhenOutputIsInvalidJson(): TestResult = runTest {
        var calls = 0
        val client = StructuredLlmClient(
            adapter {
                calls++
                json(successResponse(if (calls == 1) "{\"corrected_text\": \"I look" else validOutput))
            },
        )

        val result = client.generate(request, ProofreadResult.serializer())

        assertEquals(2, requests.size)
        assertEquals("I look forward to seeing you.", result.output.correctedText)
        assertEquals(TokenUsage(inputTokens = 812 * 2, outputTokens = 154 * 2), result.usage)
    }

    @Test
    fun givesUpAfterOneRetry(): TestResult = runTest {
        val client = StructuredLlmClient(adapter { json(successResponse("""{"corrected_text":"x"}""")) })

        val error = assertFailsWith<LlmException> { client.generate(request, ProofreadResult.serializer()) }.error

        assertIs<LlmError.InvalidOutput>(error)
        assertEquals(2, requests.size)
    }

    @Test
    fun rejectsUnknownCategoryAsInvalidOutput(): TestResult = runTest {
        val output = validOutput.replace("\"verb_form\"", "\"made_up\"")
        val client = StructuredLlmClient(adapter { json(successResponse(output)) })

        val error = assertFailsWith<LlmException> { client.generate(request, ProofreadResult.serializer()) }.error

        assertIs<LlmError.InvalidOutput>(error)
        assertEquals(2, requests.size)
    }

    @Test
    fun doesNotRetryHttpErrors(): TestResult = runTest {
        val client = StructuredLlmClient(adapter { json(errorBody("boom"), HttpStatusCode.InternalServerError) })

        assertFailsWith<LlmException> { client.generate(request, ProofreadResult.serializer()) }

        assertEquals(1, requests.size)
    }

    @Test
    fun mapsUnauthorized(): TestResult = runTest {
        val adapter = adapter { json(errorBody("Incorrect API key provided."), HttpStatusCode.Unauthorized) }

        val error = assertFailsWith<LlmException> { adapter.complete(request) }.error

        assertEquals(LlmError.Unauthorized("Incorrect API key provided."), error)
    }

    @Test
    fun mapsRateLimitWithRetryAfter(): TestResult = runTest {
        val adapter = adapter {
            respond(
                errorBody("Rate limit reached."),
                HttpStatusCode.TooManyRequests,
                headersOf(
                    HttpHeaders.ContentType to listOf("application/json"),
                    HttpHeaders.RetryAfter to listOf("20"),
                ),
            )
        }

        val error = assertFailsWith<LlmException> { adapter.complete(request) }.error

        assertEquals(LlmError.RateLimited(retryAfterSeconds = 20, message = "Rate limit reached."), error)
    }

    @Test
    fun mapsServerError(): TestResult = runTest {
        val adapter = adapter { respond("<html>Bad gateway</html>", HttpStatusCode.BadGateway) }

        val error = assertFailsWith<LlmException> { adapter.complete(request) }.error

        assertEquals(LlmError.ServerError(502, "<html>Bad gateway</html>"), error)
    }

    @Test
    fun mapsOtherHttpError(): TestResult = runTest {
        val adapter = adapter { json(errorBody("Invalid schema."), HttpStatusCode.BadRequest) }

        val error = assertFailsWith<LlmException> { adapter.complete(request) }.error

        assertEquals(LlmError.HttpError(400, "Invalid schema."), error)
    }

    @Test
    fun mapsTimeout(): TestResult = runTest {
        val engine = MockEngine {
            awaitCancellation()
        }
        val client = HttpClient(engine) {
            install(HttpTimeout) { requestTimeoutMillis = 100 }
        }
        val adapter = OpenAiResponsesAdapter(client, LlmConnectionSource { LlmConnection(config, "sk-test") })

        val error = assertFailsWith<LlmException> { adapter.complete(request) }.error

        assertEquals(LlmError.Timeout, error)
    }

    @Test
    fun missingKeyFailsWithoutCallingTheApi(): TestResult = runTest {
        val adapter = adapter(apiKey = null) { json(successResponse(validOutput)) }

        val error = assertFailsWith<LlmException> { adapter.complete(request) }.error

        assertEquals(LlmError.MissingApiKey, error)
        assertTrue(requests.isEmpty())
    }

    private fun successResponse(outputText: String): String {
        val escaped = JsonPrimitive(outputText).toString()
        return """
            {"id":"resp_123","object":"response","created_at":1760000000,"status":"completed",
             "error":null,"incomplete_details":null,"model":"test-model-2026-08-01",
             "output":[
               {"type":"reasoning","id":"rs_1","summary":[]},
               {"type":"message","id":"msg_1","status":"completed","role":"assistant",
                "content":[{"type":"output_text","text":$escaped,"annotations":[]}]}
             ],
             "store":false,
             "usage":{"input_tokens":812,"input_tokens_details":{"cached_tokens":0},
                      "output_tokens":154,"output_tokens_details":{"reasoning_tokens":64},
                      "total_tokens":966}}
        """.trimIndent()
    }

    private fun errorBody(message: String): String =
        """{"error":{"message":"$message","type":"invalid_request_error","param":null,"code":null}}"""
}
