package com.example.engine.overlay

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.example.engine.notifications.LockScreenNotificationRepository

enum class OemProfile(
    val displayName: String,
    val baseStatus: CompatibilityStatus,
    val guidance: String
) {
    STOCK_ANDROID(
        displayName = "Stock Android / AOSP",
        baseStatus = CompatibilityStatus.FULLY_SUPPORTED,
        guidance = "Full overlay & keyguard coexistence supported via Display Over Other Apps."
    ),
    PIXEL(
        displayName = "Google Pixel",
        baseStatus = CompatibilityStatus.FULLY_SUPPORTED,
        guidance = "Supported on Pixel OS. Grant Display Over Other Apps and allow Backdrop status notification."
    ),
    SAMSUNG_ONE_UI(
        displayName = "Samsung One UI",
        baseStatus = CompatibilityStatus.PARTIALLY_SUPPORTED,
        guidance = "Supported over keyguard when Display Over Other Apps and Unrestricted battery usage are enabled."
    ),
    XIAOMI_HYPEROS_MIUI(
        displayName = "Xiaomi HyperOS / MIUI",
        baseStatus = CompatibilityStatus.PARTIALLY_SUPPORTED,
        guidance = "Also enable 'Show on Lock screen' and 'Display pop-up windows while running in background' in App Info > Other Permissions."
    ),
    OPPO_COLOROS(
        displayName = "OPPO ColorOS",
        baseStatus = CompatibilityStatus.PARTIALLY_SUPPORTED,
        guidance = "Enable Display Over Other Apps and Allow Background Activity in Battery settings."
    ),
    ONEPLUS_OXYGENOS(
        displayName = "OnePlus OxygenOS",
        baseStatus = CompatibilityStatus.PARTIALLY_SUPPORTED,
        guidance = "Enable Display Over Other Apps and disable deep sleep optimization for Backdrop."
    ),
    VIVO_ORIGINOS_FUNTOUCH(
        displayName = "Vivo OriginOS / Funtouch OS",
        baseStatus = CompatibilityStatus.PARTIALLY_SUPPORTED,
        guidance = "Enable Floating Window, Lock screen display toggle in iManager / App Permissions, and High Background Power."
    ),
    REALME_UI(
        displayName = "Realme UI",
        baseStatus = CompatibilityStatus.PARTIALLY_SUPPORTED,
        guidance = "Grant Display Over Other Apps and lock Backdrop in recent tasks if OEM memory cleaner clears overlays."
    ),
    MOTOROLA(
        displayName = "Motorola",
        baseStatus = CompatibilityStatus.FULLY_SUPPORTED,
        guidance = "Coexists cleanly with Moto Lock & Peek Display when Display Over Other Apps is granted."
    ),
    NOTHING_OS(
        displayName = "Nothing OS",
        baseStatus = CompatibilityStatus.FULLY_SUPPORTED,
        guidance = "Clean AOSP-based overlay lifecycle supported with Display Over Other Apps enabled."
    ),
    TABLET(
        displayName = "Android Tablet",
        baseStatus = CompatibilityStatus.FULLY_SUPPORTED,
        guidance = "Adaptive lock-screen layout scales automatically across landscape and portrait tablet lock screens."
    ),
    FOLDABLE(
        displayName = "Foldable Device",
        baseStatus = CompatibilityStatus.PARTIALLY_SUPPORTED,
        guidance = "Supports cover and inner displays; secure system biometric & PIN prompts always remain unobstructed."
    ),
    INFINIX_XOS(
        displayName = "Infinix XOS (Transsion)",
        baseStatus = CompatibilityStatus.PARTIALLY_SUPPORTED,
        guidance = "Supported on Infinix XOS & XOS Launcher. Enable 'Display Over Other Apps', allow 'Lock Screen Display' in Phone Master > App Management, and exempt Backdrop in Power Master auto-freeze."
    ),
    CUSTOM_ROM(
        displayName = "Custom ROM (Lineage / Graphene / etc.)",
        baseStatus = CompatibilityStatus.PARTIALLY_SUPPORTED,
        guidance = "Depends on ROM background overlay policy. Hardened privacy ROMs may block overlays over secure keyguards."
    )
}

enum class CompatibilityStatus(val label: String) {
    FULLY_SUPPORTED("Fully Supported"),
    PARTIALLY_SUPPORTED("Partially Supported"),
    UNSUPPORTED("Unsupported")
}

