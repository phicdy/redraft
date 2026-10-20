package com.phicdy.redraft.shared.llm.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Body of `POST {baseUrl}/responses`. Only the fields Redraft uses are modeled. */
@Serializable
internal data class ResponsesRequest(
    @SerialName("model")
    val model: String,
    @SerialName("instructions")
    val instructions: String,
    @SerialName("input")
    val input: String,
    @SerialName("max_output_tokens")
    val maxOutputTokens: Int,
    /** Always false so that OpenAI does not retain the conversation. */
    @SerialName("store")
    val store: Boolean,
    @SerialName("text")
    val text: TextConfig,
)

@Serializable
internal data class TextConfig(
    @SerialName("format")
    val format: JsonSchemaFormat,
)

@Serializable
internal data class JsonSchemaFormat(
    @SerialName("type")
    val type: String,
    @SerialName("name")
    val name: String,
    @SerialName("schema")
    val schema: JsonObject,
    @SerialName("strict")
    val strict: Boolean,
)

/** Response object. Unknown keys and output item types are ignored. */
@Serializable
internal data class ResponsesResponse(
    @SerialName("id")
    val id: String? = null,
    @SerialName("model")
    val model: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("incomplete_details")
    val incompleteDetails: IncompleteDetails? = null,
    @SerialName("error")
    val error: ResponseError? = null,
    @SerialName("output")
    val output: List<OutputItem> = emptyList(),
    @SerialName("usage")
    val usage: Usage? = null,
)

@Serializable
internal data class IncompleteDetails(
    @SerialName("reason")
    val reason: String? = null,
)

@Serializable
internal data class ResponseError(
    @SerialName("code")
    val code: String? = null,
    @SerialName("message")
    val message: String? = null,
)

/**
 * An item of `output`. Messages carry [content]; other item types (e.g. `reasoning`)
 * are decoded with an empty content list and skipped.
 */
@Serializable
internal data class OutputItem(
    @SerialName("type")
    val type: String,
    @SerialName("content")
    val content: List<ContentPart> = emptyList(),
)

/** `output_text` parts carry [text]; `refusal` parts carry [refusal]. */
@Serializable
internal data class ContentPart(
    @SerialName("type")
    val type: String,
    @SerialName("text")
    val text: String? = null,
    @SerialName("refusal")
    val refusal: String? = null,
)

@Serializable
internal data class Usage(
    @SerialName("input_tokens")
    val inputTokens: Int = 0,
    @SerialName("output_tokens")
    val outputTokens: Int = 0,
)

/** Error body of a non-2xx response: `{"error": {"message": ..., "type": ..., "code": ...}}`. */
@Serializable
internal data class ErrorBody(
    @SerialName("error")
    val error: ResponseError? = null,
)
