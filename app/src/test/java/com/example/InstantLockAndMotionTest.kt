package com.example

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.core.SpringSpec
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.UiStyleMode
import com.example.engine.performance.ZeroLagPerformanceEngine
import com.example.ui.theme.AppMotion
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class InstantLockAndMotionTest {

    @After
    fun cleanUp() {
        ZeroLagPerformanceEngine.InstantLockScreenEngine.trimBitmaps()
        ZeroLagPerformanceEngine.clearCooldown("https://example.com/test_cooldown.jpg")
    }

    @Test
    fun instantLockBitmapsSurviveUiHiddenAndModerateTrimButClearOnComplete() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wpBitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        val cutoutBitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)

        ZeroLagPerformanceEngine.InstantLockScreenEngine.updateWarmBitmaps(
            wallpaperId = "abs_01",
            wallpaperBitmap = wpBitmap,
            cutoutBitmap = cutoutBitmap,
            artworkKey = "set_a|abs_01||",
            activeSetId = "set_a"
        )

        ZeroLagPerformanceEngine.handleTrimMemory(context, ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        val afterUiHidden = ZeroLagPerformanceEngine.InstantLockScreenEngine.warmState.value
        assertNotNull(afterUiHidden?.wallpaperBitmap)
        assertNotNull(afterUiHidden?.cutoutBitmap)

        ZeroLagPerformanceEngine.handleTrimMemory(context, ComponentCallbacks2.TRIM_MEMORY_MODERATE)
        val afterModerate = ZeroLagPerformanceEngine.InstantLockScreenEngine.warmState.value
        assertNotNull(afterModerate?.wallpaperBitmap)
        assertNotNull(afterModerate?.cutoutBitmap)

        ZeroLagPerformanceEngine.handleTrimMemory(context, ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        val afterComplete = ZeroLagPerformanceEngine.InstantLockScreenEngine.warmState.value
        assertNull(afterComplete?.wallpaperBitmap)
        assertNull(afterComplete?.cutoutBitmap)
    }

    @Test
    fun failedPreloadCooldownExpiresAfter45Seconds() {
        val url = "https://example.com/test_cooldown.jpg"
        val t0 = 1_000_000L
        ZeroLagPerformanceEngine.recordFailureCooldown(url, nowMs = t0)
        assertTrue(ZeroLagPerformanceEngine.isUrlOnCooldown(url, nowMs = t0 + 10_000L))
        assertTrue(ZeroLagPerformanceEngine.isUrlOnCooldown(url, nowMs = t0 + 44_999L))
        assertFalse(ZeroLagPerformanceEngine.isUrlOnCooldown(url, nowMs = t0 + 45_000L))
    }

    @Test
    fun dualModeLockMotionSpecsDifferentiateFrostedSpringsAndMaterialCriticallyDampedMotion() {
        val frostedDrag: SpringSpec<Float> = AppMotion.lockWidgetDragLiftSpring(UiStyleMode.FROSTED_GLASS_OS)
        val m3Drag: SpringSpec<Float> = AppMotion.lockWidgetDragLiftSpring(UiStyleMode.MATERIAL_3)
        assertTrue(frostedDrag.dampingRatio < 0.60f)
        assertTrue(m3Drag.dampingRatio >= 0.85f)
        assertTrue(m3Drag.stiffness > frostedDrag.stiffness)

        val frostedSnap: SpringSpec<Float> = AppMotion.lockWidgetSnapSpring(UiStyleMode.FROSTED_GLASS_OS)
        val m3Snap: SpringSpec<Float> = AppMotion.lockWidgetSnapSpring(UiStyleMode.MATERIAL_3)
        assertTrue(frostedSnap.dampingRatio < 0.60f)
        assertTrue(m3Snap.dampingRatio >= 0.85f)

        val frostedNotif: SpringSpec<Float> = AppMotion.lockNotificationDeckSpring(UiStyleMode.FROSTED_GLASS_OS)
        val m3Notif: SpringSpec<Float> = AppMotion.lockNotificationDeckSpring(UiStyleMode.MATERIAL_3)
        assertTrue(frostedNotif.dampingRatio < 0.60f)
        assertTrue(m3Notif.dampingRatio >= 0.85f)

        val frostedMediaEnter = AppMotion.lockMediaOverlayEnter(UiStyleMode.FROSTED_GLASS_OS)
        val m3MediaEnter = AppMotion.lockMediaOverlayEnter(UiStyleMode.MATERIAL_3)
        assertTrue(frostedMediaEnter != m3MediaEnter)

        val frostedChargingEnter = AppMotion.lockChargingOverlayEnter(UiStyleMode.FROSTED_GLASS_OS)
        val m3ChargingEnter = AppMotion.lockChargingOverlayEnter(UiStyleMode.MATERIAL_3)
        assertTrue(frostedChargingEnter != m3ChargingEnter)
    }
}
