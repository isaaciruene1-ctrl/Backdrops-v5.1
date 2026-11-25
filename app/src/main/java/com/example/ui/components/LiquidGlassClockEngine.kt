package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint as AndroidPaint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.createBitmap
import com.example.engine.crash.ZeroCrashEngine
import com.example.ui.screens.CanvasLookFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class LiquidGlassPreset(
    val label: String,
    val tintArgb: Long,
    val tintOpacity: Float,
    val saturation: Float,
    val chromaticAberration: Float
) {
    CLEAR_COLORLESS("Colorless", 0xFFFFFFFFL, 0.0f, 1.42f, 0.24f),
    CLEAR_CRYSTAL("Clear Crystal", 0xFFFFFFFFL, 0.08f, 1.45f, 0.30f),
    COOL_ICE("Cool Ice", 0xFFCFE8FFL, 0.14f, 1.48f, 0.38f),
    WARM_QUARTZ("Warm Quartz", 0xFFFFE0C2L, 0.15f, 1.52f, 0.28f),
    PRISMATIC("Prismatic", 0xFFEFE6FFL, 0.12f, 1.70f, 0.72f)
}

/**
 * Physically-inspired Liquid Glass optical parameter set exposed with exact bounded ranges
 * and JSON serialization inside [ClockDesignConfig].
 */
data class LiquidGlassConfig(
    val refractionStrength: Float = DEFAULT_REFRACTION_STRENGTH,
    val ior: Float = DEFAULT_IOR,
    val bevelWidth: Float = DEFAULT_BEVEL_WIDTH,
    val chromaticAberration: Float = DEFAULT_CHROMATIC_ABERRATION,
    val frostBlur: Float = DEFAULT_FROST_BLUR,
    val glassSaturation: Float = DEFAULT_GLASS_SATURATION,
    val tintArgb: Long = DEFAULT_TINT_ARGB,
    val tintOpacity: Float = DEFAULT_TINT_OPACITY,
    val specularIntensity: Float = DEFAULT_SPECULAR_INTENSITY,
    val lightAngle: Float = DEFAULT_LIGHT_ANGLE,
    val innerThicknessShadow: Float = DEFAULT_INNER_THICKNESS_SHADOW,
    val backdropDropShadow: Float = DEFAULT_BACKDROP_DROP_SHADOW,
    val adaptiveContrast: Boolean = true
) {
    fun sanitized(): LiquidGlassConfig = copy(
        refractionStrength = refractionStrength.coerceIn(0f, 40f),
        ior = ior.coerceIn(1.00f, 1.80f),
        bevelWidth = bevelWidth.coerceIn(2f, 24f),
        chromaticAberration = chromaticAberration.coerceIn(0f, 1.0f),
        frostBlur = frostBlur.coerceIn(0f, 24f),
        glassSaturation = glassSaturation.coerceIn(1.0f, 2.0f),
        tintOpacity = tintOpacity.coerceIn(0f, 0.50f),
        specularIntensity = specularIntensity.coerceIn(0f, 1.50f),
        lightAngle = ((lightAngle % 360f) + 360f) % 360f,
        innerThicknessShadow = innerThicknessShadow.coerceIn(0f, 0.60f),
        backdropDropShadow = backdropDropShadow.coerceIn(0f, 1.0f)
    )

    fun matchesPreset(preset: LiquidGlassPreset): Boolean {
        val rgbMatches = (preset.tintArgb and 0x00FFFFFFL) == (tintArgb and 0x00FFFFFFL)
        if (!rgbMatches) return false
        return when (preset) {
            LiquidGlassPreset.CLEAR_COLORLESS -> tintOpacity <= 0.025f
            LiquidGlassPreset.CLEAR_CRYSTAL -> tintOpacity > 0.025f
            else -> true
        }
    }

    fun activePresetOrNull(): LiquidGlassPreset? =
        LiquidGlassPreset.entries.firstOrNull { matchesPreset(it) }

    fun toJsonObject(): JSONObject = JSONObject().apply {
        val s = sanitized()
        put("refractionStrength", s.refractionStrength.toDouble())
        put("ior", s.ior.toDouble())
        put("bevelWidth", s.bevelWidth.toDouble())
        put("chromaticAberration", s.chromaticAberration.toDouble())
        put("frostBlur", s.frostBlur.toDouble())
        put("glassSaturation", s.glassSaturation.toDouble())
        put("tintArgb", s.tintArgb)
        put("tintOpacity", s.tintOpacity.toDouble())
        put("specularIntensity", s.specularIntensity.toDouble())
        put("lightAngle", s.lightAngle.toDouble())
        put("innerThicknessShadow", s.innerThicknessShadow.toDouble())
        put("backdropDropShadow", s.backdropDropShadow.toDouble())
        put("adaptiveContrast", s.adaptiveContrast)
    }

    companion object {
        const val DEFAULT_REFRACTION_STRENGTH = 18.0f
        const val DEFAULT_IOR = 1.48f
        const val DEFAULT_BEVEL_WIDTH = 10.0f
        const val DEFAULT_CHROMATIC_ABERRATION = 0.35f
        const val DEFAULT_FROST_BLUR = 6.0f
        const val DEFAULT_GLASS_SATURATION = 1.45f
        const val DEFAULT_TINT_ARGB = 0xFFFFFFFFL
        const val DEFAULT_TINT_OPACITY = 0.10f
        const val DEFAULT_SPECULAR_INTENSITY = 0.85f
        const val DEFAULT_LIGHT_ANGLE = 135.0f
        const val DEFAULT_INNER_THICKNESS_SHADOW = 0.22f
        const val DEFAULT_BACKDROP_DROP_SHADOW = 0.35f

        fun fromJsonObject(obj: JSONObject?): LiquidGlassConfig {
            if (obj == null) return LiquidGlassConfig()
            return LiquidGlassConfig(
                refractionStrength = obj.optDouble("refractionStrength", DEFAULT_REFRACTION_STRENGTH.toDouble()).toFloat(),
                ior = obj.optDouble("ior", DEFAULT_IOR.toDouble()).toFloat(),
                bevelWidth = obj.optDouble("bevelWidth", DEFAULT_BEVEL_WIDTH.toDouble()).toFloat(),
                chromaticAberration = obj.optDouble("chromaticAberration", DEFAULT_CHROMATIC_ABERRATION.toDouble()).toFloat(),
                frostBlur = obj.optDouble("frostBlur", DEFAULT_FROST_BLUR.toDouble()).toFloat(),
                glassSaturation = obj.optDouble("glassSaturation", DEFAULT_GLASS_SATURATION.toDouble()).toFloat(),
                tintArgb = obj.optLong("tintArgb", DEFAULT_TINT_ARGB),
                tintOpacity = obj.optDouble("tintOpacity", DEFAULT_TINT_OPACITY.toDouble()).toFloat(),
                specularIntensity = obj.optDouble("specularIntensity", DEFAULT_SPECULAR_INTENSITY.toDouble()).toFloat(),
                lightAngle = obj.optDouble("lightAngle", DEFAULT_LIGHT_ANGLE.toDouble()).toFloat(),
                innerThicknessShadow = obj.optDouble("innerThicknessShadow", DEFAULT_INNER_THICKNESS_SHADOW.toDouble()).toFloat(),
                backdropDropShadow = obj.optDouble("backdropDropShadow", DEFAULT_BACKDROP_DROP_SHADOW.toDouble()).toFloat(),
                adaptiveContrast = obj.optBoolean("adaptiveContrast", true)
            ).sanitized()
        }
    }
}

