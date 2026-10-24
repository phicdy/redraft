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

/**
 * Everything an adapter needs to reach a provider: the connection settings and the API key.
 * [apiKey] is null when no key has been saved for [config].
 */
data class LlmConnection(
    val config: ProviderConfig,
    val apiKey: String?,
)

/**
 * Supplies the [LlmConnection] for the next request.
 *
 * Read once per request so that changes made in the settings screen apply without restarting.
 * The app bridges it from the data layer, which this module must not depend on.
 */
fun interface LlmConnectionSource {
    suspend fun current(): LlmConnection
}
