package com.vocalplayer.app.audio

/**
 * Calculates the vocal-focus filter used by the player.
 *
 * The real-time player uses the equalizer settings generated here. The PCM
 * helper is kept separate so the center-channel algorithm can be tested and
 * reused by an audio processor without depending on Android framework types.
 */
class VocalIsolationProcessor {

    private var enabled = false
    private var isolationLevel = 1f

    companion object {
        const val VOCAL_LOW_FREQ = 300
        const val VOCAL_MID_FREQ = 1000
        const val VOCAL_HIGH_FREQ = 3400

        const val CENTER_EXTRACTION_STRENGTH = 1f
        const val BASS_CUT_DB = -12f
        const val TREBLE_CUT_DB = -8f
        const val VOCAL_BOOST_DB = 6f
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun isProcessorEnabled(): Boolean = enabled

    fun setLevel(level: Float) {
        isolationLevel = level.coerceIn(0f, 1f)
    }

    fun getLevel(): Float = isolationLevel

    fun getCenterExtractionStrength(): Float =
        if (enabled) CENTER_EXTRACTION_STRENGTH * isolationLevel else 0f

    /** Returns the requested equalizer level in millibels for [frequencyHz]. */
    fun getBandLevel(frequencyHz: Int): Short {
        if (!enabled) return 0

        val levelDb = when {
            frequencyHz < VOCAL_LOW_FREQ -> BASS_CUT_DB * isolationLevel
            frequencyHz > VOCAL_HIGH_FREQ -> TREBLE_CUT_DB * isolationLevel
            frequencyHz in 800..2500 -> VOCAL_BOOST_DB * isolationLevel
            frequencyHz in VOCAL_LOW_FREQ..800 -> VOCAL_BOOST_DB * 0.6f * isolationLevel
            else -> VOCAL_BOOST_DB * 0.3f * isolationLevel
        }

        return (levelDb * 100)
            .toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            .toShort()
    }

    /**
     * Produces levels for evenly spaced bands over [freqRange].
     *
     * Android equalizers expose each band's actual center frequency, so the
     * player normally calls [getBandLevel] directly. This method remains useful
     * to other processors and is deliberately safe for empty/single-band input.
     */
    fun getEqualizerSettings(numBands: Int, freqRange: IntRange): Map<Int, Short> {
        if (!enabled || numBands <= 0) return emptyMap()

        val denominator = (numBands - 1).coerceAtLeast(1)
        return buildMap(numBands) {
            repeat(numBands) { band ->
                val frequency = freqRange.first +
                    (freqRange.last - freqRange.first) * band / denominator
                put(band, getBandLevel(frequency))
            }
        }
    }

    /**
     * Blends a stereo PCM buffer toward its center channel in place.
     * [frameCount] is bounded by the available complete stereo frames.
     */
    fun processStereoBuffer(buffer: ShortArray, frameCount: Int) {
        val strength = getCenterExtractionStrength()
        if (strength <= 0f || frameCount <= 0) return

        val safeFrameCount = frameCount.coerceAtMost(buffer.size / 2)
        repeat(safeFrameCount) { frame ->
            val leftIndex = frame * 2
            val rightIndex = leftIndex + 1
            val left = buffer[leftIndex].toFloat()
            val right = buffer[rightIndex].toFloat()
            val center = (left + right) / 2f

            buffer[leftIndex] = blendAndClamp(left, center, strength)
            buffer[rightIndex] = blendAndClamp(right, center, strength)
        }
    }

    private fun blendAndClamp(original: Float, center: Float, strength: Float): Short =
        (original + (center - original) * strength)
            .coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat())
            .toInt()
            .toShort()
}
