package com.phicdy.redraft.shared.llm

/**
 * Failures the UI can show to the user. Adapters map provider-specific failures
 * to these, and the common layer adds [InvalidOutput] after its retry.
 */
sealed interface LlmError {
    /** No API key is stored for the provider. */
    data object MissingApiKey : LlmError

    /** HTTP 401 or 403: the key is wrong, revoked or lacks access. */
    data class Unauthorized(val message: String?) : LlmError

    /** HTTP 429. [retryAfterSeconds] comes from the Retry-After header when present. */
    data class RateLimited(val retryAfterSeconds: Long?, val message: String?) : LlmError

    /** HTTP 5xx. */
    data class ServerError(val status: Int, val message: String?) : LlmError

    /** Any other non-success HTTP status (400, 404, ...). */
    data class HttpError(val status: Int, val message: String?) : LlmError

    /** The request or connection timed out. */
    data object Timeout : LlmError

    /** The server could not be reached. */
    data class Network(val message: String?) : LlmError

    /** The HTTP call succeeded but the body is not a response this adapter understands. */
    data class UnexpectedResponse(val message: String?) : LlmError

    /** The model declined to answer. [message] is the model's refusal text. */
    data class Refused(val message: String) : LlmError

    /** The model stopped early, e.g. because of the output token limit or a content filter. */
    data class Incomplete(val reason: String?) : LlmError

    /** The provider reported that generation failed. */
    data class GenerationFailed(val code: String?, val message: String?) : LlmError

    /** The output did not match the expected structure, even after one retry. */
    data class InvalidOutput(val message: String?) : LlmError
}

/** Carries an [LlmError] through suspend calls that return a plain result. */
class LlmException(val error: LlmError, cause: Throwable? = null) : Exception(error.toString(), cause)
