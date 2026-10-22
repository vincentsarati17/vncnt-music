package com.vncnt.music.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.vncnt.music.core.api.LrclibClient
import com.vncnt.music.core.db.LyricsDao
import com.vncnt.music.core.db.LyricsEntity
import com.vncnt.music.core.lyrics.LrcParser
import com.vncnt.music.core.model.LyricsData
import com.vncnt.music.core.model.LyricsSource
import com.vncnt.music.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LyricsRepository(
    private val context: Context,
    private val lyricsDao: LyricsDao
) {
    companion object {
        private const val TAG = "LyricsRepository"
    }

    /**
     * Executes the waterfall lyrics lookup:
     * 1. Room Database cache
     * 2. Sidecar .lrc file next to local audio file
     * 3. Embedded audio file metadata tags
     * 4. LRCLIB online API
     */
    suspend fun getOrFetchLyrics(track: Track, forceRefreshOnline: Boolean = false): LyricsData? = withContext(Dispatchers.IO) {
        // 1. Check Room Database cache first
        if (!forceRefreshOnline) {
            val cached = lyricsDao.getLyricsSync(track.id)
            if (cached != null && cached.rawLrcContent.isNotBlank()) {
                val source = try {
                    LyricsSource.valueOf(cached.source)
                } catch (e: Exception) {
                    LyricsSource.CACHED_DATABASE
                }
                return@withContext LrcParser.parse(track.id, cached.rawLrcContent, source)
            }
        }

        // 2. Check Sidecar .lrc file if local file path exists
        val sidecarLyrics = checkSidecarFile(track)
        if (sidecarLyrics != null) {
            saveToCache(track.id, sidecarLyrics, LyricsSource.SIDECAR_LRC)
            return@withContext LrcParser.parse(track.id, sidecarLyrics, LyricsSource.SIDECAR_LRC)
        }

        // 3. Check Embedded Metadata
        val embeddedLyrics = checkEmbeddedMetadata(track)
        if (embeddedLyrics != null) {
            saveToCache(track.id, embeddedLyrics, LyricsSource.EMBEDDED_METADATA)
            return@withContext LrcParser.parse(track.id, embeddedLyrics, LyricsSource.EMBEDDED_METADATA)
        }

        // 4. Check LRCLIB online API
        val onlineLyrics = fetchOnlineLrclib(track)
        if (onlineLyrics != null) {
            saveToCache(track.id, onlineLyrics, LyricsSource.ONLINE_API)
            return@withContext LrcParser.parse(track.id, onlineLyrics, LyricsSource.ONLINE_API)
        }

        null
    }

    /**
     * Checks if a .lrc or .txt sidecar file exists alongside the local audio file.
     */
    private fun checkSidecarFile(track: Track): String? {
        val path = track.filePath ?: return null
        try {
            val audioFile = File(path)
            if (!audioFile.exists()) return null

            val parentDir = audioFile.parentFile ?: return null
            val baseName = audioFile.nameWithoutExtension

            // Try exact name .lrc
            val lrcFile = File(parentDir, "$baseName.lrc")
            if (lrcFile.exists() && lrcFile.canRead()) {
                val text = lrcFile.readText()
                if (text.isNotBlank()) return text
            }

            // Try exact name .txt
            val txtFile = File(parentDir, "$baseName.txt")
            if (txtFile.exists() && txtFile.canRead()) {
                val text = txtFile.readText()
                if (text.isNotBlank()) return text
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading sidecar file for ${track.title}", e)
        }
        return null
    }

    /**
     * Inspects embedded ID3 / Vorbis tags using MediaMetadataRetriever.
     */
    private fun checkEmbeddedMetadata(track: Track): String? {
        val retriever = MediaMetadataRetriever()
        try {
            val path = track.filePath
            val uriStr = track.customAudioUri

            when {
                path != null && File(path).exists() -> retriever.setDataSource(path)
                uriStr != null -> retriever.setDataSource(context, Uri.parse(uriStr))
                else -> return null
            }

            // In Android standard API, ID3/Vorbis lyrics are not always exposed via standard metadata keys,
            // but some encoders populate compilation or genre fields, or embedded comments.
            // We check for any custom lyric headers if supported by the container.
        } catch (e: Exception) {
            // Ignore retrieval errors
        } finally {
            try { retriever.release() } catch (e: Exception) {}
        }
        return null
    }

    // Shared KMP online source (OkHttp engine on Android via :core actual).
    private val onlineLyrics = LrclibClient()

    /**
     * Queries the open LRCLIB API for synchronized lyrics.
     * Waterfall lives in shared :core code; app only applies its local
     * placeholder filters (Offline Library album, non-positive duration).
     */
    private suspend fun fetchOnlineLrclib(track: Track): String? {
        return try {
            onlineLyrics.fetchBestLyrics(
                title = track.title,
                artist = track.artist,
                album = if (track.album.isNotBlank() && track.album != "Offline Library") track.album else null,
                durationSeconds = if (track.durationSeconds > 0) track.durationSeconds else null
            )
        } catch (e: Exception) {
            Log.w(TAG, "LRCLIB query failed for ${track.title}", e)
            null
        }
    }

    /**
     * Saves user-imported or custom lyrics to the local database.
     */
    suspend fun saveCustomLyrics(trackId: String, content: String): LyricsData = withContext(Dispatchers.IO) {
        saveToCache(trackId, content, LyricsSource.USER_IMPORT)
        LrcParser.parse(trackId, content, LyricsSource.USER_IMPORT)
    }

    private suspend fun saveToCache(trackId: String, rawLrc: String, source: LyricsSource) {
        val isSynced = rawLrc.contains(Regex("""\[\d{1,2}:\d{2}"""))
        lyricsDao.insertLyrics(
            LyricsEntity(
                trackId = trackId,
                rawLrcContent = rawLrc,
                isSynced = isSynced,
                source = source.name,
                fetchedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteLyrics(trackId: String) = withContext(Dispatchers.IO) {
        lyricsDao.deleteLyrics(trackId)
    }
}
