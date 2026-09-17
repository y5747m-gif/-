package com.vocalplayer.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vocal_player_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        private val BACKGROUND_STYLE = stringPreferencesKey("background_style")
        private val CUSTOM_BG_URI = stringPreferencesKey("custom_bg_uri")
        private val VOCAL_ISOLATION_ENABLED = booleanPreferencesKey("vocal_isolation_enabled")
        private val VOCAL_ISOLATION_LEVEL = floatPreferencesKey("vocal_isolation_level")
        private val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        private val CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        private val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        private val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        private val SHOW_VISUALIZER = booleanPreferencesKey("show_visualizer")
        private val BUTTON_ANIMATIONS = booleanPreferencesKey("button_animations")
        private val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            isDarkTheme = prefs[IS_DARK_THEME] ?: true,
            backgroundStyle = try {
                BackgroundStyle.valueOf(prefs[BACKGROUND_STYLE] ?: BackgroundStyle.DEFAULT.name)
            } catch (e: Exception) { BackgroundStyle.DEFAULT },
            customBackgroundUri = prefs[CUSTOM_BG_URI],
            vocalIsolationEnabled = prefs[VOCAL_ISOLATION_ENABLED] ?: false,
            vocalIsolationLevel = prefs[VOCAL_ISOLATION_LEVEL] ?: 1f,
            playbackSpeed = prefs[PLAYBACK_SPEED] ?: 1f,
            crossfadeEnabled = prefs[CROSSFADE_ENABLED] ?: false,
            gaplessPlayback = prefs[GAPLESS_PLAYBACK] ?: true,
            audioQuality = try {
                AudioQuality.valueOf(prefs[AUDIO_QUALITY] ?: AudioQuality.HIGH.name)
            } catch (e: Exception) { AudioQuality.HIGH },
            showVisualizer = prefs[SHOW_VISUALIZER] ?: true,
            buttonAnimations = prefs[BUTTON_ANIMATIONS] ?: true,
            hapticFeedback = prefs[HAPTIC_FEEDBACK] ?: true
        )
    }

    suspend fun updateDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[IS_DARK_THEME] = enabled }
    }

    suspend fun updateBackgroundStyle(style: BackgroundStyle) {
        context.dataStore.edit { it[BACKGROUND_STYLE] = style.name }
    }

    suspend fun updateCustomBackgroundUri(uri: String?) {
        context.dataStore.edit {
            if (uri != null) it[CUSTOM_BG_URI] = uri else it.remove(CUSTOM_BG_URI)
        }
    }

    suspend fun updateVocalIsolation(enabled: Boolean) {
        context.dataStore.edit { it[VOCAL_ISOLATION_ENABLED] = enabled }
    }

    suspend fun updateVocalIsolationLevel(level: Float) {
        context.dataStore.edit { it[VOCAL_ISOLATION_LEVEL] = level }
    }

    suspend fun updatePlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[PLAYBACK_SPEED] = speed }
    }

    suspend fun updateShowVisualizer(show: Boolean) {
        context.dataStore.edit { it[SHOW_VISUALIZER] = show }
    }

    suspend fun updateButtonAnimations(enabled: Boolean) {
        context.dataStore.edit { it[BUTTON_ANIMATIONS] = enabled }
    }

    suspend fun updateHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun updateCrossfadeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[CROSSFADE_ENABLED] = enabled }
    }

    suspend fun updateGaplessPlayback(enabled: Boolean) {
        context.dataStore.edit { it[GAPLESS_PLAYBACK] = enabled }
    }

    suspend fun updateAudioQuality(quality: AudioQuality) {
        context.dataStore.edit { it[AUDIO_QUALITY] = quality.name }
    }
}
