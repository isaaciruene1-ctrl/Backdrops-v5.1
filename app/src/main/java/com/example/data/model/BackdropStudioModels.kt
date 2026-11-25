package com.example.data.model

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject

/**
 * 10-Font Studio Typography system: combines bundled open-source fonts and
 * optical open-source equivalents for Google Sans (DM Sans) and Apple SF Pro (Inter).
 */
enum class BackdropStudioFont(
    val displayName: String,
    val subtitle: String,
    val sampleHeadline: String
) {
    BACKDROP_DEFAULT("Backdrop Default", "Outfit & Plus Jakarta Sans studio pairing", "Balanced Editorial"),
    GOOGLE_SANS("Google Sans", "Clean geometric optical equivalent (DM Sans)", "Friendly & Crisp"),
    APPLE_SF_PRO("Apple SF Pro", "Optical clarity & legible curves (Inter)", "Optical System"),
    GLYPH("Glyph", "Dot-matrix modular hardware typography", "09:41 MATRIX"),
    INTER("Inter", "Precision neutral UI sans-serif", "Tabular Clarity"),
    ROBOTO("Roboto", "Classic Android humanist sans-serif", "Material Standard"),
    POPPINS("Poppins", "Rounded geometric display warmth", "Geometric Soft"),
    MONTSERRAT("Montserrat", "Wide urban poster geometry", "Architectural"),
    OUTFIT("Outfit", "Clean modern geometric sans", "Studio Modern"),
    SPACE_GROTESK("Space Grotesk", "Technical asymmetric grotesque", "Tech Brutalist");

    companion object {
        fun fromName(raw: String?): BackdropStudioFont =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: BACKDROP_DEFAULT
    }
}

/**
 * Dedicated Glow Material configuration used by both Clock Faces and Frosted OS UI accents.
 */
@Immutable
data class GlowMaterialConfig(
    val intensity: Float = 0.72f,
    val radiusDp: Float = 18f,
    val colorArgb: Long = 0xFF8FB8FFL,
    val spread: Float = 0.45f,
    val softness: Float = 0.65f,
    val opacity: Float = 0.85f,
    val edgeIllumination: Float = 0.68f,
    val backgroundInteraction: Float = 0.55f,
    val lightDirectionDeg: Float = 135f
) {
    fun sanitized(): GlowMaterialConfig = copy(
        intensity = intensity.coerceIn(0.10f, 1.50f),
        radiusDp = radiusDp.coerceIn(4f, 42f),
        spread = spread.coerceIn(0f, 1f),
        softness = softness.coerceIn(0.10f, 1f),
        opacity = opacity.coerceIn(0.15f, 1f),
        edgeIllumination = edgeIllumination.coerceIn(0f, 1f),
        backgroundInteraction = backgroundInteraction.coerceIn(0f, 1f),
        lightDirectionDeg = ((lightDirectionDeg % 360f) + 360f) % 360f
    )

    fun toJsonObject(): JSONObject = JSONObject().apply {
        val s = sanitized()
        put("intensity", s.intensity.toDouble())
        put("radiusDp", s.radiusDp.toDouble())
        put("colorArgb", s.colorArgb)
        put("spread", s.spread.toDouble())
        put("softness", s.softness.toDouble())
        put("opacity", s.opacity.toDouble())
        put("edgeIllumination", s.edgeIllumination.toDouble())
        put("backgroundInteraction", s.backgroundInteraction.toDouble())
        put("lightDirectionDeg", s.lightDirectionDeg.toDouble())
    }

    fun toJsonString(): String = toJsonObject().toString()

    companion object {
        fun fromJsonObject(obj: JSONObject?): GlowMaterialConfig {
            if (obj == null) return GlowMaterialConfig()
            return GlowMaterialConfig(
                intensity = obj.optDouble("intensity", 0.72).toFloat(),
                radiusDp = obj.optDouble("radiusDp", 18.0).toFloat(),
                colorArgb = obj.optLong("colorArgb", 0xFF8FB8FFL),
                spread = obj.optDouble("spread", 0.45).toFloat(),
                softness = obj.optDouble("softness", 0.65).toFloat(),
                opacity = obj.optDouble("opacity", 0.85).toFloat(),
                edgeIllumination = obj.optDouble("edgeIllumination", 0.68).toFloat(),
                backgroundInteraction = obj.optDouble("backgroundInteraction", 0.55).toFloat(),
                lightDirectionDeg = obj.optDouble("lightDirectionDeg", 135.0).toFloat()
            ).sanitized()
        }

        fun fromJsonString(raw: String?): GlowMaterialConfig =
            if (raw.isNullOrBlank()) GlowMaterialConfig()
            else runCatching { fromJsonObject(JSONObject(raw)) }.getOrDefault(GlowMaterialConfig())
    }
}

