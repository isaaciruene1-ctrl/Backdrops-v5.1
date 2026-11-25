package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.IntSize
import androidx.test.core.app.ApplicationProvider
import com.example.data.catalog.WallpaperCatalog
import com.example.data.media.WallpaperMediaService
import com.example.data.model.UiStyleMode
import com.example.engine.depth.ClockHorizontalAlign
import com.example.engine.depth.DepthDetectionEngine
import com.example.engine.depth.DepthDetectionResult
import com.example.engine.depth.DepthSubjectCategory
import com.example.engine.depth.MaskBrushMode
import com.example.engine.depth.MaskBrushStroke
import com.example.engine.depth.OnDevicePhotoDepthEngine
import com.example.ui.components.ClockBoxRect
import com.example.ui.components.ClockDesignConfig
import com.example.ui.components.HyperOsClockFace
import com.example.ui.components.HyperOsClockFinish
import com.example.ui.components.LiquidGlassClockEngine
import com.example.ui.components.LiquidGlassConfig
import com.example.ui.screens.CanvasLookFilter
import com.example.ui.screens.DepthStudioScreen
import com.example.ui.screens.renderEditedWallpaperBitmap
import com.example.ui.theme.BackdropTheme
import com.example.ui.viewmodel.CanvasFramingState
import com.example.ui.viewmodel.DepthStudioUiState
import com.example.ui.viewmodel.DepthStudioViewModel
import kotlinx.coroutines.delay
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
class DepthStudioStressTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun makeTwoTonePortraitBitmap(w: Int, h: Int): Bitmap =
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.rgb(24, 62, 138))
            val canvas = android.graphics.Canvas(this)
            val paint = android.graphics.Paint().apply {
                color = AndroidColor.rgb(232, 118, 64)
            }
            canvas.drawRect(w * 0.25f, h * 0.35f, w * 0.75f, h.toFloat(), paint)
        }

    // 1. Tiny 1x1 and 16x16 bitmaps do not crash or divide by zero
    @Test
    fun stress01_depthEngineTiny1x1And16x16BitmapsDoNotCrash() : Unit = runBlocking {
        val engine = DepthDetectionEngine()
        val tiny1 = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.MAGENTA)
        }
        val tiny16 = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.CYAN)
        }
        val r1 = engine.detectAndExtract(tiny1, titleHint = "1px")
        val r16 = engine.detectAndExtract(tiny16, titleHint = "16px")
        assertFalse(r1.isDepthEligible)
        assertFalse(r16.isDepthEligible)
        assertNull(r1.cutoutBitmap)
    }

    // 2. Oversized 3000x4000 bitmap downscales cleanly without OOM
    @Test
    fun stress02_depthEngineOversized3000x4000DownscalesWithoutOom() : Unit = runBlocking {
        val engine = DepthDetectionEngine()
        val big = makeTwoTonePortraitBitmap(3000, 4000)
        val result = engine.detectAndExtract(big, titleHint = "Oversized Portrait", cacheKey = "stress_oversized")
        assertTrue(result.processingTimeMs >= 0L)
        result.cutoutBitmap?.let { cut ->
            assertFalse(cut.isRecycled)
            assertTrue(maxOf(cut.width, cut.height) <= 1280)
        }
    }

    // 3. Recycled source bitmap returns graceful flat fallback without IllegalStateException
    @Test
    fun stress03_depthEngineRecycledBitmapSafeGracefulFallback() : Unit = runBlocking {
        val engine = DepthDetectionEngine()
        val bmp = makeTwoTonePortraitBitmap(64, 96)
        bmp.recycle()
        assertTrue(bmp.isRecycled)
        val result = engine.detectAndExtract(bmp, titleHint = "Recycled")
        assertFalse(result.isDepthEligible)
        assertNull(result.cutoutBitmap)
    }

    // 4. Portrait cutout extraction handles extreme tall & wide aspect ratios safely
    @Test
    fun stress04_depthEngineExtremeAspectRatioSafeExtraction() = runBlocking {
        val engine = DepthDetectionEngine()
        val tallPortrait = makeTwoTonePortraitBitmap(90, 260)
        val wideHorizon = makeTwoTonePortraitBitmap(240, 90)
        val rTall = engine.detectAndExtract(tallPortrait, titleHint = "Tall Tower")
        val rWide = engine.detectAndExtract(wideHorizon, titleHint = "Wide Ridge")
        assertTrue(rTall.processingTimeMs >= 0L)
        assertTrue(rWide.processingTimeMs >= 0L)
        rTall.cutoutBitmap?.let { assertFalse(it.isRecycled) }
        rWide.cutoutBitmap?.let { assertFalse(it.isRecycled) }
    }

    // 5. Manual Mask Keep, Erase & Clear refinement with extreme feather values
    @Test
    fun stress05_manualMaskKeepEraseAndClearRefinementWithFeather() : Unit = runBlocking {
        val engine = DepthDetectionEngine()
        val src = makeTwoTonePortraitBitmap(120, 180)
        val cutout = makeTwoTonePortraitBitmap(120, 180)
        val strokes = listOf(
            MaskBrushStroke(listOf(0.5f to 0.5f, 0.6f to 0.6f), radiusFraction = 0.08f, mode = MaskBrushMode.KEEP),
            MaskBrushStroke(listOf(0.2f to 0.2f, 0.3f to 0.3f), radiusFraction = 0.05f, mode = MaskBrushMode.ERASE)
        )
        val refinedMin = engine.applyManualMaskRefinement(src, cutout, strokes, featherStrength = 0.05f)
        val refinedMax = engine.applyManualMaskRefinement(src, cutout, strokes, featherStrength = 3.0f)
        val emptyStrokes = engine.applyManualMaskRefinement(src, cutout, emptyList(), featherStrength = 0.5f)
        assertNotNull(refinedMin)
        assertNotNull(refinedMax)
        assertNotNull(emptyStrokes)
        assertEquals(120, refinedMin!!.width)
        assertEquals(180, refinedMin.height)
    }

    // 6. Out-of-bounds mask coordinates and recycled cutout handled safely
    @Test
    fun stress06_manualMaskOutOfBoundsAndRecycledCutoutHandledSafely() : Unit = runBlocking {
        val engine = DepthDetectionEngine()
        val src = makeTwoTonePortraitBitmap(100, 150)
        val deadCutout = makeTwoTonePortraitBitmap(100, 150).apply { recycle() }
        val oobStrokes = listOf(
            MaskBrushStroke(listOf(-2.5f to -1.5f, 4.0f to 3.2f), radiusFraction = 0.9f, mode = MaskBrushMode.KEEP),
            MaskBrushStroke(emptyList(), radiusFraction = 0.0001f, mode = MaskBrushMode.ERASE)
        )
        val refined = engine.applyManualMaskRefinement(src, deadCutout, oobStrokes, featherStrength = 0.6f)
        assertNotNull(refined)
        assertFalse(refined!!.isRecycled)

        src.recycle()
        val afterRecycledSource = engine.applyManualMaskRefinement(src, null, oobStrokes)
        assertNull(afterRecycledSource)
    }

    // 7. OnDevicePhotoDepthEngine handles bad URI and small portrait bitmaps gracefully
    @Test
    fun stress07_photoDepthEngineInSampleDecodeCorruptUriAndSmallImage() : Unit = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val photoEngine = OnDevicePhotoDepthEngine()
        val badRes = photoEngine.decodeAndExtractPhoto(ctx, Uri.parse("content://nonexistent.provider/missing/999.jpg"))
        assertNull(badRes)

        val smallPortrait = makeTwoTonePortraitBitmap(48, 72)
        val smallRes = photoEngine.extractFromPhotoBitmap(smallPortrait, cacheKey = "small_portrait_stress")
        assertTrue(smallRes.processingTimeMs >= 0L)
    }

    // 8. LiquidGlassClockEngine multi-radius bevel map with tiny/minimal glyph bitmap
    @Test
    fun stress08_liquidGlassEngineBevelMapMinimalDimensions() {
        val tinyMask = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.WHITE)
        }
        val heightMap = LiquidGlassClockEngine.buildMultiRadiusBevelHeightMap(tinyMask, bevelWidthPx = 40f)
        assertFalse(heightMap.isRecycled)
        assertEquals(2, heightMap.width)
        assertEquals(2, heightMap.height)
    }

    // 9. LiquidGlassClockEngine extractCroppedBackgroundPatch under extreme zoom, pan, rotation & filters
    @Test
    fun stress09_liquidGlassExtractPatchWithExtremeZoomPanRotationAndFilters() {
        val src = makeTwoTonePortraitBitmap(240, 400)
        CanvasLookFilter.entries.forEachIndexed { idx, filter ->
            val patch = LiquidGlassClockEngine.extractCroppedBackgroundPatch(
                backdropBitmap = if (idx == 0) src else src,
                viewportSize = IntSize(1080, 2400),
                clockBoxRect = ClockBoxRect(120f, 300f, 640f, 220f),
                zoomScale = if (idx % 2 == 0) 6.0f else 0.1f,
                panOffset = Offset(1500f, -1500f),
                dimFraction = 0.9f,
                lookFilter = filter,
                frostBlurPx = 30f,
                fallbackPrimary = Color.Blue,
                fallbackSecondary = Color.DarkGray,
                patchWidth = 180,
                patchHeight = 80,
                rotationDegrees = -45f + idx * 18f
            )
            assertFalse(patch.unblurredPatch.isRecycled)
            assertFalse(patch.frostedPatch.isRecycled)
            assertTrue(patch.avgLuminance in 0f..1f)
        }
        src.recycle()
        // Recycled bitmap falls back cleanly to gradient patch
        val fallbackPatch = LiquidGlassClockEngine.extractCroppedBackgroundPatch(
            backdropBitmap = src,
            viewportSize = IntSize(1, 1),
            clockBoxRect = null,
            zoomScale = 1f,
            panOffset = Offset.Zero,
            dimFraction = 0f,
            lookFilter = CanvasLookFilter.ORIGINAL,
            frostBlurPx = 4f,
            fallbackPrimary = Color.Cyan,
            fallbackSecondary = Color.Black,
            patchWidth = 64,
            patchHeight = 32
        )
        assertFalse(fallbackPatch.unblurredPatch.isRecycled)
    }

    // 10. LiquidGlassClockEngine software optical render under boundary optics parameters
    @Test
    fun stress10_liquidGlassRenderSoftwareBitmapExtremeOpticsParams() {
        val glyphMask = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.TRANSPARENT)
            for (y in 8 until 24) {
                for (x in 12 until 52) setPixel(x, y, AndroidColor.WHITE)
            }
        }
        val heightMap = LiquidGlassClockEngine.buildMultiRadiusBevelHeightMap(glyphMask, bevelWidthPx = 12f)
        val bg = makeTwoTonePortraitBitmap(64, 32)
        val extremeConfig = LiquidGlassConfig(
            refractionStrength = 99f,
            ior = 2.5f,
            bevelWidth = 50f,
            chromaticAberration = 5.0f,
            frostBlur = 50f,
            glassSaturation = 4.0f,
            tintOpacity = 1.0f,
            specularIntensity = 3.0f,
            lightAngle = 725f,
            innerThicknessShadow = 2.0f,
            backdropDropShadow = 3.0f
        ).sanitized()
        val params = LiquidGlassClockEngine.resolveAdaptiveOptics(extremeConfig, avgLuminance = 0.95f)
        val rendered = LiquidGlassClockEngine.renderOpticalGlassSoftwareBitmap(
            glyphMap = heightMap,
            backgroundPatch = bg,
            frostedPatch = bg,
            params = params,
            scaleXAspect = 0.1f,
            scaleYAspect = 2.5f,
            opacity = 0.85f
        )
        assertFalse(rendered.isRecycled)
        assertEquals(64, rendered.width)
        assertEquals(32, rendered.height)
    }

    // 11. Rapid 20 wallpaper switches cancel stale depth scan jobs safely without race corruption
    @Test
    fun stress11_depthStudioViewModelRapid20WallpaperSwitchesCancelsStaleJobs() : Unit = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val vm = DepthStudioViewModel(WallpaperMediaService(ctx))
        val sampleWallpapers = WallpaperCatalog.allWallpapers.take(20)
        sampleWallpapers.forEach { wp ->
            vm.loadWallpaperForDepthStudio(wp)
        }
        delay(80L)
        assertEquals(sampleWallpapers.last().id, vm.activeWallpaperId.value)
        assertFalse(vm.state.value is DepthStudioUiState.BitmapError)
    }

    // 12. 50-cycle Undo/Redo history across framing, clock, and mask edits stays consistent
    @Test
    fun stress12_depthStudioViewModel50CycleUndoRedoFramingAndClockConsistency() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val vm = DepthStudioViewModel(WallpaperMediaService(ctx))
        repeat(50) { idx ->
            vm.commitClockConfig { it.copy(weightLevel = 300 + (idx % 6) * 100) }
            vm.commitCanvasFraming { it.copy(zoomScale = 1.0f + (idx % 5) * 0.15f) }
        }
        assertTrue(vm.canUndo.value)
        var undoSteps = 0
        while (vm.canUndo.value && undoSteps < 60) {
            vm.undoEdit()
            undoSteps++
        }
        assertTrue(undoSteps > 0)
        assertTrue(vm.canRedo.value)
        var redoSteps = 0
        while (vm.canRedo.value && redoSteps < 60) {
            vm.redoEdit()
            redoSteps++
        }
        assertEquals(undoSteps, redoSteps)
    }

    // 13. Bad photo URI resolves cleanly to BitmapError without crashing ViewModel
    @Test
    fun stress13_depthStudioViewModelBadPhotoUriRecoversToBitmapErrorWithoutCrash() : Unit = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val vm = DepthStudioViewModel(WallpaperMediaService(ctx))
        vm.loadCustomPhotoForDepthStudio(ctx, Uri.parse("content://invalid.host/does_not_exist.png"))
        var waited = 0
        while (vm.state.value is DepthStudioUiState.LoadingFullBitmap && waited < 60) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            delay(50L)
            waited++
        }
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertTrue(
            "Expected BitmapError for unreadable URI, got ${vm.state.value}",
            vm.state.value is DepthStudioUiState.BitmapError
        )
    }

    // 14. DepthStudioScreen renders safely during LoadingFullBitmap & ScanningSubject states
    @Test
    fun stress14_depthStudioScreenRendersSafelyDuringLoadingAndScanningStates() {
        val wp = WallpaperCatalog.allWallpapers.first()
        val scanBmp = makeTwoTonePortraitBitmap(90, 160)
        var uiState by mutableStateOf<DepthStudioUiState>(DepthStudioUiState.LoadingFullBitmap)

        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.MATERIAL_3, reducedMotionEnabled = true) {
                DepthStudioScreen(
                    initialWallpaper = wp,
                    depthState = uiState,
                    clockConfig = ClockDesignConfig(),
                    canUndo = false,
                    canRedo = false,
                    isApplying = false,
                    onLoadWallpaper = { _, _ -> },
                    onPickCustomPhoto = { _, _ -> },
                    onBeginClockGesture = {},
                    onPreviewClockConfig = {},
                    onEndClockGesture = {},
                    onCommitClockConfig = {},
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
        composeRule.runOnUiThread {
            uiState = DepthStudioUiState.ScanningSubject(scanBmp, "Alpine")
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("depth_back_button").assertIsDisplayed()
    }

    // 15. DepthStudioScreen renders safely in EngineError & BitmapError states and triggers retry
    @Test
    fun stress15_depthStudioScreenRendersSafelyInEngineErrorAndBitmapErrorWithRetry() {
        val wp = WallpaperCatalog.allWallpapers.first()
        val bmp = makeTwoTonePortraitBitmap(90, 160)
        var forceRescanCalls = 0
        var uiState by mutableStateOf<DepthStudioUiState>(DepthStudioUiState.BitmapError("Simulated decode failure"))

        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.FROSTED_GLASS_OS, reducedMotionEnabled = true) {
                DepthStudioScreen(
                    initialWallpaper = wp,
                    depthState = uiState,
                    clockConfig = ClockDesignConfig(),
                    canUndo = false,
                    canRedo = false,
                    isApplying = false,
                    onLoadWallpaper = { _, force -> if (force) forceRescanCalls++ },
                    onPickCustomPhoto = { _, _ -> },
                    onBeginClockGesture = {},
                    onPreviewClockConfig = {},
                    onEndClockGesture = {},
                    onCommitClockConfig = {},
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
        composeRule.runOnUiThread {
            uiState = DepthStudioUiState.EngineError(wp.id, bmp, "Simulated contour timeout")
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("depth_rescan_button").performClick()
        composeRule.waitForIdle()
        assertTrue(forceRescanCalls >= 1)
    }

    // 16. Frosted Glass OS mode renders zero Material 3 sliders across Depth / Clock / Canvas tabs
    @Test
    fun stress16_depthStudioScreenFrostedGlassRendersZeroMaterialSlidersAcrossAllTabs() {
        val wp = WallpaperCatalog.allWallpapers.first()
        val bmp = makeTwoTonePortraitBitmap(90, 160)
        val ready = DepthStudioUiState.Ready(
            key = wp.id,
            fullBitmap = bmp,
            result = DepthDetectionResult(
                subjectLabel = "Ridge",
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
        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.FROSTED_GLASS_OS, reducedMotionEnabled = true) {
                DepthStudioScreen(
                    initialWallpaper = wp,
                    depthState = ready,
                    clockConfig = ClockDesignConfig(finish = HyperOsClockFinish.BLUR_GLASS),
                    canUndo = false,
                    canRedo = false,
                    isApplying = false,
                    onLoadWallpaper = { _, _ -> },
                    onPickCustomPhoto = { _, _ -> },
                    onBeginClockGesture = {},
                    onPreviewClockConfig = {},
                    onEndClockGesture = {},
                    onCommitClockConfig = {},
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
        composeRule.onNodeWithTag("depth_clock_y_slider").performScrollTo().assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag("liquid_glass_slider_track").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithTag("material3_slider").fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithTag("depth_tab_Clock").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("clock_subpill_TYPE_FINISH").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("clock_weight_slider").performScrollTo().assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag("liquid_glass_slider_track").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithTag("material3_slider").fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithTag("depth_tab_Canvas").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("depth_zoom_slider").performScrollTo().assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag("liquid_glass_slider_track").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithTag("material3_slider").fetchSemanticsNodes().isEmpty())
    }

    // 17. Primary slider dropdowns in DepthStudioScreen start open and collapse/re-expand cleanly on tap
    @Test
    fun stress17_depthStudioScreenPrimarySlidersStartOpenAndToggleOnTap() {
        val wp = WallpaperCatalog.allWallpapers.first()
        val bmp = makeTwoTonePortraitBitmap(90, 160)
        val ready = DepthStudioUiState.Ready(
            key = wp.id,
            fullBitmap = bmp,
            result = DepthDetectionResult(
                subjectLabel = "Peak",
                subjectCategory = DepthSubjectCategory.SUMMIT_SILHOUETTE,
                isDepthEligible = true,
                depthQualityScore = 84,
                cutoutBitmap = bmp,
                recommendedClockTopFraction = 0.18f,
                recommendedAlign = ClockHorizontalAlign.CENTER,
                recommendedClockColorHex = "#FFFFFFFF",
                subjectTopFraction = 0.35f,
                subjectPeakXFraction = 0.5f,
                foregroundCoverageFraction = 0.40f,
                processingTimeMs = 10L
            )
        )
        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.MATERIAL_3, reducedMotionEnabled = true) {
                DepthStudioScreen(
                    initialWallpaper = wp,
                    depthState = ready,
                    clockConfig = ClockDesignConfig(finish = HyperOsClockFinish.BLUR_GLASS),
                    canUndo = false,
                    canRedo = false,
                    isApplying = false,
                    onLoadWallpaper = { _, _ -> },
                    onPickCustomPhoto = { _, _ -> },
                    onBeginClockGesture = {},
                    onPreviewClockConfig = {},
                    onEndClockGesture = {},
                    onCommitClockConfig = {},
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
        composeRule.onNodeWithTag("depth_clock_y_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("depth_position_sliders_dropdown_toggle").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("depth_clock_y_slider").assertDoesNotExist()
        composeRule.onNodeWithTag("depth_position_sliders_dropdown_toggle").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("depth_clock_y_slider").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("depth_tab_Clock").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("clock_subpill_TYPE_FINISH").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("clock_weight_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("depth_clock_weight_opacity_dropdown_toggle").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("clock_weight_slider").assertDoesNotExist()
    }

    // 18. Cycling all HyperOsClockFace and HyperOsClockFinish combinations produces zero crashes
    @Test
    fun stress18_depthStudioScreenAllClockFacesAndFinishesCycleZeroCrash() {
        val wp = WallpaperCatalog.allWallpapers.first()
        val bmp = makeTwoTonePortraitBitmap(90, 160)
        var cfg by mutableStateOf(ClockDesignConfig())
        val ready = DepthStudioUiState.Ready(
            key = wp.id,
            fullBitmap = bmp,
            result = DepthDetectionResult(
                subjectLabel = "Skyline",
                subjectCategory = DepthSubjectCategory.BOTANICAL_ARCH,
                isDepthEligible = true,
                depthQualityScore = 82,
                cutoutBitmap = bmp,
                recommendedClockTopFraction = 0.16f,
                recommendedAlign = ClockHorizontalAlign.CENTER,
                recommendedClockColorHex = "#FFFFFFFF",
                subjectTopFraction = 0.34f,
                subjectPeakXFraction = 0.5f,
                foregroundCoverageFraction = 0.38f,
                processingTimeMs = 11L
            )
        )
        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.FROSTED_GLASS_OS, reducedMotionEnabled = true) {
                DepthStudioScreen(
                    initialWallpaper = wp,
                    depthState = ready,
                    clockConfig = cfg,
                    canUndo = false,
                    canRedo = false,
                    isApplying = false,
                    onLoadWallpaper = { _, _ -> },
                    onPickCustomPhoto = { _, _ -> },
                    onBeginClockGesture = {},
                    onPreviewClockConfig = { cfg = it },
                    onEndClockGesture = {},
                    onCommitClockConfig = { t -> cfg = t(cfg) },
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
        HyperOsClockFace.entries.forEach { face ->
            HyperOsClockFinish.entries.forEach { finish ->
                composeRule.runOnUiThread {
                    cfg = cfg.copy(face = face, finish = finish)
                }
                composeRule.waitForIdle()
            }
        }
        assertTrue(composeRule.onAllNodesWithTag("depth_back_button").fetchSemanticsNodes().isNotEmpty())
    }

    // 19. Manual Mask Brush mode toggles Keep / Erase & brush sliders safely
    @Test
    fun stress19_depthStudioScreenManualMaskBrushModeAndMagnifierToggle() {
        val wp = WallpaperCatalog.allWallpapers.first()
        val bmp = makeTwoTonePortraitBitmap(90, 160)
        val ready = DepthStudioUiState.Ready(
            key = wp.id,
            fullBitmap = bmp,
            result = DepthDetectionResult(
                subjectLabel = "Figure",
                subjectCategory = DepthSubjectCategory.PORTRAIT_FIGURE,
                isDepthEligible = true,
                depthQualityScore = 76,
                cutoutBitmap = bmp,
                recommendedClockTopFraction = 0.16f,
                recommendedAlign = ClockHorizontalAlign.CENTER,
                recommendedClockColorHex = "#FFFFFFFF",
                subjectTopFraction = 0.30f,
                subjectPeakXFraction = 0.5f,
                foregroundCoverageFraction = 0.44f,
                processingTimeMs = 14L
            )
        )
        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.MATERIAL_3, reducedMotionEnabled = true) {
                DepthStudioScreen(
                    initialWallpaper = wp,
                    depthState = ready,
                    clockConfig = ClockDesignConfig(),
                    canUndo = false,
                    canRedo = false,
                    isApplying = false,
                    onLoadWallpaper = { _, _ -> },
                    onPickCustomPhoto = { _, _ -> },
                    onBeginClockGesture = {},
                    onPreviewClockConfig = {},
                    onEndClockGesture = {},
                    onCommitClockConfig = {},
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
        composeRule.onNodeWithTag("depth_mask_brush_drawer_toggle").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("depth_mask_erase_pill").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("depth_mask_keep_pill").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("depth_mask_brush_drawer_toggle").performScrollTo().performClick()
        composeRule.waitForIdle()
    }

    // 20. renderEditedWallpaperBitmap safely handles rotations, dim, filters & recycled fallback
    @Test
    fun stress20_renderEditedWallpaperBitmapStressRotationsDimAndRecycledSource() {
        val src = makeTwoTonePortraitBitmap(240, 480)
        CanvasLookFilter.entries.forEachIndexed { idx, filter ->
            val out = renderEditedWallpaperBitmap(
                source = src,
                zoomScale = 1.35f,
                panOffset = Offset(24f, -18f),
                viewportWidthPx = 1080f,
                viewportHeightPx = 2400f,
                rotationDegrees = -45f + idx * 15f,
                dimFraction = 0.45f,
                lookFilter = filter,
                targetScreenAspect = 9f / 20f,
                allowParallaxWidth = idx % 2 == 1
            )
            assertFalse(out.isRecycled)
            assertTrue(out.width > 0 && out.height > 0)
        }
        src.recycle()
        val deadResult = renderEditedWallpaperBitmap(
            source = src,
            zoomScale = 1f,
            panOffset = Offset.Zero,
            viewportWidthPx = 1080f,
            viewportHeightPx = 2400f,
            dimFraction = 0f,
            lookFilter = CanvasLookFilter.ORIGINAL,
            targetScreenAspect = 0.5f,
            allowParallaxWidth = false
        )
        assertNotNull(deadResult)
        assertTrue(deadResult.width > 0 && deadResult.height > 0)
    }
}
