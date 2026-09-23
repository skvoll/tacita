package one.volkov.apps.tacita.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import one.volkov.apps.tacita.data.AppSettings

@Composable
fun TacitaTheme(content: @Composable () -> Unit) {
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
fun SettingsScreen(
    settings: AppSettings,
    onEnabledChange: (Boolean) -> Unit,
    onTargetPercentChange: (Int) -> Unit,
    onAllowVolumeAbove20Change: (Boolean) -> Unit,
    onCheckOnScreenOnChange: (Boolean) -> Unit,
    onIncludeBluetoothChange: (Boolean) -> Unit,
) {
    val sliderMaximum = if (settings.allowVolumeAbove20) 100 else 20
    val targetPercent = settings.targetPercent.coerceIn(0, sliderMaximum)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { CollapsibleHeader(settings.enabled, scrollBehavior) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .consumeWindowInsets(innerPadding).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val mainCardColor by animateColorAsState(
                if (settings.enabled) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow,
                label = "mainCardColor",
            )
            SettingCard(mainCardColor) {
                ToggleRow(
                    checked = settings.enabled,
                    onCheckedChange = onEnabledChange,
                    icon = if (settings.enabled) Icons.Default.PhonelinkLock else Icons.Default.PhoneAndroid,
                    title = "Lower volume when screen turns off",
                    supportingText = if (settings.enabled) "On · Automatically lower media volume when your screen turns off."
                    else "Off · Your media volume stays unchanged.",
                )
                if (settings.enabled) {
                    ToggleSubsection(
                        checked = settings.checkOnScreenOn,
                        onCheckedChange = onCheckOnScreenOnChange,
                        icon = if (settings.checkOnScreenOn) PhonelinkUnlock else Icons.Default.PhoneAndroid,
                        title = "Check when screen turns on",
                        supportingText = if (settings.checkOnScreenOn) "On · Also lower volume when your screen turns on."
                        else "Off · Only check when your screen turns off.",
                    )
                }
            }
            if (settings.enabled) {
                TargetVolumeCard(
                    targetPercent = targetPercent,
                    extendedRange = settings.allowVolumeAbove20,
                    onValueChange = onTargetPercentChange,
                    onExtendedRangeChange = onAllowVolumeAbove20Change,
                )
                BluetoothCard(settings.includeBluetooth, onIncludeBluetoothChange)
            }
            HowItWorksCard(settings)
            Footer()
        }
    }
}

@Composable
private fun ToggleRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    title: String,
    supportingText: String,
    compact: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { stateDescription = if (checked) "On" else "Off" }
            .heightIn(min = 48.dp),
    ) {
        if (compact) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        } else {
            SettingIcon(icon)
        }
        Spacer(Modifier.width(if (compact) 12.dp else 16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(supportingText, color = MaterialTheme.colorScheme.onSurfaceVariant, style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ToggleSubsection(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    title: String,
    supportingText: String,
) {
    Column(Modifier.padding(start = 16.dp)) {
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        ToggleRow(checked, onCheckedChange, icon, title, supportingText, compact = true)
    }
}

@Composable
private fun TargetVolumeCard(
    targetPercent: Int,
    extendedRange: Boolean,
    onValueChange: (Int) -> Unit,
    onExtendedRangeChange: (Boolean) -> Unit,
) {
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
                Text(if (displayedPercent == 0) "Mute media when checked." else "Lower media volume to $displayedPercent% when checked. Never raises volume.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
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
                onValueChange(value.roundToInt())
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
        ToggleSubsection(
            checked = extendedRange,
            onCheckedChange = onExtendedRangeChange,
            icon = if (extendedRange) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
            title = "Allow volume above 20%",
            supportingText = if (extendedRange) "On · Choose a target up to 100%." else "Off · Target volume is limited to 20%.",
        )
    }
}

@Composable
private fun BluetoothCard(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SettingCard(MaterialTheme.colorScheme.surfaceContainerLow) {
        ToggleRow(
            checked = checked,
            onCheckedChange = onCheckedChange,
            icon = if (checked) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
            title = "Apply to Bluetooth audio",
            supportingText = if (checked) "On · Also lower volume on Bluetooth audio devices."
            else "Off · Leave Bluetooth audio volume unchanged.",
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(if (checked) "The target also applies to headphone and car audio volume." else "Headphone and car audio volume are left unchanged.", color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HowItWorksCard(settings: AppSettings) {
    SettingCard(MaterialTheme.colorScheme.surfaceContainerLow) {
        Text("How it works", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        if (settings.enabled) {
            HowItWorksRow(Icons.Default.PlayArrow, "If media is playing", "Do nothing")
            HowItWorksRow(if (settings.includeBluetooth) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled, "If Bluetooth audio is connected", if (settings.includeBluetooth) "Lower to ${settings.targetPercent}% if above target" else "Leave volume unchanged")
            HowItWorksRow(Icons.Default.PhonelinkLock, "When screen turns off", "Lower to ${settings.targetPercent}% if above target; check again after 1 minute")
            HowItWorksRow(if (settings.checkOnScreenOn) PhonelinkUnlock else Icons.Default.PhoneAndroid, "When screen turns on", if (settings.checkOnScreenOn) "Also check and lower to ${settings.targetPercent}%" else "Leave volume unchanged")
        } else {
            HowItWorksRow(Icons.Default.Info, "Start with the switch above", "Tacita is disabled. Turn it on, then choose how quiet you want your media volume to be.")
            HowItWorksRow(Icons.Default.PhonelinkLock, "Put your phone away", "When your screen turns off, Tacita lowers media volume to your chosen level and checks again after a minute. It never turns the volume up.")
            HowItWorksRow(Icons.Default.PlayArrow, "Keep listening", "Music and videos already playing keep their volume. Your ringtone and alarms stay unchanged too.")
            HowItWorksRow(Icons.Default.Bluetooth, "Choose what happens with Bluetooth", "Headphones and car audio are left alone by default. You can choose to lower their volume too.")
            HowItWorksRow(PhonelinkUnlock, "Check when you wake your phone", "You can also have Tacita lower media volume when your screen turns on.")
        }
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
