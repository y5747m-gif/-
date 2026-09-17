package com.vocalplayer.app.audio

import android.content.Context
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.vocalplayer.app.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AudioPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private val vocalProcessor = VocalIsolationProcessor()
    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var progressJob: Job? = null

    fun initialize() {
        exoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true // handleAudioFocus
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                addListener(playerListener)
            }

        // Initialize audio effects
        try {
            exoPlayer?.audioSessionId?.let { sessionId ->
                if (sessionId != 0) {
                    initAudioEffects(sessionId)
                }
            }
        } catch (e: Exception) {
            // Audio effects may not be available on all devices
        }

        startProgressUpdates()
    }

    @OptIn(UnstableApi::class)
    private fun initAudioEffects(sessionId: Int) {
        try {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = false
            }
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                enabled = false
            }
        } catch (e: Exception) {
            // Effects not supported
        }
    }

    private fun applyVocalIsolationEffects() {
        val player = exoPlayer ?: return

        if (vocalProcessor.isProcessorEnabled()) {
            // Re-init effects with current audio session
            try {
                equalizer?.release()
                loudnessEnhancer?.release()

                val sessionId = player.audioSessionId
                if (sessionId != 0) {
                    initAudioEffects(sessionId)

                    // Apply equalizer settings for vocal isolation
                    equalizer?.let { eq ->
                        eq.enabled = true
                        val freqRange = IntRange(
                            eq.getCenterFreq(0).toInt() / 1000,
                            eq.getCenterFreq(eq.numberOfBands.toInt() - 1).toInt() / 1000
                        )
                        val settings = vocalProcessor.getEqualizerSettings(
                            eq.numberOfBands.toInt(), freqRange
                        )
                        settings.forEach { (band, level) ->
                            try {
                                eq.setBandLevel(band.toShort(), level)
                            } catch (_: Exception) {}
                        }
                    }

                    loudnessEnhancer?.let { le ->
                        le.enabled = true
                        le.setTargetGain(
                            (VocalIsolationProcessor.VOCAL_BOOST_DB *
                                    vocalProcessor.getLevel() * 100).toInt()
                        )
                    }
                }
            } catch (e: Exception) {
                // Fall back gracefully
            }
        } else {
            equalizer?.enabled = false
            loudnessEnhancer?.enabled = false
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    _playerState.update {
                        it.copy(duration = exoPlayer?.duration ?: 0L)
                    }
                }
                Player.STATE_ENDED -> {
                    handleTrackEnded()
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val state = _playerState.value
            val newIndex = exoPlayer?.currentMediaItemIndex ?: -1
            if (newIndex >= 0 && newIndex < state.playlist.size) {
                _playerState.update {
                    it.copy(
                        currentTrack = state.playlist[newIndex],
                        currentIndex = newIndex
                    )
                }
            }
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            if (audioSessionId != 0) {
                initAudioEffects(audioSessionId)
                if (vocalProcessor.isProcessorEnabled()) {
                    applyVocalIsolationEffects()
                }
            }
        }
    }

    private fun handleTrackEnded() {
        val state = _playerState.value
        when (state.repeatMode) {
            RepeatMode.ONE -> {
                exoPlayer?.seekTo(0)
                exoPlayer?.play()
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                if (state.currentIndex < state.playlist.size - 1) {
                    playNext()
                } else {
                    _playerState.update { it.copy(isPlaying = false) }
                }
            }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(100)
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        _playerState.update {
                            it.copy(currentPosition = player.currentPosition)
                        }
                    }
                }
            }
        }
    }

    fun loadPlaylist(tracks: List<AudioTrack>, startIndex: Int = 0) {
        val player = exoPlayer ?: return

        _playerState.update { it.copy(playlist = tracks) }

        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setUri(track.uri)
                .setMediaId(track.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .build()
                )
                .build()
        }

        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()

        if (startIndex in tracks.indices) {
            _playerState.update {
                it.copy(
                    currentTrack = tracks[startIndex],
                    currentIndex = startIndex
                )
            }
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun playPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) player.pause() else player.play()
        }
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
        _playerState.update { it.copy(currentPosition = position) }
    }

    fun playNext() {
        val state = _playerState.value
        if (state.playlist.isEmpty()) return

        val nextIndex = if (state.isShuffleEnabled) {
            (0 until state.playlist.size).random()
        } else {
            (state.currentIndex + 1) % state.playlist.size
        }

        exoPlayer?.seekTo(nextIndex, 0L)
        exoPlayer?.play()
    }

    fun playPrevious() {
        val state = _playerState.value
        if (state.playlist.isEmpty()) return

        // If more than 3 seconds in, restart current track
        if (exoPlayer?.currentPosition ?: 0 > 3000) {
            exoPlayer?.seekTo(0)
            return
        }

        val prevIndex = if (state.currentIndex > 0) state.currentIndex - 1
        else state.playlist.size - 1

        exoPlayer?.seekTo(prevIndex, 0L)
        exoPlayer?.play()
    }

    fun toggleShuffle() {
        _playerState.update { it.copy(isShuffleEnabled = !it.isShuffleEnabled) }
    }

    fun toggleRepeat() {
        _playerState.update {
            it.copy(
                repeatMode = when (it.repeatMode) {
                    RepeatMode.OFF -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.OFF
                }
            )
        }
    }

    fun setVocalIsolation(enabled: Boolean) {
        vocalProcessor.setEnabled(enabled)
        _playerState.update { it.copy(isVocalIsolationEnabled = enabled) }
        applyVocalIsolationEffects()
    }

    fun toggleVocalIsolation() {
        val newState = !vocalProcessor.isProcessorEnabled()
        setVocalIsolation(newState)
    }

    fun setVocalIsolationLevel(level: Float) {
        vocalProcessor.setLevel(level)
        _playerState.update { it.copy(vocalIsolationLevel = level) }
        if (vocalProcessor.isProcessorEnabled()) {
            applyVocalIsolationEffects()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
        _playerState.update { it.copy(playbackSpeed = speed) }
    }

    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
        _playerState.update { it.copy(volume = volume) }
    }

    fun release() {
        progressJob?.cancel()
        equalizer?.release()
        loudnessEnhancer?.release()
        exoPlayer?.release()
        exoPlayer = null
        scope.cancel()
    }

    fun getPlayer(): ExoPlayer? = exoPlayer
}
