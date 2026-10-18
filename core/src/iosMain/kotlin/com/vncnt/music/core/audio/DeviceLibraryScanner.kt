package com.vncnt.music.core.audio

import com.vncnt.music.core.model.Track
import kotlinx.cinterop.ExperimentalForeignApi
import platform.MediaPlayer.MPMediaItem
import platform.MediaPlayer.MPMediaItemPropertyAlbumTitle
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyAssetURL
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPMediaLibrary
import platform.MediaPlayer.MPMediaQuery

/**
 * Device music library scanner (MPMediaQuery). All MediaPlayer work stays on
 * the Kotlin side; Swift only triggers scan/authorization and consumes the
 * Kotlin-built List<Track> — the two FFI shapes this shell already ships.
 *
 * KVC values arrive as Any?; NSString and NSNumber both render their real
 * content through `description`, so toString() is the whole conversion.
 * Items without an asset URL (cloud-only / DRM) are skipped. Unauthorized,
 * empty, and simulator libraries all yield an empty list — callers keep the
 * demo tracks as fallback.
 */
@OptIn(ExperimentalForeignApi::class)
class DeviceLibraryScanner {

    /** Fires when the authorization prompt finishes, any outcome. */
    var onFinished: (() -> Unit)? = null

    fun scan(): List<Track> {
        val query = MPMediaQuery.songsQuery()
        val items = query.items ?: return emptyList()
        val tracks = mutableListOf<Track>()
        for ((index, element) in items.withIndex()) {
            val mediaItem = element as? MPMediaItem ?: continue
            val uri = mediaItem.valueForProperty(MPMediaItemPropertyAssetURL)?.toString() ?: continue
            tracks += Track(
                id = "device-$index",
                title = kvc(mediaItem, MPMediaItemPropertyTitle) ?: "Untitled",
                artist = kvc(mediaItem, MPMediaItemPropertyArtist) ?: "Unknown Artist",
                album = kvc(mediaItem, MPMediaItemPropertyAlbumTitle) ?: "Unknown Album",
                durationSeconds = (kvc(mediaItem, MPMediaItemPropertyPlaybackDuration)
                    ?.toDoubleOrNull() ?: 0.0).toInt(),
                genre = "Library",
                audioToneType = "DEFAULT",
                filePath = uri,
                isHiRes = false,
                audioQualityBadge = "Library"
            )
        }
        return tracks
    }

    fun requestAuthorization() {
        MPMediaLibrary.requestAuthorization { _ ->
            onFinished?.invoke()
        }
    }

    private fun kvc(item: MPMediaItem, property: String): String? =
        item.valueForProperty(property)?.toString()
}