/**
 * Hardware-aware Liquid Glass rendering tiers with manual override & thermal/battery warning.
 */
enum class LiquidGlassTier(
    val label: String,
    val subtitle: String,
    val shaderScale: Float,
    val blurMultiplier: Float,
    val refractionMultiplier: Float,
    val specularPasses: Int,
    val requiresHardwareWarning: Boolean
) {
    AUTO_RECOMMENDED("Recommended (Auto)", "Adapts shader precision automatically to device GPU & RAM", 0.85f, 1.0f, 1.0f, 2, false),
    LIGHT("Light Glass", "Lightweight translucent frosting for maximum battery & 120fps", 0.55f, 0.70f, 0.65f, 1, false),
    BALANCED("Balanced Optics", "Smooth real-time bevel refraction and adaptive specular rim", 0.82f, 1.0f, 1.0f, 2, false),
    ULTRA("Ultra Liquid", "High-sampling chromatic dispersion & double specular rim", 1.0f, 1.18f, 1.18f, 3, true),
    EXTREME("Extreme Crystal", "Full-resolution multi-pass optics (high GPU load)", 1.25f, 1.35f, 1.32f, 4, true);

    companion object {
        fun fromName(raw: String?): LiquidGlassTier =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: AUTO_RECOMMENDED

        fun recommendForDevice(ramMb: Int, sdkInt: Int, isLowRamDevice: Boolean): LiquidGlassTier = when {
            isLowRamDevice || ramMb in 1..3072 || sdkInt < 28 -> LIGHT
            ramMb in 3073..6144 || sdkInt < 33 -> BALANCED
            else -> ULTRA
        }
    }
}

/**
 * Phone-brand & custom lock-screen notification design presets.
 */
enum class NotificationStylePreset(
    val label: String,
    val subtitle: String,
    val defaultCornerDp: Float,
    val defaultTransparency: Float,
    val defaultBlur: Float,
    val defaultSpacingDp: Float,
    val defaultShadow: Float
) {
    APPLE_IOS("Frosted Stack", "Translucent frosted rounded card stack with layered depth", 24f, 0.72f, 0.88f, 10f, 0.35f),
    XIAOMI_HYPEROS("Xiaomi HyperOS", "Soft layered capsules with compact icon & HyperOS 4 stack", 22f, 0.78f, 0.82f, 9f, 0.28f),
    SAMSUNG_ONE_UI("Samsung One UI", "High-legibility cards, rounded rectangles & clean hierarchy", 26f, 0.84f, 0.70f, 10f, 0.25f),
    GOOGLE_PIXEL("Google Pixel", "Material You tonal surfaces, adaptive accent & clean spacing", 28f, 0.90f, 0.45f, 10f, 0.20f),
    MINIMAL("Minimal", "Ultra-clean distraction-free notification rows", 16f, 0.62f, 0.35f, 6f, 0.12f),
    GLASS("Glass", "Frosted specular border crystal cards", 24f, 0.56f, 0.96f, 10f, 0.42f),
    FLOATING("Floating", "Airy elevated pills with distinct separation", 50f, 0.82f, 0.65f, 12f, 0.48f),
    COMPACT("Compact", "Dense slim notification bar for maximum wallpaper visibility", 18f, 0.74f, 0.55f, 6f, 0.22f);

    companion object {
        fun fromName(raw: String?): NotificationStylePreset =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: APPLE_IOS
    }
}

enum class NotificationLayoutMode(val label: String) {
    STACK("Stack"),
    LIST("List"),
    COUNT("Count");

