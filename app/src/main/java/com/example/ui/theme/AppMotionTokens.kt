package com.example.ui.theme

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.data.model.UiStyleMode

/**
 * Animation Engine V5 — Sub-Engine A: Spring Physics Solver.
 * Distinct physical damping & stiffness curves for Material 3 vs Liquid Glass (Frosted Glass OS).
 */
internal object SpringPhysicsSubEngine {
    fun <T> spatialSpring(uiStyle: UiStyleMode, reducedMotion: Boolean, speedMultiplier: Float): SpringSpec<T> {
        if (reducedMotion) return spring(dampingRatio = 0.98f, stiffness = 440f)
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> spring(dampingRatio = 0.88f, stiffness = 560f * speed)
            UiStyleMode.FROSTED_GLASS_OS -> spring(dampingRatio = 0.44f, stiffness = 275f * speed)
            UiStyleMode.GLYPH_UI -> spring(dampingRatio = 1.0f, stiffness = 940f * speed)
        }
    }

    fun <T> emphasisSpring(uiStyle: UiStyleMode, reducedMotion: Boolean, speedMultiplier: Float): SpringSpec<T> {
        if (reducedMotion) return spring(dampingRatio = 0.98f, stiffness = 460f)
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> spring(dampingRatio = 0.90f, stiffness = 620f * speed)
            UiStyleMode.FROSTED_GLASS_OS -> spring(dampingRatio = 0.40f, stiffness = 245f * speed)
            UiStyleMode.GLYPH_UI -> spring(dampingRatio = 1.0f, stiffness = 1080f * speed)
        }
    }
}

/**
 * Animation Engine V5 — Sub-Engine B: Screen Choreography & Tactile Motion Curves.
 */
object AppMotion {

    fun <T> spatialSpring(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean = false,
        speedMultiplier: Float = 1.0f
    ): SpringSpec<T> = SpringPhysicsSubEngine.spatialSpring(uiStyle, reducedMotion, speedMultiplier)

    fun <T> emphasisSpring(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean = false,
        speedMultiplier: Float = 1.0f
    ): SpringSpec<T> = SpringPhysicsSubEngine.emphasisSpring(uiStyle, reducedMotion, speedMultiplier)

