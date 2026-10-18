package com.vncnt.music.core.audio

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNumber
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandEvent
import platform.MediaPlayer.MPRemoteCommandHandlerStatus
import platform.MediaPlayer.MPRemoteCommandTarget
import platform.MediaPlayer.nowPlayingInfo
import kotlinx.cinterop.CValue

/**
 * Lock-screen / Control Center integration. Swift passes primitives and one
 * zero-arg toggle closure — the proven FFI vocabulary; all MediaPlayer
 * work stays here. Remote commands arrive via the MPRemoteCommandTarget
 * protocol (all three transport commands funnel through onToggle).
 */
@OptIn(ExperimentalForeignApi::class)
class NowPlayingPresenter : MPRemoteCommandTarget {

    var onToggle: (() -> Unit)? = null

    fun bindRemoteCommands() {
        val center = MPRemoteCommandCenter.sharedCommandCenter()
        center.togglePlayPauseCommand.addTarget(this)
        center.playCommand.addTarget(this)
        center.pauseCommand.addTarget(this)
    }

    override fun remoteCommandReceived(event: MPRemoteCommandEvent): MPRemoteCommandHandlerStatus {
        onToggle?.invoke()
        return MPRemoteCommandHandlerStatus.Success
    }

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
