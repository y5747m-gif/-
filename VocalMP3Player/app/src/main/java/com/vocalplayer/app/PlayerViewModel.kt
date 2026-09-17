package com.vocalplayer.app

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vocalplayer.app.audio.AudioPlayerManager
import com.vocalplayer.app.audio.AudioScanner
import com.vocalplayer.app.data.*
import com.vocalplayer.app.service.MusicPlaybackService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val audioScanner = AudioScanner(application)
    private val settingsDataStore = SettingsDataStore(application)
    private val audioPlayerManager = AudioPlayerManager(application)

    val playerState: StateFlow<PlayerState> = audioPlayerManager.playerState
    val settings: StateFlow<AppSettings> = settingsDataStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _audioTracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val audioTracks: StateFlow<List<AudioTrack>> = _audioTracks.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Player)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    init {
        audioPlayerManager.initialize()
        loadAudioFiles()
        observeSettings()
        startMusicService()
    }

    private fun startMusicService() {
        val context = getApplication<Application>()
        // Hand the player to the service (the service builds its own MediaSession
        // around it) and start it for background playback.
        MusicPlaybackService.sharedPlayer = audioPlayerManager.getPlayer()

        val intent = Intent(context, MusicPlaybackService::class.java)
        try {
            context.startService(intent)
        } catch (e: Exception) {
            // Service might not start on some devices
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsDataStore.settings.collect { appSettings ->
                audioPlayerManager.setVocalIsolation(appSettings.vocalIsolationEnabled)
                audioPlayerManager.setVocalIsolationLevel(appSettings.vocalIsolationLevel)
                audioPlayerManager.setPlaybackSpeed(appSettings.playbackSpeed)
            }
        }
    }

    private fun loadAudioFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val tracks = audioScanner.scanAudioFiles()
                _audioTracks.value = tracks
                if (tracks.isNotEmpty()) {
                    audioPlayerManager.loadPlaylist(tracks)
                }
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun playTrack(track: AudioTrack) {
        val index = _audioTracks.value.indexOf(track)
        if (index >= 0) {
            audioPlayerManager.loadPlaylist(_audioTracks.value, index)
            audioPlayerManager.play()
        }
    }

    fun playPause() = audioPlayerManager.playPause()
    fun playNext() = audioPlayerManager.playNext()
    fun playPrevious() = audioPlayerManager.playPrevious()
    fun seekTo(position: Long) = audioPlayerManager.seekTo(position)
    fun toggleShuffle() = audioPlayerManager.toggleShuffle()
    fun toggleRepeat() = audioPlayerManager.toggleRepeat()

    /**
     * Toggles vocal isolation. Settings are the single source of truth; the
     * player applies the change via [observeSettings].
     */
    fun toggleVocalIsolation() {
        val current = settings.value.vocalIsolationEnabled
        viewModelScope.launch {
            settingsDataStore.updateVocalIsolation(!current)
        }
    }

    /**
     * Persists the vocal isolation default without double-applying it.
     */
    fun setVocalIsolationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateVocalIsolation(enabled)
        }
    }

    fun setVocalIsolationLevel(level: Float) {
        viewModelScope.launch {
            settingsDataStore.updateVocalIsolationLevel(level)
        }
        audioPlayerManager.setVocalIsolationLevel(level)
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            settingsDataStore.updatePlaybackSpeed(speed)
        }
        audioPlayerManager.setPlaybackSpeed(speed)
    }

    fun setVolume(volume: Float) = audioPlayerManager.setVolume(volume)

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // Settings
    fun updateBackgroundStyle(style: BackgroundStyle) {
        viewModelScope.launch { settingsDataStore.updateBackgroundStyle(style) }
    }

    fun updateCustomBackgroundUri(uri: String?) {
        viewModelScope.launch { settingsDataStore.updateCustomBackgroundUri(uri) }
    }

    fun updateShowVisualizer(show: Boolean) {
        viewModelScope.launch { settingsDataStore.updateShowVisualizer(show) }
    }

    fun updateButtonAnimations(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateButtonAnimations(enabled) }
    }

    fun updateHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateHapticFeedback(enabled) }
    }

    fun updateCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateCrossfadeEnabled(enabled) }
    }

    fun refreshLibrary() {
        loadAudioFiles()
    }

    override fun onCleared() {
        super.onCleared()
        // The player is owned here; the service only borrows it.
        MusicPlaybackService.sharedPlayer = null
        audioPlayerManager.release()
    }
}

sealed class Screen {
    object Player : Screen()
    object Library : Screen()
    object Settings : Screen()
}
