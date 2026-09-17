package com.vocalplayer.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MusicNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Handled via MediaSession callbacks
        when (intent.action) {
            "com.vocalplayer.action.PLAY_PAUSE" -> {
                // Media session handles this
            }
            "com.vocalplayer.action.NEXT" -> {
                // Media session handles this
            }
            "com.vocalplayer.action.PREV" -> {
                // Media session handles this
            }
            "com.vocalplayer.action.STOP" -> {
                // Media session handles this
            }
        }
    }
}
