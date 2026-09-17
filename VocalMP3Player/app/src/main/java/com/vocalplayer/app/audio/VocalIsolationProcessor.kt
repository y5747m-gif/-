package com.vocalplayer.app.audio



/**
 * Vocal Isolation Processor
 *
 * Uses multiple audio processing techniques to isolate vocals:
 * 1. Center Channel Extraction (works for stereo tracks where vocals are centered)
 * 2. Frequency-based filtering to focus on vocal frequency range (300Hz - 3kHz)
 * 3. Dynamic range processing to enhance vocal content
 */
class VocalIsolationProcessor {

    private var isEnabled = false
    private var isolationLevel = 1.0f

    // Audio effect parameters
    companion object {
        // Vocal frequency range (Hz)
        const val VOCAL_LOW_FREQ = 300
        const val VOCAL_MID_FREQ = 1000
        const val VOCAL_HIGH_FREQ = 3400

        // Processing constants
        const val CENTER_EXTRACTION_STRENGTH = 0.7f
        const val BASS_CUT_DB = -12f
        const val TREBLE_CUT_DB = -8f
        const val VOCAL_BOOST_DB = 6f
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun isProcessorEnabled(): Boolean = isEnabled

    fun setLevel(level: Float) {
        isolationLevel = level.coerceIn(0f, 1f)
    }

    fun getLevel(): Float = isolationLevel

    /**
     * Process audio buffer for vocal isolation
     * This works with the raw PCM data from the audio decoder
     *
     * For ExoPlayer, we apply the effect using Android's built-in audio effects
     * which operate at the AudioTrack level.
     */
    fun getCenterExtractionStrength(): Float {
        return if (isEnabled) {
            CENTER_EXTRACTION_STRENGTH * isolationLevel
        } else {
            0f
        }
    }

    /**
     * Calculate the frequency filter parameters for the equalizer
     * Returns a map of frequency band adjustments
     */
    fun getEqualizerSettings(numBands: Int, freqRange: IntRange): Map<Int, Short> {
        val settings = mutableMapOf<Int, Short>()

        if (!isEnabled) return settings

        for (i in 0 until numBands) {
            val centerFreq = freqRange.first + (freqRange.last - freqRange.first) * i / numBands
            val level = when {
                // Boost vocal range (300Hz - 3.4kHz)
                centerFreq in VOCAL_LOW_FREQ..VOCAL_HIGH_FREQ -> {
                    val vocalBoost = when {
                        centerFreq in 800..2500 -> VOCAL_BOOST_DB * isolationLevel
                        centerFreq in 300..800 -> VOCAL_BOOST_DB * 0.6f * isolationLevel
                        else -> VOCAL_BOOST_DB * 0.3f * isolationLevel
                    }
                    (vocalBoost * 100).toInt().toShort()
                }
                // Cut bass frequencies (below vocal range)
                centerFreq < VOCAL_LOW_FREQ -> {
                    (BASS_CUT_DB * isolationLevel * 100).toInt().toShort()
                }
                // Cut high frequencies (above vocal range)
                centerFreq > VOCAL_HIGH_FREQ -> {
                    (TREBLE_CUT_DB * isolationLevel * 100).toInt().toShort()
                }
                else -> 0
            }
            settings[i] = level
        }

        return settings
    }

    /**
     * Process stereo audio data to extract center channel (vocals)
     * Modifies the buffer in-place
     *
     * Algorithm: Center = (L + R) / 2
     * Side = (L - R) / 2 (this is where most music/instruments sit)
     * Vocals = Center - Side * strength
     */
    fun processStereoBuffer(buffer: ShortArray, frameCount: Int) {
        if (!isEnabled) return

        val strength = CENTER_EXTRACTION_STRENGTH * isolationLevel

        for (i in 0 until frameCount) {
            val leftIdx = i * 2
            val rightIdx = i * 2 + 1

            if (rightIdx >= buffer.size) break

            val left = buffer[leftIdx].toFloat()
            val right = buffer[rightIdx].toFloat()

            // Center channel (vocals + centered instruments)
            val center = (left + right) / 2f

            // Side channel (panned instruments)
            val side = (left - right) / 2f

            // Extract center by reducing side content
            val vocal = center - side * strength

            // Apply the processed signal to both channels
            buffer[leftIdx] = vocal.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat()).toInt().toShort()
            buffer[rightIdx] = vocal.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat()).toInt().toShort()
        }
    }
}
