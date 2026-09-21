package one.volkov.apps.tacita.data

data class AppSettings(
    val enabled: Boolean = false,
    val targetPercent: Int = 5,
    val includeBluetooth: Boolean = false,
    val allowVolumeAbove20: Boolean = false,
    val checkOnScreenOn: Boolean = false,
)
