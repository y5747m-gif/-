package com.vocalplayer.app.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VocalIsolationProcessorTest {

    @Test
    fun levelIsClampedToSupportedRange() {
        val processor = VocalIsolationProcessor()

        processor.setLevel(-2f)
        assertEquals(0f, processor.getLevel(), 0f)

        processor.setLevel(3f)
        assertEquals(1f, processor.getLevel(), 0f)
    }

    @Test
    fun disabledProcessorReturnsNeutralSettings() {
        val processor = VocalIsolationProcessor()

        assertEquals(0f, processor.getCenterExtractionStrength(), 0f)
        assertEquals(0, processor.getBandLevel(1_000).toInt())
        assertTrue(processor.getEqualizerSettings(5, 60..12_000).isEmpty())
    }

    @Test
    fun enabledProcessorCutsOuterFrequenciesAndBoostsVocals() {
        val processor = VocalIsolationProcessor().apply {
            setEnabled(true)
            setLevel(1f)
        }

        assertEquals(-1_200, processor.getBandLevel(100).toInt())
        assertEquals(600, processor.getBandLevel(1_000).toInt())
        assertEquals(-800, processor.getBandLevel(8_000).toInt())
    }

    @Test
    fun equalizerSettingsIncludeBothFrequencyRangeEndpoints() {
        val processor = VocalIsolationProcessor().apply { setEnabled(true) }

        val settings = processor.getEqualizerSettings(2, 100..8_000)

        assertEquals(processor.getBandLevel(100), settings[0])
        assertEquals(processor.getBandLevel(8_000), settings[1])
    }

    @Test
    fun fullStrengthStereoProcessingKeepsOnlyCenterChannel() {
        val processor = VocalIsolationProcessor().apply {
            setEnabled(true)
            setLevel(1f)
        }
        val samples = shortArrayOf(1_000, -1_000, 2_000, 2_000)

        processor.processStereoBuffer(samples, frameCount = 2)

        assertArrayEquals(shortArrayOf(0, 0, 2_000, 2_000), samples)
    }

    @Test
    fun stereoProcessingRespectsLevelAndAvailableFrames() {
        val processor = VocalIsolationProcessor().apply {
            setEnabled(true)
            setLevel(0.5f)
        }
        val samples = shortArrayOf(1_000, -1_000, 123)

        processor.processStereoBuffer(samples, frameCount = 100)

        assertArrayEquals(shortArrayOf(500, -500, 123), samples)
    }
}