    fun <T> glassMorphSpring(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean = false,
        speedMultiplier: Float = 1.0f
    ): SpringSpec<T> {
        if (reducedMotion) {
            return spring(dampingRatio = 0.96f, stiffness = 440f)
        }
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> spring(dampingRatio = 0.84f, stiffness = 520f * speed)
            UiStyleMode.FROSTED_GLASS_OS -> spring(dampingRatio = 0.42f, stiffness = 235f * speed)
            UiStyleMode.GLYPH_UI -> spring(dampingRatio = 1.0f, stiffness = 900f * speed)
        }
    }

    fun floatEffectsSpec(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean = false,
        speedMultiplier: Float = 1.0f
    ): FiniteAnimationSpec<Float> {
        if (reducedMotion) return tween(120, easing = FastOutSlowInEasing)
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> spring(dampingRatio = 0.90f, stiffness = 540f * speed)
            UiStyleMode.FROSTED_GLASS_OS -> spring(dampingRatio = 0.52f, stiffness = 285f * speed)
            UiStyleMode.GLYPH_UI -> tween((115f / speed).toInt().coerceIn(65, 190), easing = LinearEasing)
        }
    }

    fun colorSpec(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean = false,
        speedMultiplier: Float = 1.0f
    ): FiniteAnimationSpec<Color> {
        if (reducedMotion) return tween(110, easing = FastOutSlowInEasing)
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> tween((190 / speed).toInt().coerceIn(90, 320), easing = FastOutSlowInEasing)
            UiStyleMode.FROSTED_GLASS_OS -> spring(dampingRatio = 0.68f, stiffness = 310f * speed)
            UiStyleMode.GLYPH_UI -> tween((95f / speed).toInt().coerceIn(55, 160), easing = LinearEasing)
        }
    }

    fun screenEnter(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean,
        speedMultiplier: Float = 1.0f
    ): EnterTransition {
        if (reducedMotion) return fadeIn(tween(140))
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> {
                fadeIn(tween((200f / speed).toInt().coerceIn(90, 340), easing = FastOutSlowInEasing)) +
                    slideInVertically(
                        animationSpec = spring(dampingRatio = 0.86f, stiffness = 520f * speed)
                    ) { it / 26 }
            }
            UiStyleMode.FROSTED_GLASS_OS -> {
                fadeIn(tween((240f / speed).toInt().coerceIn(110, 400), easing = FastOutSlowInEasing)) +
                    scaleIn(
                        initialScale = 0.89f,
                        animationSpec = spring(dampingRatio = 0.48f, stiffness = 280f * speed)
                    )
            }
            UiStyleMode.GLYPH_UI -> {
                val dur = (130f / speed).toInt().coerceIn(70, 210)
                fadeIn(tween(dur, easing = LinearEasing)) +
                    slideInVertically(tween(dur, easing = FastOutSlowInEasing)) { it / 32 }
            }
        }
    }

    fun screenExit(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean,
        speedMultiplier: Float = 1.0f
    ): ExitTransition {
        if (reducedMotion) return fadeOut(tween(120))
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> {
                fadeOut(tween((160f / speed).toInt().coerceIn(80, 280), easing = FastOutSlowInEasing)) +
                    slideOutVertically(tween((170f / speed).toInt().coerceIn(80, 290), easing = FastOutSlowInEasing)) { -it / 30 }
            }
            UiStyleMode.FROSTED_GLASS_OS -> {
                fadeOut(tween((180f / speed).toInt().coerceIn(85, 310), easing = FastOutSlowInEasing)) +
                    scaleOut(
                        targetScale = 1.045f,
                        animationSpec = tween((180f / speed).toInt().coerceIn(85, 310), easing = FastOutSlowInEasing)
                    )
            }
            UiStyleMode.GLYPH_UI -> {
                fadeOut(tween((100f / speed).toInt().coerceIn(55, 165), easing = LinearEasing))
            }
        }
    }

    fun clockFaceEnter(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean,
        speedMultiplier: Float = 1.0f
    ): EnterTransition {
        if (reducedMotion) return fadeIn(tween(110))
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> {
                fadeIn(tween((180f / speed).toInt().coerceIn(85, 310), easing = FastOutSlowInEasing)) +
                    slideInVertically(
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 560f * speed)
                    ) { it / 10 } +
                    scaleIn(
                        initialScale = 0.94f,
                        animationSpec = spring(dampingRatio = 0.84f, stiffness = 540f * speed)
                    )
            }
            UiStyleMode.FROSTED_GLASS_OS -> {
                fadeIn(tween((240f / speed).toInt().coerceIn(110, 410), easing = FastOutSlowInEasing)) +
                    scaleIn(
                        initialScale = 0.84f,
                        animationSpec = spring(dampingRatio = 0.44f, stiffness = 240f * speed)
                    ) +
                    slideInVertically(
                        animationSpec = spring(dampingRatio = 0.48f, stiffness = 260f * speed)
                    ) { it / 16 }
            }
            UiStyleMode.GLYPH_UI -> {
                val dur = (130f / speed).toInt().coerceIn(70, 210)
                fadeIn(tween(dur, easing = LinearEasing)) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(dur, easing = LinearEasing))
            }
        }
    }

    fun clockFaceExit(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean,
        speedMultiplier: Float = 1.0f
    ): ExitTransition {
        if (reducedMotion) return fadeOut(tween(90))
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> {
                fadeOut(tween((140f / speed).toInt().coerceIn(70, 240), easing = FastOutSlowInEasing)) +
                    slideOutVertically(
                        animationSpec = tween((150f / speed).toInt().coerceIn(75, 250), easing = FastOutSlowInEasing)
                    ) { -it / 12 } +
                    scaleOut(
                        targetScale = 0.96f,
                        animationSpec = tween((150f / speed).toInt().coerceIn(75, 250), easing = FastOutSlowInEasing)
                    )
            }
            UiStyleMode.FROSTED_GLASS_OS -> {
                fadeOut(tween((170f / speed).toInt().coerceIn(80, 290), easing = FastOutSlowInEasing)) +
                    scaleOut(
                        targetScale = 1.06f,
                        animationSpec = spring(dampingRatio = 0.62f, stiffness = 310f * speed)
                    )
            }
            UiStyleMode.GLYPH_UI -> {
                fadeOut(tween((95f / speed).toInt().coerceIn(50, 160), easing = LinearEasing))
            }
        }
    }

    fun drawerEnter(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean,
        speedMultiplier: Float = 1.0f
    ): EnterTransition {
        if (reducedMotion) return fadeIn(tween(120))
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> {
                fadeIn(tween((170f / speed).toInt().coerceIn(80, 280))) +
                    expandVertically(spring(dampingRatio = 0.88f, stiffness = 540f * speed))
            }
            UiStyleMode.FROSTED_GLASS_OS -> {
                fadeIn(tween((220f / speed).toInt().coerceIn(100, 360))) +
                    expandVertically(spring(dampingRatio = 0.52f, stiffness = 290f * speed)) +
                    scaleIn(initialScale = 0.93f, animationSpec = spring(dampingRatio = 0.48f, stiffness = 300f * speed))
            }
            UiStyleMode.GLYPH_UI -> {
                val dur = (130f / speed).toInt().coerceIn(70, 210)
                fadeIn(tween(dur, easing = LinearEasing)) +
                    expandVertically(tween(dur, easing = FastOutSlowInEasing))
            }
        }
    }

    fun drawerExit(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean,
        speedMultiplier: Float = 1.0f
    ): ExitTransition {
        if (reducedMotion) return fadeOut(tween(100))
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> {
                fadeOut(tween((140f / speed).toInt().coerceIn(70, 240))) +
                    shrinkVertically(tween((170f / speed).toInt().coerceIn(80, 290), easing = FastOutSlowInEasing))
            }
            UiStyleMode.FROSTED_GLASS_OS -> {
                fadeOut(tween((155f / speed).toInt().coerceIn(75, 260), easing = FastOutSlowInEasing)) +
                    shrinkVertically(
                        animationSpec = tween((185f / speed).toInt().coerceIn(85, 300), easing = FastOutSlowInEasing),
                        shrinkTowards = androidx.compose.ui.Alignment.Top
                    )
            }
            UiStyleMode.GLYPH_UI -> {
                val dur = (105f / speed).toInt().coerceIn(60, 170)
                fadeOut(tween(dur, easing = LinearEasing)) +
                    shrinkVertically(tween(dur, easing = LinearEasing))
            }
        }
    }

    val MaterialEmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val MaterialEmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    fun <T> lockWidgetDragLiftSpring(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean = false,
        speedMultiplier: Float = 1.0f
    ): SpringSpec<T> {
        if (reducedMotion) return spring(dampingRatio = 0.96f, stiffness = 460f)
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> spring(dampingRatio = 0.88f, stiffness = 540f * speed)
            UiStyleMode.FROSTED_GLASS_OS -> spring(dampingRatio = 0.46f, stiffness = 290f * speed)
            UiStyleMode.GLYPH_UI -> spring(dampingRatio = 1.0f, stiffness = 980f * speed)
        }
    }

    fun <T> lockWidgetSnapSpring(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean = false,
        speedMultiplier: Float = 1.0f
    ): SpringSpec<T> {
        if (reducedMotion) return spring(dampingRatio = 0.98f, stiffness = 460f)
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> spring(dampingRatio = 0.90f, stiffness = 520f * speed)
            UiStyleMode.FROSTED_GLASS_OS -> spring(dampingRatio = 0.48f, stiffness = 260f * speed)
            UiStyleMode.GLYPH_UI -> spring(dampingRatio = 1.0f, stiffness = 920f * speed)
        }
    }

    fun <T> lockNotificationDeckSpring(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean = false,
        speedMultiplier: Float = 1.0f
    ): SpringSpec<T> {
        if (reducedMotion) return spring(dampingRatio = 0.96f, stiffness = 440f)
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> spring(dampingRatio = 0.88f, stiffness = 490f * speed)
            UiStyleMode.FROSTED_GLASS_OS -> spring(dampingRatio = 0.50f, stiffness = 255f * speed)
            UiStyleMode.GLYPH_UI -> spring(dampingRatio = 1.0f, stiffness = 860f * speed)
        }
    }

    fun lockMediaOverlayEnter(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean = false,
        speedMultiplier: Float = 1.0f
    ): EnterTransition {
        if (reducedMotion) return fadeIn(tween(120))
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> {
                val dur = (280f / speed).toInt().coerceIn(120, 440)
                fadeIn(tween(dur, easing = MaterialEmphasizedDecelerate)) +
                    scaleIn(
                        initialScale = 0.94f,
                        animationSpec = tween(dur, easing = MaterialEmphasizedDecelerate)
                    )
            }
            UiStyleMode.FROSTED_GLASS_OS -> {
                fadeIn(tween((220f / speed).toInt().coerceIn(100, 360), easing = FastOutSlowInEasing)) +
                    scaleIn(
                        initialScale = 0.88f,
                        animationSpec = spring(dampingRatio = 0.46f, stiffness = 255f * speed)
                    )
            }
            UiStyleMode.GLYPH_UI -> {
                val dur = (130f / speed).toInt().coerceIn(70, 210)
                fadeIn(tween(dur, easing = LinearEasing)) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(dur, easing = LinearEasing))
            }
        }
    }

    fun lockMediaOverlayExit(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean = false,
        speedMultiplier: Float = 1.0f
    ): ExitTransition {
        if (reducedMotion) return fadeOut(tween(100))
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> {
                val dur = (190f / speed).toInt().coerceIn(90, 300)
                fadeOut(tween(dur, easing = MaterialEmphasizedAccelerate)) +
                    scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(dur, easing = MaterialEmphasizedAccelerate)
                    )
            }
            UiStyleMode.FROSTED_GLASS_OS -> {
                fadeOut(tween((180f / speed).toInt().coerceIn(85, 290), easing = FastOutSlowInEasing)) +
                    scaleOut(
                        targetScale = 1.05f,
                        animationSpec = spring(dampingRatio = 0.58f, stiffness = 310f * speed)
                    )
            }
            UiStyleMode.GLYPH_UI -> {
                fadeOut(tween((100f / speed).toInt().coerceIn(55, 165), easing = LinearEasing))
            }
        }
    }

    fun lockChargingOverlayEnter(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean = false,
        speedMultiplier: Float = 1.0f
    ): EnterTransition {
        if (reducedMotion) return fadeIn(tween(120))
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> {
                val dur = (260f / speed).toInt().coerceIn(110, 400)
                fadeIn(tween(dur, easing = MaterialEmphasizedDecelerate)) +
                    scaleIn(initialScale = 0.92f, animationSpec = tween(dur, easing = MaterialEmphasizedDecelerate))
            }
            UiStyleMode.FROSTED_GLASS_OS -> {
                fadeIn(tween((220f / speed).toInt().coerceIn(100, 350))) +
                    scaleIn(initialScale = 0.82f, animationSpec = spring(dampingRatio = 0.44f, stiffness = 250f * speed))
            }
            UiStyleMode.GLYPH_UI -> {
                fadeIn(tween((135f / speed).toInt().coerceIn(70, 210), easing = LinearEasing))
            }
        }
    }

    fun lockChargingOverlayExit(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean = false,
        speedMultiplier: Float = 1.0f
    ): ExitTransition {
        if (reducedMotion) return fadeOut(tween(100))
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> {
                val dur = (180f / speed).toInt().coerceIn(85, 290)
                fadeOut(tween(dur, easing = MaterialEmphasizedAccelerate)) +
                    scaleOut(targetScale = 0.94f, animationSpec = tween(dur, easing = MaterialEmphasizedAccelerate))
            }
            UiStyleMode.FROSTED_GLASS_OS -> {
                fadeOut(tween((175f / speed).toInt().coerceIn(85, 280))) +
                    scaleOut(targetScale = 1.04f, animationSpec = spring(dampingRatio = 0.60f, stiffness = 300f * speed))
            }
            UiStyleMode.GLYPH_UI -> {
                fadeOut(tween((100f / speed).toInt().coerceIn(55, 165), easing = LinearEasing))
            }
        }
    }

    fun lockNotifContentEnter(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean = false,
        speedMultiplier: Float = 1.0f
    ): EnterTransition {
        if (reducedMotion) return fadeIn(tween(110))
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> {
                val dur = (240f / speed).toInt().coerceIn(100, 380)
                fadeIn(tween(dur, easing = MaterialEmphasizedDecelerate)) +
                    slideInVertically(tween(dur, easing = MaterialEmphasizedDecelerate)) { it / 8 }
            }
            UiStyleMode.FROSTED_GLASS_OS -> {
                fadeIn(tween((220f / speed).toInt().coerceIn(100, 360))) +
                    slideInVertically(spring(dampingRatio = 0.50f, stiffness = 265f * speed)) { it / 10 } +
                    scaleIn(initialScale = 0.93f, animationSpec = spring(dampingRatio = 0.52f, stiffness = 280f * speed))
            }
            UiStyleMode.GLYPH_UI -> {
                val dur = (125f / speed).toInt().coerceIn(65, 200)
                fadeIn(tween(dur, easing = LinearEasing)) +
                    slideInVertically(tween(dur, easing = FastOutSlowInEasing)) { it / 14 }
            }
        }
    }

    fun lockNotifContentExit(
        uiStyle: UiStyleMode,
        reducedMotion: Boolean = false,
        speedMultiplier: Float = 1.0f
    ): ExitTransition {
        if (reducedMotion) return fadeOut(tween(90))
        val speed = speedMultiplier.coerceIn(0.6f, 1.5f)
        return when (uiStyle) {
            UiStyleMode.MATERIAL_3 -> {
                val dur = (170f / speed).toInt().coerceIn(80, 270)
                fadeOut(tween(dur, easing = MaterialEmphasizedAccelerate)) +
                    slideOutVertically(tween(dur, easing = MaterialEmphasizedAccelerate)) { -it / 10 }
            }
            UiStyleMode.FROSTED_GLASS_OS -> {
                fadeOut(tween((160f / speed).toInt().coerceIn(80, 260), easing = FastOutSlowInEasing)) +
                    slideOutVertically(tween((165f / speed).toInt().coerceIn(80, 270), easing = FastOutSlowInEasing)) { -it / 12 }
            }
            UiStyleMode.GLYPH_UI -> {
                fadeOut(tween((95f / speed).toInt().coerceIn(50, 160), easing = LinearEasing))
            }
        }
    }
}

