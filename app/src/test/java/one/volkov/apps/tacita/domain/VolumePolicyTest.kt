package one.volkov.apps.tacita.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumePolicyTest {
    @Test
    fun `calculates a bounded target volume`() {
        assertEquals(5, VolumePolicy.targetVolume(maximumVolume = 20, targetPercent = 25))
        assertEquals(0, VolumePolicy.targetVolume(maximumVolume = 20, targetPercent = -10))
        assertEquals(20, VolumePolicy.targetVolume(maximumVolume = 20, targetPercent = 110))
    }

    @Test
    fun `only lowers the current volume`() {
        assertTrue(VolumePolicy.shouldLower(currentVolume = 6, targetVolume = 5))
        assertFalse(VolumePolicy.shouldLower(currentVolume = 5, targetVolume = 5))
        assertFalse(VolumePolicy.shouldLower(currentVolume = 4, targetVolume = 5))
    }
}
