package com.vncnt.music.core.api

import com.vncnt.music.core.http.defaultHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

/**
 * Portable LRCLIB client (https://lrclib.net). Pure Ktor, runs on every target.
 * Mirrors the old Retrofit waterfall exactly: exact match first (synced
 * preferred, plain accepted), then `search` fallback preferring the first
 * synced candidate, else the first plain candidate.
 */
class LrclibClient(
    private val http: HttpClient = defaultHttpClient(),
    private val baseUrl: String = DEFAULT_BASE_URL
) {

    suspend fun exactMatch(
        title: String,
        artist: String,
        album: String? = null,
        durationSeconds: Int? = null
    ): LrclibTrack? {
        if (title.isBlank() || artist.isBlank()) return null
        val response = runCatching {
            http.get("${baseUrl}api/get") {
                header(HttpHeaders.UserAgent, USER_AGENT)
                parameter("track_name", title)
                parameter("artist_name", artist)
                if (!album.isNullOrBlank()) parameter("album_name", album)
                if (durationSeconds != null && durationSeconds > 0) {
                    parameter("duration", durationSeconds)
                }
            }
        }.getOrNull() ?: return null
        if (response.status != HttpStatusCode.OK) return null
        return runCatching { response.body<LrclibTrack>() }.getOrNull()
    }

    suspend fun search(query: String): List<LrclibTrack> {
        if (query.isBlank()) return emptyList()
        val response = runCatching {
            http.get("${baseUrl}api/search") {
                header(HttpHeaders.UserAgent, USER_AGENT)
                parameter("q", query)
            }
        }.getOrNull() ?: return emptyList()
        if (response.status != HttpStatusCode.OK) return emptyList()
        return runCatching { response.body<List<LrclibTrack>>() }.getOrNull() ?: emptyList()
    }

    /** Full waterfall: exact match, then search fallback. Null when nothing usable. */
    suspend fun fetchBestLyrics(
        title: String,
        artist: String,
        album: String? = null,
        durationSeconds: Int? = null
    ): String? {
        exactMatch(title, artist, album, durationSeconds)?.let { pick(it) }?.let { return it }

        val query = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" ")
        if (query.isBlank()) return null
        val candidates = search(query)
        if (candidates.isEmpty()) return null
        val best = candidates.firstOrNull { !it.syncedLyrics.isNullOrBlank() }
            ?: candidates.firstOrNull { !it.plainLyrics.isNullOrBlank() }
        return best?.let { pick(it) }
    }

    private fun pick(track: LrclibTrack): String? {
        val lyrics = track.syncedLyrics ?: track.plainLyrics
        return lyrics?.takeIf { it.isNotBlank() }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://lrclib.net/"
        const val USER_AGENT = "VncntMusicApp/1.0 (Hi-Fi Player)"
    }
}
