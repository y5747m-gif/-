package com.vocalplayer.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vocal_player_settings")

class SettingsDataStore(context: Context) {

    private val appContext = context.applicationContext

    private companion object {
        val BACKGROUND_STYLE = stringPreferencesKey("background_style")
        val CUSTOM_BG_URI = stringPreferencesKey("custom_bg_uri")
        val VOCAL_ISOLATION_ENABLED = booleanPreferencesKey("vocal_isolation_enabled")
        val VOCAL_ISOLATION_LEVEL = floatPreferencesKey("vocal_isolation_level")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val SHOW_VISUALIZER = booleanPreferencesKey("show_visualizer")
        val BUTTON_ANIMATIONS = booleanPreferencesKey("button_animations")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")

        const val MIN_PLAYBACK_SPEED = 0.5f
        const val MAX_PLAYBACK_SPEED = 2f
    }

    val settings: Flow<AppSettings> = appContext.dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            AppSettings(
                backgroundStyle = preferences[BACKGROUND_STYLE]
                    ?.let { runCatching { BackgroundStyle.valueOf(it) }.getOrNull() }
                    ?: BackgroundStyle.DEFAULT,
                customBackgroundUri = preferences[CUSTOM_BG_URI],
                vocalIsolationEnabled = preferences[VOCAL_ISOLATION_ENABLED] ?: false,
                vocalIsolationLevel = (preferences[VOCAL_ISOLATION_LEVEL] ?: 1f).coerceIn(0f, 1f),
                playbackSpeed = (preferences[PLAYBACK_SPEED] ?: 1f)
                    .coerceIn(MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED),
                showVisualizer = preferences[SHOW_VISUALIZER] ?: true,
                buttonAnimations = preferences[BUTTON_ANIMATIONS] ?: true,
                hapticFeedback = preferences[HAPTIC_FEEDBACK] ?: true
            )
        }

    suspend fun updateBackgroundStyle(style: BackgroundStyle) {
        appContext.dataStore.edit { it[BACKGROUND_STYLE] = style.name }
    }

    suspend fun updateCustomBackgroundUri(uri: String?) {
        appContext.dataStore.edit { preferences ->
            if (uri.isNullOrBlank()) preferences.remove(CUSTOM_BG_URI)
            else preferences[CUSTOM_BG_URI] = uri
        }
    }

    suspend fun updateVocalIsolation(enabled: Boolean) {
        appContext.dataStore.edit { it[VOCAL_ISOLATION_ENABLED] = enabled }
    }

    suspend fun updateVocalIsolationLevel(level: Float) {
        appContext.dataStore.edit { it[VOCAL_ISOLATION_LEVEL] = level.coerceIn(0f, 1f) }
    }

    suspend fun updatePlaybackSpeed(speed: Float) {
        appContext.dataStore.edit {
            it[PLAYBACK_SPEED] = speed.coerceIn(MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED)
        }
    }

    suspend fun updateShowVisualizer(show: Boolean) {
        appContext.dataStore.edit { it[SHOW_VISUALIZER] = show }
    }

    suspend fun updateButtonAnimations(enabled: Boolean) {
        appContext.dataStore.edit { it[BUTTON_ANIMATIONS] = enabled }
    }

    suspend fun updateHapticFeedback(enabled: Boolean) {
        appContext.dataStore.edit { it[HAPTIC_FEEDBACK] = enabled }
    }

}
