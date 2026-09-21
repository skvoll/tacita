package one.volkov.apps.tacita

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import one.volkov.apps.tacita.data.AppSettings
import one.volkov.apps.tacita.data.SettingsRepository
import one.volkov.apps.tacita.service.LockMonitorService
import one.volkov.apps.tacita.ui.SettingsScreen
import one.volkov.apps.tacita.ui.TacitaTheme

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        startTacitaIfEnabled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        startTacitaIfEnabled()
        setContent {
            TacitaTheme {
                val settings by SettingsRepository.settings(this@MainActivity)
                    .collectAsState(initial = AppSettings())
                SettingsScreen(
                    settings = settings,
                    onEnabledChange = ::updateTacitaState,
                    onTargetPercentChange = ::updateTargetPercent,
                    onAllowVolumeAbove20Change = ::updateAllowVolumeAbove20,
                    onCheckOnScreenOnChange = ::updateCheckOnScreenOn,
                    onIncludeBluetoothChange = ::updateIncludeBluetooth,
                )
            }
        }
    }

    private fun updateTacitaState(enabled: Boolean) = lifecycleScope.launch {
        if (enabled) {
            SettingsRepository.updateEnabled(this@MainActivity, true)
            requestNotificationsThenStart()
        } else {
            stopService(Intent(this@MainActivity, LockMonitorService::class.java))
            SettingsRepository.updateEnabled(this@MainActivity, false)
        }
    }

    private fun updateTargetPercent(targetPercent: Int) = lifecycleScope.launch {
        SettingsRepository.updateTargetPercent(this@MainActivity, targetPercent)
    }

    private fun updateAllowVolumeAbove20(allowed: Boolean) = lifecycleScope.launch {
        SettingsRepository.updateAllowVolumeAbove20(this@MainActivity, allowed)
    }

    private fun updateCheckOnScreenOn(enabled: Boolean) = lifecycleScope.launch {
        SettingsRepository.updateCheckOnScreenOn(this@MainActivity, enabled)
    }

    private fun updateIncludeBluetooth(enabled: Boolean) = lifecycleScope.launch {
        SettingsRepository.updateIncludeBluetooth(this@MainActivity, enabled)
    }

    private fun startTacitaIfEnabled() = lifecycleScope.launch {
        if (SettingsRepository.settings(this@MainActivity).first().enabled) requestNotificationsThenStart()
    }

    private fun requestNotificationsThenStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else startTacita()
    }

    private fun startTacita() = ContextCompat.startForegroundService(this, Intent(this, LockMonitorService::class.java))
}
