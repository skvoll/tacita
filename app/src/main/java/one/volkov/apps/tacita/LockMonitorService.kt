package one.volkov.apps.tacita

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlin.math.roundToInt

class LockMonitorService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var screenReceiverRegistered = false
    private var screenOffCheckJob: Job? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOffCheckJob?.cancel()
                    screenOffCheckJob = serviceScope.launch {
                        adjustVolumeForLock()
                        delay(SCREEN_OFF_FOLLOW_UP_DELAY_MS)
                        adjustVolumeForLock()
                    }
                }
                Intent.ACTION_SCREEN_ON -> serviceScope.launch {
                    val settings = SettingsRepository.settings(applicationContext).first()
                    if (settings.checkOnScreenOn) adjustVolumeForLock()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerScreenReceiver()
        startForeground(NOTIFICATION_ID, monitoringNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        if (screenReceiverRegistered) unregisterReceiver(screenReceiver)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun adjustVolumeForLock() {
        val settings = SettingsRepository.settings(applicationContext).first()
        val audioManager = getSystemService(AudioManager::class.java) ?: return

        if (!settings.enabled) return
        if (audioManager.isMusicActive) return
        if (!settings.includeBluetooth && audioManager.hasBluetoothOutput()) return

        val maximumVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVolume = (maximumVolume * settings.targetPercent / 100f)
            .roundToInt()
            .coerceIn(0, maximumVolume)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (targetVolume < currentVolume) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
        }
    }

    private fun AudioManager.hasBluetoothOutput(): Boolean = getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        .any { device ->
            device.type in bluetoothDeviceTypes
        }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        screenReceiverRegistered = true
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun monitoringNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.notification_title))
        .setContentText(getString(R.string.notification_text))
        .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
        .setOngoing(true)
        .build()

    private companion object {
        const val CHANNEL_ID = "lock_monitor"
        const val NOTIFICATION_ID = 1
        const val SCREEN_OFF_FOLLOW_UP_DELAY_MS = 60_000L

        val bluetoothDeviceTypes: Set<Int> = buildSet {
            add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
            add(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) add(AudioDeviceInfo.TYPE_HEARING_AID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(AudioDeviceInfo.TYPE_BLE_HEADSET)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(AudioDeviceInfo.TYPE_BLE_SPEAKER)
        }
    }
}
