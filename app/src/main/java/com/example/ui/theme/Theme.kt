package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.example.data.model.AppLanguageOption
import com.example.data.model.BackdropStudioFont
import com.example.data.model.DockLayoutMode
import com.example.data.model.GlowMaterialConfig
import com.example.data.model.LiquidGlassTier
import com.example.data.model.MaterialNavStyle
import com.example.data.model.ThemeMode
import com.example.data.model.UiStyleMode
import com.example.engine.performance.ZeroLagPerformanceEngine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Immutable
data class ThemeCustomizationTokens(
    val dynamicThemingEnabled: Boolean = true,
    val customAccentHex: Long = 0xFF6B57A8,
    val m3SurfaceDepth: Float = 1.0f,
    val m3CornerRadiusScale: Float = 1.0f,
    val m3ElevationScale: Float = 1.0f,
    val m3DensityScale: Float = 1.0f,
    val m3NavStyle: MaterialNavStyle = MaterialNavStyle.PILL_TONAL,
    val m3TypographyScale: Float = 1.0f,
    val m3MotionSpeed: Float = 1.0f,
    val glassBlurLevel: Float = 1.0f,
    val glassRefractionLevel: Float = 1.0f,
    val glassTransparencyLevel: Float = 1.0f,
    val glassLightResponse: Float = 1.0f,
    val glassSpecularHighlights: Float = 1.0f,
    val glassBorderSoftness: Float = 1.0f,
    val glassAdaptiveContrast: Float = 1.0f,
    val dockLayoutMode: DockLayoutMode = DockLayoutMode.AUTOMATIC,
    val useClassicClockHandles: Boolean = false,
    val language: AppLanguageOption = AppLanguageOption.ENGLISH,
    val studioFont: BackdropStudioFont = BackdropStudioFont.BACKDROP_DEFAULT,
    val glowMaterial: GlowMaterialConfig = GlowMaterialConfig(),
    val liquidGlassTier: LiquidGlassTier = LiquidGlassTier.AUTO_RECOMMENDED
)

@Immutable
data class PaletteContrastReport(
    val primaryContrast: Float,
    val containerContrast: Float,
    val surfaceContrast: Float,
    val meetsWcagAa: Boolean
)

val LocalUiStyleMode = staticCompositionLocalOf { UiStyleMode.MATERIAL_3 }
val LocalDarkThemeFlag = staticCompositionLocalOf { false }
val LocalAmoledDarkFlag = staticCompositionLocalOf { false }
val LocalHapticsEnabled = staticCompositionLocalOf { true }
val LocalReducedMotionFlag = staticCompositionLocalOf { false }
val LocalGlassDepthIntensity = staticCompositionLocalOf { 1.0f }
val LocalThemeCustomization = staticCompositionLocalOf { ThemeCustomizationTokens() }

val BackdropCustomAccentSwatches: List<Pair<String, Long>> = listOf(
    "Periwinkle" to 0xFF6B57A8,
    "Ocean Cyan" to 0xFF2B7A9B,
    "Emerald Mint" to 0xFF2F856B,
    "Sunset Coral" to 0xFFC95E4B,
    "Amber Gold" to 0xFFB87D1E,
    "Berry Plum" to 0xFFA23E72,
    "Cobalt Steel" to 0xFF3E63A8,
    "Graphite Slate" to 0xFF4F5565
)

private val PastelLightColorScheme = lightColorScheme(
    primary = PastelPeriwinkle,
    onPrimary = Color.White,
    primaryContainer = PastelPeriwinkleContainer,
    onPrimaryContainer = OnPastelPeriwinkleContainer,
    secondary = PastelSage,
    onSecondary = Color.White,
    secondaryContainer = PastelSageContainer,
    onSecondaryContainer = OnPastelSageContainer,
    tertiary = PastelApricot,
    onTertiary = Color.White,
    tertiaryContainer = PastelApricotContainer,
    onTertiaryContainer = OnPastelApricotContainer,
    background = PastelAlabaster,
    onBackground = LightInk,
    surface = PastelAlabaster,
    onSurface = LightInk,
    surfaceVariant = PastelSurfaceContainer,
    onSurfaceVariant = LightMutedInk,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = PastelSurfaceLow,
    surfaceContainer = PastelSurfaceContainer,
    surfaceContainerHigh = PastelSurfaceHigh,
    surfaceContainerHighest = PastelSurfaceHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = PastelRoseError
)

