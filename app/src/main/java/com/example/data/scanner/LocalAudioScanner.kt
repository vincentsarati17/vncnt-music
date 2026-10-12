package com.example.data.scanner

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScanResult(
    val tracksFound: List<Track>,
    val totalFound: Int,
    val flacOrLosslessCount: Int,
    val isSampleFallback: Boolean = false
)

class LocalAudioScanner(private val context: Context) {

    suspend fun scanDeviceAudio(): ScanResult = withContext(Dispatchers.IO) {
        val foundTracks = mutableListOf<Track>()
        var losslessCount = 0

        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idColumn = it.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleColumn = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistColumn = it.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumColumn = it.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val durationColumn = it.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val mimeColumn = it.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val dataColumn = it.getColumnIndex(MediaStore.Audio.Media.DATA)

                var trackNumberCounter = 100
                while (it.moveToNext()) {
                    if (idColumn < 0) continue
                    val id = it.getLong(idColumn)
                    val title = if (titleColumn >= 0) it.getString(titleColumn) ?: "Offline Track" else "Offline Track"
                    val artist = if (artistColumn >= 0) it.getString(artistColumn) ?: "Local Artist" else "Local Artist"
                    val album = if (albumColumn >= 0) it.getString(albumColumn) ?: "Local Storage" else "Local Storage"
                    val durationMs = if (durationColumn >= 0) it.getInt(durationColumn) else 180000
                    val durationSec = (durationMs / 1000).coerceAtLeast(10)
                    val mimeType = if (mimeColumn >= 0) it.getString(mimeColumn) ?: "" else ""
                    val filePath = if (dataColumn >= 0) it.getString(dataColumn) else null

                    val contentUri = ContentUris.withAppendedId(collection, id)

                    val isHiRes = mimeType.contains("flac", ignoreCase = true) ||
                            mimeType.contains("wav", ignoreCase = true) ||
                            mimeType.contains("alac", ignoreCase = true)

                    val badge = when {
                        mimeType.contains("flac", ignoreCase = true) -> {
                            losslessCount++
                            "Local FLAC • 24-bit/96kHz"
                        }
                        mimeType.contains("wav", ignoreCase = true) -> {
                            losslessCount++
                            "Lossless WAV • 16-bit PCM"
                        }
                        mimeType.contains("aac", ignoreCase = true) -> "AAC Master • 320kbps"
                        else -> "MP3 Offline • 320kbps"
                    }

                    foundTracks.add(
                        Track(
                            id = "local_media_$id",
                            title = title,
                            artist = if (artist.equals("<unknown>", ignoreCase = true)) "Local Artist" else artist,
                            album = album,
                            durationSeconds = durationSec,
                            genre = "Local Offline",
                            imageResName = "",
                            audioToneType = "DEFAULT",
                            customAudioUri = contentUri.toString(),
                            filePath = filePath ?: contentUri.toString(),
                            isFavorite = false,
                            isHiRes = isHiRes,
                            audioQualityBadge = badge,
                            trackNumber = trackNumberCounter++
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        ScanResult(
            tracksFound = foundTracks,
            totalFound = foundTracks.size,
            flacOrLosslessCount = losslessCount,
            isSampleFallback = false
        )
    }

    /**
     * Extracts genuine audio metadata from a selected document or content Uri.
     */
    fun extractTrackFromUri(uri: Uri): Track {
        val retriever = android.media.MediaMetadataRetriever()
        var title = "Local Audio File"
        var artist = "Local Artist"
        var album = "Local Storage"
        var durationSec = 180
        var genre = "Offline Audio"
        var mimeType = context.contentResolver.getType(uri) ?: ""

        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                if (it.isNotBlank()) title = it
            }
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                if (it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true)) artist = it
            }
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                if (it.isNotBlank()) album = it
            }
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                durationSec = (it / 1000).toInt().coerceAtLeast(1)
            }
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_GENRE)?.let {
                if (it.isNotBlank()) genre = it
            }
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?.let {
                if (it.isNotBlank()) mimeType = it
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: extract display name from document provider
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && cursor.moveToFirst()) {
                        cursor.getString(nameIdx)?.let { name ->
                            title = name.substringBeforeLast(".")
                        }
                    }
                }
            } catch (ignored: Exception) {}
        } finally {
            try { retriever.release() } catch (ignored: Exception) {}
        }

        val isHiRes = mimeType.contains("flac", ignoreCase = true) ||
                mimeType.contains("wav", ignoreCase = true) ||
                mimeType.contains("alac", ignoreCase = true)

        val badge = when {
            mimeType.contains("flac", ignoreCase = true) -> "Local FLAC • 24-bit/96kHz"
            mimeType.contains("wav", ignoreCase = true) -> "Lossless WAV • 16-bit PCM"
            mimeType.contains("aac", ignoreCase = true) -> "AAC Master • 320kbps"
            else -> "MP3 Offline • 320kbps"
        }

        val id = "imported_${System.currentTimeMillis()}_${(1000..9999).random()}"

        return Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationSeconds = durationSec,
            genre = genre,
            imageResName = "",
            audioToneType = "DEFAULT",
            customAudioUri = uri.toString(),
            filePath = uri.toString(),
            isFavorite = false,
            isHiRes = isHiRes,
            audioQualityBadge = badge,
            trackNumber = 1
        )
    }
}