data class OverlayDiagnosticsReport(
    val detectedProfile: OemProfile,
    val effectiveStatus: CompatibilityStatus,
    val manufacturer: String,
    val model: String,
    val androidSdk: Int,
    val androidRelease: String,
    val canDrawOverlays: Boolean,
    val canManageOverlayIntent: Boolean,
    val isBatteryOptimizationIgnored: Boolean,
    val isKeyguardSecure: Boolean,
    val isNotificationAccessGranted: Boolean = false,
    val summaryMessage: String,
    val oemInstructions: String,
    val allTestedProfiles: List<OemProfile> = OemProfile.entries
)

/**
 * Compatibility & System Integration Engine V5 — Sub-Engine A: OEM & Keyguard Policy Detector.
 */
internal object OemKeyguardPolicySubEngine {
    fun detect(
        manufacturer: String,
        brand: String,
        model: String,
        display: String,
        fingerprint: String,
        smallestScreenWidthDp: Int,
        hasHingeFeature: Boolean
    ): OemProfile {
        val mfr = manufacturer.lowercase()
        val br = brand.lowercase()
        val mod = model.lowercase()
        val disp = display.lowercase()
        val fp = fingerprint.lowercase()

        if (hasHingeFeature || mod.contains("fold") || mod.contains("flip") || mod.contains("razr") || mod.contains("find n")) {
            return OemProfile.FOLDABLE
        }
        if (smallestScreenWidthDp >= 600 || mod.contains("tab") || mod.contains("pad")) {
            return OemProfile.TABLET
        }
        if (disp.contains("lineage") || disp.contains("graphene") || disp.contains("calyx") ||
            disp.contains("crdroid") || disp.contains("evolution") || fp.contains("test-keys")
        ) {
            return OemProfile.CUSTOM_ROM
        }
        return when {
            mfr.contains("infinix") || br.contains("infinix") || mfr.contains("transsion") ||
                br.contains("transsion") || mfr.contains("tecno") || br.contains("tecno") ||
                mfr.contains("itel") || br.contains("itel") || disp.contains("xos") || disp.contains("hios") -> OemProfile.INFINIX_XOS
            mfr.contains("google") || br.contains("google") || mod.contains("pixel") -> OemProfile.PIXEL
            mfr.contains("samsung") || br.contains("samsung") -> OemProfile.SAMSUNG_ONE_UI
            mfr.contains("xiaomi") || br.contains("redmi") || br.contains("poco") -> OemProfile.XIAOMI_HYPEROS_MIUI
            mfr.contains("oneplus") || br.contains("oneplus") -> OemProfile.ONEPLUS_OXYGENOS
            mfr.contains("realme") || br.contains("realme") -> OemProfile.REALME_UI
            mfr.contains("oppo") || br.contains("oppo") -> OemProfile.OPPO_COLOROS
            mfr.contains("vivo") || br.contains("iqoo") -> OemProfile.VIVO_ORIGINOS_FUNTOUCH
            mfr.contains("motorola") || br.contains("motorola") -> OemProfile.MOTOROLA
            mfr.contains("nothing") || br.contains("nothing") -> OemProfile.NOTHING_OS
            else -> OemProfile.STOCK_ANDROID
        }
    }
}

/**
 * Compatibility & System Integration Engine V5 — Sub-Engine B: Live Host Capability & Widget Support Inspector.
 */
