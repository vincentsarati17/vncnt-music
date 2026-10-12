package com.vncnt.music.core.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun platformHttpClient(): HttpClient = HttpClient(Darwin) {
    applyDefaults()
}
