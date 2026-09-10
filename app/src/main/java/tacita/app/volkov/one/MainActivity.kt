package tacita.app.volkov.one

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val Sage = Color(0xFF006B50)
private val PaleSage = Color(0xFFE4F4EE)
private val AppBackground = Color(0xFFFAFDFC)
private val MutedText = Color(0xFF53626A)

private object TacitaIcons {
    val VolumeUp = ImageVector.Builder("VolumeUp", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(3f, 9f); lineTo(7f, 9f); lineTo(12f, 4f); verticalLineTo(20f)
            lineTo(7f, 15f); horizontalLineTo(3f); close()
            moveTo(16f, 8f); curveTo(17.25f, 9.25f, 17.25f, 14.75f, 16f, 16f)
            lineTo(17.4f, 17.4f); curveTo(20.7f, 14.1f, 20.7f, 9.9f, 17.4f, 6.6f); close()
        }
    }.build()
    val Bluetooth = ImageVector.Builder("Bluetooth", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f); lineTo(17.5f, 7.5f); lineTo(13f, 12f); lineTo(17.5f, 16.5f)
            lineTo(12f, 22f); verticalLineTo(14.8f); lineTo(7.5f, 19.3f); lineTo(6.1f, 17.9f)
            lineTo(11.2f, 12.8f); lineTo(6.1f, 7.7f); lineTo(7.5f, 6.3f); lineTo(12f, 10.8f); close()
            moveTo(14f, 6.8f); verticalLineTo(10.2f); lineTo(15.7f, 8.5f); close()
            moveTo(14f, 13.8f); verticalLineTo(17.2f); lineTo(15.7f, 15.5f); close()
        }
    }.build()
}

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        startMonitoringIfEnabled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startMonitoringIfEnabled()
        setContent { TacitaTheme { SettingsScreen(onMonitoringChanged = ::updateMonitoring) } }
    }

    private fun updateMonitoring(enabled: Boolean) = lifecycleScope.launch {
        if (enabled) {
            SettingsRepository.updateEnabled(this@MainActivity, true)
            requestNotificationsThenStart()
        } else {
            stopService(Intent(this@MainActivity, LockMonitorService::class.java))
            SettingsRepository.updateEnabled(this@MainActivity, false)
        }
    }

    private fun startMonitoringIfEnabled() = lifecycleScope.launch {
        if (SettingsRepository.settings(this@MainActivity).first().enabled) requestNotificationsThenStart()
    }

    private fun requestNotificationsThenStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else startMonitoring()
    }

    private fun startMonitoring() = ContextCompat.startForegroundService(this, Intent(this, LockMonitorService::class.java))
}

@Composable
private fun TacitaTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = lightColorScheme(
        primary = Sage, onPrimary = Color.White, primaryContainer = PaleSage, onPrimaryContainer = Sage,
        background = AppBackground, onBackground = Color(0xFF172126), surface = Color.White, onSurface = Color(0xFF172126),
    ),
    content = content,
)

@Composable
private fun SettingsScreen(onMonitoringChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val settings by SettingsRepository.settings(context).collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val audioManager = context.getSystemService(AudioManager::class.java)
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val targetVolume = settings.targetVolume.coerceIn(0, maxVolume)
    val targetPercent = if (maxVolume == 0) 0 else targetVolume * 100 / maxVolume

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header()
            MonitoringCard(settings.enabled, onMonitoringChanged)
            TargetVolumeCard(targetPercent, maxVolume) { percent ->
                scope.launch { SettingsRepository.updateTargetVolume(context, (percent / 100f * maxVolume).roundToInt()) }
            }
            BluetoothCard(settings.includeBluetooth) { enabled ->
                scope.launch { SettingsRepository.updateIncludeBluetooth(context, enabled) }
            }
            HowItWorksCard(settings.includeBluetooth)
            Footer()
        }
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        AndroidView(
            factory = { viewContext -> ImageView(viewContext).apply { setImageResource(R.mipmap.ic_launcher) } },
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Tacita", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Lower media volume when your screen turns off.", color = MutedText, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MonitoringCard(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    val containerColor by animateColorAsState(
        if (enabled) PaleSage else MaterialTheme.colorScheme.surface,
        label = "monitoringCardColor",
    )
    SettingCard(containerColor) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            SettingIcon(Icons.Default.Lock)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Lower volume when screen turns off", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Automatically lower media volume when your screen turns off.", color = MutedText, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = enabled, onCheckedChange = onEnabledChange, modifier = Modifier.semantics { stateDescription = if (enabled) "On" else "Off" })
        }
    }
}

@Composable
private fun TargetVolumeCard(targetPercent: Int, maxVolume: Int, onValueChange: (Float) -> Unit) {
    SettingCard(MaterialTheme.colorScheme.surface) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            SettingIcon(TacitaIcons.VolumeUp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Target volume", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Set the media volume level to use when your screen turns off.", color = MutedText, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(12.dp))
            Text("$targetPercent%", color = Sage, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Slider(value = targetPercent.toFloat(), onValueChange = onValueChange, valueRange = 0f..100f, enabled = maxVolume > 0, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0%", color = MutedText, style = MaterialTheme.typography.labelMedium)
            Text("100%", color = MutedText, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun BluetoothCard(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    SettingCard(MaterialTheme.colorScheme.surface) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            SettingIcon(TacitaIcons.Bluetooth)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Apply to Bluetooth audio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Also lower media volume when a Bluetooth audio device is connected.", color = MutedText, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = enabled, onCheckedChange = onEnabledChange, modifier = Modifier.semantics { stateDescription = if (enabled) "On" else "Off" })
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().background(PaleSage, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Sage, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Off by default to avoid changing headphone or car audio volume.", color = MutedText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HowItWorksCard(includeBluetooth: Boolean) {
    SettingCard(MaterialTheme.colorScheme.surface) {
        Text("How it works", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        HowItWorksRow(Icons.Default.PlayArrow, "If media is playing", "Do nothing")
        HowItWorksRow(TacitaIcons.Bluetooth, "If Bluetooth audio is connected", if (includeBluetooth) "Apply selected level" else "Ignore by default")
        HowItWorksRow(Icons.Default.Lock, "When screen turns off and playback is idle", "Lower volume to selected level")
    }
}

@Composable
private fun Footer() = Text(
    "Quiet when it matters.",
    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    textAlign = TextAlign.Center,
    color = Sage,
    style = MaterialTheme.typography.labelLarge,
    fontWeight = FontWeight.Medium,
)

@Composable
private fun SettingCard(containerColor: Color, content: @Composable ColumnScope.() -> Unit) = Card(
    shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = containerColor), modifier = Modifier.fillMaxWidth(),
) {
    Column(modifier = Modifier.padding(16.dp), content = content)
}

@Composable
private fun SettingIcon(icon: ImageVector) = Box(
    modifier = Modifier.size(48.dp).background(PaleSage, CircleShape), contentAlignment = Alignment.Center,
) { Icon(icon, contentDescription = null, tint = Sage, modifier = Modifier.size(24.dp)) }

@Composable
private fun HowItWorksRow(icon: ImageVector, condition: String, result: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        val compactLayout = maxWidth < 400.dp
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(40.dp).background(PaleSage, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Sage, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            if (compactLayout) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(condition, style = MaterialTheme.typography.bodyMedium)
                    Text(result, color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text(condition, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(12.dp))
                Text(result, color = MutedText, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
            }
        }
    }
}