internal data class ResolvedOpticalParams(
    val avgLuminance: Float,
    val refractionStrength: Float,
    val ior: Float,
    val bevelWidth: Float,
    val chromaticAberration: Float,
    val frostBlur: Float,
    val glassSaturation: Float,
    val tintR: Float,
    val tintG: Float,
    val tintB: Float,
    val tintOpacity: Float,
    val specularIntensity: Float,
    val lightAngleRad: Float,
    val innerThicknessShadow: Float,
    val backdropDropShadow: Float
)

internal data class BackdropOpticalPatch(
    val unblurredPatch: Bitmap,
    val frostedPatch: Bitmap,
    val avgLuminance: Float
)

/**
 * Multi-pass optical Liquid Glass rendering engine with:
 * - Pass 1: Live cropped/zoomed/panned wallpaper pixels underneath the clock box + Gaussian [frostBlur]
 * - Pass 2: Glyph alpha mask -> multi-radius rounded bevel height map + 16px shadow halo map
 * - Composite: AGSL [RuntimeShader] (API 33+) or exact per-pixel optical software fallback (API 24-32 & tests)
 */
object LiquidGlassClockEngine {

    private val heightMapCache = android.util.LruCache<Int, Bitmap>(6)

    init {
        ZeroCrashEngine.registerMemoryCleanupHook("LiquidGlassClockEngine") {
            heightMapCache.evictAll()
        }
    }

    internal fun getCachedHeightMap(key: Int): Bitmap? {
        val bmp = heightMapCache.get(key)
        return if (bmp != null && !bmp.isRecycled && bmp.width > 0 && bmp.height > 0) bmp else null
    }

    internal fun putCachedHeightMap(key: Int, bmp: Bitmap) {
        if (!bmp.isRecycled && bmp.width > 0 && bmp.height > 0) {
            heightMapCache.put(key, bmp)
        }
    }

    internal fun resolveAdaptiveOptics(
        config: LiquidGlassConfig,
        avgLuminance: Float,
        blurMultiplier: Float = 1f,
        refractionMultiplier: Float = 1f,
        transparencyMultiplier: Float = 1f,
        lightResponseMultiplier: Float = 1f,
        specularMultiplier: Float = 1f,
        contrastMultiplier: Float = 1f
    ): ResolvedOpticalParams {
        val safe = config.sanitized()
        val lum = avgLuminance.coerceIn(0f, 1f)

        var specular = (safe.specularIntensity * specularMultiplier).coerceIn(0f, 1.50f)
        var innerShadow = (safe.innerThicknessShadow * lightResponseMultiplier).coerceIn(0f, 0.65f)
        var dropShadow = (safe.backdropDropShadow * contrastMultiplier).coerceIn(0f, 1.0f)

        if (safe.adaptiveContrast) {
            if (lum > 0.70f) {
                val brightFactor = ((lum - 0.70f) / 0.30f).coerceIn(0f, 1f) * contrastMultiplier
                innerShadow = (innerShadow + brightFactor * 0.24f).coerceIn(0f, 0.65f)
                dropShadow = (dropShadow + brightFactor * 0.42f).coerceIn(0f, 1.0f)
            } else if (lum < 0.25f) {
                val darkFactor = ((0.25f - lum) / 0.25f).coerceIn(0f, 1f) * lightResponseMultiplier
                specular = (specular + darkFactor * 0.42f).coerceIn(0f, 1.50f)
            }
        }

        val tintInt = safe.tintArgb.toInt()
        val tr = ((tintInt ushr 16) and 0xFF) / 255f
        val tg = ((tintInt ushr 8) and 0xFF) / 255f
        val tb = (tintInt and 0xFF) / 255f

        return ResolvedOpticalParams(
            avgLuminance = lum,
            refractionStrength = (safe.refractionStrength * refractionMultiplier).coerceIn(0f, 44f),
            ior = safe.ior,
            bevelWidth = safe.bevelWidth,
            chromaticAberration = (safe.chromaticAberration * refractionMultiplier.coerceIn(0.6f, 1.4f)).coerceIn(0f, 8f),
            frostBlur = (safe.frostBlur * blurMultiplier).coerceIn(0f, 24f),
            glassSaturation = safe.glassSaturation,
            tintR = tr,
            tintG = tg,
            tintB = tb,
            tintOpacity = (safe.tintOpacity * transparencyMultiplier).coerceIn(0f, 0.85f),
            specularIntensity = specular,
            lightAngleRad = (safe.lightAngle * PI / 180.0).toFloat(),
            innerThicknessShadow = innerShadow,
            backdropDropShadow = dropShadow
        )
    }