    companion object {
        fun fromJsonStringOrLegacy(raw: String?): NotificationLayoutMode = when (raw?.trim()?.uppercase()) {
            "STACK", "STACKED_BY_APP" -> STACK
            "LIST", "EXPANDED_CARDS" -> LIST
            "COUNT", "COMPACT_PILLS" -> COUNT
            else -> STACK
        }
    }
}

@Immutable
data class LockNotificationStyleConfig(
    val preset: NotificationStylePreset = NotificationStylePreset.APPLE_IOS,
    val layoutMode: NotificationLayoutMode = NotificationLayoutMode.STACK,
    val customBackgroundArgb: Long? = null,
    val transparency: Float = 0.78f,
    val blurIntensity: Float = 0.85f,
    val cornerRadiusDp: Float = 24f,
    val cardSpacingDp: Float = 9f,
    val iconSizeDp: Float = 30f,
    val compactTextHierarchy: Boolean = false,
    val showTimestamp: Boolean = true,
    val shadowIntensity: Float = 0.34f,
    val maxVisibleGroups: Int = 5,
    val autoAdaptToClock: Boolean = true
) {
    fun sanitized(): LockNotificationStyleConfig = copy(
        transparency = transparency.coerceIn(0.25f, 0.98f),
        blurIntensity = blurIntensity.coerceIn(0f, 1f),
        cornerRadiusDp = cornerRadiusDp.coerceIn(8f, 36f),
        cardSpacingDp = cardSpacingDp.coerceIn(4f, 18f),
        iconSizeDp = iconSizeDp.coerceIn(22f, 40f),
        shadowIntensity = shadowIntensity.coerceIn(0f, 1f),
        maxVisibleGroups = maxVisibleGroups.coerceIn(1, 8)
    )

    fun toJsonString(): String = JSONObject().apply {
        val s = sanitized()
        put("preset", s.preset.name)
        put("layoutMode", s.layoutMode.name)
        s.customBackgroundArgb?.let { put("customBackgroundArgb", it) }
        put("transparency", s.transparency.toDouble())
        put("blurIntensity", s.blurIntensity.toDouble())
        put("cornerRadiusDp", s.cornerRadiusDp.toDouble())
        put("cardSpacingDp", s.cardSpacingDp.toDouble())
        put("iconSizeDp", s.iconSizeDp.toDouble())
        put("compactTextHierarchy", s.compactTextHierarchy)
        put("showTimestamp", s.showTimestamp)
        put("shadowIntensity", s.shadowIntensity.toDouble())
        put("maxVisibleGroups", s.maxVisibleGroups)
        put("autoAdaptToClock", s.autoAdaptToClock)
    }.toString()

    companion object {
        fun fromPreset(preset: NotificationStylePreset): LockNotificationStyleConfig = LockNotificationStyleConfig(
            preset = preset,
            layoutMode = NotificationLayoutMode.STACK,
            transparency = preset.defaultTransparency,
            blurIntensity = preset.defaultBlur,
            cornerRadiusDp = preset.defaultCornerDp,
            cardSpacingDp = preset.defaultSpacingDp,
            shadowIntensity = preset.defaultShadow
        )

        fun fromJsonString(raw: String?): LockNotificationStyleConfig {
            if (raw.isNullOrBlank()) return LockNotificationStyleConfig()
            return runCatching {
                val obj = JSONObject(raw)
                val preset = NotificationStylePreset.fromName(obj.optString("preset"))
                val layout = NotificationLayoutMode.fromJsonStringOrLegacy(obj.optString("layoutMode"))
                val bg = if (obj.has("customBackgroundArgb")) obj.optLong("customBackgroundArgb") else null
                LockNotificationStyleConfig(
                    preset = preset,
                    layoutMode = layout,
                    customBackgroundArgb = bg,
                    transparency = obj.optDouble("transparency", preset.defaultTransparency.toDouble()).toFloat(),
                    blurIntensity = obj.optDouble("blurIntensity", preset.defaultBlur.toDouble()).toFloat(),
                    cornerRadiusDp = obj.optDouble("cornerRadiusDp", preset.defaultCornerDp.toDouble()).toFloat(),
                    cardSpacingDp = obj.optDouble("cardSpacingDp", preset.defaultSpacingDp.toDouble()).toFloat(),
                    iconSizeDp = obj.optDouble("iconSizeDp", 30.0).toFloat(),
                    compactTextHierarchy = obj.optBoolean("compactTextHierarchy", false),
                    showTimestamp = obj.optBoolean("showTimestamp", true),
                    shadowIntensity = obj.optDouble("shadowIntensity", preset.defaultShadow.toDouble()).toFloat(),
                    maxVisibleGroups = obj.optInt("maxVisibleGroups", 5),
                    autoAdaptToClock = obj.optBoolean("autoAdaptToClock", true)
                ).sanitized()
            }.getOrDefault(LockNotificationStyleConfig())
        }
    }
}

