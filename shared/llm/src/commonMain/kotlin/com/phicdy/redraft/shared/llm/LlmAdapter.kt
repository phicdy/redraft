package com.phicdy.redraft.shared.llm

/**
 * One provider's wire format.
 *
 * An adapter only converts [LlmRequest] to the provider's request and the provider's
 * response back to the raw output text. Parsing, validation and retries happen in
 * [StructuredLlmClient] so that every provider behaves the same.
 *
 * Implementations throw [LlmException] for every failure they can classify.
 */
interface LlmAdapter {
    /** Returns the model's output text, which should be JSON matching [LlmRequest.schema]. */
    suspend fun complete(request: LlmRequest): LlmResult<String>
}

/** Supplies the API key of a provider config. Backed by `ApiKeyStore` in the app. */
fun interface ApiKeyProvider {
    suspend fun apiKey(providerId: String): String?
}
