package tacita.app.volkov.one

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val Sage = Color(0xFF0C5B43)
private val PaleSage = Color(0xFFE7F4EE)
private val AppBackground = Color(0xFFFAFDFC)
private val MutedText = Color(0xFF56636A)

private object TacitaIcons {
    val VolumeUp = ImageVector.Builder("VolumeUp", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(3f, 9f)
            lineTo(7f, 9f)
            lineTo(12f, 4f)
            verticalLineTo(20f)
            lineTo(7f, 15f)
            horizontalLineTo(3f)
            close()
            moveTo(16f, 8f)
            curveTo(17.25f, 9.25f, 17.25f, 14.75f, 16f, 16f)
            lineTo(17.4f, 17.4f)
            curveTo(20.7f, 14.1f, 20.7f, 9.9f, 17.4f, 6.6f)
            close()
        }
    }.build()

    val Bluetooth = ImageVector.Builder("Bluetooth", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            lineTo(17.5f, 7.5f)
            lineTo(13f, 12f)
            lineTo(17.5f, 16.5f)
            lineTo(12f, 22f)
            verticalLineTo(14.8f)
            lineTo(7.5f, 19.3f)
            lineTo(6.1f, 17.9f)
            lineTo(11.2f, 12.8f)
            lineTo(6.1f, 7.7f)
            lineTo(7.5f, 6.3f)
            lineTo(12f, 10.8f)
            close()
            moveTo(14f, 6.8f)
            verticalLineTo(10.2f)
            lineTo(15.7f, 8.5f)
            close()
            moveTo(14f, 13.8f)
            verticalLineTo(17.2f)
            lineTo(15.7f, 15.5f)
            close()
        }
    }.build()
}

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { startMonitoring() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationsThenStart()
        setContent {
            TacitaTheme {
                SettingsScreen(onMonitoringChanged = ::updateMonitoring)
            }
        }
    }

    private fun updateMonitoring(enabled: Boolean) {
        if (enabled) requestNotificationsThenStart() else stopService(Intent(this, LockMonitorService::class.java))
    }

    private fun requestNotificationsThenStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startMonitoring()
        }
    }

    private fun startMonitoring() {
        ContextCompat.startForegroundService(this, Intent(this, LockMonitorService::class.java))
    }
}

@Composable
private fun TacitaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Sage,
            onPrimary = Color.White,
            primaryContainer = PaleSage,
            onPrimaryContainer = Sage,
            background = AppBackground,
            onBackground = Color(0xFF172126),
            surface = Color.White,
            onSurface = Color(0xFF172126),
        ),
        content = content,
    )
}

@Composable
private fun SettingsScreen(onMonitoringChanged: (Boolean) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings by SettingsRepository.settings(context).collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val audioManager = context.getSystemService(AudioManager::class.java)
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val targetPercent = if (maxVolume == 0) 0 else settings.targetVolume * 100 / maxVolume

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tacita", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("Lower media volume when your phone locks", color = MutedText, fontSize = 18.sp)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
            }

            FeatureCard(containerColor = PaleSage) {
                SettingIcon(Icons.Default.Lock)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable on lock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Automatically lower media volume when your phone locks.", color = MutedText)
                }
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = { enabled ->
                        scope.launch { SettingsRepository.updateEnabled(context, enabled) }
                        onMonitoringChanged(enabled)
                    },
                    modifier = Modifier.semantics { stateDescription = if (settings.enabled) "On" else "Off" },
                )
            }

            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingIcon(TacitaIcons.VolumeUp)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Target volume", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Set the media volume level to use when your phone locks.", color = MutedText)
                        }
                        Text("$targetPercent%", color = Sage, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = targetPercent.toFloat(),
                        onValueChange = { percent ->
                            val volume = (percent / 100f * maxVolume).roundToInt()
                            scope.launch { SettingsRepository.updateTargetVolume(context, volume) }
                        },
                        valueRange = 0f..100f,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0%", color = MutedText)
                        Text("100%", color = MutedText)
                    }
                }
            }

            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingIcon(TacitaIcons.Bluetooth)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Apply when Bluetooth audio is connected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Also lower volume when a Bluetooth audio device is connected.", color = MutedText)
                        }
                        Switch(
                            checked = settings.includeBluetooth,
                            onCheckedChange = { checked -> scope.launch { SettingsRepository.updateIncludeBluetooth(context, checked) } },
                            modifier = Modifier.semantics { stateDescription = if (settings.includeBluetooth) "On" else "Off" },
                        )
                    }
                    InfoPanel()
                }
            }

            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("How it works", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    HowItWorksRow(Icons.Default.PlayArrow, "If media is playing", "Do nothing")
                    HowItWorksRow(TacitaIcons.Bluetooth, "If Bluetooth audio is connected", "Do nothing by default")
                    HowItWorksRow(Icons.Default.Lock, "When phone locks and playback is idle", "Lower volume to selected level")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("✦", color = Sage, fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Text("A quieter phone for a calmer you.", color = MutedText)
            }
        }
    }
}

@Composable
private fun FeatureCard(containerColor: Color, content: @Composable RowScope.() -> Unit) {
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun SettingIcon(icon: ImageVector) {
    Box(
        modifier = Modifier.size(64.dp).background(PaleSage, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Sage, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun InfoPanel() {
    Row(
        modifier = Modifier.fillMaxWidth().background(PaleSage, RoundedCornerShape(18.dp)).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = Sage)
        Text(
            "By default, Tacita ignores Bluetooth audio devices, so it won't change the volume when you're using headphones or a car audio system.",
            color = MutedText,
        )
    }
}

@Composable
private fun HowItWorksRow(icon: ImageVector, title: String, outcome: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(PaleSage, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Sage)
        }
        Spacer(Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(outcome, modifier = Modifier.width(112.dp), color = MutedText, textAlign = TextAlign.End)
    }
}