private val PastelDarkColorScheme = darkColorScheme(
    primary = DarkPastelLilac,
    onPrimary = Color(0xFF281956),
    primaryContainer = DarkPastelLilacContainer,
    onPrimaryContainer = OnDarkPastelLilacContainer,
    secondary = DarkPastelMint,
    onSecondary = Color(0xFF0E382D),
    secondaryContainer = DarkPastelMintContainer,
    onSecondaryContainer = OnDarkPastelMintContainer,
    tertiary = DarkPastelPeach,
    onTertiary = Color(0xFF4A1B12),
    tertiaryContainer = DarkPastelPeachContainer,
    onTertiaryContainer = OnDarkPastelPeachContainer,
    background = TwilightObsidian,
    onBackground = DarkInk,
    surface = TwilightObsidian,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceContainer,
    onSurfaceVariant = DarkMutedInk,
    surfaceContainerLowest = Color(0xFF0A090F),
    surfaceContainerLow = DarkSurfaceLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkRoseError
)

val GlyphLightColorScheme = lightColorScheme(
    primary = GlyphLightInk,
    onPrimary = GlyphWhite,
    primaryContainer = GlyphLightSurfaceHigh,
    onPrimaryContainer = GlyphLightInk,
    secondary = Color(0xFF2B2B2B),
    onSecondary = GlyphWhite,
    secondaryContainer = GlyphLightSurface,
    onSecondaryContainer = GlyphLightInk,
    tertiary = GlyphRedDot,
    onTertiary = GlyphWhite,
    tertiaryContainer = Color(0xFFFFE5E5),
    onTertiaryContainer = Color(0xFF5A0A0A),
    background = GlyphOffWhite,
    onBackground = GlyphLightInk,
    surface = GlyphOffWhite,
    onSurface = GlyphLightInk,
    surfaceVariant = GlyphLightSurface,
    onSurfaceVariant = GlyphLightMutedInk,
    surfaceContainerLowest = GlyphWhite,
    surfaceContainerLow = GlyphLightSurfaceLow,
    surfaceContainer = GlyphLightSurface,
    surfaceContainerHigh = GlyphLightSurfaceHigh,
    surfaceContainerHighest = GlyphLightSurfaceHighest,
    outline = GlyphLightOutline,
    outlineVariant = GlyphLightOutlineVariant,
    error = GlyphRedDot
)

val GlyphDarkColorScheme = darkColorScheme(
    primary = GlyphDarkInk,
    onPrimary = GlyphPitchBlack,
    primaryContainer = GlyphDarkSurfaceHighest,
    onPrimaryContainer = GlyphDarkInk,
    secondary = Color(0xFFD0D0D0),
    onSecondary = GlyphPitchBlack,
    secondaryContainer = GlyphDarkSurfaceHigh,
    onSecondaryContainer = GlyphDarkInk,
    tertiary = GlyphRedDot,
    onTertiary = GlyphWhite,
    tertiaryContainer = Color(0xFF3A1010),
    onTertiaryContainer = Color(0xFFFFDADA),
    background = GlyphPitchBlack,
    onBackground = GlyphDarkInk,
    surface = GlyphPitchBlack,
    onSurface = GlyphDarkInk,
    surfaceVariant = GlyphDarkSurfaceHigh,
    onSurfaceVariant = GlyphDarkMutedInk,
    surfaceContainerLowest = GlyphPitchBlack,
    surfaceContainerLow = GlyphDarkSurfaceLow,
    surfaceContainer = GlyphDarkSurface,
    surfaceContainerHigh = GlyphDarkSurfaceHigh,
    surfaceContainerHighest = GlyphDarkSurfaceHighest,
    outline = GlyphDarkOutline,
    outlineVariant = GlyphDarkOutlineVariant,
    error = Color(0xFFFF5A5A)
)

