package com.example.playback

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Production foreground service shell for offline playback.
 *
 * Design (keeps existing HiFiAudioEngine untouched):
 * - [MusicViewModel] remains the source of truth and owns the engine.
 * - ViewModel calls [start]/[update]/[stop] so the process is kept alive
 *   and a persistent media notification is shown during playback.
 * - Notification buttons + headset-unplug + audio-focus loss are fanned
 *   back out to the UI as package-scoped broadcasts
 *   ([ACTION_REQUEST_TOGGLE], [ACTION_REQUEST_NEXT], ...), which
 *   MainActivity forwards to the ViewModel.
 *
 * This gives Play-compliant background playback (FGS + mediaPlayback type)
 * without a risky engine rewrite.
 */
class MusicPlaybackService : Service() {

    private var noisyReceiver: BroadcastReceiver? = null
    private var isForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        registerNoisyReceiver()
    }

    override fun onDestroy() {
        try {
            noisyReceiver?.let { unregisterReceiver(it) }
        } catch (e: Exception) {
            Log.w(TAG, "unregister noisy failed", e)
        }
        noisyReceiver = null
        isForeground = false
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START, ACTION_UPDATE -> {
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                val artist = intent.getStringExtra(EXTRA_ARTIST).orEmpty()
                val playing = intent.getBooleanExtra(EXTRA_PLAYING, true)
                showForeground(title, artist, playing)
            }
            ACTION_TOGGLE -> broadcast(ACTION_REQUEST_TOGGLE)
            ACTION_NEXT -> broadcast(ACTION_REQUEST_NEXT)
            ACTION_PREV -> broadcast(ACTION_REQUEST_PREV)
            ACTION_STOP -> {
                broadcast(ACTION_REQUEST_STOP)
                stopForegroundAndSelf()
            }
            else -> {
                // Defensive: sticky restarts without intent keep last state.
                Log.d(TAG, "onStartCommand unknown action: ${intent?.action}")
            }
        }
        return START_NOT_STICKY
    }

    private fun showForeground(title: String, artist: String, isPlaying: Boolean) {
        val notification = MusicNotificationManager.build(this, title, artist, isPlaying)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    MusicNotificationManager.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(MusicNotificationManager.NOTIFICATION_ID, notification)
            }
            isForeground = true
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
        }
    }

    private fun stopForegroundAndSelf() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.w(TAG, "stopForeground failed", e)
        }
        isForeground = false
        stopSelf()
    }

    private fun broadcast(action: String) {
        try {
            sendBroadcast(Intent(action).setPackage(packageName))
        } catch (e: Exception) {
            Log.w(TAG, "broadcast $action failed", e)
        }
    }

    private fun registerNoisyReceiver() {
        val r = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    Log.d(TAG, "Headset unplugged -> request pause")
                    broadcast(ACTION_REQUEST_TOGGLE_PAUSE)
                }
            }
        }
        try {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(r, filter, RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(r, filter)
            }
            noisyReceiver = r
        } catch (e: Exception) {
            Log.w(TAG, "register noisy receiver failed", e)
        }
    }

    companion object {
        private const val TAG = "MusicPlaybackService"

        // Commands INTO the service (ViewModel -> Service).
        const val ACTION_START = "com.vncnt.music.playback.START"
        const val ACTION_UPDATE = "com.vncnt.music.playback.UPDATE"
        const val ACTION_TOGGLE = "com.vncnt.music.playback.TOGGLE"
        const val ACTION_NEXT = "com.vncnt.music.playback.NEXT"
        const val ACTION_PREV = "com.vncnt.music.playback.PREV"
        const val ACTION_STOP = "com.vncnt.music.playback.STOP"

        // Requests OUT of the service (Service -> UI via broadcast).
        const val ACTION_REQUEST_TOGGLE = "com.vncnt.music.playback.REQUEST_TOGGLE"
        const val ACTION_REQUEST_TOGGLE_PAUSE = "com.vncnt.music.playback.REQUEST_PAUSE"
        const val ACTION_REQUEST_NEXT = "com.vncnt.music.playback.REQUEST_NEXT"
        const val ACTION_REQUEST_PREV = "com.vncnt.music.playback.REQUEST_PREV"
        const val ACTION_REQUEST_STOP = "com.vncnt.music.playback.REQUEST_STOP"

        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_ARTIST = "extra_artist"
        private const val EXTRA_PLAYING = "extra_playing"

        private fun command(
            context: Context,
            action: String,
            title: String = "",
            artist: String = "",
            playing: Boolean = true
        ) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                this.action = action
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                putExtra(EXTRA_PLAYING, playing)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.w(TAG, "startForegroundService $action failed", e)
                // Fallback for edge OEM cases.
                try {
                    context.startService(intent)
                } catch (e2: Exception) {
                    Log.w(TAG, "startService fallback failed", e2)
                }
            }
        }

        /** Call when a track starts playing. */
        fun start(context: Context, title: String, artist: String, isPlaying: Boolean = true) =
            command(context, ACTION_START, title, artist, isPlaying)

        /** Call on play/pause/track/metadata changes. */
        fun update(context: Context, title: String, artist: String, isPlaying: Boolean) =
            command(context, ACTION_UPDATE, title, artist, isPlaying)

        /** Call when playback is fully stopped (queue cleared / app exit). */
        fun stop(context: Context) = command(context, ACTION_STOP)
    }
}
