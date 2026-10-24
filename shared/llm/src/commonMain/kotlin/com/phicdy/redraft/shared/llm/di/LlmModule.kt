package com.phicdy.redraft.shared.llm.di

import com.phicdy.redraft.shared.llm.LlmAdapter
import com.phicdy.redraft.shared.llm.LlmClient
import com.phicdy.redraft.shared.llm.StructuredLlmClient
import com.phicdy.redraft.shared.llm.WritingAssistant
import com.phicdy.redraft.shared.llm.openai.OpenAiResponsesAdapter
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

val llmJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** Structured outputs can take a while on reasoning models, so the request timeout is generous. */
private const val REQUEST_TIMEOUT_MILLIS: Long = 120_000
private const val CONNECT_TIMEOUT_MILLIS: Long = 15_000

/**
 * LLM bindings.
 *
 * `LlmConnectionSource` is not bound here because connection settings and API keys live in
 * the data layer; the app module bridges it.
 *
 * The HTTP engine is picked from the platform classpath (OkHttp on Android, Java on desktop).
 */
val llmModule: Module = module {
    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(llmJson)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            }
        }
    }
    single<LlmAdapter> { OpenAiResponsesAdapter(httpClient = get(), connectionSource = get()) }
    single<LlmClient> { StructuredLlmClient(adapter = get()) }
    single<WritingAssistant> { WritingAssistant(client = get()) }
}