fun buildBackdropShapes(cornerScale: Float = 1.0f, uiStyle: UiStyleMode = UiStyleMode.MATERIAL_3): Shapes {
    if (uiStyle == UiStyleMode.GLYPH_UI) {
        return Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(22.dp),
            extraLarge = RoundedCornerShape(26.dp)
        )
    }
    val clamped = cornerScale.coerceIn(0.5f, 1.5f)
    val s = (1f + (clamped - 1f) * 1.45f).coerceIn(0.28f, 1.90f)
    return Shapes(
        extraSmall = RoundedCornerShape((10f * s).dp),
        small = RoundedCornerShape((14f * s).dp),
        medium = RoundedCornerShape((20f * s).dp),
        large = RoundedCornerShape((28f * s).dp),
        extraLarge = RoundedCornerShape((34f * s).dp)
    )
}

val BackdropShapes = buildBackdropShapes(1.0f)

fun contrastRatio(foreground: Color, background: Color): Float {
    val l1 = foreground.luminance()
    val l2 = background.luminance()
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}

fun hslToComposeColor(hueDeg: Float, saturation: Float, lightness: Float): Color {
    val hsl = floatArrayOf(
        ((hueDeg % 360f) + 360f) % 360f,
        saturation.coerceIn(0f, 1f),
        lightness.coerceIn(0f, 1f)
    )
    return Color(ColorUtils.HSLToColor(hsl))
}

fun composeColorToHsl(color: Color): FloatArray {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    return hsl
}

private fun pickBestOnColor(container: Color, preferredDarkTint: Color, preferredLightTint: Color): Color {
    val darkC = contrastRatio(preferredDarkTint, container)
    val lightC = contrastRatio(preferredLightTint, container)
    val best = if (darkC >= lightC) preferredDarkTint else preferredLightTint
    return if (contrastRatio(best, container) >= 4.5f) {
        best
    } else {
        if (contrastRatio(Color(0xFF100E18), container) >= contrastRatio(Color.White, container)) {
            Color(0xFF100E18)
        } else {
            Color.White
        }
    }
}

fun evaluateSchemeContrast(scheme: ColorScheme): PaletteContrastReport {
    val p = contrastRatio(scheme.onPrimary, scheme.primary)
    val c = contrastRatio(scheme.onPrimaryContainer, scheme.primaryContainer)
    val s = contrastRatio(scheme.onSurface, scheme.surface)
    return PaletteContrastReport(
        primaryContrast = p,
        containerContrast = c,
        surfaceContrast = s,
        meetsWcagAa = p >= 4.5f && c >= 4.5f && s >= 4.5f
    )
}

/**
 * Full HSL / tonal custom dynamic palette generator deriving Primary, Secondary, Tertiary,
 * Surfaces, Containers, On-colours, Outlines, and Accents for Light and Dark modes with
 * automatic WCAG contrast compliance.
 */
