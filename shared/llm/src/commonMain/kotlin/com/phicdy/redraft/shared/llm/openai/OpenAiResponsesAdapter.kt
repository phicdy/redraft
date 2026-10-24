package com.phicdy.redraft.shared.llm.openai

import com.phicdy.redraft.shared.llm.LlmAdapter
import com.phicdy.redraft.shared.llm.LlmConnectionSource
import com.phicdy.redraft.shared.llm.LlmError
import com.phicdy.redraft.shared.llm.LlmException
import com.phicdy.redraft.shared.llm.LlmRequest
import com.phicdy.redraft.shared.llm.LlmResult
import com.phicdy.redraft.shared.llm.ProviderConfig
import com.phicdy.redraft.shared.llm.TokenUsage
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kotlinx.serialization.json.Json

/**
 * Adapter for the OpenAI Responses API (`POST {baseUrl}/responses`).
 *
 * The base URL, default model, extra headers and API key come from [connectionSource], read
 * once per request, so the same adapter works against api.openai.com or a compatible proxy and
 * follows settings changes. Requests are sent with `store: false` and a strict JSON schema in
 * `text.format`.
 */
class OpenAiResponsesAdapter(
    private val httpClient: HttpClient,
    private val connectionSource: LlmConnectionSource,
    private val json: Json = openAiJson,
) : LlmAdapter {
    override suspend fun complete(request: LlmRequest): LlmResult<String> {
        val connection = connectionSource.current()
        val config = connection.config
        val apiKey = connection.apiKey
        if (apiKey.isNullOrBlank()) throw LlmException(LlmError.MissingApiKey)

        val model = request.model ?: config.defaultModel
        val body = json.encodeToString(ResponsesRequest.serializer(), request.toResponsesRequest(model))
        val response = send(config, apiKey, body)
        val text = readBody(response)
        if (!response.status.isSuccess()) {
            throw LlmException(httpError(response, text))
        }
        val parsed = try {
            json.decodeFromString(ResponsesResponse.serializer(), text)
        } catch (e: IllegalArgumentException) {
            // SerializationException is an IllegalArgumentException.
            throw LlmException(LlmError.UnexpectedResponse(e.message), e)
        }
        return parsed.toResult(fallbackModel = model)
    }

    private suspend fun send(config: ProviderConfig, apiKey: String, body: String): HttpResponse = guardTransport {
        httpClient.post(endpoint(config)) {
            headers {
                set(HttpHeaders.Authorization, "Bearer $apiKey")
                config.extraHeaders.forEach { (name, value) -> set(name, value) }
            }
            setBody(TextContent(body, ContentType.Application.Json))
        }
    }

    private suspend fun readBody(response: HttpResponse): String = guardTransport { response.bodyAsText() }

    private fun endpoint(config: ProviderConfig): String = config.baseUrl.trimEnd('/') + "/responses"

    private fun httpError(response: HttpResponse, body: String): LlmError {
        val message = errorMessage(body)
        val status = response.status.value
        return when {
            status == 401 || status == 403 -> LlmError.Unauthorized(message)
            status == 429 -> LlmError.RateLimited(
                retryAfterSeconds = response.headers[HttpHeaders.RetryAfter]?.trim()?.toLongOrNull(),
                message = message,
            )
            status == 408 -> LlmError.Timeout
            status in 500..599 -> LlmError.ServerError(status, message)
            else -> LlmError.HttpError(status, message)
        }
    }

    private fun errorMessage(body: String): String? = try {
        json.decodeFromString(ErrorBody.serializer(), body).error?.message
    } catch (e: IllegalArgumentException) {
        body.takeIf { it.isNotBlank() }?.take(MAX_ERROR_MESSAGE_LENGTH)
    }

    private fun LlmRequest.toResponsesRequest(model: String): ResponsesRequest = ResponsesRequest(
        model = model,
        instructions = instructions,
        input = input,
        maxOutputTokens = maxOutputTokens,
        store = false,
        text = TextConfig(
            format = JsonSchemaFormat(
                type = "json_schema",
                name = schema.name,
                schema = schema.schema,
                strict = true,
            ),
        ),
    )

    private companion object {
        const val MAX_ERROR_MESSAGE_LENGTH = 500
    }
}

/** Ignores fields this adapter does not model, so API additions do not break parsing. */
internal val openAiJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

private fun ResponsesResponse.toResult(fallbackModel: String): LlmResult<String> {
    if (error != null || status == "failed") {
        throw LlmException(LlmError.GenerationFailed(error?.code, error?.message))
    }
    if (status == "incomplete") {
        throw LlmException(LlmError.Incomplete(incompleteDetails?.reason))
    }
    if (status != null && status != "completed") {
        throw LlmException(LlmError.UnexpectedResponse("Unexpected status: $status"))
    }
    val parts = output.filter { it.type == "message" }.flatMap { it.content }
    parts.firstOrNull { it.type == "refusal" }?.let { refusal ->
        throw LlmException(LlmError.Refused(refusal.refusal.orEmpty()))
    }
    val text = parts.filter { it.type == "output_text" }.mapNotNull { it.text }.joinToString("")
    if (text.isEmpty()) {
        throw LlmException(LlmError.UnexpectedResponse("The response has no output_text"))
    }
    return LlmResult(
        output = text,
        usage = TokenUsage(
            inputTokens = usage?.inputTokens ?: 0,
            outputTokens = usage?.outputTokens ?: 0,
        ),
        model = model ?: fallbackModel,
    )
}

/** Maps transport failures to [LlmError]. Coroutine cancellation is not an IOException and passes through. */
private inline fun <T> guardTransport(block: () -> T): T = try {
    block()
} catch (e: HttpRequestTimeoutException) {
    throw LlmException(LlmError.Timeout, e)
} catch (e: ConnectTimeoutException) {
    throw LlmException(LlmError.Timeout, e)
} catch (e: SocketTimeoutException) {
    throw LlmException(LlmError.Timeout, e)
} catch (e: IOException) {
    throw LlmException(LlmError.Network(e.message), e)
}