internal object HostCapabilityInspectorSubEngine {
    fun inspect(context: Context): OverlayDiagnosticsReport {
        val pm = context.packageManager
        val smallestSwDp = context.resources.configuration.smallestScreenWidthDp
        val hasHinge = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE) }.getOrDefault(false)
        } else {
            false
        }
        val profile = OemKeyguardPolicySubEngine.detect(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            brand = Build.BRAND.orEmpty(),
            model = Build.MODEL.orEmpty(),
            display = Build.DISPLAY.orEmpty(),
            fingerprint = Build.FINGERPRINT.orEmpty(),
            smallestScreenWidthDp = smallestSwDp,
            hasHingeFeature = hasHinge
        )
        val canDraw = runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false)
        val overlayIntent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        val canHandleOverlaySettings = runCatching { overlayIntent.resolveActivity(pm) != null }.getOrDefault(false)

        val powerMgr = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryIgnored = runCatching { powerMgr?.isIgnoringBatteryOptimizations(context.packageName) }.getOrNull() ?: false

        val kgm = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val keyguardSecure = runCatching { kgm?.isKeyguardSecure }.getOrNull() ?: false
        val notifAccess = LockScreenNotificationRepository.isNotificationAccessEnabled(context)

        val isRestrictedFormFactor = runCatching {
            pm.hasSystemFeature(PackageManager.FEATURE_WATCH) ||
                pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
                (pm.hasSystemFeature(PackageManager.FEATURE_RAM_LOW) && !canHandleOverlaySettings)
        }.getOrDefault(false)

        val effectiveStatus = when {
            isRestrictedFormFactor || Build.VERSION.SDK_INT < Build.VERSION_CODES.O -> CompatibilityStatus.UNSUPPORTED
            !canDraw -> CompatibilityStatus.PARTIALLY_SUPPORTED
            profile.baseStatus == CompatibilityStatus.FULLY_SUPPORTED -> CompatibilityStatus.FULLY_SUPPORTED
            else -> profile.baseStatus
        }

        val summary = when (effectiveStatus) {
            CompatibilityStatus.FULLY_SUPPORTED ->
                "Ready on ${profile.displayName}. Backdrop's non-bypassing clock overlay coexists cleanly with system security."
            CompatibilityStatus.PARTIALLY_SUPPORTED -> if (!canDraw) {
                "Grant 'Display over other apps' permission to activate Lock Screen Mode on ${profile.displayName}."
            } else {
                "Supported with OEM power/lock-screen rules. Secure PIN, pattern & biometric unlock always stay active."
            }
            CompatibilityStatus.UNSUPPORTED ->
                "Application overlays are not supported by this OS/form factor (${Build.MODEL}). Backdrop studio & system wallpapers still work fully."
        }

        return OverlayDiagnosticsReport(
            detectedProfile = profile,
            effectiveStatus = effectiveStatus,
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            androidSdk = Build.VERSION.SDK_INT,
            androidRelease = Build.VERSION.RELEASE.orEmpty(),
            canDrawOverlays = canDraw,
            canManageOverlayIntent = canHandleOverlaySettings,
            isBatteryOptimizationIgnored = batteryIgnored,
            isKeyguardSecure = keyguardSecure,
            isNotificationAccessGranted = notifAccess,
            summaryMessage = summary,
            oemInstructions = profile.guidance
        )
    }
}

/**
 * Universal KLCK-style Lock-Screen Overlay Compatibility Manager (V5).
 * Strictly adheres to Android security boundaries:
 * - Never bypasses or disables PIN/Pattern/Password/Biometric authentication.
 * - Evaluates real device OEM characteristics, overlay permission, keyguard state, and battery exemptions.
 */
object LockScreenCompatibilityManager {

    fun detectOemProfile(
        manufacturer: String = Build.MANUFACTURER.orEmpty(),
        brand: String = Build.BRAND.orEmpty(),
        model: String = Build.MODEL.orEmpty(),
        display: String = Build.DISPLAY.orEmpty(),
        fingerprint: String = Build.FINGERPRINT.orEmpty(),
        smallestScreenWidthDp: Int = 400,
        hasHingeFeature: Boolean = false
    ): OemProfile = OemKeyguardPolicySubEngine.detect(
        manufacturer = manufacturer,
        brand = brand,
        model = model,
        display = display,
        fingerprint = fingerprint,
        smallestScreenWidthDp = smallestScreenWidthDp,
        hasHingeFeature = hasHingeFeature
    )

    fun inspectDevice(context: Context): OverlayDiagnosticsReport =
        HostCapabilityInspectorSubEngine.inspect(context)

    fun buildOverlayPermissionIntent(context: Context): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    @Suppress("BatteryLife")
    fun buildBatteryOptimizationIntent(context: Context): Intent = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun buildInfinixPhoneMasterIntent(context: Context): Intent {
        val candidates = listOf(
            Intent().setClassName("com.transsion.phonemanager", "com.transsion.phonemanager.MainActivity"),
            Intent().setClassName("com.transsion.mobilemaster", "com.transsion.mobilemaster.MainActivity"),
            Intent("com.transsion.phonemanager.AUTORUN"),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        )
        val pm = context.packageManager
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(pm) != null) {
                return intent
            }
        }
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