internal fun buildSeededColorScheme(
    seedHex: Long,
    isDark: Boolean,
    surfaceDepth: Float
): ColorScheme {
    val seed = Color(seedHex.toInt()).let { if (it.alpha < 0.9f) it.copy(alpha = 1f) else it }
    val hsl = composeColorToHsl(seed)
    val hue = hsl[0]
    val sat = hsl[1].coerceIn(0.18f, 0.88f)
    val depthBias = ((surfaceDepth - 1.0f) * 0.25f).coerceIn(-0.08f, 0.14f)

    val secondaryHue = (hue + 34f) % 360f
    val tertiaryHue = (hue + 148f) % 360f

    return if (!isDark) {
        val rawPrimary = hslToComposeColor(hue, (sat * 0.92f).coerceIn(0.24f, 0.82f), hsl[2].coerceIn(0.28f, 0.42f))
        val primary = if (contrastRatio(Color.White, rawPrimary) < 4.5f && contrastRatio(Color(0xFF12101A), rawPrimary) < 4.5f) {
            hslToComposeColor(hue, (sat * 0.90f).coerceIn(0.24f, 0.80f), 0.34f)
        } else {
            rawPrimary
        }
        val onPrimary = pickBestOnColor(primary, Color(0xFF14111E), Color.White)
        val primaryContainer = hslToComposeColor(hue, (sat * 0.72f).coerceIn(0.20f, 0.75f), 0.90f)
        val onPrimaryContainer = pickBestOnColor(
            primaryContainer,
            hslToComposeColor(hue, sat.coerceIn(0.30f, 0.85f), 0.14f),
            Color.White
        )

        val secondary = hslToComposeColor(secondaryHue, (sat * 0.62f).coerceIn(0.18f, 0.64f), 0.37f)
        val onSecondary = pickBestOnColor(secondary, Color(0xFF12151C), Color.White)
        val secondaryContainer = hslToComposeColor(secondaryHue, (sat * 0.56f).coerceIn(0.16f, 0.62f), 0.90f)
        val onSecondaryContainer = pickBestOnColor(
            secondaryContainer,
            hslToComposeColor(secondaryHue, 0.65f, 0.14f),
            Color.White
        )

        val tertiary = hslToComposeColor(tertiaryHue, (sat * 0.58f).coerceIn(0.18f, 0.60f), 0.38f)
        val onTertiary = pickBestOnColor(tertiary, Color(0xFF16131E), Color.White)
        val tertiaryContainer = hslToComposeColor(tertiaryHue, (sat * 0.55f).coerceIn(0.16f, 0.60f), 0.91f)
        val onTertiaryContainer = pickBestOnColor(
            tertiaryContainer,
            hslToComposeColor(tertiaryHue, 0.65f, 0.15f),
            Color.White
        )

        val neutralSat = (sat * 0.24f + depthBias * 0.35f).coerceIn(0.05f, 0.26f)
        val bg = hslToComposeColor(hue, neutralSat, (0.98f - depthBias * 0.08f).coerceIn(0.95f, 0.99f))
        val onSurface = hslToComposeColor(hue, (neutralSat * 0.8f).coerceAtMost(0.20f), 0.11f)
        val onSurfaceVariant = hslToComposeColor(hue, (neutralSat * 0.7f).coerceAtMost(0.18f), 0.30f)

        PastelLightColorScheme.copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = bg,
            onBackground = onSurface,
            surface = bg,
            onSurface = onSurface,
            surfaceVariant = hslToComposeColor(hue, neutralSat * 1.1f, 0.92f),
            onSurfaceVariant = onSurfaceVariant,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = hslToComposeColor(hue, neutralSat * 1.05f, (0.955f - depthBias * 0.08f).coerceIn(0.92f, 0.97f)),
            surfaceContainer = hslToComposeColor(hue, neutralSat * 1.15f, (0.93f - depthBias * 0.10f).coerceIn(0.89f, 0.95f)),
            surfaceContainerHigh = hslToComposeColor(hue, neutralSat * 1.25f, (0.90f - depthBias * 0.12f).coerceIn(0.85f, 0.93f)),
            surfaceContainerHighest = hslToComposeColor(hue, neutralSat * 1.35f, (0.865f - depthBias * 0.12f).coerceIn(0.81f, 0.90f)),
            outline = hslToComposeColor(hue, neutralSat * 0.9f, 0.46f),
            outlineVariant = hslToComposeColor(hue, neutralSat * 0.8f, 0.78f)
        )
    } else {
        val primary = hslToComposeColor(hue, (sat * 0.86f).coerceIn(0.28f, 0.85f), 0.78f)
        val onPrimary = pickBestOnColor(primary, hslToComposeColor(hue, sat, 0.14f), Color.White)
        val primaryContainer = hslToComposeColor(hue, (sat * 0.68f).coerceIn(0.22f, 0.72f), 0.27f)
        val onPrimaryContainer = pickBestOnColor(primaryContainer, Color(0xFF14111E), hslToComposeColor(hue, sat * 0.5f, 0.92f))

        val secondary = hslToComposeColor(secondaryHue, (sat * 0.68f).coerceIn(0.22f, 0.70f), 0.76f)
        val onSecondary = pickBestOnColor(secondary, hslToComposeColor(secondaryHue, 0.60f, 0.14f), Color.White)
        val secondaryContainer = hslToComposeColor(secondaryHue, (sat * 0.56f).coerceIn(0.18f, 0.62f), 0.25f)
        val onSecondaryContainer = pickBestOnColor(secondaryContainer, Color(0xFF12151C), hslToComposeColor(secondaryHue, 0.45f, 0.92f))

        val tertiary = hslToComposeColor(tertiaryHue, (sat * 0.66f).coerceIn(0.22f, 0.68f), 0.78f)
        val onTertiary = pickBestOnColor(tertiary, hslToComposeColor(tertiaryHue, 0.60f, 0.15f), Color.White)
        val tertiaryContainer = hslToComposeColor(tertiaryHue, (sat * 0.54f).coerceIn(0.18f, 0.60f), 0.26f)
        val onTertiaryContainer = pickBestOnColor(tertiaryContainer, Color(0xFF14111E), hslToComposeColor(tertiaryHue, 0.45f, 0.92f))

        val neutralSat = (sat * 0.28f + depthBias * 0.35f).coerceIn(0.06f, 0.30f)
        val baseSurface = hslToComposeColor(hue, neutralSat, (0.075f + depthBias * 0.08f).coerceIn(0.05f, 0.11f))
        val onSurface = hslToComposeColor(hue, neutralSat * 0.45f, 0.95f)
        val onSurfaceVariant = hslToComposeColor(hue, neutralSat * 0.55f, 0.80f)

        PastelDarkColorScheme.copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = baseSurface,
            onBackground = onSurface,
            surface = baseSurface,
            onSurface = onSurface,
            surfaceVariant = hslToComposeColor(hue, neutralSat * 1.1f, 0.14f),
            onSurfaceVariant = onSurfaceVariant,
            surfaceContainerLowest = hslToComposeColor(hue, neutralSat, 0.045f),
            surfaceContainerLow = hslToComposeColor(hue, neutralSat * 1.05f, (0.105f + depthBias * 0.12f).coerceIn(0.08f, 0.14f)),
            surfaceContainer = hslToComposeColor(hue, neutralSat * 1.15f, (0.14f + depthBias * 0.14f).coerceIn(0.11f, 0.18f)),
            surfaceContainerHigh = hslToComposeColor(hue, neutralSat * 1.22f, (0.185f + depthBias * 0.16f).coerceIn(0.15f, 0.23f)),
            surfaceContainerHighest = hslToComposeColor(hue, neutralSat * 1.30f, (0.235f + depthBias * 0.18f).coerceIn(0.19f, 0.29f)),
            outline = hslToComposeColor(hue, neutralSat * 0.8f, 0.58f),
            outlineVariant = hslToComposeColor(hue, neutralSat * 0.8f, 0.34f)
        )
    }
}

