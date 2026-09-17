package com.vocalplayer.app.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.vocalplayer.app.audio.AudioPlayerManager

/** Owns playback so audio and media controls survive Activity recreation. */
class MusicPlaybackService : MediaSessionService() {

    private lateinit var playerManager: AudioPlayerManager
    private var mediaSession: MediaSession? = null

    inner class LocalBinder : Binder() {
        fun getPlayerManager(): AudioPlayerManager = playerManager
    }

    private val localBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        playerManager = AudioPlayerManager(applicationContext).also { it.initialize() }
        val player = checkNotNull(playerManager.getPlayer())
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onBind(intent: Intent?): IBinder? =
        if (intent?.action == ACTION_BIND_LOCAL) localBinder else super.onBind(intent)

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        playerManager.release()
        super.onDestroy()
    }

    companion object {
        const val ACTION_BIND_LOCAL = "com.vocalplayer.app.action.BIND_PLAYBACK_SERVICE"
    }
}
