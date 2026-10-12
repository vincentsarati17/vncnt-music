package com.vncnt.music.core.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Runs on every target via commonTest. Covers the exact→search waterfall.
class LrclibClientTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun client(
        exactStatus: HttpStatusCode = HttpStatusCode.OK,
        exactBody: String = SYNCED_BODY,
        searchBody: String = SEARCH_BODY
    ): LrclibClient {
        val http = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(this@LrclibClientTest.json) }
            engine {
                addHandler { request ->
                    when {
                        request.url.encodedPath.endsWith("/api/get") ->
                            respond(exactBody, exactStatus, headersOf(HttpHeaders.ContentType, "application/json"))
                        request.url.encodedPath.endsWith("/api/search") ->
                            respond(searchBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                        else -> respond("{}", HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, "application/json"))
                    }
                }
            }
        }
        return LrclibClient(http)
    }

    @Test
    fun exactMatchPrefersSyncedLyrics() = runTest {
        val result = client().fetchBestLyrics("Song", "Artist")
        assertEquals("[00:01.00]Hello", result)
    }

    @Test
    fun exactPlainLyricsAcceptedWhenNoSynced() = runTest {
        val result = client(exactBody = PLAIN_BODY).fetchBestLyrics("Song", "Artist")
        assertEquals("Just lyrics", result)
    }

    @Test
    fun fallbackSearchPrefersFirstSyncedCandidate() = runTest {
        val result = client(
            exactStatus = HttpStatusCode.NotFound,
            exactBody = """{"message":"Not found"}"""
        ).fetchBestLyrics("Song", "Artist")
        assertEquals("[00:02.00]Second", result)
    }

    @Test
    fun nullWhenNothingUsable() = runTest {
        val result = client(
            exactStatus = HttpStatusCode.NotFound,
            exactBody = """{"message":"Not found"}""",
            searchBody = "[]"
        ).fetchBestLyrics("Song", "Artist")
        assertNull(result)
    }

    @Test
    fun blankInputShortCircuitsWithoutNetwork() = runTest {
        val result = client().fetchBestLyrics("", "")
        assertNull(result)
    }

    companion object {
        private const val SYNCED_BODY = """{
          "id": 1, "trackName": "Song", "artistName": "Artist",
          "plainLyrics": "Just lyrics", "syncedLyrics": "[00:01.00]Hello"
        }"""
        private const val PLAIN_BODY = """{
          "id": 2, "trackName": "Song", "artistName": "Artist",
          "plainLyrics": "Just lyrics", "syncedLyrics": null
        }"""
        private const val SEARCH_BODY = """[
          {"id": 10, "trackName": "Song", "artistName": "Artist", "plainLyrics": "First"},
          {"id": 11, "trackName": "Song", "artistName": "Artist", "syncedLyrics": "[00:02.00]Second"}
        ]"""
    }
}