enum class ChargingAnimationPreset(val label: String, val subtitle: String) {
    PULSE_RING("Pulse Ring", "Expanding luminous battery orbital ring"),
    LIQUID_WAVE("Liquid Wave", "Fluid rising charge reservoir with meniscus"),
    ORB_HALO("Halo Arc", "Hyper-smooth kinetic glow halo around bottom charge"),
    MINIMAL_BEAM("Minimal Beam", "Sleek floating pill with charging bolt & percentage"),
    ENERGY_CORE("Energy Core", "Harmonic kinetic flux rings with radiant core embers"),
    OFF("Disabled", "Hide charging overlay animation")
}

enum class ChargingPosition(val label: String) {
    BOTTOM("Bottom"),
    CENTER("Center")
}

@Immutable
data class ChargingAnimationConfig(
    val enabled: Boolean = true,
    val preset: ChargingAnimationPreset = ChargingAnimationPreset.PULSE_RING,
    val speedMultiplier: Float = 1.0f,
    val sizeScale: Float = 1.0f,
    val position: ChargingPosition = ChargingPosition.BOTTOM,
    val dynamicAccentColor: Boolean = true,
    val customAccentArgb: Long = 0xFF4CD964L,
    val showPercentage: Boolean = true,
    val showStatusText: Boolean = true
) {
    fun toJsonString(): String = JSONObject().apply {
        put("enabled", enabled)
        put("preset", preset.name)
        put("speedMultiplier", speedMultiplier.coerceIn(0.5f, 2.0f).toDouble())
        put("sizeScale", sizeScale.coerceIn(0.65f, 1.5f).toDouble())
        put("position", position.name)
        put("dynamicAccentColor", dynamicAccentColor)
        put("customAccentArgb", customAccentArgb)
        put("showPercentage", showPercentage)
        put("showStatusText", showStatusText)
    }.toString()

    companion object {
        fun fromJsonString(raw: String?): ChargingAnimationConfig {
            if (raw.isNullOrBlank()) return ChargingAnimationConfig()
            return runCatching {
                val o = JSONObject(raw)
                ChargingAnimationConfig(
                    enabled = o.optBoolean("enabled", true),
                    preset = runCatching { ChargingAnimationPreset.valueOf(o.optString("preset")) }
                        .getOrDefault(ChargingAnimationPreset.PULSE_RING),
                    speedMultiplier = o.optDouble("speedMultiplier", 1.0).toFloat().coerceIn(0.5f, 2.0f),
                    sizeScale = o.optDouble("sizeScale", 1.0).toFloat().coerceIn(0.65f, 1.5f),
                    position = runCatching { ChargingPosition.valueOf(o.optString("position")) }
                        .getOrDefault(ChargingPosition.BOTTOM),
                    dynamicAccentColor = o.optBoolean("dynamicAccentColor", true),
                    customAccentArgb = o.optLong("customAccentArgb", 0xFF4CD964L),
                    showPercentage = o.optBoolean("showPercentage", true),
                    showStatusText = o.optBoolean("showStatusText", true)
                )
            }.getOrDefault(ChargingAnimationConfig())
        }
    }
}

enum class UnlockButtonStyle(val label: String) {
    DYNAMIC_ACCENT("Dynamic Accent"),
    SOLID("Solid Contrast"),
    FROSTED("Frosted Glass"),
    TRANSPARENT("Transparent Pill"),
    M3_TONAL("Material Tonal"),
    CUSTOM("Custom Colours")
}

