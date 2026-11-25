package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.catalog.WallpaperCatalog
import com.example.data.model.LockScreenSetDoc
import com.example.data.model.ThemeMode
import com.example.data.model.UiStyleMode
import com.example.data.model.UserPreferencesDoc
import com.example.engine.depth.ClockHorizontalAlign
import com.example.engine.notifications.LockNotificationGroup
import com.example.engine.notifications.LockNotificationItem
import com.example.engine.overlay.LockScreenOverlaySurface
import com.example.ui.screens.CanvasLookFilter
import com.example.ui.screens.PreviewStudioScreen
import com.example.ui.theme.BackdropTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.random.Random

/**
 * Dedicated 5-Part Stress Test Suite for Liquid Glass Optical Restoration,
 * Format/Signature Pill Legibility, 33-Face & 22-Font Stability, Multi-Aspect Framing,
 * and Cross-Surface Live Wallpaper Refraction.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class LiquidGlassFivePartStressTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun part1_opticalRefractionBevelNormalsChromaticDispersionAndHaloAudit() {
        val w = 180
        val h = 140

        // Multi-stop gradient backdrop with strong RGB variation across bevel edges
        val bg = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            val c = Canvas(this)
            val paint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, w.toFloat(), h.toFloat(),
                    intArrayOf(
                        AndroidColor.rgb(255, 72, 48),
                        AndroidColor.rgb(255, 224, 96),
                        AndroidColor.rgb(36, 172, 245)
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        }

        // Empty transparent bitmap must NOT pass bitmapHasVisibleAlpha
        val emptyMask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.TRANSPARENT)
        }
        assertFalse("Blank glyph mask must be rejected before LRU caching", LiquidGlassClockEngine.bitmapHasVisibleAlpha(emptyMask))

        // Full-opacity 1.0f glyph mask matching fixed GraphicsLayer capture
        val mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.TRANSPARENT)
            val c = Canvas(this)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE }
            c.drawRoundRect(52f, 26f, 128f, 114f, 14f, 14f, p)
        }
        assertTrue("Opaque glyph mask must pass visibility check", LiquidGlassClockEngine.bitmapHasVisibleAlpha(mask))

        val glyphMap = LiquidGlassClockEngine.buildMultiRadiusBevelHeightMap(mask, bevelWidthPx = 10f)

        // Core interior height (`R`) must reach deep interior bevel plateau (regression guard against 0.28f alpha dim collapse!)
        val corePx = glyphMap.getPixel(90, 70)
        val coreAlphaContainer = (corePx ushr 24) and 0xFF
        val coreHeight = (corePx ushr 16) and 0xFF
        val coreMask = (corePx ushr 8) and 0xFF
        assertEquals("Packed heightMap must keep A=255 so premultiplication never strips exterior halos", 255, coreAlphaContainer)
        assertTrue("Interior bevel height must reach full dynamic range (>235), got $coreHeight", coreHeight >= 235)
        assertEquals("Interior glyph mask in G channel must remain 255", 255, coreMask)

        // Exterior shadow halo in `B` just outside the glyph contour must stay preserved (not wiped by premul!)
        val exteriorHaloPx = glyphMap.getPixel(48, 70)
        val exteriorMask = (exteriorHaloPx ushr 8) and 0xFF
        val exteriorHalo = exteriorHaloPx and 0xFF
        assertEquals(0, exteriorMask)
        assertTrue("Diffused glyph drop-shadow halo outside glyph must remain preserved (>25), got $exteriorHalo", exteriorHalo > 25)

        val patch = LiquidGlassClockEngine.extractCroppedBackgroundPatch(
            backdropBitmap = bg,
            viewportSize = IntSize(w, h),
            clockBoxRect = ClockBoxRect(0f, 0f, w.toFloat(), h.toFloat()),
            zoomScale = 1f,
            panOffset = Offset.Zero,
            dimFraction = 0f,
            lookFilter = CanvasLookFilter.ORIGINAL,
            frostBlurPx = 3f,
            fallbackPrimary = Color.Black,
            fallbackSecondary = Color.Blue,
            patchWidth = w,
            patchHeight = h
        )

        val refractedCfg = LiquidGlassConfig() // Previous-gen 18px refraction, 1.48 IOR, 0.35 chromatic aberration
        val dispersedCfg = refractedCfg.copy(refractionStrength = 26f, ior = 1.64f, chromaticAberration = 0.90f, frostBlur = 1f)
        val flatCfg = dispersedCfg.copy(refractionStrength = 0f, chromaticAberration = 0f, specularIntensity = 0f, innerThicknessShadow = 0f)

        val glassOut = LiquidGlassClockEngine.renderOpticalGlassSoftwareBitmap(
            glyphMap = glyphMap,
            backgroundPatch = patch.unblurredPatch,
            frostedPatch = patch.frostedPatch,
            params = LiquidGlassClockEngine.resolveAdaptiveOptics(dispersedCfg, patch.avgLuminance)
        )
        val flatOut = LiquidGlassClockEngine.renderOpticalGlassSoftwareBitmap(
            glyphMap = glyphMap,
            backgroundPatch = patch.unblurredPatch,
            frostedPatch = patch.frostedPatch,
            params = LiquidGlassClockEngine.resolveAdaptiveOptics(flatCfg, patch.avgLuminance)
        )

        // Corners far from glyphs must remain strictly transparent
        assertEquals(0, (glassOut.getPixel(3, 3) ushr 24) and 0xFF)
        assertEquals(0, (glassOut.getPixel(w - 3, h - 3) ushr 24) and 0xFF)

        // Bevel displacement & chromatic divergence check
        var bevelDelta = 0
        var dispersionShift = 0
        for (x in 52..62) {
            val pGlass = glassOut.getPixel(x, 70)
            val pFlat = flatOut.getPixel(x, 70)
            val gR = (pGlass ushr 16) and 0xFF
            val gB = pGlass and 0xFF
            val fR = (pFlat ushr 16) and 0xFF
            val fG = (pFlat ushr 8) and 0xFF
            val fB = pFlat and 0xFF
            bevelDelta += abs(gR - fR) + abs(((pGlass ushr 8) and 0xFF) - fG) + abs(gB - fB)
            dispersionShift += abs((gR - gB) - (fR - fB))
        }
        assertTrue("Expected significant optical bevel displacement + Fresnel rim delta, got $bevelDelta", bevelDelta > 150)
        assertTrue("Expected measurable chromatic dispersion delta along bevel shoulder, got $dispersionShift", dispersionShift > 12)
    }

    @Test
    fun part2_formatLiveSecondsAndSignatureHighContrastPillStability() {
        var is24Hour by mutableStateOf(true)
        var sigMode by mutableStateOf(ClockSignatureMode.DATE_WEATHER)
        var sigText by mutableStateOf("")
        var activeFace by mutableStateOf(HyperOsClockFace.POSTER_SPLIT)

        val backdrop = Bitmap.createBitmap(200, 320, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.rgb(210, 232, 250)) // Bright sky backdrop to stress pill legibility
        }

        composeRule.setContent {
            BackdropTheme(
                themeMode = ThemeMode.LIGHT,
                amoledDark = false,
                uiStyle = UiStyleMode.FROSTED_GLASS_OS,
                useDynamicColor = false
            ) {
                Box(modifier = Modifier.size(360.dp, 640.dp)) {
                    HyperOsLockClock(
                        face = activeFace,
                        font = activeFace.recommendedFont,
                        finish = HyperOsClockFinish.BLUR_GLASS,
                        primaryColor = Color.White,
                        secondaryColor = Color(0xFF7EC8FF),
                        scaleFactor = 0.85f,
                        is24HourFormat = is24Hour,
                        showDateAndWeather = true,
                        widgetsEnabled = true,
                        showDateWidget = true,
                        showWeatherWidget = true,
                        showBatteryWidget = true,
                        telemetry = ClockWidgetTelemetry(weatherText = "24° CLEAR", batteryPercent = 91),
                        signatureMode = sigMode,
                        signatureText = sigText,
                        liquidGlassConfig = LiquidGlassConfig(),
                        backdropBitmap = backdrop,
                        viewportSize = IntSize(1080, 2400),
                        clockBoxRect = ClockBoxRect(180f, 320f, 720f, 320f)
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("liquid_glass_metadata_pill").assertIsDisplayed()
        composeRule.onNodeWithText("24° CLEAR", substring = true).assertIsDisplayed()

        // Toggle Format -> 12h and switch SignatureMode -> WEEKDAY_SECONDS + custom signature text
        composeRule.runOnUiThread {
            is24Hour = false
            sigMode = ClockSignatureMode.WEEKDAY_SECONDS
            sigText = "ATELIER 26"
            activeFace = HyperOsClockFace.MAGAZINE_STACKED
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("liquid_glass_metadata_pill").assertIsDisplayed()
        composeRule.onNodeWithText("AM", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(":28", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("ATELIER 26").assertIsDisplayed()

        // Switch SignatureMode -> EDITORIAL_TAG & KINETIC_MONO
        composeRule.runOnUiThread {
            sigMode = ClockSignatureMode.EDITORIAL_TAG
            activeFace = HyperOsClockFace.KINETIC_MONO
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("AM EDITION", substring = true).assertIsDisplayed()

        // Switch SignatureMode -> PURE_TIME (high-contrast pill cleanly hides)
        composeRule.runOnUiThread {
            sigMode = ClockSignatureMode.PURE_TIME
        }
        composeRule.waitForIdle()
        assertTrue(
            "PURE_TIME should hide metadata pill cleanly",
            composeRule.onAllNodesWithTag("liquid_glass_metadata_pill").fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun part3_highFrequencySliderAnd33Face22FontSweepStress() {
        val w = 128
        val h = 96
        val bg = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            val c = Canvas(this)
            val p = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, w.toFloat(), h.toFloat(),
                    AndroidColor.rgb(20, 44, 96),
                    AndroidColor.rgb(240, 145, 72),
                    Shader.TileMode.CLAMP
                )
            }
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        }
        val mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.TRANSPARENT)
            val c = Canvas(this)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE }
            c.drawRoundRect(24f, 18f, 104f, 78f, 10f, 10f, p)
        }
        val glyphMap = LiquidGlassClockEngine.buildMultiRadiusBevelHeightMap(mask, bevelWidthPx = 8f)

        val rng = Random(2026)
        val presets = LiquidGlassPreset.entries
        val filters = CanvasLookFilter.entries

        // 120 rapid randomized/extreme optical slider & look-filter renders
        repeat(120) { iter ->
            val preset = presets[iter % presets.size]
            val filter = filters[iter % filters.size]
            val cfg = LiquidGlassConfig(
                refractionStrength = if (iter % 15 == 0) 48f else rng.nextFloat() * 44f - 2f,
                ior = if (iter % 17 == 0) 2.2f else 0.90f + rng.nextFloat() * 1.1f,
                bevelWidth = 1f + rng.nextFloat() * 28f,
                chromaticAberration = rng.nextFloat() * 1.4f - 0.2f,
                frostBlur = rng.nextFloat() * 28f - 1f,
                glassSaturation = 0.8f + rng.nextFloat() * 1.6f,
                tintArgb = preset.tintArgb,
                tintOpacity = rng.nextFloat() * 0.7f,
                specularIntensity = rng.nextFloat() * 1.8f,
                lightAngle = rng.nextFloat() * 720f - 180f,
                innerThicknessShadow = rng.nextFloat() * 0.8f,
                backdropDropShadow = rng.nextFloat() * 1.3f,
                adaptiveContrast = (iter % 2 == 0)
            ).sanitized()

            assertFalse(cfg.refractionStrength.isNaN())
            assertFalse(cfg.ior.isNaN())
            assertTrue(cfg.refractionStrength in 0f..40f)
            assertTrue(cfg.ior in 1.00f..1.80f)

            val patch = LiquidGlassClockEngine.extractCroppedBackgroundPatch(
                backdropBitmap = bg,
                viewportSize = IntSize(320, 480),
                clockBoxRect = ClockBoxRect(40f, 80f, 220f, 140f),
                zoomScale = 0.85f + rng.nextFloat() * 3.15f,
                panOffset = Offset(rng.nextFloat() * 300f - 150f, rng.nextFloat() * 300f - 150f),
                dimFraction = rng.nextFloat() * 0.55f,
                lookFilter = filter,
                frostBlurPx = cfg.frostBlur,
                fallbackPrimary = Color.Black,
                fallbackSecondary = Color.White,
                patchWidth = w,
                patchHeight = h,
                rotationDegrees = if (iter % 6 == 0) (rng.nextFloat() * 60f - 30f) else 0f
            )
            val resolved = LiquidGlassClockEngine.resolveAdaptiveOptics(
                config = cfg,
                avgLuminance = patch.avgLuminance,
                blurMultiplier = 0.4f + rng.nextFloat() * 1.1f,
                refractionMultiplier = 0.4f + rng.nextFloat() * 1.1f
            )
            val rendered = LiquidGlassClockEngine.renderOpticalGlassSoftwareBitmap(
                glyphMap = glyphMap,
                backgroundPatch = patch.unblurredPatch,
                frostedPatch = patch.frostedPatch,
                params = resolved,
                scaleXAspect = 0.55f + rng.nextFloat() * 1.45f,
                scaleYAspect = 0.55f + rng.nextFloat() * 1.45f,
                opacity = 0.25f + rng.nextFloat() * 0.75f
            )
            assertEquals(w, rendered.width)
            assertEquals(h, rendered.height)
            assertTrue(((rendered.getPixel(w / 2, h / 2) ushr 24) and 0xFF) > 40)
        }

        // Sweep all 33 clock faces and all 22 bundled fonts under BLUR_GLASS in Compose
        var activeFace by mutableStateOf(HyperOsClockFace.entries.first())
        var activeFont by mutableStateOf(HyperOsClockFont.entries.first())
        var activeWeight by mutableStateOf(850)

        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.FROSTED_GLASS_OS, reducedMotionEnabled = true) {
                Box(modifier = Modifier.size(360.dp, 540.dp)) {
                    HyperOsLockClock(
                        face = activeFace,
                        font = activeFont,
                        finish = HyperOsClockFinish.BLUR_GLASS,
                        primaryColor = Color.White,
                        secondaryColor = Color(0xFFA6D8FF),
                        weightLevel = activeWeight,
                        scaleFactor = 0.85f,
                        backdropBitmap = bg,
                        viewportSize = IntSize(1080, 2200),
                        clockBoxRect = ClockBoxRect(140f, 220f, 800f, 360f)
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val allFaces = HyperOsClockFace.entries
        val allFonts = HyperOsClockFont.entries
        val weights = listOf(150, 400, 700, 900)
        for (i in allFaces.indices) {
            composeRule.runOnUiThread {
                activeFace = allFaces[i]
                activeFont = allFonts[i % allFonts.size]
                activeWeight = weights[i % weights.size]
            }
            composeRule.waitForIdle()
        }
    }

    @Test
    fun part4_multiAspectZoomPanRotationAndCropAlignmentStress() {
        // 4 distinct color quadrants so any crop shift produces measurably distinct RGB samples
        val srcW = 400
        val srcH = 400
        val src = Bitmap.createBitmap(srcW, srcH, Bitmap.Config.ARGB_8888).apply {
            val c = Canvas(this)
            c.drawRect(0f, 0f, 200f, 200f, Paint().apply { color = AndroidColor.rgb(245, 42, 42) })      // Top-Left: Red
            c.drawRect(200f, 0f, 400f, 200f, Paint().apply { color = AndroidColor.rgb(42, 225, 68) })    // Top-Right: Green
            c.drawRect(0f, 200f, 200f, 400f, Paint().apply { color = AndroidColor.rgb(42, 92, 245) })    // Bottom-Left: Blue
            c.drawRect(200f, 200f, 400f, 400f, Paint().apply { color = AndroidColor.rgb(245, 210, 42) }) // Bottom-Right: Gold
        }

        val viewports = listOf(
            IntSize(1080, 2400), // Tall Phone
            IntSize(1800, 1800), // Square Unfolded
            IntSize(2560, 1600)  // Tablet Landscape
        )

        viewports.forEach { vp ->
            val topLeftPatch = LiquidGlassClockEngine.extractCroppedBackgroundPatch(
                backdropBitmap = src,
                viewportSize = vp,
                clockBoxRect = ClockBoxRect(vp.width * 0.1f, vp.height * 0.1f, vp.width * 0.25f, vp.height * 0.15f),
                zoomScale = 2.0f,
                panOffset = Offset(220f, 220f),
                dimFraction = 0f,
                lookFilter = CanvasLookFilter.ORIGINAL,
                frostBlurPx = 0f,
                fallbackPrimary = Color.Black,
                fallbackSecondary = Color.White,
                patchWidth = 96,
                patchHeight = 72
            )
            val bottomRightPatch = LiquidGlassClockEngine.extractCroppedBackgroundPatch(
                backdropBitmap = src,
                viewportSize = vp,
                clockBoxRect = ClockBoxRect(vp.width * 0.65f, vp.height * 0.70f, vp.width * 0.25f, vp.height * 0.15f),
                zoomScale = 2.0f,
                panOffset = Offset(-220f, -220f),
                dimFraction = 0f,
                lookFilter = CanvasLookFilter.ORIGINAL,
                frostBlurPx = 0f,
                fallbackPrimary = Color.Black,
                fallbackSecondary = Color.White,
                patchWidth = 96,
                patchHeight = 72
            )

            val tlCenter = topLeftPatch.unblurredPatch.getPixel(48, 36)
            val brCenter = bottomRightPatch.unblurredPatch.getPixel(48, 36)
            assertTrue("Top-left panned patch should sample red quadrant, got ${Integer.toHexString(tlCenter)}",
                AndroidColor.red(tlCenter) > 150 && AndroidColor.blue(tlCenter) < 120)
            assertTrue("Bottom-right panned patch should sample gold/green-high quadrant, got ${Integer.toHexString(brCenter)}",
                AndroidColor.red(brCenter) > 150 && AndroidColor.green(brCenter) > 130)

            // Verify zoom-out (0.85x) + rotation + stretched aspect never throw or collapse bounds
            val mask = Bitmap.createBitmap(96, 72, Bitmap.Config.ARGB_8888).apply {
                eraseColor(AndroidColor.TRANSPARENT)
                Canvas(this).drawRoundRect(16f, 12f, 80f, 60f, 8f, 8f, Paint().apply { color = AndroidColor.WHITE })
            }
            val map = LiquidGlassClockEngine.buildMultiRadiusBevelHeightMap(mask, 8f)
            val zoomOutPatch = LiquidGlassClockEngine.extractCroppedBackgroundPatch(
                backdropBitmap = src,
                viewportSize = vp,
                clockBoxRect = ClockBoxRect(vp.width * 0.2f, vp.height * 0.2f, vp.width * 0.5f, vp.height * 0.2f),
                zoomScale = 0.85f,
                panOffset = Offset(0f, 0f),
                dimFraction = 0.2f,
                lookFilter = CanvasLookFilter.WARM_FILM,
                frostBlurPx = 6f,
                fallbackPrimary = Color.Black,
                fallbackSecondary = Color.White,
                patchWidth = 96,
                patchHeight = 72,
                rotationDegrees = -28f
            )
            val stretchedOut = LiquidGlassClockEngine.renderOpticalGlassSoftwareBitmap(
                glyphMap = map,
                backgroundPatch = zoomOutPatch.unblurredPatch,
                frostedPatch = zoomOutPatch.frostedPatch,
                params = LiquidGlassClockEngine.resolveAdaptiveOptics(LiquidGlassConfig(), zoomOutPatch.avgLuminance),
                scaleXAspect = 1.75f,
                scaleYAspect = 0.55f
            )
            assertTrue(((stretchedOut.getPixel(48, 36) ushr 24) and 0xFF) > 100)
        }
    }

    @Test
    fun part5_crossSurfacePreviewAndLockScreenLiveBackdropRefraction() {
        // Luminance adaptation verification: Bright sunny sky vs Dark OLED backdrop
        val base = LiquidGlassConfig(adaptiveContrast = true)
        val sunnySkyOptics = LiquidGlassClockEngine.resolveAdaptiveOptics(base, avgLuminance = 0.90f)
        val oledSkyOptics = LiquidGlassClockEngine.resolveAdaptiveOptics(base, avgLuminance = 0.08f)
        assertTrue(sunnySkyOptics.innerThicknessShadow > base.innerThicknessShadow + 0.10f)
        assertTrue(sunnySkyOptics.backdropDropShadow > base.backdropDropShadow + 0.20f)
        assertTrue(oledSkyOptics.specularIntensity > base.specularIntensity + 0.20f)

        val wallpaper = WallpaperCatalog.allWallpapers.first()
        val glassClockJson = ClockDesignConfig(
            face = HyperOsClockFace.POSTER_SPLIT,
            finish = HyperOsClockFinish.BLUR_GLASS,
            is24HourFormat = false,
            signatureMode = ClockSignatureMode.DATE_WEATHER,
            signatureText = "LIQUID EDITION",
            liquidGlass = LiquidGlassConfig()
        ).toJsonString()

        val sampleNow = System.currentTimeMillis()
        val sampleGroup = LockNotificationGroup(
            packageName = "com.example.chat",
            appName = "Messages",
            appIconBitmap = null,
            notifications = listOf(
                LockNotificationItem(
                    key = "n1",
                    packageName = "com.example.chat",
                    appName = "Messages",
                    senderOrTitle = "Alex",
                    bodyPreview = "See you at sunset",
                    postTime = sampleNow,
                    isSensitive = false,
                    isClearable = true,
                    contentIntent = null
                )
            ),
            latestTimestamp = sampleNow
        )
        val decodedConfig = ClockDesignConfig.fromJsonString(glassClockJson)!!
        var showLockOverlay by mutableStateOf(false)

        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.FROSTED_GLASS_OS, reducedMotionEnabled = true) {
                if (!showLockOverlay) {
                    PreviewStudioScreen(
                        wallpaper = wallpaper,
                        isApplying = false,
                        onBack = {},
                        onApplyWallpaper = { _, _, _ -> }
                    )
                } else {
                    LockScreenOverlaySurface(
                        selectedSet = LockScreenSetDoc(
                            id = "glass_set_1",
                            name = "Glass Preview",
                            wallpaperId = wallpaper.id,
                            clockConfigJson = glassClockJson,
                            zoomScale = 1.25f,
                            panOffsetX = 18f,
                            panOffsetY = -14f,
                            dimFraction = 0.12f
                        ),
                        clockConfig = decodedConfig,
                        telemetry = ClockWidgetTelemetry(weatherText = "19° BREEZE", batteryPercent = 82),
                        onDismissToUnlock = {},
                        preferences = UserPreferencesDoc(
                            activeLockWallpaperId = wallpaper.id,
                            uiStyle = UiStyleMode.FROSTED_GLASS_OS.name
                        ),
                        notificationGroups = listOf(sampleGroup),
                        isPreviewMode = true
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("preview_mode_Lock Screen").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("preview_clock_finish_row")
            .performScrollTo()
            .performScrollToNode(hasText("Liquid Glass"))
        composeRule.onNodeWithText("Liquid Glass").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("liquid_glass_metadata_pill").assertIsDisplayed()

        // Switch to LockScreenOverlaySurface with saved Liquid Glass set, telemetry, and notification card
        composeRule.runOnUiThread { showLockOverlay = true }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lockscreen_overlay_root").assertIsDisplayed()
        composeRule.onNodeWithTag("lock_screen_battery_badge").assertDoesNotExist()
        composeRule.onNodeWithTag("lockscreen_unlock_hint").assertIsDisplayed()
        composeRule.onNodeWithTag("liquid_glass_metadata_pill").assertIsDisplayed()
        composeRule.onNodeWithText("LIQUID EDITION").assertIsDisplayed()
    }
}
