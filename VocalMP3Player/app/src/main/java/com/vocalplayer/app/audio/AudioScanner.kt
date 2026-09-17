package com.vocalplayer.app.audio

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.vocalplayer.app.data.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioScanner(context: Context) {

    private val contentResolver = context.applicationContext.contentResolver

    suspend fun scanAudioFiles(): List<AudioTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<AudioTrack>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArguments = arrayOf(MIN_TRACK_DURATION_MS.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArguments,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumIdColumn)
                tracks += AudioTrack(
                    id = id,
                    title = cursor.getString(titleColumn).knownOr("Unknown title"),
                    artist = cursor.getString(artistColumn).knownOr("Unknown artist"),
                    album = cursor.getString(albumColumn).knownOr("Unknown album"),
                    duration = cursor.getLong(durationColumn).coerceAtLeast(0L),
                    uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ),
                    albumArtUri = albumId.takeIf { it > 0L }?.let {
                        ContentUris.withAppendedId(ALBUM_ART_CONTENT_URI, it)
                    }
                )
            }
        }

        tracks
    }

    private fun String?.knownOr(fallback: String): String =
        this?.trim()?.takeIf { it.isNotEmpty() && it != UNKNOWN_MEDIA_VALUE } ?: fallback

    private companion object {
        const val MIN_TRACK_DURATION_MS = 10_000L
        const val UNKNOWN_MEDIA_VALUE = "<unknown>"
        val ALBUM_ART_CONTENT_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}