    /**
     * Extracts the exact cropped/positioned wallpaper pixel patch directly underneath the clock's
     * live bounding box in screen space, accounting for `ContentScale.Crop`, zoom, pan, look filter, and dim.
     */
    internal fun extractCroppedBackgroundPatch(
        backdropBitmap: Bitmap?,
        viewportSize: IntSize,
        clockBoxRect: ClockBoxRect?,
        zoomScale: Float,
        panOffset: Offset,
        dimFraction: Float,
        lookFilter: CanvasLookFilter,
        frostBlurPx: Float,
        fallbackPrimary: Color,
        fallbackSecondary: Color,
        patchWidth: Int,
        patchHeight: Int,
        rotationDegrees: Float = 0f
    ): BackdropOpticalPatch {
        val w = patchWidth.coerceIn(32, 640)
        val h = patchHeight.coerceIn(24, 640)
        val out = ZeroCrashEngine.safeCreateBitmap(w, h)
            ?: ZeroCrashEngine.safeCreateBitmap(32, 24)
            ?: createBitmap(32, 24)
        val canvas = AndroidCanvas(out)

        val safeBackdrop = backdropBitmap?.let { bmp ->
            if (bmp.isRecycled || bmp.width <= 0 || bmp.height <= 0) {
                null
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bmp.config == Bitmap.Config.HARDWARE) {
                runCatching { bmp.copy(Bitmap.Config.ARGB_8888, false) }.getOrNull()
            } else {
                bmp
            }
        }
        if (safeBackdrop != null && !safeBackdrop.isRecycled &&
            safeBackdrop.width > 0 && safeBackdrop.height > 0
        ) {
            val vw = viewportSize.width.coerceAtLeast(1).toFloat()
            val vh = viewportSize.height.coerceAtLeast(1).toFloat()
            val srcW = safeBackdrop.width.toFloat()
            val srcH = safeBackdrop.height.toFloat()

            val baseCropScale = max(vw / srcW, vh / srcH)
            val safeZoom = zoomScale.coerceIn(0.85f, 4.0f)
            val totalScale = (baseCropScale * safeZoom).coerceAtLeast(0.01f)

            val boxLeft = clockBoxRect?.leftPx ?: (vw * 0.18f)
            val boxTop = clockBoxRect?.topPx ?: (vh * 0.14f)
            val boxW = (clockBoxRect?.widthPx ?: (vw * 0.64f)).coerceAtLeast(8f)
            val boxH = (clockBoxRect?.heightPx ?: (vh * 0.22f)).coerceAtLeast(8f)

            val matrix = Matrix().apply {
                postTranslate(-srcW * 0.5f, -srcH * 0.5f)
                postScale(totalScale, totalScale)
                if (abs(rotationDegrees) > 0.05f) {
                    postRotate(rotationDegrees)
                }
                postTranslate(
                    vw * 0.5f + panOffset.x - boxLeft,
                    vh * 0.5f + panOffset.y - boxTop
                )
                postScale(w.toFloat() / boxW, h.toFloat() / boxH)
            }
            val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG)
            lookFilter.toColorMatrixArray()?.let { cm ->
                paint.colorFilter = ColorMatrixColorFilter(AndroidColorMatrix(cm))
            }
            canvas.drawColor(AndroidColor.rgb(18, 22, 32))
            runCatching { canvas.drawBitmap(safeBackdrop, matrix, paint) }
            if (safeBackdrop !== backdropBitmap && !safeBackdrop.isRecycled) {
                safeBackdrop.recycle()
            }
            if (dimFraction > 0.01f) {
                canvas.drawColor(AndroidColor.argb((dimFraction.coerceIn(0f, 0.55f) * 255f).roundToInt(), 0, 0, 0))
            }
        } else {
            val c1 = androidColorFromCompose(fallbackPrimary)
            val c2 = androidColorFromCompose(fallbackSecondary)
            val grad = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.LinearGradient(
                    0f, 0f, w.toFloat(), h.toFloat(),
                    intArrayOf(blendRgb(c1, AndroidColor.DKGRAY, 0.35f), blendRgb(c2, AndroidColor.BLACK, 0.25f)),
                    null,
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), grad)
        }

        val lum = computeAverageLuminance(out)
        val rawFrost = (frostBlurPx.coerceIn(0f, 24f) * (w.toFloat() / max(clockBoxRect?.widthPx ?: w.toFloat(), 1f))).coerceIn(0f, 18f)
        val frostRadius = if (frostBlurPx >= 1.5f) rawFrost.roundToInt().coerceAtLeast(1) else rawFrost.roundToInt()
        val frosted = if (frostRadius >= 1) {
            boxBlurBitmap(out, frostRadius)
        } else {
            out
        }
        return BackdropOpticalPatch(
            unblurredPatch = out,
            frostedPatch = frosted,
            avgLuminance = lum
        )
    }

    internal fun bitmapHasVisibleAlpha(bitmap: Bitmap): Boolean {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return false
        val w = bitmap.width
        val h = bitmap.height
        val row = IntArray(w)
        val stepY = max(1, h / 18)
        var y = 0
        while (y < h) {
            bitmap.getPixels(row, 0, w, 0, y, w, 1)
            var x = 0
            while (x < w) {
                if (((row[x] ushr 24) and 0xFF) > 12) return true
                x += 3
            }
            y += stepY
        }
        return false
    }

    /**
     * Generates a premultiplication-lossless packed glyph height + mask + shadow map
     * from an offscreen crisp glyph alpha bitmap (`A` stays `0xFF` everywhere so Skia/Bitmap
     * premultiplication never drops exterior drop-shadow halos or bevel contours):
     * - `A`: `255` (opaque container so RGB channels remain unscaled)
     * - `R`: smooth multi-radius interior bevel height (0 at edge -> 255 in stroke core)
     * - `G`: crisp anti-aliased glyph mask (0..255)
     * - `B`: wide 16px diffused glyph drop-shadow alpha mask
     */
    internal fun buildMultiRadiusBevelHeightMap(
        glyphMaskBitmap: Bitmap,
        bevelWidthPx: Float
    ): Bitmap {
        val w = glyphMaskBitmap.width.coerceAtLeast(2)
        val h = glyphMaskBitmap.height.coerceAtLeast(2)
        val n = w * h
        val rawPixels = IntArray(n)
        glyphMaskBitmap.getPixels(rawPixels, 0, w, 0, 0, w, h)

        val alphaF = FloatArray(n) { i -> ((rawPixels[i] ushr 24) and 0xFF) / 255f }

        val bw = bevelWidthPx.coerceIn(2f, 24f)
        val rSmall = max(1, (bw * 0.35f).roundToInt())
        val rMid = max(1, (bw * 0.70f).roundToInt())
        val rLarge = max(2, bw.roundToInt())

        val b1 = separableBoxBlurFloat(alphaF, w, h, rSmall)
        val b2 = separableBoxBlurFloat(alphaF, w, h, rMid)
        val b3 = separableBoxBlurFloat(alphaF, w, h, rLarge)
        val shadowField = separableBoxBlurFloat(alphaF, w, h, 8)

        val outPixels = IntArray(n)
        for (i in 0 until n) {
            val a = alphaF[i].coerceIn(0f, 1f)
            val combinedBlur = 0.50f * b1[i] + 0.32f * b2[i] + 0.18f * b3[i]
            val height = if (a > 0.01f) {
                ((combinedBlur - 0.42f) / 0.58f).coerceIn(0f, 1f) * min(1f, a * 1.2f)
            } else {
                0f
            }
            val rByte = (height * 255f).roundToInt().coerceIn(0, 255)
            val gByte = (a * 255f).roundToInt().coerceIn(0, 255)
            val bByte = (shadowField[i].coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
            outPixels[i] = (0xFF shl 24) or (rByte shl 16) or (gByte shl 8) or bByte
        }

        val out = ZeroCrashEngine.safeCreateBitmap(w, h) ?: createBitmap(w, h)
        out.setPixels(outPixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * Exact CPU optical fallback for API < 33 and deterministic pixel-verified unit tests.
     * Simulates IOR lens refraction, prismatic RGB chromatic aberration, frost diffusion,
     * jewel saturation, translucent tint, directional Fresnel specular rim, inner thickness
     * darkening, and glyph-shaped drop shadow.
     */
    internal fun renderOpticalGlassSoftwareBitmap(
        glyphMap: Bitmap,
        backgroundPatch: Bitmap,
        frostedPatch: Bitmap,
        params: ResolvedOpticalParams,
        scaleXAspect: Float = 1f,
        scaleYAspect: Float = 1f,
        opacity: Float = 1f
    ): Bitmap {
        val w = glyphMap.width.coerceAtLeast(2)
        val h = glyphMap.height.coerceAtLeast(2)
        val bgScaled = if (backgroundPatch.width == w && backgroundPatch.height == h) backgroundPatch
        else {
            val target = ZeroCrashEngine.safeCreateBitmap(w, h)
            if (target != null) {
                AndroidCanvas(target).drawBitmap(backgroundPatch, null, Rect(0, 0, w, h), AndroidPaint(AndroidPaint.FILTER_BITMAP_FLAG))
                target
            } else {
                Bitmap.createScaledBitmap(backgroundPatch, w, h, true)
            }
        }
        val frostScaled = if (frostedPatch.width == w && frostedPatch.height == h) frostedPatch
        else {
            val target = ZeroCrashEngine.safeCreateBitmap(w, h)
            if (target != null) {
                AndroidCanvas(target).drawBitmap(frostedPatch, null, Rect(0, 0, w, h), AndroidPaint(AndroidPaint.FILTER_BITMAP_FLAG))
                target
            } else {
                Bitmap.createScaledBitmap(frostedPatch, w, h, true)
            }
        }

        val n = w * h
        val mapPx = IntArray(n)
        val bgPx = IntArray(n)
        val frPx = IntArray(n)
        glyphMap.getPixels(mapPx, 0, w, 0, 0, w, h)
        bgScaled.getPixels(bgPx, 0, w, 0, 0, w, h)
        frostScaled.getPixels(frPx, 0, w, 0, 0, w, h)
        if (bgScaled !== backgroundPatch) bgScaled.recycle()
        if (frostScaled !== frostedPatch) frostScaled.recycle()

        val outPx = IntArray(n)
        val lightX = cos(params.lightAngleRad)
        val lightY = sin(params.lightAngleRad)
        val iorCurve = ((params.ior - 1.0f) / 0.48f).coerceIn(0.15f, 2.2f)
        val frostMix = (params.frostBlur / 24.0f * 0.85f).coerceIn(0f, 0.92f)
        val safeScaleX = scaleXAspect.coerceAtLeast(0.35f)
        val safeScaleY = scaleYAspect.coerceAtLeast(0.35f)
        val dropOffsetY = (6f / safeScaleY).roundToInt().coerceIn(1, max(1, h - 1))
        val safeOp = opacity.coerceIn(0f, 1f)

        fun sampleBilinearRgb(buf: IntArray, fx: Float, fy: Float, channelShift: Int): Float {
            val cx = fx.coerceIn(0f, (w - 1).toFloat())
            val cy = fy.coerceIn(0f, (h - 1).toFloat())
            val x0 = cx.toInt()
            val y0 = cy.toInt()
            val x1 = min(x0 + 1, w - 1)
            val y1 = min(y0 + 1, h - 1)
            val tx = cx - x0
            val ty = cy - y0
            val c00 = ((buf[y0 * w + x0] ushr channelShift) and 0xFF) / 255f
            val c10 = ((buf[y0 * w + x1] ushr channelShift) and 0xFF) / 255f
            val c01 = ((buf[y1 * w + x0] ushr channelShift) and 0xFF) / 255f
            val c11 = ((buf[y1 * w + x1] ushr channelShift) and 0xFF) / 255f
            val top = c00 + (c10 - c00) * tx
            val bot = c01 + (c11 - c01) * tx
            return top + (bot - top) * ty
        }

        for (y in 0 until h) {
            val row = y * w
            val shadowRow = max(0, y - dropOffsetY) * w
            val ym = max(0, y - 1) * w
            val yp = min(h - 1, y + 1) * w

            for (x in 0 until w) {
                val idx = row + x
                val cMap = mapPx[idx]
                val mask = ((cMap ushr 8) and 0xFF) / 255f
                val shadowMask = ((mapPx[shadowRow + x] and 0xFF)) / 255f
                val shadowAlpha = (shadowMask * params.backdropDropShadow * 0.72f * (1f - mask * 0.85f)).coerceIn(0f, 0.85f)

                if (mask <= 0.004f) {
                    val sA = (shadowAlpha * safeOp * 255f).roundToInt().coerceIn(0, 255)
                    outPx[idx] = if (sA == 0) 0 else (sA shl 24)
                    continue
                }

                val height = ((cMap ushr 16) and 0xFF) / 255f
                val xm = max(0, x - 1)
                val xp = min(w - 1, x + 1)
                val hL = ((mapPx[row + xm] ushr 16) and 0xFF) / 255f
                val hR = ((mapPx[row + xp] ushr 16) and 0xFF) / 255f
                val hT = ((mapPx[ym + x] ushr 16) and 0xFF) / 255f
                val hB = ((mapPx[yp + x] ushr 16) and 0xFF) / 255f

                val gx = (hL - hR) * 3.8f * iorCurve
                val gy = (hT - hB) * 3.8f * iorCurve
                val gz = 0.42f + height * 0.65f
                val invLen = 1f / sqrt(gx * gx + gy * gy + gz * gz)
                val nx = gx * invLen
                val ny = gy * invLen
                val nz = gz * invLen

                val rawEdge = (1f - height).coerceIn(0f, 1f)
                val edgeMask = rawEdge * rawEdge * (3f - 2f * rawEdge)
                val refrFactor = params.refractionStrength * (edgeMask * 1.15f + (1f - edgeMask) * 0.18f * (params.ior - 1f))
                val rx = (x.toFloat() - (nx * refrFactor) / safeScaleX).coerceIn(0f, (w - 1).toFloat())
                val ry = (y.toFloat() - (ny * refrFactor) / safeScaleY).coerceIn(0f, (h - 1).toFloat())

                val n2dLen = sqrt(nx * nx + ny * ny)
                val dirX = if (n2dLen > 0.001f) nx / n2dLen else 1f
                val dirY = if (n2dLen > 0.001f) ny / n2dLen else 0.4f
                val caScale = params.chromaticAberration * 0.004f * (0.25f + edgeMask * 1.15f)
                val caDx = w * caScale * dirX
                val caDy = h * caScale * dirY

                val sharpR = sampleBilinearRgb(bgPx, rx + caDx, ry + caDy, 16)
                val sharpG = sampleBilinearRgb(bgPx, rx, ry, 8)
                val sharpB = sampleBilinearRgb(bgPx, rx - caDx, ry - caDy, 0)

                val frostR = sampleBilinearRgb(frPx, rx + caDx * 0.5f, ry + caDy * 0.5f, 16)
                val frostG = sampleBilinearRgb(frPx, rx, ry, 8)
                val frostB = sampleBilinearRgb(frPx, rx - caDx * 0.5f, ry - caDy * 0.5f, 0)

                var r = sharpR + (frostR - sharpR) * frostMix
                var g = sharpG + (frostG - sharpG) * frostMix
                var b = sharpB + (frostB - sharpB) * frostMix

                // Jewel saturation boost
                val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
                r = (luma + (r - luma) * params.glassSaturation).coerceIn(0f, 1f)
                g = (luma + (g - luma) * params.glassSaturation).coerceIn(0f, 1f)
                b = (luma + (b - luma) * params.glassSaturation).coerceIn(0f, 1f)

                // Translucent tint blend
                val tAlpha = params.tintOpacity
                r = r * (1f - tAlpha) + (r * 0.45f + params.tintR * 0.85f).coerceIn(0f, 1f) * tAlpha
                g = g * (1f - tAlpha) + (g * 0.45f + params.tintG * 0.85f).coerceIn(0f, 1f) * tAlpha
                b = b * (1f - tAlpha) + (b * 0.45f + params.tintB * 0.85f).coerceIn(0f, 1f) * tAlpha

                // Inner thickness darkening on shadow-facing bevel edge
                val shadowAlign = max(-(dirX * lightX + dirY * lightY), 0f) * min(1f, n2dLen * 2.2f)
                val innerDark = (shadowAlign * params.innerThicknessShadow * (0.35f + 0.95f * edgeMask)).coerceIn(0f, 0.72f)
                r *= (1f - innerDark)
                g *= (1f - innerDark)
                b *= (1f - innerDark)

                // Directional Fresnel Specular + 0.35x secondary bounce rim highlight
                val fresnel = (1f - nz).coerceIn(0f, 1f).toDouble().pow(1.85).toFloat()
                val rawKeyDot = (dirX * lightX + dirY * lightY)
                val keySpec = max(rawKeyDot, 0f).toDouble().pow(2.4).toFloat()
                val bounceSpec = max(-rawKeyDot, 0f).toDouble().pow(2.8).toFloat() * 0.35f
                val rimMask = ((edgeMask + 0.06f) * min(1f, n2dLen * 2.4f + 0.15f)).coerceIn(0f, 1f)
                val specAmt = ((keySpec * (0.35f + 0.85f * fresnel) + bounceSpec * (0.25f + 0.75f * fresnel)) * params.specularIntensity * rimMask).coerceIn(0f, 1.4f)

                r = (r + (1f * 0.65f + params.tintR * 0.35f) * specAmt).coerceIn(0f, 1f)
                g = (g + (1f * 0.65f + params.tintG * 0.35f) * specAmt).coerceIn(0f, 1f)
                b = (b + (1f * 0.65f + params.tintB * 0.35f) * specAmt).coerceIn(0f, 1f)

                // Composite crisp glass over drop shadow
                val compAlpha = max(mask, shadowAlpha).coerceIn(0f, 1f)
                val compScale = if (compAlpha > 1e-4f) mask / compAlpha else 1f
                val outR = (r * compScale).coerceIn(0f, 1f)
                val outG = (g * compScale).coerceIn(0f, 1f)
                val outB = (b * compScale).coerceIn(0f, 1f)

                val a8 = (compAlpha * safeOp * 255f).roundToInt().coerceIn(0, 255)
                val r8 = (outR * 255f).roundToInt().coerceIn(0, 255)
                val g8 = (outG * 255f).roundToInt().coerceIn(0, 255)
                val b8 = (outB * 255f).roundToInt().coerceIn(0, 255)
                outPx[idx] = (a8 shl 24) or (r8 shl 16) or (g8 shl 8) or b8
            }
        }

        val result = ZeroCrashEngine.safeCreateBitmap(w, h) ?: createBitmap(w, h)
        result.setPixels(outPx, 0, w, 0, 0, w, h)
        return result
    }

    internal fun computeAverageLuminance(bitmap: Bitmap): Float {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return 0.5f
        val px = IntArray(w * h)
        bitmap.getPixels(px, 0, w, 0, 0, w, h)
        var sum = 0.0
        var count = 0
        val step = max(1, (w * h) / 600)
        var i = 0
        while (i < px.size) {
            val c = px[i]
            val r = ((c ushr 16) and 0xFF) / 255.0
            val g = ((c ushr 8) and 0xFF) / 255.0
            val b = (c and 0xFF) / 255.0
            sum += 0.2126 * r + 0.7152 * g + 0.0722 * b
            count++
            i += step
        }
        return (sum / max(1, count)).toFloat().coerceIn(0f, 1f)
    }

    private fun boxBlurBitmap(src: Bitmap, radius: Int): Bitmap {
        val w = src.width
        val h = src.height
        val r = radius.coerceIn(1, 24)
        val n = w * h
        val px = IntArray(n)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val tmp = IntArray(n)
        val out = IntArray(n)

        // Two-pass separable sliding box blur approximates smooth Gaussian diffusion quickly
        repeat(2) { pass ->
            val srcArr = if (pass == 0) px else out
            for (y in 0 until h) {
                val row = y * w
                var sr = 0; var sg = 0; var sb = 0
                for (dx in -r..r) {
                    val cx = dx.coerceIn(0, w - 1)
                    val c = srcArr[row + cx]
                    sr += (c ushr 16) and 0xFF
                    sg += (c ushr 8) and 0xFF
                    sb += c and 0xFF
                }
                val denom = 2 * r + 1
                for (x in 0 until w) {
                    tmp[row + x] = (0xFF shl 24) or
                        ((sr / denom) shl 16) or
                        ((sg / denom) shl 8) or
                        (sb / denom)
                    val remIdx = (x - r).coerceIn(0, w - 1)
                    val addIdx = (x + r + 1).coerceIn(0, w - 1)
                    val cr = srcArr[row + remIdx]
                    val ca = srcArr[row + addIdx]
                    sr += ((ca ushr 16) and 0xFF) - ((cr ushr 16) and 0xFF)
                    sg += ((ca ushr 8) and 0xFF) - ((cr ushr 8) and 0xFF)
                    sb += (ca and 0xFF) - (cr and 0xFF)
                }
            }
            for (x in 0 until w) {
                var sr = 0; var sg = 0; var sb = 0
                for (dy in -r..r) {
                    val cy = dy.coerceIn(0, h - 1)
                    val c = tmp[cy * w + x]
                    sr += (c ushr 16) and 0xFF
                    sg += (c ushr 8) and 0xFF
                    sb += c and 0xFF
                }
                val denom = 2 * r + 1
                for (y in 0 until h) {
                    out[y * w + x] = (0xFF shl 24) or
                        ((sr / denom) shl 16) or
                        ((sg / denom) shl 8) or
                        (sb / denom)
                    val remIdx = (y - r).coerceIn(0, h - 1) * w + x
                    val addIdx = (y + r + 1).coerceIn(0, h - 1) * w + x
                    val cr = tmp[remIdx]
                    val ca = tmp[addIdx]
                    sr += ((ca ushr 16) and 0xFF) - ((cr ushr 16) and 0xFF)
                    sg += ((ca ushr 8) and 0xFF) - ((cr ushr 8) and 0xFF)
                    sb += (ca and 0xFF) - (cr and 0xFF)
                }
            }
        }
        val blurred = ZeroCrashEngine.safeCreateBitmap(w, h) ?: return src
        blurred.setPixels(out, 0, w, 0, 0, w, h)
        return blurred
    }

    private fun separableBoxBlurFloat(src: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        val r = radius.coerceIn(1, 32)
        val n = w * h
        val tmp = FloatArray(n)
        val out = FloatArray(n)
        val denom = (2 * r + 1).toFloat()

        for (y in 0 until h) {
            val row = y * w
            var acc = 0f
            for (dx in -r..r) acc += src[row + dx.coerceIn(0, w - 1)]
            for (x in 0 until w) {
                tmp[row + x] = acc / denom
                val rem = src[row + (x - r).coerceIn(0, w - 1)]
                val add = src[row + (x + r + 1).coerceIn(0, w - 1)]
                acc += add - rem
            }
        }
        for (x in 0 until w) {
            var acc = 0f
            for (dy in -r..r) acc += tmp[dy.coerceIn(0, h - 1) * w + x]
            for (y in 0 until h) {
                out[y * w + x] = acc / denom
                val rem = tmp[(y - r).coerceIn(0, h - 1) * w + x]
                val add = tmp[(y + r + 1).coerceIn(0, h - 1) * w + x]
                acc += add - rem
            }
        }
        return out
    }

    private fun androidColorFromCompose(color: Color): Int = AndroidColor.argb(
        (color.alpha * 255f).roundToInt().coerceIn(0, 255),
        (color.red * 255f).roundToInt().coerceIn(0, 255),
        (color.green * 255f).roundToInt().coerceIn(0, 255),
        (color.blue * 255f).roundToInt().coerceIn(0, 255)
    )

    private fun blendRgb(a: Int, b: Int, t: Float): Int {
        val f = t.coerceIn(0f, 1f)
        val r = (((a ushr 16) and 0xFF) * (1f - f) + ((b ushr 16) and 0xFF) * f).roundToInt()
        val g = (((a ushr 8) and 0xFF) * (1f - f) + ((b ushr 8) and 0xFF) * f).roundToInt()
        val bl = ((a and 0xFF) * (1f - f) + (b and 0xFF) * f).roundToInt()
        return AndroidColor.rgb(r, g, bl)
    }

    internal const val LIQUID_GLASS_AGSL = """
        uniform shader uBackground;
        uniform shader uFrosted;
        uniform shader uGlyphMap;
        uniform float2 uSize;
        uniform float2 uBgSize;
        uniform float2 uScaleXY;
        uniform float uRefraction;
        uniform float uIor;
        uniform float uChromaticAberration;
        uniform float uFrostAmount;
        uniform float uSaturation;
        uniform float3 uTintRgb;
        uniform float uTintOpacity;
        uniform float uSpecular;
        uniform float2 uLightDir;
        uniform float uInnerShadow;
        uniform float uDropShadow;
        uniform float uOpacity;

        half4 main(float2 fragCoord) {
            float4 centerMap = uGlyphMap.eval(fragCoord);
            float mask = centerMap.g;
            float height = centerMap.r;

            float dropOffsetY = 6.0 / max(uScaleXY.y, 0.35);
            float shadowMask = uGlyphMap.eval(clamp(fragCoord - float2(0.0, dropOffsetY), float2(0.0), uSize - float2(1.0))).b;
            float shadowAlpha = clamp(shadowMask * uDropShadow * 0.72 * (1.0 - mask * 0.85), 0.0, 0.85);

            if (mask <= 0.003) {
                return half4(0.0, 0.0, 0.0, half(shadowAlpha * uOpacity));
            }

            float stepPx = 1.5;
            float hL = uGlyphMap.eval(clamp(fragCoord - float2(stepPx, 0.0), float2(0.0), uSize - float2(1.0))).r;
            float hR = uGlyphMap.eval(clamp(fragCoord + float2(stepPx, 0.0), float2(0.0), uSize - float2(1.0))).r;
            float hT = uGlyphMap.eval(clamp(fragCoord - float2(0.0, stepPx), float2(0.0), uSize - float2(1.0))).r;
            float hB = uGlyphMap.eval(clamp(fragCoord + float2(0.0, stepPx), float2(0.0), uSize - float2(1.0))).r;

            float iorCurve = clamp((uIor - 1.0) / 0.48, 0.15, 2.2);
            float3 n = normalize(float3((hL - hR) * 3.8 * iorCurve, (hT - hB) * 3.8 * iorCurve, 0.42 + height * 0.65));

            float edgeMask = smoothstep(0.0, 0.90, 1.0 - height) * smoothstep(0.0, 0.15, mask);
            float refrPx = uRefraction * (edgeMask * 1.15 + (1.0 - edgeMask) * 0.18 * (uIor - 1.0));
            float2 bgScale = uBgSize / max(uSize, float2(1.0));
            float2 safeScale = max(uScaleXY, float2(0.35));
            float2 bgCoord = clamp((fragCoord - (n.xy * refrPx) / safeScale) * bgScale, float2(1.0), uBgSize - float2(1.0));

            float2 dir2 = length(n.xy) > 0.001 ? normalize(n.xy) : float2(1.0, 0.4);
            float2 caShift = uBgSize * (uChromaticAberration * 0.004) * (0.25 + edgeMask * 1.15) * dir2;

            float rSharp = uBackground.eval(clamp(bgCoord + caShift, float2(1.0), uBgSize - float2(1.0))).r;
            float gSharp = uBackground.eval(bgCoord).g;
            float bSharp = uBackground.eval(clamp(bgCoord - caShift, float2(1.0), uBgSize - float2(1.0))).b;

            float rFrost = uFrosted.eval(clamp(bgCoord + caShift * 0.5, float2(1.0), uBgSize - float2(1.0))).r;
            float gFrost = uFrosted.eval(bgCoord).g;
            float bFrost = uFrosted.eval(clamp(bgCoord - caShift * 0.5, float2(1.0), uBgSize - float2(1.0))).b;

            float3 glassRgb = mix(float3(rSharp, gSharp, bSharp), float3(rFrost, gFrost, bFrost), uFrostAmount);
            float luma = dot(glassRgb, float3(0.2126, 0.7152, 0.0722));
            glassRgb = clamp(mix(float3(luma), glassRgb, uSaturation), 0.0, 1.0);

            float3 tintedTarget = clamp(glassRgb * 0.45 + uTintRgb * 0.85, 0.0, 1.0);
            glassRgb = mix(glassRgb, tintedTarget, uTintOpacity);

            float n2Mag = clamp(length(n.xy) * 2.2, 0.0, 1.0);
            float shadowAlign = max(dot(dir2, -uLightDir), 0.0) * n2Mag;
            float innerDark = clamp(shadowAlign * uInnerShadow * (0.35 + 0.95 * edgeMask), 0.0, 0.72);
            glassRgb *= (1.0 - innerDark);

            float fresnel = pow(clamp(1.0 - n.z, 0.0, 1.0), 1.85);
            float keyDot = dot(dir2, uLightDir);
            float keySpec = pow(max(keyDot, 0.0), 2.4);
            float bounceSpec = pow(max(-keyDot, 0.0), 2.8) * 0.35;
            float rimMask = clamp((edgeMask + 0.06) * clamp(length(n.xy) * 2.4 + 0.15, 0.0, 1.0), 0.0, 1.0);
            float specAmt = clamp((keySpec * (0.35 + 0.85 * fresnel) + bounceSpec * (0.25 + 0.75 * fresnel)) * uSpecular * rimMask, 0.0, 1.4);

            float3 specColor = mix(float3(1.0), uTintRgb, 0.35);
            glassRgb = clamp(glassRgb + specColor * specAmt, 0.0, 1.0);

            float finalAlpha = max(mask, shadowAlpha) * uOpacity;
            float3 premulRgb = glassRgb * (mask * uOpacity);
            return half4(half3(premulRgb), half(finalAlpha));
        }
    """
}

private data class CapturedGlyphState(
    val heightMap: Bitmap,
    val captureKey: Any?,
    val size: IntSize
)

/**
 * Dual-pass Compose optical clock surface for [HyperOsClockFinish.BLUR_GLASS]:
 * Captures the live typography + glyphs offscreen into [GraphicsLayer] -> builds multi-radius
 * bevel height map -> binds real cropped wallpaper background + frost patch -> executes either
 * hardware AGSL [RuntimeShader] (API 33+) or optical software shader fallback at 60fps.
 */
@Composable
internal fun LiquidGlassClockLayer(
    liquidGlassConfig: LiquidGlassConfig,
    backdropBitmap: Bitmap?,
    viewportSize: IntSize,
    clockBoxRect: ClockBoxRect?,
    zoomScale: Float,
    panOffset: Offset,
    dimFraction: Float,
    lookFilter: CanvasLookFilter,
    fallbackPrimary: Color,
    fallbackSecondary: Color,
    scaleXAspect: Float,
    scaleYAspect: Float,
    opacity: Float,
    captureKey: Any?,
    modifier: Modifier = Modifier,
    rotationDegrees: Float = 0f,
    maskContent: @Composable () -> Unit
) {
    val glyphCaptureLayer: GraphicsLayer = rememberGraphicsLayer()
    var maskSize by remember { mutableStateOf(IntSize.Zero) }
    val safeConfig = remember(liquidGlassConfig) { liquidGlassConfig.sanitized() }
    val initialLruKey = remember(captureKey, maskSize, safeConfig.bevelWidth) {
        if (maskSize.width <= 0 || maskSize.height <= 0) null
        else ((captureKey?.hashCode() ?: 0) * 31 + maskSize.width) * 31 +
            maskSize.height * 17 + safeConfig.bevelWidth.roundToInt()
    }
    var capturedState by remember {
        val seedBmp = initialLruKey?.let { LiquidGlassClockEngine.getCachedHeightMap(it) }
        mutableStateOf(seedBmp?.let { CapturedGlyphState(it, captureKey, maskSize) })
    }

    val density = LocalDensity.current
    val customTokens = com.example.ui.theme.LocalThemeCustomization.current
    val tier = customTokens.liquidGlassTier
    val effectiveFrostBlur = (safeConfig.frostBlur * customTokens.glassBlurLevel * tier.blurMultiplier).coerceIn(0f, 24f)

    // Rebuild (or read LRU) multi-radius bevel height map asynchronously on Dispatchers.Default without clearing previous frame
    LaunchedEffect(captureKey, maskSize, safeConfig.bevelWidth, tier.shaderScale) {
        if (maskSize.width <= 0 || maskSize.height <= 0) return@LaunchedEffect
        val lruKey = ((captureKey?.hashCode() ?: 0) * 31 + maskSize.width) * 31 +
            maskSize.height * 17 + safeConfig.bevelWidth.roundToInt()
        val cached = LiquidGlassClockEngine.getCachedHeightMap(lruKey)
        if (cached != null) {
            capturedState = CapturedGlyphState(cached, captureKey, maskSize)
            return@LaunchedEffect
        }

        // Wait two frame commits so Compose finishes laying out & drawing full-alpha typography into glyphCaptureLayer
        withFrameNanos { }
        withFrameNanos { }

        suspend fun captureMaskOnce(): Bitmap? = ZeroCrashEngine.runSafely("LiquidGlass_captureMask", null) {
            glyphCaptureLayer.alpha = 1f
            val img = glyphCaptureLayer.toImageBitmap()
            val rawBmp = img.asAndroidBitmap()
            if (rawBmp.width > 0 && rawBmp.height > 0) {
                val swBmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    rawBmp.config == Bitmap.Config.HARDWARE
                ) {
                    rawBmp.copy(Bitmap.Config.ARGB_8888, false)
                } else if (rawBmp.config != Bitmap.Config.ARGB_8888) {
                    rawBmp.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    rawBmp
                }
                if (swBmp != null && swBmp.width > 0 && swBmp.height > 0) {
                    val hasPixels = withContext(Dispatchers.Default) {
                        LiquidGlassClockEngine.bitmapHasVisibleAlpha(swBmp)
                    }
                    if (!hasPixels) {
                        if (swBmp !== rawBmp) swBmp.recycle()
                        return@runSafely null
                    }
                    withContext(Dispatchers.Default) {
                        val maxDim = (540f * tier.shaderScale).roundToInt().coerceIn(220, 680)
                        val workBmp = if (swBmp.width > maxDim || swBmp.height > maxDim) {
                            val scaleFactor = minOf(
                                maxDim.toFloat() / swBmp.width.toFloat(),
                                maxDim.toFloat() / swBmp.height.toFloat()
                            )
                            val targetW = max(24, (swBmp.width * scaleFactor).roundToInt())
                            val targetH = max(24, (swBmp.height * scaleFactor).roundToInt())
                            val scaled = ZeroCrashEngine.safeCreateBitmap(targetW, targetH)
                            if (scaled != null) {
                                AndroidCanvas(scaled).drawBitmap(swBmp, null, Rect(0, 0, targetW, targetH), AndroidPaint(AndroidPaint.FILTER_BITMAP_FLAG))
                                scaled
                            } else {
                                Bitmap.createScaledBitmap(swBmp, targetW, targetH, true)
                            }
                        } else {
                            swBmp
                        }
                        val ratioScale = workBmp.width.toFloat() / rawBmp.width.toFloat()
                        val heightMap = LiquidGlassClockEngine.buildMultiRadiusBevelHeightMap(
                            glyphMaskBitmap = workBmp,
                            bevelWidthPx = (safeConfig.bevelWidth * ratioScale).coerceIn(2f, 24f)
                        )
                        if (workBmp !== swBmp) workBmp.recycle()
                        if (swBmp !== rawBmp) swBmp.recycle()
                        heightMap
                    }
                } else null
            } else null
        }

        var captured = captureMaskOnce()
        if (captured == null) {
            withFrameNanos { }
            captured = captureMaskOnce()
        }
        if (captured != null) {
            LiquidGlassClockEngine.putCachedHeightMap(lruKey, captured)
            capturedState = CapturedGlyphState(captured, captureKey, maskSize)
        }
    }

    val initialPatch = remember {
        LiquidGlassClockEngine.extractCroppedBackgroundPatch(
            backdropBitmap = null,
            viewportSize = IntSize(64, 64),
            clockBoxRect = null,
            zoomScale = 1f,
            panOffset = Offset.Zero,
            dimFraction = dimFraction,
            lookFilter = lookFilter,
            frostBlurPx = 0f,
            fallbackPrimary = fallbackPrimary,
            fallbackSecondary = fallbackSecondary,
            patchWidth = 48,
            patchHeight = 32,
            rotationDegrees = 0f
        )
    }
    var bgPatch by remember { mutableStateOf(initialPatch) }

    // Retain the last valid height map across minor updates (12h/24h, telemetry, signature edits) so glyphs never flash white
    val activeCapture = capturedState?.takeIf { !it.heightMap.isRecycled && it.heightMap.width > 0 && it.heightMap.height > 0 }

    // Re-extract live cropped background pixel patch asynchronously on Dispatchers.Default whenever clock moves or optics change
    LaunchedEffect(
        backdropBitmap,
        viewportSize,
        clockBoxRect?.leftPx?.roundToInt(),
        clockBoxRect?.topPx?.roundToInt(),
        clockBoxRect?.widthPx?.roundToInt(),
        clockBoxRect?.heightPx?.roundToInt(),
        zoomScale,
        panOffset.x.roundToInt(),
        panOffset.y.roundToInt(),
        rotationDegrees.roundToInt(),
        dimFraction,
        lookFilter,
        effectiveFrostBlur,
        maskSize,
        activeCapture?.heightMap,
        fallbackPrimary,
        fallbackSecondary
    ) {
        delay(45L)
        val targetW = (activeCapture?.heightMap?.width ?: maskSize.width.coerceIn(64, 420)).coerceIn(48, 540)
        val targetH = (activeCapture?.heightMap?.height ?: maskSize.height.coerceIn(32, 320)).coerceIn(32, 540)
        val nextPatch = withContext(Dispatchers.Default) {
            ZeroCrashEngine.runSafely("LiquidGlass_extractPatch", null) {
                LiquidGlassClockEngine.extractCroppedBackgroundPatch(
                    backdropBitmap = backdropBitmap,
                    viewportSize = viewportSize,
                    clockBoxRect = clockBoxRect,
                    zoomScale = zoomScale,
                    panOffset = panOffset,
                    dimFraction = dimFraction,
                    lookFilter = lookFilter,
                    frostBlurPx = effectiveFrostBlur,
                    fallbackPrimary = fallbackPrimary,
                    fallbackSecondary = fallbackSecondary,
                    patchWidth = targetW,
                    patchHeight = targetH,
                    rotationDegrees = rotationDegrees
                )
            }
        }
        if (nextPatch != null && !nextPatch.unblurredPatch.isRecycled && !nextPatch.frostedPatch.isRecycled) {
            bgPatch = nextPatch
        }
    }

    val resolvedOptics = remember(safeConfig, bgPatch.avgLuminance, customTokens, tier) {
        LiquidGlassClockEngine.resolveAdaptiveOptics(
            config = safeConfig,
            avgLuminance = bgPatch.avgLuminance,
            blurMultiplier = customTokens.glassBlurLevel * tier.blurMultiplier,
            refractionMultiplier = customTokens.glassRefractionLevel * tier.refractionMultiplier,
            transparencyMultiplier = customTokens.glassTransparencyLevel,
            lightResponseMultiplier = customTokens.glassLightResponse,
            specularMultiplier = customTokens.glassSpecularHighlights * (0.85f + tier.specularPasses * 0.12f),
            contrastMultiplier = customTokens.glassAdaptiveContrast
        )
    }

    // Immutable per-snapshot RuntimeShader + BitmapShaders prevent RenderThread race glitches during rapid slider drags
    val preparedAgslPaint = remember(
        activeCapture?.heightMap,
        bgPatch.unblurredPatch,
        bgPatch.frostedPatch,
        resolvedOptics,
        scaleXAspect,
        scaleYAspect,
        opacity
    ) {
        val glyphMap = activeCapture?.heightMap
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            glyphMap != null && !glyphMap.isRecycled &&
            !bgPatch.unblurredPatch.isRecycled && !bgPatch.frostedPatch.isRecycled
        ) {
            runCatching {
                val shader = RuntimeShader(LiquidGlassClockEngine.LIQUID_GLASS_AGSL)
                val glyphShader = android.graphics.BitmapShader(
                    glyphMap,
                    android.graphics.Shader.TileMode.CLAMP,
                    android.graphics.Shader.TileMode.CLAMP
                )
                val bgShader = android.graphics.BitmapShader(
                    bgPatch.unblurredPatch,
                    android.graphics.Shader.TileMode.CLAMP,
                    android.graphics.Shader.TileMode.CLAMP
                )
                val frostShader = android.graphics.BitmapShader(
                    bgPatch.frostedPatch,
                    android.graphics.Shader.TileMode.CLAMP,
                    android.graphics.Shader.TileMode.CLAMP
                )
                shader.setInputShader("uBackground", bgShader)
                shader.setInputShader("uFrosted", frostShader)
                shader.setInputShader("uGlyphMap", glyphShader)
                shader.setFloatUniform("uSize", glyphMap.width.toFloat(), glyphMap.height.toFloat())
                shader.setFloatUniform("uBgSize", bgPatch.unblurredPatch.width.toFloat(), bgPatch.unblurredPatch.height.toFloat())
                shader.setFloatUniform("uScaleXY", scaleXAspect.coerceAtLeast(0.35f), scaleYAspect.coerceAtLeast(0.35f))
                shader.setFloatUniform("uRefraction", resolvedOptics.refractionStrength)
                shader.setFloatUniform("uIor", resolvedOptics.ior)
                shader.setFloatUniform("uChromaticAberration", resolvedOptics.chromaticAberration)
                shader.setFloatUniform("uFrostAmount", (resolvedOptics.frostBlur / 24.0f * 0.85f).coerceIn(0f, 0.92f))
                shader.setFloatUniform("uSaturation", resolvedOptics.glassSaturation)
                shader.setFloatUniform("uTintRgb", resolvedOptics.tintR, resolvedOptics.tintG, resolvedOptics.tintB)
                shader.setFloatUniform("uTintOpacity", resolvedOptics.tintOpacity)
                shader.setFloatUniform("uSpecular", resolvedOptics.specularIntensity)
                shader.setFloatUniform("uLightDir", cos(resolvedOptics.lightAngleRad), sin(resolvedOptics.lightAngleRad))
                shader.setFloatUniform("uInnerShadow", resolvedOptics.innerThicknessShadow)
                shader.setFloatUniform("uDropShadow", resolvedOptics.backdropDropShadow)
                shader.setFloatUniform("uOpacity", opacity.coerceIn(0f, 1f))
                AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    this.shader = shader
                }
            }.getOrNull()
        } else {
            null
        }
    }

    var softwareRendered by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(preparedAgslPaint, activeCapture?.heightMap, bgPatch, resolvedOptics, scaleXAspect, scaleYAspect, opacity) {
        val hMap = activeCapture?.heightMap
        if (preparedAgslPaint == null &&
            hMap != null && !hMap.isRecycled &&
            !bgPatch.unblurredPatch.isRecycled && !bgPatch.frostedPatch.isRecycled
        ) {
            delay(45L)
            val rendered = withContext(Dispatchers.Default) {
                ZeroCrashEngine.runSafely("LiquidGlass_softwareRender", null) {
                    LiquidGlassClockEngine.renderOpticalGlassSoftwareBitmap(
                        glyphMap = hMap,
                        backgroundPatch = bgPatch.unblurredPatch,
                        frostedPatch = bgPatch.frostedPatch,
                        params = resolvedOptics,
                        scaleXAspect = scaleXAspect,
                        scaleYAspect = scaleYAspect,
                        opacity = opacity
                    ).asImageBitmap()
                }
            }
            if (rendered != null) {
                softwareRendered = rendered
            }
        } else if (preparedAgslPaint != null) {
            softwareRendered = null
        }
    }

    Box(modifier = modifier) {
        // Record offscreen typography mask ALWAYS at alpha = 1.0f; isolate initial warm-up opacity via saveLayerAlpha
        Box(
            modifier = Modifier
                .onSizeChanged { measured ->
                    if (measured.width > 0 && measured.height > 0) {
                        maskSize = measured
                    }
                }
                .drawWithContent {
                    glyphCaptureLayer.alpha = 1f
                    glyphCaptureLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    if (activeCapture == null) {
                        drawIntoCanvas { canvas ->
                            val nativeCanvas = canvas.nativeCanvas
                            val saveCount = nativeCanvas.saveLayerAlpha(
                                0f,
                                0f,
                                size.width.coerceAtLeast(1f),
                                size.height.coerceAtLeast(1f),
                                (0.24f * 255f).roundToInt()
                            )
                            try {
                                drawLayer(glyphCaptureLayer)
                            } finally {
                                nativeCanvas.restoreToCount(saveCount)
                            }
                        }
                    }
                }
        ) {
            maskContent()
        }

        val freshCapture = activeCapture?.takeIf { !it.heightMap.isRecycled && it.heightMap.width > 0 && it.heightMap.height > 0 }
        val displaySize = if (maskSize.width > 0 && maskSize.height > 0) maskSize else freshCapture?.size ?: IntSize.Zero
        if (freshCapture != null && displaySize.width > 0 && displaySize.height > 0) {
            val canvasWidthDp = with(density) { displaySize.width.toDp() }
            val canvasHeightDp = with(density) { displaySize.height.toDp() }

            Canvas(modifier = Modifier.size(canvasWidthDp, canvasHeightDp)) {
                val hMap = freshCapture.heightMap
                if (hMap.isRecycled || hMap.width <= 0 || hMap.height <= 0) return@Canvas
                val glyphW = hMap.width.toFloat().coerceAtLeast(1f)
                val glyphH = hMap.height.toFloat().coerceAtLeast(1f)
                val activeSoftware = softwareRendered
                if (preparedAgslPaint != null && !bgPatch.unblurredPatch.isRecycled && !bgPatch.frostedPatch.isRecycled) {
                    val drawn = runCatching {
                        drawIntoCanvas { canvas ->
                            val nativeCanvas = canvas.nativeCanvas
                            val sx = size.width / glyphW
                            val sy = size.height / glyphH
                            nativeCanvas.save()
                            nativeCanvas.scale(sx, sy)
                            nativeCanvas.drawRect(0f, 0f, glyphW, glyphH, preparedAgslPaint)
                            nativeCanvas.restore()
                        }
                        true
                    }.getOrDefault(false)
                    if (!drawn && activeSoftware != null) {
                        drawImage(
                            image = activeSoftware,
                            dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))
                        )
                    }
                } else if (activeSoftware != null) {
                    drawImage(
                        image = activeSoftware,
                        dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))
                    )
                }
            }
        }
    }
}
