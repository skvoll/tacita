package one.volkov.apps.tacita.domain

import kotlin.math.roundToInt

object VolumePolicy {
    fun targetVolume(maximumVolume: Int, targetPercent: Int): Int =
        (maximumVolume * targetPercent / 100f).roundToInt().coerceIn(0, maximumVolume)

    fun shouldLower(currentVolume: Int, targetVolume: Int): Boolean = targetVolume < currentVolume
}