/**
 * Hardware-backed haptic engine combining Android Vibrator / VibratorManager `VibrationEffect`
 * (predefined crisp detents on API 29+, one-shot fallback API 24-28) with Compose HapticFeedback.
 */
class StudioHapticsController(
    private val context: Context?,
    private val haptic: HapticFeedback,
    private val enabled: Boolean
) {
    private var lastTickNs: Long = 0L

    private val vibrator: Vibrator? by lazy(LazyThreadSafetyMode.NONE) {
        val ctx = context?.applicationContext ?: return@lazy null
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                mgr?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }.getOrNull()
    }

    private fun vibrateHardware(effectIdApi29: Int, durationMs: Long, amplitude: Int, fallbackCompose: HapticFeedbackType) {
        val vib = vibrator
        val firedHardware = if (vib != null && runCatching { vib.hasVibrator() }.getOrDefault(false)) {
            runCatching {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                        vib.vibrate(VibrationEffect.createPredefined(effectIdApi29))
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        vib.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
                    }
                    else -> {
                        @Suppress("DEPRECATION")
                        vib.vibrate(durationMs)
                    }
                }
                true
            }.getOrDefault(false)
        } else {
            false
        }
        if (!firedHardware) {
            runCatching { haptic.performHapticFeedback(fallbackCompose) }
        }
    }

    fun performTick(minIntervalMs: Long = 40L) {
        if (!enabled) return
        val now = System.nanoTime()
        if ((now - lastTickNs) >= minIntervalMs * 1_000_000L) {
            lastTickNs = now
            vibrateHardware(
                effectIdApi29 = VibrationEffect.EFFECT_TICK,
                durationMs = 12L,
                amplitude = 90,
                fallbackCompose = HapticFeedbackType.TextHandleMove
            )
        }
    }

    fun performConfirm() {
        if (!enabled) return
        lastTickNs = System.nanoTime()
        vibrateHardware(
            effectIdApi29 = VibrationEffect.EFFECT_CLICK,
            durationMs = 20L,
            amplitude = 145,
            fallbackCompose = HapticFeedbackType.LongPress
        )
    }

    fun performSnapDetent() {
        if (!enabled) return
        val now = System.nanoTime()
        if ((now - lastTickNs) >= 55L * 1_000_000L) {
            lastTickNs = now
            vibrateHardware(
                effectIdApi29 = VibrationEffect.EFFECT_HEAVY_CLICK,
                durationMs = 26L,
                amplitude = 185,
                fallbackCompose = HapticFeedbackType.LongPress
            )
        }
    }

    fun performBoundaryBounce() {
        if (!enabled) return
        val now = System.nanoTime()
        if ((now - lastTickNs) >= 110L * 1_000_000L) {
            lastTickNs = now
            vibrateHardware(
                effectIdApi29 = VibrationEffect.EFFECT_DOUBLE_CLICK,
                durationMs = 18L,
                amplitude = 120,
                fallbackCompose = HapticFeedbackType.TextHandleMove
            )
        }
    }
}

@Composable
fun rememberStudioHaptics(): StudioHapticsController {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val enabled = LocalHapticsEnabled.current
    return remember(context, haptic, enabled) { StudioHapticsController(context, haptic, enabled) }
}
