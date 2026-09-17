package com.vocalplayer.app.audio

import android.content.Context
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.vocalplayer.app.data.AudioTrack
import com.vocalplayer.app.data.PlayerState
import com.vocalplayer.app.data.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
class AudioPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private val vocalProcessor = VocalIsolationProcessor()
    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var effectsSessionId = -1
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var progressJob: Job? = null

    fun initialize() {
        if (exoPlayer != null) return

        exoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply { addListener(playerListener) }

        startProgressUpdates()
    }

    private fun initializeAudioEffects(sessionId: Int) {
        if (sessionId <= 0 || effectsSessionId == sessionId && equalizer != null) return

        releaseAudioEffects()
        try {
            equalizer = Equalizer(0, sessionId)
            loudnessEnhancer = LoudnessEnhancer(sessionId)
            effectsSessionId = sessionId
            applyVocalIsolationEffects()
        } catch (_: RuntimeException) {
            releaseAudioEffects()
        }
    }

    private fun applyVocalIsolationEffects() {
        val player = exoPlayer ?: return
        val enabled = vocalProcessor.isProcessorEnabled()

        if (!enabled) {
            runCatching { equalizer?.enabled = false }
            runCatching { loudnessEnhancer?.enabled = false }
            return
        }

        val sessionId = player.audioSessionId
        if (sessionId <= 0) return
        if (effectsSessionId != sessionId || equalizer == null) {
            initializeAudioEffects(sessionId)
            if (effectsSessionId != sessionId) return
        }

        equalizer?.let { effect ->
            runCatching {
                val supportedRange = effect.bandLevelRange
                val minimum = supportedRange.firstOrNull()?.toInt() ?: Short.MIN_VALUE.toInt()
                val maximum = supportedRange.lastOrNull()?.toInt() ?: Short.MAX_VALUE.toInt()

                repeat(effect.numberOfBands.toInt()) { index ->
                    val band = index.toShort()
                    val frequencyHz = effect.getCenterFreq(band) / 1000
                    val level = vocalProcessor.getBandLevel(frequencyHz)
                        .toInt()
                        .coerceIn(minimum, maximum)
                        .toShort()
                    effect.setBandLevel(band, level)
                }
                effect.enabled = true
            }
        }

        loudnessEnhancer?.let { effect ->
            runCatching {
                effect.setTargetGain(
                    (VocalIsolationProcessor.VOCAL_BOOST_DB *
                        vocalProcessor.getLevel() * 100).toInt()
                )
                effect.enabled = true
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = exoPlayer ?: return
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _playerState.update { it.copy(isBuffering = true) }
                }

                Player.STATE_READY -> {
                    val fallbackDuration = _playerState.value.currentTrack?.duration ?: 0L
                    val duration = player.duration.takeIf { it > 0L } ?: fallbackDuration
                    _playerState.update {
                        it.copy(
                            isBuffering = false,
                            duration = duration,
                            currentPosition = player.currentPosition.coerceAtLeast(0L),
                            errorMessage = null
                        )
                    }
                }

                Player.STATE_ENDED -> {
                    _playerState.update {
                        it.copy(
                            isPlaying = false,
                            isBuffering = false,
                            currentPosition = it.duration
                        )
                    }
                }

                Player.STATE_IDLE -> {
                    _playerState.update { it.copy(isBuffering = false) }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val player = exoPlayer ?: return
            val state = _playerState.value
            val newIndex = player.currentMediaItemIndex
            if (newIndex in state.playlist.indices) {
                val track = state.playlist[newIndex]
                _playerState.update {
                    it.copy(
                        currentTrack = track,
                        currentIndex = newIndex,
                        currentPosition = player.currentPosition.coerceAtLeast(0L),
                        duration = track.duration,
                        errorMessage = null
                    )
                }
            }
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            initializeAudioEffects(audioSessionId)
        }

        override fun onPlayerError(error: PlaybackException) {
            _playerState.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = error.localizedMessage ?: "This audio file could not be played."
                )
            }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(PROGRESS_UPDATE_INTERVAL_MS)
                val player = exoPlayer ?: continue
                if (player.isPlaying) {
                    _playerState.update {
                        it.copy(currentPosition = player.currentPosition.coerceAtLeast(0L))
                    }
                }
            }
        }
    }

    fun loadPlaylist(tracks: List<AudioTrack>, startIndex: Int = 0) {
        val player = exoPlayer ?: return
        if (tracks.isEmpty()) {
            player.clearMediaItems()
            _playerState.value = PlayerState(
                volume = _playerState.value.volume,
                isVocalIsolationEnabled = vocalProcessor.isProcessorEnabled(),
                vocalIsolationLevel = vocalProcessor.getLevel(),
                playbackSpeed = _playerState.value.playbackSpeed,
                isShuffleEnabled = _playerState.value.isShuffleEnabled,
                repeatMode = _playerState.value.repeatMode
            )
            return
        }

        val safeStartIndex = startIndex.coerceIn(tracks.indices)
        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setUri(track.uri)
                .setMediaId(track.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setArtworkUri(track.albumArtUri)
                        .build()
                )
                .build()
        }

        _playerState.update {
            it.copy(
                playlist = tracks,
                currentTrack = tracks[safeStartIndex],
                currentIndex = safeStartIndex,
                currentPosition = 0L,
                duration = tracks[safeStartIndex].duration,
                errorMessage = null
            )
        }
        player.setMediaItems(mediaItems, safeStartIndex, 0L)
        player.prepare()
    }

    fun play() {
        exoPlayer?.takeIf { it.mediaItemCount > 0 }?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun playPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) player.pause() else if (player.mediaItemCount > 0) player.play()
        }
    }

    fun seekTo(position: Long) {
        val player = exoPlayer ?: return
        val duration = player.duration.takeIf { it > 0L } ?: _playerState.value.duration
        val safePosition = position.coerceIn(0L, duration.coerceAtLeast(0L))
        player.seekTo(safePosition)
        _playerState.update { it.copy(currentPosition = safePosition) }
    }

    fun playNext() {
        val player = exoPlayer ?: return
        if (player.mediaItemCount == 0) return

        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else {
            player.seekTo(0, 0L)
        }
        player.play()
    }

    fun playPrevious() {
        val player = exoPlayer ?: return
        if (player.mediaItemCount == 0) return

        if (player.currentPosition > PREVIOUS_RESTART_THRESHOLD_MS) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(player.mediaItemCount - 1, 0L)
        }
        player.play()
    }

    fun toggleShuffle() {
        val player = exoPlayer ?: return
        val enabled = !player.shuffleModeEnabled
        player.shuffleModeEnabled = enabled
        _playerState.update { it.copy(isShuffleEnabled = enabled) }
    }

    fun toggleRepeat() {
        val player = exoPlayer ?: return
        val mode = when (_playerState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        player.repeatMode = mode.toPlayerRepeatMode()
        _playerState.update { it.copy(repeatMode = mode) }
    }

    fun setVocalIsolation(enabled: Boolean) {
        vocalProcessor.setEnabled(enabled)
        _playerState.update { it.copy(isVocalIsolationEnabled = enabled) }
        applyVocalIsolationEffects()
    }

    fun toggleVocalIsolation() {
        setVocalIsolation(!vocalProcessor.isProcessorEnabled())
    }

    fun setVocalIsolationLevel(level: Float) {
        vocalProcessor.setLevel(level)
        val safeLevel = vocalProcessor.getLevel()
        _playerState.update { it.copy(vocalIsolationLevel = safeLevel) }
        if (vocalProcessor.isProcessorEnabled()) applyVocalIsolationEffects()
    }

    fun setPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED)
        exoPlayer?.setPlaybackSpeed(safeSpeed)
        _playerState.update { it.copy(playbackSpeed = safeSpeed) }
    }

    fun setVolume(volume: Float) {
        val safeVolume = volume.coerceIn(0f, 1f)
        exoPlayer?.volume = safeVolume
        _playerState.update { it.copy(volume = safeVolume) }
    }

    fun release() {
        progressJob?.cancel()
        progressJob = null
        releaseAudioEffects()
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        scope.cancel()
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    private fun releaseAudioEffects() {
        runCatching { equalizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        loudnessEnhancer = null
        effectsSessionId = -1
    }

    private fun RepeatMode.toPlayerRepeatMode(): Int = when (this) {
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
    }

    private companion object {
        const val PROGRESS_UPDATE_INTERVAL_MS = 250L
        const val PREVIOUS_RESTART_THRESHOLD_MS = 3_000L
        const val MIN_PLAYBACK_SPEED = 0.5f
        const val MAX_PLAYBACK_SPEED = 2f
    }
}
