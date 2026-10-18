package com.vncnt.music.core.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNumber
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.nowPlayingInfo

/**
 * Lock-screen / Control Center metadata (title, artist, position, rate).
 * Remote transport commands (play/pause) are bound in PlayerState.swift:
 * the documented Swift addTarget(handler:) API is authoritative there, while
 * K/N's MediaPlayer package does not expose MPRemoteCommandTarget at all.
 */
@OptIn(ExperimentalForeignApi::class)
class NowPlayingPresenter {

    fun update(
        title: String,
        artist: String,
        durationMs: Long,
        positionMs: Long,
        isPlaying: Boolean
    ) {
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = mapOf<Any?, Any?>(
            MPMediaItemPropertyTitle to title,
            MPMediaItemPropertyArtist to artist,
            MPMediaItemPropertyPlaybackDuration to NSNumber(double = durationMs / 1000.0),
            MPNowPlayingInfoPropertyElapsedPlaybackTime to NSNumber(double = positionMs / 1000.0),
            MPNowPlayingInfoPropertyPlaybackRate to NSNumber(double = if (isPlaying) 1.0 else 0.0)
        )
    }

    fun clear() {
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = mapOf<Any?, Any?>()
    }
}
