package com.vncnt.music

import android.app.Application
import android.os.Build
import com.vncnt.music.playback.MusicNotificationManager

/**
 * Production Application entry point.
 *
 * - Creates the media notification channel eagerly so the foreground
 *   playback service can post without a race.
 * - Central place for future init (Crashlytics, Coil, AppCheck).
 */
class VncntMusicApp : Application() {

    override fun onCreate() {
        super.onCreate()
        MusicNotificationManager.createChannel(this)
        // Reduce StrictMode noise in release; devs can enable locally.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // No-op hook kept for future crash-reporting SDK init.
        }
    }
}