/**
 * Guarantees WCAG-grade foreground/background separation across dynamic Material You dark palettes
 * when layered on translucent Frosted Glass OS panels and AMOLED pitch-black backdrops.
 */
internal fun ensureDarkGlassContrastScheme(
    scheme: ColorScheme,
    amoledDark: Boolean,
    isGlass: Boolean,
    dynamicThemingEnabled: Boolean = true,
    surfaceDepth: Float = 1.0f
): ColorScheme {
    fun needsLightContentFix(container: Color, content: Color): Boolean {
        return abs(container.luminance() - content.luminance()) < 0.34f
    }

    val safePrimary = when {
        !dynamicThemingEnabled -> {
            if (scheme.primary.luminance() < 0.24f) lerp(scheme.primary, Color.White, 0.32f)
            else scheme.primary
        }
        scheme.primary.luminance() < 0.36f -> DarkPastelLilac
        else -> scheme.primary
    }
    val safeOnPrimary = if (needsLightContentFix(safePrimary, scheme.onPrimary)) {
        if (safePrimary.luminance() > 0.45f) Color(0xFF14111E) else Color.White
    } else {
        scheme.onPrimary
    }

    val safePrimaryContainer = when {
        !dynamicThemingEnabled -> scheme.primaryContainer
        scheme.primaryContainer.luminance() > 0.42f || scheme.primaryContainer.luminance() < 0.04f -> DarkPastelLilacContainer
        else -> scheme.primaryContainer
    }
    val safeOnPrimaryContainer = if (needsLightContentFix(safePrimaryContainer, scheme.onPrimaryContainer) || scheme.onPrimaryContainer.luminance() < 0.65f) {
        if (!dynamicThemingEnabled) lerp(safePrimary, Color.White, 0.84f) else OnDarkPastelLilacContainer
    } else {
        scheme.onPrimaryContainer
    }

    val safeSecondary = when {
        !dynamicThemingEnabled -> scheme.secondary
        scheme.secondary.luminance() < 0.36f -> DarkPastelMint
        else -> scheme.secondary
    }
    val safeSecondaryContainer = when {
        !dynamicThemingEnabled -> scheme.secondaryContainer
        scheme.secondaryContainer.luminance() > 0.42f || scheme.secondaryContainer.luminance() < 0.04f -> DarkPastelMintContainer
        else -> scheme.secondaryContainer
    }
    val safeOnSecondaryContainer = if (needsLightContentFix(safeSecondaryContainer, scheme.onSecondaryContainer) || scheme.onSecondaryContainer.luminance() < 0.65f) {
        if (!dynamicThemingEnabled) lerp(safeSecondary, Color.White, 0.84f) else OnDarkPastelMintContainer
    } else {
        scheme.onSecondaryContainer
    }

    val baseDark = scheme.copy(
        primary = safePrimary,
        onPrimary = safeOnPrimary,
        primaryContainer = safePrimaryContainer,
        onPrimaryContainer = safeOnPrimaryContainer,
        secondary = safeSecondary,
        secondaryContainer = safeSecondaryContainer,
        onSecondaryContainer = safeOnSecondaryContainer,
        onBackground = DarkInk,
        onSurface = DarkInk,
        onSurfaceVariant = DarkMutedInk,
        outline = DarkOutline,
        outlineVariant = if (isGlass) safePrimary.copy(alpha = 0.40f) else DarkOutlineVariant
    )

    val depthTint = ((surfaceDepth - 0.6f) / 0.8f).coerceIn(0f, 1f) * 0.28f
    return if (amoledDark) {
        baseDark.copy(
            background = AmoledPitchBlack,
            surface = AmoledPitchBlack,
            surfaceContainerLowest = AmoledPitchBlack,
            surfaceContainerLow = lerp(Color(0xFF100F18), safePrimary, depthTint * 0.45f),
            surfaceContainer = lerp(Color(0xFF181724), safePrimary, depthTint * 0.65f),
            surfaceContainerHigh = lerp(Color(0xFF232132), safePrimary, depthTint * 0.82f),
            surfaceContainerHighest = lerp(Color(0xFF302E42), safePrimary, depthTint)
        )
    } else if (isGlass) {
        baseDark.copy(
            surfaceContainerLow = lerp(scheme.surfaceContainerLow, safePrimary, (0.04f + depthTint).coerceAtMost(0.28f)),
            surfaceContainer = lerp(scheme.surfaceContainer, safePrimary, (0.07f + depthTint).coerceAtMost(0.32f)),
            surfaceContainerHigh = lerp(scheme.surfaceContainerHigh, safePrimary, (0.10f + depthTint).coerceAtMost(0.36f)),
            surfaceContainerHighest = lerp(scheme.surfaceContainerHighest, safePrimary, (0.13f + depthTint).coerceAtMost(0.42f))
        )
    } else {
        baseDark.copy(
            surfaceContainerLow = lerp(scheme.surfaceContainerLow, safePrimary, (depthTint * 0.45f).coerceAtMost(0.18f)),
            surfaceContainer = lerp(scheme.surfaceContainer, safePrimary, (depthTint * 0.65f).coerceAtMost(0.24f)),
            surfaceContainerHigh = lerp(scheme.surfaceContainerHigh, safePrimary, (depthTint * 0.85f).coerceAtMost(0.30f)),
            surfaceContainerHighest = lerp(scheme.surfaceContainerHighest, safePrimary, depthTint.coerceAtMost(0.36f))
        )
    }
}

