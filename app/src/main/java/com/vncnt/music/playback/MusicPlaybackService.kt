package com.vncnt.music.playback

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
// androidx.media:media ships these session classes under the legacy
// support-package names (even MediaStyle.setMediaSession expects that Token).
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.vncnt.music.MainActivity

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
 * - A [MediaSessionCompat] is kept active for the lifetime of the service so
 *   the system can render the lock-screen / quick-settings media widget from
 *   the MediaStyle notification, and so widget buttons seek/skip via the
 *   same broadcast path.
 *
 * This gives Play-compliant background playback (FGS + mediaPlayback type)
 * without a risky engine rewrite.
 */
class MusicPlaybackService : Service() {

    private var noisyReceiver: BroadcastReceiver? = null
    private var isForeground = false

    private var session: MediaSessionCompat? = null
    private var lastPlaying = false
    private var lastPositionMs = 0L
    private var lastDurationMs = 0L
    private var lastArtName: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        setupSession()
        registerNoisyReceiver()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        session?.release()
        session = null
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
                val positionMs = intent.getLongExtra(EXTRA_POSITION_MS, 0L)
                val durationMs = intent.getLongExtra(EXTRA_DURATION_MS, 0L)
                val artName = intent.getStringExtra(EXTRA_ART_NAME)
                showForeground(title, artist, playing, positionMs, durationMs, artName)
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

    private fun setupSession() {
        val s = MediaSessionCompat(this, TAG).apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    // togglePlayPause resumes when paused; no-op when playing.
                    if (!lastPlaying) broadcast(ACTION_REQUEST_TOGGLE)
                }

                override fun onPause() {
                    // pauseFromSystem is a guarded no-op when already paused.
                    broadcast(ACTION_REQUEST_TOGGLE_PAUSE)
                }

                override fun onSkipToNext() = broadcast(ACTION_REQUEST_NEXT)

                override fun onSkipToPrevious() = broadcast(ACTION_REQUEST_PREV)

                override fun onStop() = broadcast(ACTION_REQUEST_STOP)

                override fun onSeekTo(positionMs: Long) {
                    sendBroadcast(
                        Intent(ACTION_REQUEST_SEEK)
                            .setPackage(packageName)
                            .putExtra(EXTRA_POSITION_MS, positionMs)
                    )
                }
            })
            val launchIntent = Intent(this@MusicPlaybackService, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            setSessionActivity(
                PendingIntent.getActivity(
                    this@MusicPlaybackService, 0, launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            isActive = true
        }
        session = s
    }

    private fun showForeground(
        title: String,
        artist: String,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
        artName: String?
    ) {
        lastPlaying = isPlaying
        lastPositionMs = positionMs
        lastDurationMs = durationMs
        lastArtName = artName
        publishSessionState(title, artist)

        val notification = MusicNotificationManager.build(
            this, title, artist, isPlaying, session?.sessionToken
        )
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

    /** Push metadata + playback state so the lock-screen widget stays current. */
    private fun publishSessionState(title: String, artist: String) {
        val s = session ?: return
        val meta = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, lastDurationMs)
        decodeArtwork(lastArtName)?.let { meta.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it) }
        s.setMetadata(meta.build())
        s.setPlaybackState(playbackState())
    }

    private fun playbackState(): PlaybackStateCompat =
        PlaybackStateCompat.Builder()
            .setActions(ACTIONS)
            .setState(
                if (lastPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                lastPositionMs,
                if (lastPlaying) 1f else 0f
            )
            .build()

    /** Called by the ViewModel on every position tick (cheap, no FGS work). */
    fun updatePosition(positionMs: Long) {
        lastPositionMs = positionMs
        session?.setPlaybackState(playbackState())
    }

    private fun decodeArtwork(name: String?): Bitmap? {
        if (name.isNullOrBlank()) return null
        return try {
            val resId = resources.getIdentifier(name, "drawable", packageName)
            if (resId != 0) BitmapFactory.decodeResource(resources, resId) else null
        } catch (e: Exception) {
            Log.w(TAG, "decode artwork failed", e)
            null
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
        const val ACTION_REQUEST_SEEK = "com.vncnt.music.playback.REQUEST_SEEK"

        const val EXTRA_POSITION_MS = "extra_position_ms"
        private const val EXTRA_DURATION_MS = "extra_duration_ms"
        private const val EXTRA_ART_NAME = "extra_art_name"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_ARTIST = "extra_artist"
        private const val EXTRA_PLAYING = "extra_playing"

        private val ACTIONS = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_SEEK_TO or
            PlaybackStateCompat.ACTION_STOP

        // Same process: direct pointer so position ticks never re-enter the
        // foreground service start path.
        @Volatile
        private var instance: MusicPlaybackService? = null

        fun publishPosition(positionMs: Long) {
            instance?.updatePosition(positionMs)
        }

        private fun command(
            context: Context,
            action: String,
            title: String = "",
            artist: String = "",
            playing: Boolean = true,
            positionMs: Long = 0L,
            durationMs: Long = 0L,
            artName: String? = null
        ) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                this.action = action
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                putExtra(EXTRA_PLAYING, playing)
                putExtra(EXTRA_POSITION_MS, positionMs)
                putExtra(EXTRA_DURATION_MS, durationMs)
                putExtra(EXTRA_ART_NAME, artName)
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
        fun start(
            context: Context,
            title: String,
            artist: String,
            isPlaying: Boolean = true,
            positionMs: Long = 0L,
            durationMs: Long = 0L,
            artName: String? = null
        ) = command(context, ACTION_START, title, artist, isPlaying, positionMs, durationMs, artName)

        /** Call on play/pause/track/metadata changes. */
        fun update(
            context: Context,
            title: String,
            artist: String,
            isPlaying: Boolean,
            positionMs: Long = 0L,
            durationMs: Long = 0L,
            artName: String? = null
        ) = command(context, ACTION_UPDATE, title, artist, isPlaying, positionMs, durationMs, artName)

        /** Call when playback is fully stopped (queue cleared / app exit). */
        fun stop(context: Context) = command(context, ACTION_STOP)
    }
}
