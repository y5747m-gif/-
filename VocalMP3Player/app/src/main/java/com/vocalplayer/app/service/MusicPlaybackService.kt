package com.vocalplayer.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Background playback service.
 *
 * The [Player] is owned by PlayerViewModel/AudioPlayerManager and is handed to
 * this service before it is started, so the service can build the [MediaSession]
 * itself. The service must never release a player it does not own — releasing it
 * here previously killed playback and caused a double release when the ViewModel
 * was cleared.
 */
class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    companion object {
        const val CHANNEL_ID = "vocal_player_channel"
        const val NOTIFICATION_ID = 1001

        /**
         * Player shared by the ViewModel. Set before calling startService().
         * The ViewModel keeps ownership and is responsible for releasing it.
         */
        @Volatile
        var sharedPlayer: Player? = null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ensureMediaSession()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return ensureMediaSession()
    }

    private fun ensureMediaSession(): MediaSession? {
        if (mediaSession == null) {
            sharedPlayer?.let { player ->
                mediaSession = MediaSession.Builder(this, player).build()
            }
        }
        return mediaSession
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Music Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows current playing track"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        // Only release the session; the player is owned by the ViewModel.
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