@Immutable
data class UnlockButtonThemeConfig(
    val style: UnlockButtonStyle = UnlockButtonStyle.FROSTED,
    val customContainerArgb: Long = 0xFF1F2430L,
    val customContentArgb: Long = 0xFFFFFFFFL,
    val transparency: Float = 0.88f,
    val cornerRadiusDp: Float = 28f
) {
    fun toJsonString(): String = JSONObject().apply {
        put("style", style.name)
        put("customContainerArgb", customContainerArgb)
        put("customContentArgb", customContentArgb)
        put("transparency", transparency.coerceIn(0.15f, 1f).toDouble())
        put("cornerRadiusDp", cornerRadiusDp.coerceIn(8f, 32f).toDouble())
    }.toString()

    companion object {
        fun fromJsonString(raw: String?): UnlockButtonThemeConfig {
            if (raw.isNullOrBlank()) return UnlockButtonThemeConfig()
            return runCatching {
                val o = JSONObject(raw)
                UnlockButtonThemeConfig(
                    style = runCatching { UnlockButtonStyle.valueOf(o.optString("style")) }
                        .getOrDefault(UnlockButtonStyle.FROSTED),
                    customContainerArgb = o.optLong("customContainerArgb", 0xFF1F2430L),
                    customContentArgb = o.optLong("customContentArgb", 0xFFFFFFFFL),
                    transparency = o.optDouble("transparency", 0.88).toFloat().coerceIn(0.15f, 1f),
                    cornerRadiusDp = o.optDouble("cornerRadiusDp", 28.0).toFloat().coerceIn(8f, 32f)
                )
            }.getOrDefault(UnlockButtonThemeConfig())
        }
    }
}

/**
 * Complete 22-widget Backdrop suite supporting both Lock Screen Studio and Android Home AppWidgets.
 */
enum class StudioWidgetType(
    val title: String,
    val subtitle: String,
    val gridSpanLabel: String
) {
    DIGITAL_CLOCK("Digital Clock", "Crisp typography clock with seconds & AM/PM", "4×1"),
    ANALOG_CLOCK("Analog Clock", "Sweeping mechanical dial & minimalist indices", "2×2"),
    WEATHER("Weather Widget", "Live Open-Meteo temperature, condition & city", "2×2"),
    CALENDAR("Calendar Widget", "Month date card + live device calendar events", "4×2"),
    BATTERY("Battery Widget", "Device charge ring, voltage & charging status", "2×1"),
    MUSIC_PLAYER("Music Player", "Active media artwork, track metadata & transport", "4×2"),
    QUICK_INFO("Quick Info", "Combined date, next agenda & ambient telemetry", "4×1"),
    PHOTO_FRAME("Photo Frame", "Curated canvas or Photo Picker portrait frame", "2×2"),
    SYSTEM_INFO("System Info", "RAM pressure, storage, display & GPU tier diagnostics", "2×2"),
    WORLD_CLOCK("Dual-City World Clock", "Side-by-side local & international timezone comparison", "4×1"),
    FOCUS_TIMER("Focus Timer", "Pomodoro 25:00 timer with tap-to-start, pause & reset", "2×1"),
    HABIT_STREAK("Habit Streak", "Daily streak counter with interactive check-in progress", "2×2"),
    STOPWATCH("Stopwatch", "Precision lap stopwatch with tap start, pause & reset", "2×1"),
    COUNTDOWN("Countdown", "Quick-set minute countdown with +1m/+5m presets", "2×1"),
    COMPASS("Compass", "Live magnetic orientation dial & cardinal bearing readout", "2×2"),
    SUNRISE_SUNSET("Sunrise & Sunset", "Daily solar arc progress with dawn & dusk times", "4×1"),
    DAY_PROGRESS("Day Progress", "Real-time day and week completion meter", "4×1"),
    PINNED_NOTE("Pinned Note", "Glanceable lock & home reminder with instant cycle presets", "4×2"),
    NETWORK_STATUS("Network Status", "Live Wi-Fi / cellular link speed & connectivity status", "2×1"),
    STEP_COUNTER("Step Counter", "Motion-synced daily activity counter toward 8k goal", "2×2"),
    AUDIO_VOLUME("Audio & Volume", "Media output level meter with quick volume step controls", "4×1"),
    DAILY_QUOTE("Daily Quote", "Curated studio typography quote rotating each day", "4×2")
}

