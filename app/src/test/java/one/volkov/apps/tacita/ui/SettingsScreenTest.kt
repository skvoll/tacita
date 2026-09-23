package one.volkov.apps.tacita.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import one.volkov.apps.tacita.data.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun settingsHideWhenDisabledAndNestedSwitchesKeepIndependentState() {
        val settings = mutableStateOf(AppSettings())
        compose.setContent {
            MaterialTheme {
                SettingsScreen(
                    settings = settings.value,
                    onEnabledChange = { settings.value = settings.value.copy(enabled = it) },
                    onTargetPercentChange = { settings.value = settings.value.copy(targetPercent = it) },
                    onAllowVolumeAbove20Change = { settings.value = settings.value.copy(allowVolumeAbove20 = it) },
                    onCheckOnScreenOnChange = { settings.value = settings.value.copy(checkOnScreenOn = it) },
                    onIncludeBluetoothChange = { settings.value = settings.value.copy(includeBluetooth = it) },
                )
            }
        }
        fun assertSettingsHidden() {
            listOf("Check when screen turns on", "Target volume", "Allow volume above 20%", "Apply to Bluetooth audio")
                .forEach { compose.onNodeWithText(it).assertDoesNotExist() }
            compose.onNodeWithText("How it works").assertIsDisplayed()
            compose.onNodeWithText("Start with the switch above").assertIsDisplayed()
        }

        assertSettingsHidden()
        compose.onNodeWithText(MAIN_TOGGLE).assertIsOff().performClick()
        compose.onNodeWithText("Start with the switch above").assertDoesNotExist()
        compose.onNodeWithText("Check when screen turns on").performScrollTo().assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText("On · Also lower volume when your screen turns on.").assertIsDisplayed()
        compose.onNodeWithText(MAIN_TOGGLE).assertIsOn()
        compose.onNodeWithText("Allow volume above 20%").performScrollTo().assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText("On · Choose a target up to 100%.").assertIsDisplayed()
        compose.onNodeWithText("100%").assertIsDisplayed()
        compose.onNodeWithText("Apply to Bluetooth audio").performScrollTo().assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText("On · Also lower volume on Bluetooth audio devices.").assertIsDisplayed()

        val enabledSettings = settings.value
        compose.onNodeWithText(MAIN_TOGGLE).performScrollTo().performClick().assertIsOff()
        assertSettingsHidden()
        compose.onNodeWithText(MAIN_TOGGLE).performClick().assertIsOn()
        compose.runOnIdle { assertEquals(enabledSettings, settings.value) }
        compose.onNodeWithText("Check when screen turns on").assertIsOn()
        compose.onNodeWithText("Allow volume above 20%").performScrollTo().assertIsOn().performClick().assertIsOff()
        compose.onNodeWithText("Off · Target volume is limited to 20%.").assertIsDisplayed()
        compose.onNodeWithText("100%").assertDoesNotExist()
    }

    private companion object {
        const val MAIN_TOGGLE = "Lower volume when screen turns off"
    }
}
