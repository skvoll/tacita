package one.volkov.apps.tacita.platform

import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import one.volkov.apps.tacita.domain.VolumePolicy

class MediaVolumeController(private val audioManager: AudioManager) {
    fun lowerTo(targetPercent: Int, includeBluetooth: Boolean) {
        if (audioManager.isMusicActive) return
        if (!includeBluetooth && hasBluetoothOutput()) return

        val maximumVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVolume = VolumePolicy.targetVolume(maximumVolume, targetPercent)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (VolumePolicy.shouldLower(currentVolume, targetVolume)) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
        }
    }

    private fun hasBluetoothOutput(): Boolean = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        .any { device -> device.type in bluetoothDeviceTypes }

    private companion object {
        val bluetoothDeviceTypes: Set<Int> = buildSet {
            add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
            add(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) add(AudioDeviceInfo.TYPE_HEARING_AID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(AudioDeviceInfo.TYPE_BLE_HEADSET)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(AudioDeviceInfo.TYPE_BLE_SPEAKER)
        }
    }
}
