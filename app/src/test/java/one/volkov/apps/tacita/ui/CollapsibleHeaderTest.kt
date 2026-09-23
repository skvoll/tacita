package one.volkov.apps.tacita.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.io.File
import one.volkov.apps.tacita.data.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CollapsibleHeaderTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun headerKeepsOneUnclippedTitleThroughoutCollapseAndExpansion() {
        checkHeaderGeometry(fontScale = 1f)
    }

    @Test
    fun headerFitsNarrowScreenWithLargerText() {
        checkHeaderGeometry(fontScale = 1.15f)
    }

    @Test
    fun headerGeometryWorksInDarkTheme() {
        checkHeaderGeometry(fontScale = 1.15f, darkTheme = true)
    }

    private fun checkHeaderGeometry(fontScale: Float, darkTheme: Boolean = false) {
        lateinit var behavior: TopAppBarScrollBehavior
        val enabled = mutableStateOf(true)
        compose.setContent {
            MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                    behavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                    Box(Modifier.width(360.dp).consumeWindowInsets(WindowInsets.systemBars)) {
                        CollapsibleHeader(enabled.value, behavior, Modifier.testTag("header"))
                    }
                }
            }
        }

        val expandedBounds = compose.onNodeWithTag("header").fetchSemanticsNode().boundsInRoot
        val titleBounds = compose.onNodeWithText("Tacita").fetchSemanticsNode().boundsInRoot
        val subtitleBounds = compose.onNodeWithText(SUBTITLE).fetchSemanticsNode().boundsInRoot
        val badgeBounds = compose.onNodeWithText("Tacita is active").fetchSemanticsNode().boundsInRoot
        assertEquals(titleBounds.left, subtitleBounds.left, 1f)
        assertTrue(badgeBounds.top >= subtitleBounds.bottom)
        assertTrue(badgeBounds.bottom <= expandedBounds.bottom)
        assertTrue("Expanded header should fit its content", expandedBounds.height < with(compose.density) { 220.dp.toPx() })

        var previousHeight = expandedBounds.height
        for (step in 0..10) {
            compose.runOnIdle { behavior.state.heightOffset = behavior.state.heightOffsetLimit * step / 10f }
            val header = compose.onNodeWithTag("header").fetchSemanticsNode().boundsInRoot
            val title = compose.onNodeWithText("Tacita").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            compose.onAllNodesWithText("Tacita").assertCountEquals(1)
            compose.onAllNodesWithTag("statusDot").assertCountEquals(1)
            val dot = compose.onNodeWithTag("statusDot").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue("Status dot must stay inside the header", dot.top >= header.top && dot.bottom <= header.bottom)
            assertTrue("Status dot must fit horizontally", dot.left >= header.left && dot.right <= header.right)
            if (step == 10) {
                assertTrue("Collapsed status dot follows the title", dot.left > title.right)
                assertEquals(title.center.y, dot.center.y, 1f)
            }
            assertTrue("Header height must decrease monotonically", header.height <= previousHeight)
            assertTrue("Title must stay inside the header", title.top >= header.top && title.bottom <= header.bottom)
            assertTrue("Title must fit horizontally", title.left >= header.left && title.right <= header.right)
            if (step == 0 || step == 5 || step == 10) {
                val theme = if (darkTheme) "dark" else "light"
                val preview = File("build/reports/header-preview/$theme-font-$fontScale-step-$step.png")
                preview.parentFile?.mkdirs()
                preview.outputStream().use { output ->
                    compose.onNodeWithTag("header").captureToImage().asAndroidBitmap()
                        .compress(Bitmap.CompressFormat.PNG, 100, output)
                }
            }
            previousHeight = header.height
        }
        compose.onNodeWithTag("header").assertHeightIsEqualTo(64.dp)
        compose.onNodeWithText(SUBTITLE).assertDoesNotExist()
        compose.onNodeWithText("Tacita is active").assertDoesNotExist()

        // Changing status while collapsed must not expand the toolbar or leave an invalid offset.
        compose.runOnIdle { enabled.value = false }
        compose.onNodeWithTag("statusDot").assertIsDisplayed()
        compose.onNodeWithTag("header").assertHeightIsEqualTo(64.dp)
        compose.runOnIdle { assertEquals(1f, behavior.state.collapsedFraction, 0.001f) }
        for (step in 9 downTo 0) {
            compose.runOnIdle { behavior.state.heightOffset = behavior.state.heightOffsetLimit * step / 10f }
            val header = compose.onNodeWithTag("header").fetchSemanticsNode().boundsInRoot
            val title = compose.onNodeWithText("Tacita").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            compose.onAllNodesWithTag("statusDot").assertCountEquals(1)
            compose.onNodeWithTag("statusDot").assertIsDisplayed()
            assertTrue(header.height >= previousHeight)
            assertTrue(title.top >= header.top && title.bottom <= header.bottom)
            previousHeight = header.height
        }
        compose.onNodeWithText(SUBTITLE).assertIsDisplayed()
        compose.onNodeWithText("Tacita is active").assertDoesNotExist()
        compose.onNodeWithText("Disabled").assertIsDisplayed()
        val disabledDot = compose.onNodeWithTag("statusDot").captureToImage().asAndroidBitmap()
        val colors = if (darkTheme) darkColorScheme() else lightColorScheme()
        assertEquals(colors.onSurfaceVariant.toArgb(), disabledDot.getPixel(disabledDot.width / 2, disabledDot.height / 2))

        compose.onNodeWithTag("header").performTouchInput { swipeUp() }
        compose.onNodeWithTag("header").assertHeightIsEqualTo(64.dp)
        compose.onNodeWithTag("header").performTouchInput { swipeDown() }
        compose.onNodeWithText(SUBTITLE).assertIsDisplayed()
        compose.runOnIdle { assertEquals(0f, behavior.state.collapsedFraction, 0.001f) }
    }

    @Test
    fun scrollingSettingsCollapsesAndReexpandsHeader() {
        compose.setContent {
            MaterialTheme {
                SettingsScreen(
                    settings = AppSettings(enabled = true),
                    onEnabledChange = {},
                    onTargetPercentChange = {},
                    onAllowVolumeAbove20Change = {},
                    onCheckOnScreenOnChange = {},
                    onIncludeBluetoothChange = {},
                )
            }
        }
        compose.onNodeWithText("Tacita is active").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        compose.onNodeWithText("Tacita").assertIsDisplayed()
        compose.onNodeWithText("Tacita is active").assertDoesNotExist()
        repeat(3) { compose.onNode(hasScrollAction()).performTouchInput { swipeDown() } }
        compose.onNodeWithText("Tacita is active").assertIsDisplayed()
        compose.onAllNodesWithText("Tacita").assertCountEquals(1)
    }

    private companion object {
        const val SUBTITLE = "Lower media volume when your screen turns off."
    }
}
