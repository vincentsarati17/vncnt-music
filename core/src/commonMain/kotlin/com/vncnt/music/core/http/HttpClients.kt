package com.vncnt.music.core.http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Shared JSON policy: LRCLIB adds fields over time; never fail on unknown keys. */
internal val sharedJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/** 8s budgets match the old OkHttp client. Applied by every platform actual. */
fun HttpClientConfig<out HttpClientEngineConfig>.applyDefaults() {
    install(ContentNegotiation) { json(sharedJson) }
    install(HttpTimeout) {
        requestTimeoutMillis = 8_000
        connectTimeoutMillis = 8_000
    }
}

/** Platform engine (OkHttp on Android/JVM, Darwin on iOS). */
expect fun platformHttpClient(): HttpClient

/** Default client for shared consumers such as [com.vncnt.music.core.api.LrclibClient]. */
fun defaultHttpClient(): HttpClient = platformHttpClient()
