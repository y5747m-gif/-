package com.vocalplayer.app

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vocalplayer.app.audio.AudioPlayerManager
import com.vocalplayer.app.audio.AudioScanner
import com.vocalplayer.app.data.AppSettings
import com.vocalplayer.app.data.AudioTrack
import com.vocalplayer.app.data.BackgroundStyle
import com.vocalplayer.app.data.PlayerState
import com.vocalplayer.app.data.SettingsDataStore
import com.vocalplayer.app.service.MusicPlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val audioScanner = AudioScanner(application)
    private val settingsDataStore = SettingsDataStore(application)

    private var playerManager: AudioPlayerManager? = null
    private var playerStateJob: Job? = null
    private var serviceBound = false
    private var pendingTrack: AudioTrack? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsDataStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _audioTracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val audioTracks: StateFlow<List<AudioTrack>> = _audioTracks.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _libraryError = MutableStateFlow<String?>(null)
    val libraryError: StateFlow<String?> = _libraryError.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Player)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val manager = (service as? MusicPlaybackService.LocalBinder)?.getPlayerManager()
                ?: return
            playerManager = manager
            observePlayer(manager)
            applySettings(manager, settings.value)

            val queuedTrack = pendingTrack
            if (queuedTrack != null) {
                pendingTrack = null
                playTrack(queuedTrack)
            } else if (manager.playerState.value.playlist.isEmpty() && _audioTracks.value.isNotEmpty()) {
                manager.loadPlaylist(_audioTracks.value)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            detachPlayer()
        }

        override fun onBindingDied(name: ComponentName?) {
            detachPlayer()
        }

        override fun onNullBinding(name: ComponentName?) {
            detachPlayer()
        }
    }

    init {
        startAndBindService()
        observeSettings()
        loadAudioFiles()
    }

    private fun startAndBindService() {
        val context = getApplication<Application>()
        val serviceIntent = Intent(context, MusicPlaybackService::class.java)
        runCatching { context.startService(serviceIntent) }

        val bindIntent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_BIND_LOCAL
        }
        serviceBound = runCatching {
            context.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }.getOrDefault(false)
    }

    private fun observePlayer(manager: AudioPlayerManager) {
        playerStateJob?.cancel()
        playerStateJob = viewModelScope.launch {
            manager.playerState.collect { state -> _playerState.value = state }
        }
    }

    private fun detachPlayer() {
        playerStateJob?.cancel()
        playerStateJob = null
        playerManager = null
        serviceBound = false
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsDataStore.settings.collect { appSettings ->
                playerManager?.let { applySettings(it, appSettings) }
            }
        }
    }

    private fun applySettings(manager: AudioPlayerManager, appSettings: AppSettings) {
        manager.setVocalIsolation(appSettings.vocalIsolationEnabled)
        manager.setVocalIsolationLevel(appSettings.vocalIsolationLevel)
        manager.setPlaybackSpeed(appSettings.playbackSpeed)
    }

    private fun loadAudioFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            _libraryError.value = null
            try {
                val tracks = audioScanner.scanAudioFiles()
                _audioTracks.value = tracks
                val manager = playerManager
                if (tracks.isNotEmpty() && manager?.playerState?.value?.playlist?.isEmpty() == true) {
                    manager.loadPlaylist(tracks)
                }
            } catch (_: SecurityException) {
                _audioTracks.value = emptyList()
                _libraryError.value = "Music access is required to scan this device."
            } catch (error: Exception) {
                _libraryError.value = error.localizedMessage ?: "The music library could not be loaded."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun playTrack(track: AudioTrack) {
        val manager = playerManager
        if (manager == null) {
            pendingTrack = track
            return
        }

        val index = _audioTracks.value.indexOfFirst { it.id == track.id }
        if (index >= 0) {
            manager.loadPlaylist(_audioTracks.value, index)
            manager.play()
        }
    }

    fun playPause() = playerManager?.playPause()
    fun playNext() = playerManager?.playNext()
    fun playPrevious() = playerManager?.playPrevious()
    fun seekTo(position: Long) = playerManager?.seekTo(position)
    fun toggleShuffle() = playerManager?.toggleShuffle()
    fun toggleRepeat() = playerManager?.toggleRepeat()

    fun toggleVocalIsolation() {
        setVocalIsolationEnabled(!playerState.value.isVocalIsolationEnabled)
    }

    fun setVocalIsolationEnabled(enabled: Boolean) {
        playerManager?.setVocalIsolation(enabled)
        viewModelScope.launch { settingsDataStore.updateVocalIsolation(enabled) }
    }

    fun setVocalIsolationLevel(level: Float) {
        playerManager?.setVocalIsolationLevel(level)
        viewModelScope.launch { settingsDataStore.updateVocalIsolationLevel(level) }
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager?.setPlaybackSpeed(speed)
        viewModelScope.launch { settingsDataStore.updatePlaybackSpeed(speed) }
    }

    fun setVolume(volume: Float) = playerManager?.setVolume(volume)

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

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

    fun refreshLibrary() {
        loadAudioFiles()
    }

    override fun onCleared() {
        val shouldStopService = !playerState.value.isPlaying
        playerStateJob?.cancel()
        playerStateJob = null

        if (serviceBound) {
            runCatching { getApplication<Application>().unbindService(serviceConnection) }
            serviceBound = false
        }
        playerManager = null

        if (shouldStopService) {
            getApplication<Application>().stopService(
                Intent(getApplication(), MusicPlaybackService::class.java)
            )
        }
        super.onCleared()
    }
}

sealed class Screen {
    data object Player : Screen()
    data object Library : Screen()
    data object Settings : Screen()
}