@Immutable
data class StudioWidgetItem(
    val type: StudioWidgetType,
    val enabled: Boolean = false,
    val compact: Boolean = false,
    val customPhotoUri: String = "",
    val widthScale: Float = 1.0f,
    val heightScale: Float = 1.0f,
    val widgetMeta: String = "",
    val sortOrder: Int = 0,
    val isFreePositioned: Boolean = false,
    val freeXFraction: Float = 0.5f,
    val freeYFraction: Float = 0.42f
) {
    fun sanitized(): StudioWidgetItem = copy(
        widthScale = widthScale.coerceIn(0.65f, 1.65f),
        heightScale = heightScale.coerceIn(0.65f, 1.65f),
        sortOrder = sortOrder.coerceAtLeast(0),
        freeXFraction = freeXFraction.coerceIn(0.10f, 0.90f),
        freeYFraction = freeYFraction.coerceIn(0.16f, 0.84f)
    )

    companion object {
        fun listToJson(items: List<StudioWidgetItem>): String {
            val arr = JSONArray()
            items.forEachIndexed { idx, raw ->
                val item = raw.sanitized()
                arr.put(JSONObject().apply {
                    put("type", item.type.name)
                    put("enabled", item.enabled)
                    put("compact", item.compact)
                    put("customPhotoUri", item.customPhotoUri)
                    put("widthScale", item.widthScale.toDouble())
                    put("heightScale", item.heightScale.toDouble())
                    put("sortOrder", if (raw.sortOrder == 0 && idx > 0 && items.all { it.sortOrder == 0 }) idx else item.sortOrder)
                    put("isFreePositioned", item.isFreePositioned)
                    put("freeXFraction", item.freeXFraction.toDouble())
                    put("freeYFraction", item.freeYFraction.toDouble())
                    if (item.widgetMeta.isNotEmpty()) {
                        put("widgetMeta", item.widgetMeta)
                    }
                })
            }
            return arr.toString()
        }

        fun listFromJson(raw: String?): List<StudioWidgetItem> {
            val saved = if (raw.isNullOrBlank()) emptyList() else runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    val t = runCatching { StudioWidgetType.valueOf(o.optString("type")) }.getOrNull() ?: return@mapNotNull null
                    StudioWidgetItem(
                        type = t,
                        enabled = o.optBoolean("enabled", false),
                        compact = o.optBoolean("compact", false),
                        customPhotoUri = o.optString("customPhotoUri", ""),
                        widthScale = o.optDouble("widthScale", 1.0).toFloat(),
                        heightScale = o.optDouble("heightScale", 1.0).toFloat(),
                        widgetMeta = o.optString("widgetMeta", ""),
                        sortOrder = if (o.has("sortOrder")) o.optInt("sortOrder", i) else i,
                        isFreePositioned = o.optBoolean("isFreePositioned", false),
                        freeXFraction = o.optDouble("freeXFraction", 0.5).toFloat(),
                        freeYFraction = o.optDouble("freeYFraction", 0.42).toFloat()
                    ).sanitized()
                }
            }.getOrDefault(emptyList())
            val map = saved.associateBy { it.type }
            return StudioWidgetType.entries.map { t ->
                map[t] ?: StudioWidgetItem(
                    type = t,
                    enabled = t == StudioWidgetType.WEATHER || t == StudioWidgetType.BATTERY,
                    sortOrder = t.ordinal
                )
            }
        }
    }
}

enum class OfflinePackSizeOption(
    val count: Int,
    val label: String,
    val estimatedMb: Int
) {
    ESSENTIAL(25, "Essential Pack (25)", 22),
    STUDIO(60, "Studio Pack (60)", 54),
    EXPANDED(150, "Expanded Pack (150)", 135),
    ARCHIVE(300, "Archive Pack (300)", 270);

    companion object {
        fun fromCount(count: Int): OfflinePackSizeOption =
            entries.minByOrNull { kotlin.math.abs(it.count - count) } ?: STUDIO
    }
}
