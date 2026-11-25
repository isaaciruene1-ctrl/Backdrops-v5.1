package com.example

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.catalog.WallpaperCatalog
import com.example.data.model.ImageQualityPref
import com.example.data.model.StudioWidgetItem
import com.example.data.model.StudioWidgetType
import com.example.data.model.UiStyleMode
import com.example.data.model.UserPreferencesDoc
import com.example.engine.depth.ClockHorizontalAlign
import com.example.engine.depth.DepthDetectionResult
import com.example.engine.depth.DepthSubjectCategory
import com.example.ui.components.ClockDesignConfig
import com.example.ui.components.ClockGripTarget
import com.example.ui.components.ClockSpatialMath
import com.example.ui.components.ClockWidgetTelemetry
import com.example.ui.components.HyperOsClockFinish
import com.example.ui.components.StudioDestination
import com.example.ui.components.StudioWidgetsLockStrip
import com.example.ui.screens.DepthStudioScreen
import com.example.ui.screens.LockScreenModeScreen
import com.example.ui.screens.SettingsPanelScreen
import com.example.ui.theme.BackdropTheme
import com.example.ui.viewmodel.DepthStudioUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [31]) // Verify Android 12 (API 31) stability explicitly alongside Android 16 tests
class Backdrop14VerificationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun makeStageBitmap(w: Int = 120, h: Int = 200): Bitmap =
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.rgb(26, 62, 132))
            val canvas = android.graphics.Canvas(this)
            val paint = android.graphics.Paint().apply { color = AndroidColor.rgb(225, 118, 68) }
            canvas.drawRect(w * 0.25f, h * 0.4f, w * 0.75f, h.toFloat(), paint)
        }

    // 1. Android 12 (API 31): Sustained Depth Studio + M3 Mode allows Liquid Glass toggle & Glass Settings
    @Test
    fun android12_depthStudioAllowsLiquidGlassToggleInMaterial3ModeAndStaysStable() {
        val wp = WallpaperCatalog.allWallpapers.first()
        val bmp = makeStageBitmap()
        val ready = DepthStudioUiState.Ready(
            key = wp.id,
            fullBitmap = bmp,
            result = DepthDetectionResult(
                subjectLabel = "Summit",
                subjectCategory = DepthSubjectCategory.SUMMIT_SILHOUETTE,
                isDepthEligible = true,
                depthQualityScore = 88,
                cutoutBitmap = bmp,
                recommendedClockTopFraction = 0.16f,
                recommendedAlign = ClockHorizontalAlign.CENTER,
                recommendedClockColorHex = "#FFFFFFFF",
                subjectTopFraction = 0.35f,
                subjectPeakXFraction = 0.5f,
                foregroundCoverageFraction = 0.42f,
                processingTimeMs = 12L
            )
        )
        var currentClock by mutableStateOf(ClockDesignConfig(finish = HyperOsClockFinish.SOLID))
        var activeDest by mutableStateOf(StudioDestination.DEPTH)

        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.MATERIAL_3, reducedMotionEnabled = true) {
                DepthStudioScreen(
                    initialWallpaper = wp,
                    depthState = ready,
                    clockConfig = currentClock,
                    canUndo = false,
                    canRedo = false,
                    isApplying = false,
                    onLoadWallpaper = { _, _ -> },
                    onPickCustomPhoto = { _, _ -> },
                    onBeginClockGesture = {},
                    onPreviewClockConfig = { currentClock = it },
                    onEndClockGesture = {},
                    onCommitClockConfig = { transform -> currentClock = transform(currentClock) },
                    onUndoClock = {},
                    onRedoClock = {},
                    onResetProportions = {},
                    onCenterClock = {},
                    onBack = {},
                    onApplyWallpaper = { _, _, _ -> },
                    onApplyCustomBitmapWallpaper = { _, _, _ -> },
                    onNavigateStudioDestination = { activeDest = it }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(3500L)
        composeRule.waitForIdle()

        // Navigate to Clock -> Design & Materials and switch to Liquid Glass in Material 3 mode
        composeRule.onNodeWithTag("depth_tab_Clock").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("clock_subpill_TYPE_FINISH").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("clock_finish_strip")
            .performScrollTo()
            .performScrollToNode(hasTestTag("clock_finish_BLUR_GLASS"))
        composeRule.onNodeWithTag("clock_finish_BLUR_GLASS").performClick()
        composeRule.waitForIdle()
        assertEquals(HyperOsClockFinish.BLUR_GLASS, currentClock.finish)

        // Advance virtual clock several seconds in BLUR_GLASS software path on API 31 -> no crash & optics open by default
        composeRule.mainClock.advanceTimeBy(4000L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("glass_optics_drawer_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("glass_refraction_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("clock_weight_slider").performScrollTo().assertIsDisplayed()

        // Studio Switcher responds cleanly
        composeRule.onNodeWithTag("studio_switch_preview").performClick()
        composeRule.waitForIdle()
        assertEquals(StudioDestination.PREVIEW, activeDest)
    }

    // 2. Corner drag resizing does not double-scale ClockDesignConfig.scale and defaults scaleTextWithClock=true
    @Test
    fun clockSpatialCornerDragDoesNotDoubleScaleAndDefaultsScaleTextWithClock() {
        val startConfig = ClockDesignConfig()
        assertTrue(startConfig.scaleTextWithClock)
        assertFalse(startConfig.freeStretchEnabled)

        val viewport = IntSize(1080, 2400)
        val startRect = ClockSpatialMath.computeClockBoxRect(420f, 200f, viewport, startConfig)
        val updated = ClockSpatialMath.applyDragDelta(
            target = ClockGripTarget.CORNER_BR,
            startConfig = startConfig,
            startRect = startRect,
            totalDx = 120f,
            totalDy = 90f,
            viewportSize = viewport
        )
        assertEquals(1.0f, updated.scale, 0.0001f)
        assertTrue(updated.widthScale > 1.0f)
        assertTrue(updated.heightScale > 1.0f)
    }

    // 3. Studio Settings places AppearanceLayoutCard as Card #3 and collapses lower drawers
    @Test
    @Config(sdk = [36])
    fun settingsPanelHasAppearanceAsThirdCardWithNavStyleAndCollapsedSecondaryDrawers() {
        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.MATERIAL_3, reducedMotionEnabled = true) {
                SettingsPanelScreen(
                    preferences = UserPreferencesDoc(),
                    cacheBytes = 1024L,
                    onBack = {},
                    onUiStyleChange = {},
                    onThemeModeChange = {},
                    onAmoledDarkChange = {},
                    onHomeCategoryChipsChange = {},
                    onImageQualityChange = {},
                    onWifiOnlyChange = {},
                    onRefreshCacheSize = {},
                    onClearCache = {}
                )
            }
        }
        composeRule.waitForIdle()
        val settingsList = composeRule.onNodeWithTag("settings_lazy_column")

        // Card #3 (AppearanceLayoutCard) is directly visible with Theme Mode, Dock Layout & Nav Bar Style
        settingsList.performScrollToNode(hasTestTag("settings_appearance_layout_card"))
        composeRule.onNodeWithTag("settings_appearance_layout_card").assertExists()
        settingsList.performScrollToNode(hasTestTag("settings_theme_mode_DARK"))
        composeRule.onNodeWithTag("settings_theme_mode_DARK").assertIsDisplayed()
        settingsList.performScrollToNode(hasTestTag("settings_dock_layout_AUTOMATIC"))
        composeRule.onNodeWithTag("settings_dock_layout_AUTOMATIC").assertIsDisplayed()
        settingsList.performScrollToNode(hasTestTag("settings_appearance_nav_style_PILL_TONAL"))
        composeRule.onNodeWithTag("settings_appearance_nav_style_PILL_TONAL").assertIsDisplayed()

        // Lower secondary drawers start collapsed and expand on tap, showing Backdrops 1.4
        composeRule.onNodeWithTag("settings_clear_cache_button").assertDoesNotExist()
        settingsList.performScrollToNode(hasTestTag("settings_drawer_storage_about_toggle"))
        composeRule.onNodeWithTag("settings_drawer_storage_about_toggle").performClick()
        composeRule.waitForIdle()
        settingsList.performScrollToNode(hasTestTag("settings_clear_cache_button"))
        composeRule.onNodeWithTag("settings_clear_cache_button").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("Backdrops 1.4", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        )
    }

    // 4. Lock Screen Mode shows inline live preview + 3-way switcher + collapsed secondary drawers
    @Test
    @Config(sdk = [36])
    fun lockScreenModeShowsInlinePreviewAndCollapsedSecondaryDrawers() {
        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.FROSTED_GLASS_OS, reducedMotionEnabled = true) {
                LockScreenModeScreen(
                    preferences = UserPreferencesDoc(),
                    savedSets = emptyList(),
                    currentClockConfig = ClockDesignConfig(),
                    onBack = {},
                    onOpenCompatibility = {},
                    onSelectOverlaySetId = {},
                    onSetOverlayEnabled = {},
                    onNavigateStudioDestination = {}
                )
            }
        }
        composeRule.waitForIdle()
        val scrollList = composeRule.onNodeWithTag("lockscreen_mode_scroll_list")
        composeRule.onNodeWithTag("lockscreen_inline_preview_card").assertIsDisplayed()
        composeRule.onNodeWithTag("lockscreen_inline_charging_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("studio_switch_lock").assertIsDisplayed()
        scrollList.performScrollToNode(hasTestTag("lockscreen_notifications_switch"))
        composeRule.onNodeWithTag("lockscreen_notifications_switch").assertIsDisplayed()

        // Lengthy secondary customizers start collapsed by default
        composeRule.onNodeWithTag("lockscreen_credential_card").assertDoesNotExist()
        scrollList.performScrollToNode(hasTestTag("lockscreen_drawer_credential_toggle"))
        composeRule.onNodeWithTag("lockscreen_drawer_credential_toggle").performClick()
        composeRule.waitForIdle()
        scrollList.performScrollToNode(hasTestTag("lockscreen_credential_card"))
        composeRule.onNodeWithTag("lockscreen_credential_card").assertIsDisplayed()
    }

    // 5. Compact 320dp screen width renders StudioWidgetsLockStrip cleanly without overflow crash
    @Test
    fun narrowScreenRendersWidgetsLockStripCleanly() {
        val widgets = listOf(
            StudioWidgetItem(type = StudioWidgetType.SYSTEM_INFO, enabled = true, widthScale = 1.65f),
            StudioWidgetItem(type = StudioWidgetType.WEATHER, enabled = true, widthScale = 1.65f)
        )
        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.MATERIAL_3, reducedMotionEnabled = true) {
                Box(modifier = Modifier.width(320.dp)) {
                    StudioWidgetsLockStrip(
                        widgets = widgets,
                        telemetry = ClockWidgetTelemetry(weatherText = "21° Clear", batteryPercent = 82, isWeatherAvailable = true),
                        isCharging = false,
                        is24HourFormat = true,
                        isResizeMode = false
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }
}
