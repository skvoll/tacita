package one.volkov.apps.tacita

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private object TacitaIcons {
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
        startTacitaIfEnabled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        startTacitaIfEnabled()
        setContent { TacitaTheme { SettingsScreen(onEnabledChange = ::updateTacitaState) } }
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

@Composable
private fun TacitaTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(onEnabledChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val settings by SettingsRepository.settings(context).collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val sliderMaximum = if (settings.allowVolumeAbove20) 100 else 20
    val targetPercent = settings.targetPercent.coerceIn(0, sliderMaximum)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { CollapsibleHeader(settings.enabled, scrollBehavior) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()).padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ToggleCard(
                checked = settings.enabled,
                onCheckedChange = onEnabledChange,
                icon = Icons.Default.Lock,
                title = "Lower volume when screen turns off",
                supportingText = "Automatically lower media volume when your screen turns off.",
                emphasized = true,
            )
            TargetVolumeCard(
                targetPercent = targetPercent,
                extendedRange = settings.allowVolumeAbove20,
                onValueChange = { percent ->
                    scope.launch { SettingsRepository.updateTargetPercent(context, percent.roundToInt()) }
                },
            )
            ToggleCard(
                checked = settings.allowVolumeAbove20,
                onCheckedChange = { allowed ->
                    scope.launch { SettingsRepository.updateAllowVolumeAbove20(context, allowed) }
                },
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = "Allow volume above 20%",
                supportingText = "Allows Tacita to use higher target volume levels.",
            )
            BluetoothCard(settings.includeBluetooth) { enabled ->
                scope.launch { SettingsRepository.updateIncludeBluetooth(context, enabled) }
            }
            HowItWorksCard(settings.includeBluetooth)
            Footer()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CollapsibleHeader(enabled: Boolean, scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior) {
    val expandedContentAlpha = 1f - scrollBehavior.state.collapsedFraction
    val headerIconSize = 48.dp + (28.dp * expandedContentAlpha)
    val titleSize = (22f + 10f * expandedContentAlpha).sp
    LargeTopAppBar(
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AndroidView(
                        factory = { viewContext -> ImageView(viewContext).apply { setImageResource(R.drawable.tacita_header_icon) } },
                        modifier = Modifier.size(headerIconSize),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text("Tacita", fontSize = titleSize, lineHeight = titleSize, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.graphicsLayer { alpha = expandedContentAlpha }) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Lower media volume when your screen turns off.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (enabled) ActiveStatusChip()
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        expandedHeight = 220.dp,
    )
}

@Composable
private fun ActiveStatusChip() {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                "Tacita is active",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ToggleCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    title: String,
    supportingText: String,
    emphasized: Boolean = false,
) {
    val containerColor by animateColorAsState(
        if (emphasized && checked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "settingCardColor",
    )
    SettingCard(
        containerColor = containerColor,
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            SettingIcon(icon)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(supportingText, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.semantics { stateDescription = if (checked) "On" else "Off" },
            )
        }
    }
}

@Composable
private fun TargetVolumeCard(targetPercent: Int, extendedRange: Boolean, onValueChange: (Float) -> Unit) {
    val maximum = if (extendedRange) 100 else 20
    val steps = if (extendedRange) 19 else 3
    var livePercent by remember { mutableFloatStateOf(targetPercent.toFloat()) }
    LaunchedEffect(targetPercent, maximum) { livePercent = targetPercent.toFloat() }
    val displayedPercent = livePercent.roundToInt()
    val volumeIcon = when {
        displayedPercent == 0 -> Icons.AutoMirrored.Filled.VolumeOff
        displayedPercent <= 10 -> Icons.AutoMirrored.Filled.VolumeDown
        else -> Icons.AutoMirrored.Filled.VolumeUp
    }
    SettingCard(MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            SettingIcon(volumeIcon)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Target volume", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Set the media volume level to use when your screen turns off.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.width(52.dp), contentAlignment = Alignment.CenterEnd) {
                Text("$displayedPercent%", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }
        }
        Spacer(Modifier.height(12.dp))
        Slider(
            value = livePercent,
            onValueChange = { value ->
                livePercent = value
                onValueChange(value)
            },
            valueRange = 0f..maximum.toFloat(),
            steps = steps,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0%", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Text(if (extendedRange) "100%" else "20%", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
        if (extendedRange) {
            Text("20% is Tacita’s recommended limit.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BluetoothCard(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val containerColor by animateColorAsState(MaterialTheme.colorScheme.surfaceContainerLow, label = "bluetoothCardColor")
    SettingCard(
        containerColor = containerColor,
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            SettingIcon(TacitaIcons.Bluetooth)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Apply to Bluetooth audio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Also lower media volume when a Bluetooth audio device is connected.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.semantics { stateDescription = if (checked) "On" else "Off" })
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Off by default to avoid changing headphone or car audio volume.", color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HowItWorksCard(includeBluetooth: Boolean) {
    SettingCard(MaterialTheme.colorScheme.surfaceContainerLow) {
        Text("How it works", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        HowItWorksRow(Icons.Default.PlayArrow, "If media is playing", "Do nothing")
        HowItWorksRow(TacitaIcons.Bluetooth, "If Bluetooth audio is connected", if (includeBluetooth) "Apply selected level" else "Ignore by default")
        HowItWorksRow(Icons.Default.Lock, "When screen turns off and playback is idle", "Lower volume to selected level")
    }
}

@Composable
private fun Footer() = Text(
    "Quiet when it matters.", modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), textAlign = TextAlign.Center,
    color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium,
)

@Composable
private fun SettingCard(containerColor: Color, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = Card(
    shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = containerColor), modifier = modifier.fillMaxWidth(),
) { Column(modifier = Modifier.padding(16.dp), content = content) }

@Composable
private fun SettingIcon(icon: ImageVector) = Box(
    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center,
) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp)) }

@Composable
private fun HowItWorksRow(icon: ImageVector, condition: String, result: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        val compactLayout = maxWidth < 400.dp
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            if (compactLayout) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(condition, style = MaterialTheme.typography.bodyMedium)
                    Text(result, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text(condition, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(12.dp))
                Text(result, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
            }
        }
    }
}