@Composable
fun BackdropTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    amoledDark: Boolean = false,
    uiStyle: UiStyleMode = UiStyleMode.MATERIAL_3,
    useDynamicColor: Boolean = true,
    hapticFeedbackEnabled: Boolean = true,
    reducedMotionEnabled: Boolean = false,
    glassDepthIntensity: Float = 1.0f,
    customization: ThemeCustomizationTokens = ThemeCustomizationTokens(dynamicThemingEnabled = useDynamicColor),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val isGlass = uiStyle == UiStyleMode.FROSTED_GLASS_OS
    val isGlyph = uiStyle == UiStyleMode.GLYPH_UI
    val effectiveDynamic = !isGlyph && useDynamicColor && customization.dynamicThemingEnabled

    val context = LocalContext.current
    val resolvedTier = ZeroLagPerformanceEngine.resolveEffectiveLiquidGlassTier(
        context = context,
        requested = customization.liquidGlassTier
    )
    val effectiveCustomization = customization.copy(liquidGlassTier = resolvedTier)

    val colorScheme = if (isGlyph) {
        if (isDark) GlyphDarkColorScheme else GlyphLightColorScheme
    } else {
        val rawScheme = when {
            effectiveDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            else -> {
                buildSeededColorScheme(
                    seedHex = customization.customAccentHex,
                    isDark = isDark,
                    surfaceDepth = customization.m3SurfaceDepth
                )
            }
        }

        if (isDark) {
            ensureDarkGlassContrastScheme(
                scheme = rawScheme,
                amoledDark = amoledDark,
                isGlass = isGlass,
                dynamicThemingEnabled = effectiveDynamic,
                surfaceDepth = customization.m3SurfaceDepth
            )
        } else {
            val depthRatio = ((customization.m3SurfaceDepth - 0.6f) / 0.8f).coerceIn(0f, 1f) * 0.22f
            if (effectiveDynamic && depthRatio > 0.01f) {
                rawScheme.copy(
                    surfaceContainerLow = lerp(rawScheme.surfaceContainerLow, rawScheme.primary, depthRatio * 0.65f),
                    surfaceContainer = lerp(rawScheme.surfaceContainer, rawScheme.primary, depthRatio * 0.90f),
                    surfaceContainerHigh = lerp(rawScheme.surfaceContainerHigh, rawScheme.primary, depthRatio * 1.15f),
                    surfaceContainerHighest = lerp(rawScheme.surfaceContainerHighest, rawScheme.primary, depthRatio * 1.35f)
                )
            } else {
                rawScheme
            }
        }
    }

    val shapes = buildBackdropShapes(customization.m3CornerRadiusScale, uiStyle = uiStyle)
    val effectiveFontForStyle = if (isGlyph && customization.studioFont == BackdropStudioFont.BACKDROP_DEFAULT) {
        BackdropStudioFont.GLYPH
    } else {
        customization.studioFont
    }
    val typography = buildAppTypography(
        scale = customization.m3TypographyScale,
        studioFont = effectiveFontForStyle
    )

    CompositionLocalProvider(
        LocalUiStyleMode provides uiStyle,
        LocalDarkThemeFlag provides isDark,
        LocalAmoledDarkFlag provides (isDark && amoledDark),
        LocalHapticsEnabled provides hapticFeedbackEnabled,
        LocalReducedMotionFlag provides reducedMotionEnabled,
        LocalGlassDepthIntensity provides glassDepthIntensity.coerceIn(0.55f, 1.45f),
        LocalThemeCustomization provides effectiveCustomization
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
