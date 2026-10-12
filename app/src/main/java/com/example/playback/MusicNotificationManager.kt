package com.example.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.playback.MusicPlaybackService.Companion.ACTION_NEXT
import com.example.playback.MusicPlaybackService.Companion.ACTION_PREV
import com.example.playback.MusicPlaybackService.Companion.ACTION_STOP
import com.example.playback.MusicPlaybackService.Companion.ACTION_TOGGLE

/**
 * Single place that owns the media notification channel + builder.
 *
 * Uses legacy androidx.media MediaStyle (widely supported, no Media3
 * migration required) so lock-screen / Android Auto style controls keep
 * working while we keep the custom HiFiAudioEngine.
 */
object MusicNotificationManager {

    const val CHANNEL_ID = "vncnt_music_playback"
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Now playing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Vncnt Music playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            mgr.createNotificationChannel(channel)
        }
    }

    fun build(
        context: Context,
        title: String,
        artist: String,
        isPlaying: Boolean
    ): Notification {
        createChannel(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun actionPi(action: String, code: Int): PendingIntent {
            val i = Intent(context, MusicPlaybackService::class.java).setAction(action)
            return PendingIntent.getService(
                context, code, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val toggleIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val toggleTitle = if (isPlaying) "Pause" else "Play"

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title.ifBlank { "Vncnt Music" })
            .setContentText(artist.ifBlank { "Offline playback" })
            .setContentIntent(contentPi)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "Previous", actionPi(ACTION_PREV, 11))
            .addAction(toggleIcon, toggleTitle, actionPi(ACTION_TOGGLE, 12))
            .addAction(android.R.drawable.ic_media_next, "Next", actionPi(ACTION_NEXT, 13))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", actionPi(ACTION_STOP, 14))
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }
}
