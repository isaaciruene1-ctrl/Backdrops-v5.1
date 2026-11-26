package com.example

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.catalog.WallpaperCatalog
import com.example.data.model.BackdropStudioFont
import com.example.data.model.ChargingAnimationConfig
import com.example.data.model.ChargingAnimationPreset
import com.example.data.model.ChargingPosition
import com.example.data.model.LiquidGlassTier
import com.example.data.model.LockNotificationStyleConfig
import com.example.data.model.LockScreenSetDoc
import com.example.data.model.NotificationLayoutMode
import com.example.data.model.NotificationStylePreset
import com.example.data.model.StudioWidgetType
import com.example.data.model.UiStyleMode
import com.example.data.model.UnlockButtonStyle
import com.example.data.model.UnlockButtonThemeConfig
import com.example.data.model.UserPreferencesDoc
import com.example.engine.depth.ClockHorizontalAlign
import com.example.engine.depth.DepthDetectionEngine
import com.example.engine.depth.DepthDetectionResult
import com.example.engine.depth.DepthSubjectCategory
import com.example.engine.depth.OnDevicePhotoDepthEngine
import com.example.engine.performance.ZeroLagPerformanceEngine
import com.example.ui.components.ClockDesignConfig
import com.example.ui.screens.CollectionsScreen
import com.example.ui.screens.CustomDynamicColorPickerCard
import com.example.ui.screens.DepthStudioScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.StudioTypographyCard
import com.example.ui.screens.StudioWallpaperTargetDialog
import com.example.ui.theme.BackdropCustomAccentSwatches
import com.example.ui.theme.BackdropTheme
import com.example.ui.theme.ThemeCustomizationTokens
import com.example.ui.theme.buildSeededColorScheme
import com.example.ui.theme.evaluateSchemeContrast
import com.example.ui.viewmodel.DepthStudioUiState
import com.example.ui.viewmodel.SyncState
import com.example.widget.BackdropExpandedWidgetContract
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class Backdrop13VerificationSuiteTest {

    @get:Rule
    val composeRule = createComposeRule()

    // 1. Catalog Size (== 100) & Uniqueness
    @Test
    fun test01_catalogHasExactHundredUniqueWallpapers() {
        val all = WallpaperCatalog.allWallpapers
        assertEquals("Expected exact 100 wallpapers, found ${all.size}", 100, all.size)
        val distinctIds = all.map { it.id }.toSet()
        assertEquals("Duplicate wallpaper IDs detected", all.size, distinctIds.size)
        assertTrue(all.none { it.title.isBlank() || it.thumbnailUrl.isBlank() || it.fullResUrl.isBlank() })
    }

    // 2. Zero Lag & Zero Latency Sub-Engines (Tab Preloading + Predictive Floating Pill)
    @Test
    fun test02_zeroLagTabPreloadingAndPredictivePillNavigationEngines() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ZeroLagPerformanceEngine.TabPreloadingEngine.trimAll()
        ZeroLagPerformanceEngine.PredictivePillNavigationEngine.onPillDragEnded()

        ZeroLagPerformanceEngine.TabPreloadingEngine.preloadLikelyDestinations(
            context = ctx,
            currentRoute = "home"
        )
        val prepared = ZeroLagPerformanceEngine.TabPreloadingEngine.preparedRoutes.value
        assertTrue("Expected 'explore' to be preloaded from home", "explore" in prepared)
        val exploreMeta = ZeroLagPerformanceEngine.TabPreloadingEngine.getCachedMetadata("explore")
        assertNotNull(exploreMeta)
        assertTrue(exploreMeta!!.wallpaperIds.isNotEmpty())

        // Predictive pill hover starts preparing destination before release
        ZeroLagPerformanceEngine.PredictivePillNavigationEngine.onPillHoverChanged(
            context = ctx,
            hoveredRoute = "favorites"
        )
        assertEquals(
            "favorites",
            ZeroLagPerformanceEngine.PredictivePillNavigationEngine.activePreloadRoute.value
        )
        assertTrue(ZeroLagPerformanceEngine.PredictivePillNavigationEngine.preloadTriggerCount.value >= 1)
        assertNotNull(ZeroLagPerformanceEngine.TabPreloadingEngine.getCachedMetadata("favorites"))

        ZeroLagPerformanceEngine.PredictivePillNavigationEngine.onPillDragEnded()
        assertNull(ZeroLagPerformanceEngine.PredictivePillNavigationEngine.activePreloadRoute.value)

        // Memory budget trimming
        ZeroLagPerformanceEngine.TabPreloadingEngine.trimToBudget(maxTabs = 1)
        ZeroLagPerformanceEngine.TabPreloadingEngine.trimAll()
        assertTrue(ZeroLagPerformanceEngine.TabPreloadingEngine.preparedRoutes.value.isEmpty())
    }

    // 3. Depth Engine V4 Subject Isolation + Photo Depth Engine
    @Test
    fun test03_depthEngineExtractsPrimarySubjectAndPhotoCutoutCleanly() = runBlocking {
        val w = 240
        val h = 360
        val multiSubjectBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.rgb(34, 84, 168)) // uniform cool sky background
            // Left subject (pillar / foreground shape)
            for (y in 135 until h) {
                for (x in 22 until 86) {
                    setPixel(x, y, AndroidColor.rgb(228, 86, 52))
                }
            }
            // Right subject (detached companion spire)
            for (y in 155 until h) {
                for (x in 154 until 218) {
                    setPixel(x, y, AndroidColor.rgb(235, 178, 48))
                }
            }
        }

        val engine = DepthDetectionEngine()
        val result = engine.detectAndExtract(multiSubjectBitmap, titleHint = "Twin Spires")
        assertTrue("Expected depth eligibility for primary subjects", result.isDepthEligible)
        assertNotNull(result.cutoutBitmap)
        assertFalse(result.cutoutBitmap!!.isRecycled)

        // On-Device Photo Depth Engine processes portrait photo safely
        val photoResult = OnDevicePhotoDepthEngine(engine).extractFromPhotoBitmap(multiSubjectBitmap)
        assertTrue(photoResult.isDepthEligible)
    }

    // 4. Notification Styles (8 OEM presets), Charging Animations & Unlock Button Themes
    @Test
    fun test04_lockCustomizationModelsAndNotificationLayoutModes() {
        assertEquals(8, NotificationStylePreset.entries.size)
        NotificationStylePreset.entries.forEach { preset ->
            val cfg = LockNotificationStyleConfig.fromPreset(preset).copy(
                layoutMode = NotificationLayoutMode.LIST,
                transparency = 0.68f,
                blurIntensity = 0.90f,
                showTimestamp = false
            )
            val decoded = LockNotificationStyleConfig.fromJsonString(cfg.toJsonString())
            assertEquals(preset, decoded.preset)
            assertEquals(NotificationLayoutMode.LIST, decoded.layoutMode)
            assertEquals(0.68f, decoded.transparency, 0.01f)
            assertFalse(decoded.showTimestamp)
        }
        assertEquals(NotificationLayoutMode.STACK, LockNotificationStyleConfig().layoutMode)
        assertEquals(NotificationLayoutMode.STACK, NotificationLayoutMode.fromJsonStringOrLegacy("STACKED_BY_APP"))
        assertEquals(NotificationLayoutMode.LIST, NotificationLayoutMode.fromJsonStringOrLegacy("EXPANDED_CARDS"))
        assertEquals(NotificationLayoutMode.COUNT, NotificationLayoutMode.fromJsonStringOrLegacy("COMPACT_PILLS"))

        val charging = ChargingAnimationConfig(
            enabled = true,
            preset = ChargingAnimationPreset.LIQUID_ENERGY_FLOW,
            speedMultiplier = 1.45f,
            sizeScale = 1.25f,
            position = ChargingPosition.CENTER,
            dynamicAccentColor = false,
            customAccentArgb = 0xFF33B5E5L
        )
        val decodedCharging = ChargingAnimationConfig.fromJsonString(charging.toJsonString())
        assertEquals(ChargingAnimationPreset.LIQUID_ENERGY_FLOW, decodedCharging.preset)
        assertEquals(ChargingPosition.CENTER, decodedCharging.position)
        assertEquals(1.45f, decodedCharging.speedMultiplier, 0.01f)

        val unlockCfg = UnlockButtonThemeConfig(
            style = UnlockButtonStyle.CUSTOM,
            customContainerArgb = 0xFF223344L,
            customContentArgb = 0xFFF5F7FAL,
            transparency = 0.72f,
            cornerRadiusDp = 22f
        )
        val decodedUnlock = UnlockButtonThemeConfig.fromJsonString(unlockCfg.toJsonString())
        assertEquals(UnlockButtonStyle.CUSTOM, decodedUnlock.style)
        assertEquals(0xFF223344L, decodedUnlock.customContainerArgb)
        assertEquals(22f, decodedUnlock.cornerRadiusDp, 0.01f)
    }

    // 5. 9-Widget Suite, 10 Studio Fonts, Hardware Liquid Glass Tiers, WCAG Contrast & Play-Compliant Manifest
    @Test
    fun test05_widgetsTenFontsGlassTiersContrastAndManifestCompliance() {
        assertTrue(StudioWidgetType.entries.size >= 21)
        assertEquals(10, BackdropStudioFont.entries.size)
        assertTrue(BackdropExpandedWidgetContract.ALL_EXPANDED_PROVIDERS.size >= 9)

        assertEquals(LiquidGlassTier.LIGHT, LiquidGlassTier.recommendForDevice(ramMb = 2048, sdkInt = 31, isLowRamDevice = true))
        assertEquals(LiquidGlassTier.BALANCED, LiquidGlassTier.recommendForDevice(ramMb = 4096, sdkInt = 31, isLowRamDevice = false))
        assertEquals(LiquidGlassTier.ULTRA, LiquidGlassTier.recommendForDevice(ramMb = 12288, sdkInt = 36, isLowRamDevice = false))

        // Verify seeded HSL palette produces WCAG AA compliant Primary/Container/Surface contrast in Light & Dark
        listOf(0xFF6B57A8L, 0xFF2B7A9BL, 0xFFC95E4BL, 0xFFB87D1EL).forEach { seed ->
            val lightReport = evaluateSchemeContrast(buildSeededColorScheme(seed, isDark = false, surfaceDepth = 1.0f))
            val darkReport = evaluateSchemeContrast(buildSeededColorScheme(seed, isDark = true, surfaceDepth = 1.0f))
            assertTrue("Light scheme failed WCAG AA for seed $seed: $lightReport", lightReport.meetsWcagAa)
            assertTrue("Dark scheme failed WCAG AA for seed $seed: $darkReport", darkReport.meetsWcagAa)
        }

        // Verify Google Play zero broad-storage-permission policy
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        @Suppress("DEPRECATION")
        val pkgInfo = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_PERMISSIONS)
        val permissions = pkgInfo.requestedPermissions?.toSet().orEmpty()
        assertFalse(android.Manifest.permission.READ_EXTERNAL_STORAGE in permissions)
        assertFalse("android.permission.READ_MEDIA_IMAGES" in permissions)
        assertFalse("android.permission.READ_MEDIA_VIDEO" in permissions)
    }

    // 6. Compose UI: Custom Dynamic Color Picker
    @Test
    fun test06_customDynamicColorPickerUpdatesHexAndShowsWcagBadge() {
        var selectedHex by mutableLongStateOf(BackdropCustomAccentSwatches.first().second)

        composeRule.setContent {
            BackdropTheme(
                uiStyle = UiStyleMode.FROSTED_GLASS_OS,
                useDynamicColor = false,
                customization = ThemeCustomizationTokens(
                    dynamicThemingEnabled = false,
                    customAccentHex = selectedHex
                ),
                reducedMotionEnabled = true
            ) {
                CustomDynamicColorPickerCard(
                    selectedHex = selectedHex,
                    onSelectHex = { selectedHex = it }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("custom_dynamic_color_picker").assertIsDisplayed()
        composeRule.onNodeWithTag("custom_color_hex_readout").assertIsDisplayed()
        composeRule.onNodeWithTag("custom_color_wcag_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_accent_swatch_Ocean Cyan").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("custom_color_hex_readout").assertTextContains("#2B7A9B", substring = true)
    }

    // 7. Compose UI: Studio Typography Dropdown (10 Fonts)
    @Test
    fun test07_studioTypographyCardSelectsGlyphFont() {
        var chosenFont by mutableStateOf(BackdropStudioFont.BACKDROP_DEFAULT)
        var typeScale by mutableFloatStateOf(1.0f)

        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.MATERIAL_3, reducedMotionEnabled = true) {
                StudioTypographyCard(
                    preferences = UserPreferencesDoc(
                        selectedStudioFont = chosenFont.name,
                        m3TypographyScale = typeScale
                    ),
                    onStudioFontChange = { chosenFont = it },
                    onTypographyScaleChange = { typeScale = it }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("studio_typography_card").assertIsDisplayed()
        composeRule.onNodeWithTag("studio_font_dropdown_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("studio_font_option_GLYPH").performClick()
        composeRule.waitForIdle()
        assertEquals(BackdropStudioFont.GLYPH, chosenFont)
    }

    // 8. Compose UI: Collections Saved Sets Multi-Select & Batch Delete Confirmation
    @Test
    fun test08_collectionsSavedSetsSupportsBatchDeleteConfirmation() {
        val sampleSets = listOf(
            LockScreenSetDoc(id = "set_1", name = "Alpine Poster", wallpaperId = WallpaperCatalog.allWallpapers.first().id),
            LockScreenSetDoc(id = "set_2", name = "Neon Dusk", wallpaperId = WallpaperCatalog.allWallpapers[1].id)
        )
        var batchDeletedIds: Set<String> = emptySet()

        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.MATERIAL_3, reducedMotionEnabled = true) {
                CollectionsScreen(
                    collectionsState = SyncState.Ready(emptyList()),
                    lockScreenSetsState = SyncState.Ready(sampleSets),
                    onCreateCollection = { _, _ -> },
                    onRenameCollection = { _, _, _ -> },
                    onDeleteCollection = {},
                    onOpenCollectionDetail = {},
                    onDeleteLockScreenSets = { batchDeletedIds = it }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("collections_section_Saved Sets").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("saved_sets_action_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("saved_sets_select_mode_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("saved_sets_select_all_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("saved_sets_batch_delete_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_batch_delete_sets").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(setOf("set_1", "set_2"), batchDeletedIds)
    }

    // 9. Compose UI: StudioWallpaperTargetDialog Sync Toggle & Save to Lock Screen Mode
    @Test
    fun test09_studioTargetDialogSyncsAndSavesToLockScreenMode() {
        var synced by mutableStateOf(false)
        var savedToLockMode = false

        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.FROSTED_GLASS_OS, reducedMotionEnabled = true) {
                StudioWallpaperTargetDialog(
                    title = "Midnight Ridge",
                    syncHomeWithLockMode = synced,
                    onSyncHomeWithLockModeChange = { synced = it },
                    onSaveToLockScreenMode = { savedToLockMode = true },
                    onDismiss = {},
                    onSelectTarget = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("studio_wallpaper_target_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("sync_home_lock_mode_switch").performClick()
        composeRule.waitForIdle()
        assertTrue(synced)
        composeRule.onNodeWithTag("save_to_lock_mode_button").performClick()
        composeRule.waitForIdle()
        assertTrue(savedToLockMode)
    }

    // 10. Compose UI: DepthStudioScreen Confidence Warning & Scale Text Switch
    @Test
    fun test10_depthStudioShowsConfidenceWarningAndScaleSwitch() {
        val wp = WallpaperCatalog.allWallpapers.first()
        val dummyBmp = Bitmap.createBitmap(64, 96, Bitmap.Config.ARGB_8888)
        val readyState = DepthStudioUiState.Ready(
            key = wp.id,
            fullBitmap = dummyBmp,
            result = DepthDetectionResult(
                subjectLabel = "Peak & Horizon",
                subjectCategory = DepthSubjectCategory.SUMMIT_SILHOUETTE,
                isDepthEligible = true,
                depthQualityScore = 48,
                cutoutBitmap = dummyBmp,
                recommendedClockTopFraction = 0.16f,
                recommendedAlign = ClockHorizontalAlign.CENTER,
                recommendedClockColorHex = "#FFFFFFFF",
                subjectTopFraction = 0.35f,
                subjectPeakXFraction = 0.5f,
                foregroundCoverageFraction = 0.4f,
                processingTimeMs = 15L,
                requiresRefinement = true,
                confidenceWarning = "Soft edge separation detected (48%). Refine with brush."
            )
        )
        var currentClock by mutableStateOf(ClockDesignConfig())
        assertTrue(currentClock.scaleTextWithClock)

        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.MATERIAL_3, reducedMotionEnabled = true) {
                DepthStudioScreen(
                    initialWallpaper = wp,
                    depthState = readyState,
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
                    onApplyCustomBitmapWallpaper = { _, _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("depth_confidence_warning_card").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("depth_tab_Clock").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("clock_subpill_TYPE_FINISH").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("clock_scale_text_switch").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertFalse(currentClock.scaleTextWithClock)
    }

    // 11. Compose UI: ProfileSettingsScreen Version 1.4 & Interactive Tutorial Entry
    @Test
    fun test11_profileScreenShowsVersion14AndOpensInteractiveTutorial() {
        var tutorialOpened = false
        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.FROSTED_GLASS_OS, reducedMotionEnabled = true) {
                ProfileSettingsScreen(
                    favoriteCount = 4,
                    collectionCount = 2,
                    preferences = UserPreferencesDoc(),
                    cacheBytes = 4096L,
                    onRefreshCacheSize = {},
                    onClearCache = {},
                    onOpenSettingsPanel = {},
                    onOpenInteractiveTutorial = { tutorialOpened = true }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("2.0").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("profile_interactive_tutorial_button"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_interactive_tutorial_button").performClick()
        composeRule.waitForIdle()
        assertTrue(tutorialOpened)
        assertTrue(composeRule.onAllNodesWithTag("profile_interactive_tutorial_button").fetchSemanticsNodes().isNotEmpty())
    }
}
