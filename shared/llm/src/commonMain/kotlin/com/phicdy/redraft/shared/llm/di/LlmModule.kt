package com.phicdy.redraft.shared.llm.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

val llmJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** The HTTP engine is picked from the platform classpath (OkHttp on Android, Java on desktop). */
val llmModule: Module = module {
    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(llmJson)
            }
        }
    }
}
