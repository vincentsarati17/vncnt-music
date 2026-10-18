package com.vncnt.music.core.audio

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
import platform.MediaPlayer.addTarget
import platform.MediaPlayer.nowPlayingInfo
import platform.MediaPlayer.pauseCommand
import platform.MediaPlayer.playCommand
import platform.MediaPlayer.togglePlayPauseCommand

/**
 * Lock-screen / Control Center integration. Swift passes primitives and one
 * zero-arg toggle closure — the proven FFI vocabulary; all MediaPlayer
 * work stays here (iosMainowns the system-integration details).
 */
class NowPlayingPresenter {

    var onToggle: (() -> Unit)? = null

    fun bindRemoteCommands() {
        val center = MPRemoteCommandCenter.sharedCommandCenter()
        val handler: (MPRemoteCommandEvent) -> MPRemoteCommandHandlerStatus = {
            onToggle?.invoke()
            MPRemoteCommandHandlerStatus.MPRemoteCommandHandlerStatusSuccess
        }
        center.togglePlayPauseCommand.addTarget(handler)
        center.playCommand.addTarget(handler)
        center.pauseCommand.addTarget(handler)
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
            MPMediaItemPropertyPlaybackDuration to NSNumber(value = durationMs / 1000.0),
            MPNowPlayingInfoPropertyElapsedPlaybackTime to NSNumber(value = positionMs / 1000.0),
            MPNowPlayingInfoPropertyPlaybackRate to NSNumber(value = if (isPlaying) 1.0 else 0.0)
        )
    }

    fun clear() {
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = mapOf<Any?, Any?>()
    }
}
