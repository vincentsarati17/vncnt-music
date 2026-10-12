package com.vncnt.music.core.queue

import com.vncnt.music.core.model.Track

/**
 * Pure queue-navigation math, extracted from MusicViewModel.nextTrack /
 * previousTrack and MainActivity.rememberNextTrack. No coroutines, no engine:
 * callers resolve the returned index/track and perform playback themselves.
 */
object QueueNavigator {

    /** Above this playback position, "previous" restarts the track instead. */
    const val RESTART_THRESHOLD_SECONDS = 3

    fun nextIndex(
        tracks: List<Track>,
        currentId: String?,
        isShuffle: Boolean,
        randomIndex: () -> Int = { tracks.indices.random() }
    ): Int? {
        if (tracks.isEmpty()) return null
        if (isShuffle) return randomIndex().coerceIn(tracks.indices)
        val idx = tracks.indexOfFirst { it.id == currentId }
        return if (idx == -1 || idx == tracks.lastIndex) 0 else idx + 1
    }

    fun nextTrack(
        tracks: List<Track>,
        currentId: String?,
        isShuffle: Boolean,
        randomIndex: () -> Int = { tracks.indices.random() }
    ): Track? = nextIndex(tracks, currentId, isShuffle, randomIndex)?.let(tracks::get)

    fun previousIndex(
        tracks: List<Track>,
        currentId: String?,
        isShuffle: Boolean,
        randomIndex: () -> Int = { tracks.indices.random() }
    ): Int? {
        if (tracks.isEmpty()) return null
        if (isShuffle) return randomIndex().coerceIn(tracks.indices)
        val idx = tracks.indexOfFirst { it.id == currentId }
        return if (idx <= 0) tracks.lastIndex else idx - 1
    }

    fun previousTrack(
        tracks: List<Track>,
        currentId: String?,
        isShuffle: Boolean,
        randomIndex: () -> Int = { tracks.indices.random() }
    ): Track? = previousIndex(tracks, currentId, isShuffle, randomIndex)?.let(tracks::get)

    /** Matches ViewModel/Up-Next preview: wraps to first track at end of queue. */
    fun peekNext(tracks: List<Track>, currentId: String?): Track? {
        if (tracks.isEmpty()) return null
        val idx = tracks.indexOfFirst { it.id == currentId }
        return if (idx == -1 || idx == tracks.lastIndex) tracks.first() else tracks[idx + 1]
    }

    fun shouldRestartCurrent(positionSeconds: Int): Boolean =
        positionSeconds > RESTART_THRESHOLD_SECONDS
}
