package one.volkov.apps.tacita.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import one.volkov.apps.tacita.R
import one.volkov.apps.tacita.data.AppSettings
import one.volkov.apps.tacita.data.SettingsRepository
import one.volkov.apps.tacita.platform.MediaVolumeController

class LockMonitorService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var screenReceiverRegistered = false
    private var screenOffCheckJob: Job? = null
    private var volumeController: MediaVolumeController? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> scheduleScreenOffChecks()
                Intent.ACTION_SCREEN_ON -> serviceScope.launch { checkVolumeOnScreenOn() }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        volumeController = getSystemService(AudioManager::class.java)?.let(::MediaVolumeController)
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

    private fun scheduleScreenOffChecks() {
        screenOffCheckJob?.cancel()
        screenOffCheckJob = serviceScope.launch {
            checkVolume()
            delay(SCREEN_OFF_FOLLOW_UP_DELAY_MS)
            checkVolume()
        }
    }

    private suspend fun checkVolumeOnScreenOn() {
        val settings = SettingsRepository.settings(applicationContext).first()
        if (settings.checkOnScreenOn) checkVolume(settings)
    }

    private suspend fun checkVolume() {
        checkVolume(SettingsRepository.settings(applicationContext).first())
    }

    private fun checkVolume(settings: AppSettings) {
        if (settings.enabled) {
            volumeController?.lowerTo(settings.targetPercent, settings.includeBluetooth)
        }
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
    }
}
