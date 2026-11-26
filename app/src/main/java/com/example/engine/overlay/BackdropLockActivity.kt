package com.example.engine.overlay

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.data.model.LockScreenSetDoc
import com.example.data.model.LockShortcutAction
import com.example.data.model.MediaPlayerSizeTier
import com.example.data.model.StudioWidgetItem
import com.example.data.model.UserPreferencesDoc
import com.example.data.repository.GuestLocalUserStore
import kotlinx.coroutines.launch
import com.example.data.weather.OpenMeteoWeatherRepository
import com.example.data.weather.WeatherTelemetryState
import com.example.engine.notifications.LockScreenNotificationRepository
import com.example.engine.performance.ZeroLagPerformanceEngine
import com.example.ui.components.ClockDesignConfig
import com.example.ui.components.ClockWidgetTelemetry
import com.example.ui.theme.BackdropTheme
import com.example.ui.theme.ThemeCustomizationTokens

/**
 * Full-screen Backdrop custom lock-screen presentation.
 *
 * Security boundaries:
 * - Uses official `setShowWhenLocked(true)` (API 27+) / `FLAG_SHOW_WHEN_LOCKED` on wake.
 * - Never draws fake PIN/pattern/password UIs or disables keyguard protection.
 * - On unlock swipe/button, calls `KeyguardManager.requestDismissKeyguard` (on secure devices
 *   Android handles PIN/biometric seamlessly; on unsecured devices it transitions straight home).
 */
class BackdropLockActivity : ComponentActivity() {

    private var dismissReceiverRegistered = false

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_DISMISS_LOCK_UI,
                Intent.ACTION_USER_PRESENT -> finishLockSafely()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawable(ColorDrawable(0xFF090B10.toInt()))
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        ZeroLagPerformanceEngine.init(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)

        registerDismissReceiver()
        val isPreviewMode = intent?.getBooleanExtra(EXTRA_PREVIEW_MODE, false) == true

        setContent {
            val store = remember { GuestLocalUserStore.getInstance(applicationContext) }
            val prefs by store.observePreferences(GuestLocalUserStore.GUEST_USER_ID)
                .collectAsState(initial = store.getCurrentPreferencesSync())
            val sets by store.observeLockScreenSets(GuestLocalUserStore.GUEST_USER_ID)
                .collectAsState(initial = store.getLockScreenSetsSync())
            val weatherState by OpenMeteoWeatherRepository.state.collectAsState()
            val notificationGroups by LockScreenNotificationRepository.groups.collectAsState()

            val selectedSet: LockScreenSetDoc? = remember(sets, prefs.activeOverlaySetId) {
                sets.firstOrNull { it.id == prefs.activeOverlaySetId } ?: sets.firstOrNull()
            }
            val activeClock = remember(selectedSet) {
                selectedSet?.let { ClockDesignConfig.fromJsonString(it.clockConfigJson) }
                    ?: store.loadClockDesignConfig()
                    ?: ClockDesignConfig()
            }

            val weatherReady = weatherState as? WeatherTelemetryState.Ready
            val telemetry = remember(weatherReady, this@BackdropLockActivity) {
                ClockWidgetTelemetry(
                    weatherText = weatherReady?.compactReadout ?: "READY",
                    batteryPercent = OpenMeteoWeatherRepository.readBatteryPercent(this@BackdropLockActivity),
                    isWeatherAvailable = weatherReady != null
                )
            }

            BackHandler(enabled = true) {
                if (isPreviewMode) {
                    finishLockSafely()
                }
            }

            val customization = remember(prefs) {
                ThemeCustomizationTokens(
                    dynamicThemingEnabled = prefs.dynamicThemingEnabled,
                    customAccentHex = prefs.customAccentHex,
                    m3SurfaceDepth = prefs.m3SurfaceDepth,
                    m3CornerRadiusScale = prefs.m3CornerRadiusScale,
                    m3ElevationScale = prefs.m3ElevationScale,
                    m3DensityScale = prefs.m3DensityScale,
                    m3NavStyle = prefs.parsedMaterialNavStyle,
                    m3TypographyScale = prefs.m3TypographyScale,
                    m3MotionSpeed = prefs.m3MotionSpeed,
                    glassBlurLevel = prefs.glassBlurLevel,
                    glassRefractionLevel = prefs.glassRefractionLevel,
                    glassTransparencyLevel = prefs.glassTransparencyLevel,
                    glassLightResponse = prefs.glassLightResponse,
                    glassSpecularHighlights = prefs.glassSpecularHighlights,
                    glassBorderSoftness = prefs.glassBorderSoftness,
                    glassAdaptiveContrast = prefs.glassAdaptiveContrast,
                    dockLayoutMode = prefs.parsedDockLayoutMode,
                    useClassicClockHandles = prefs.useClassicClockHandles,
                    language = prefs.parsedLanguage,
                    studioFont = prefs.parsedStudioFont,
                    glowMaterial = prefs.parsedGlowMaterial,
                    liquidGlassTier = prefs.parsedLiquidGlassTier
                )
            }

            BackdropTheme(
                themeMode = prefs.parsedThemeMode,
                amoledDark = prefs.amoledDark,
                uiStyle = prefs.parsedUiStyle,
                useDynamicColor = prefs.dynamicThemingEnabled,
                hapticFeedbackEnabled = prefs.hapticFeedbackEnabled,
                reducedMotionEnabled = prefs.reducedMotionEnabled,
                glassDepthIntensity = prefs.glassDepthIntensity,
                customization = customization
            ) {
                LockScreenOverlaySurface(
                    preferences = prefs,
                    selectedSet = selectedSet,
                    clockConfig = activeClock,
                    telemetry = telemetry,
                    notificationGroups = if (prefs.lockNotificationsEnabled) notificationGroups else emptyList(),
                    hideSensitiveNotifications = prefs.hideSensitiveNotifications,
                    isPreviewMode = isPreviewMode,
                    onOpenNotification = { item ->
                        requestUnlockAndRun {
                            runCatching { item.contentIntent?.send() }
                        }
                    },
                    onDismissNotification = { key ->
                        LockScreenNotificationRepository.dismissNotification(key)
                    },
                    onClearAllNotifications = {
                        LockScreenNotificationRepository.clearAllClearable()
                    },
                    onUpdateWidgetItem = { updatedItem ->
                        lifecycleScope.launch {
                            store.updatePreferences(GuestLocalUserStore.GUEST_USER_ID) { current ->
                                val nextList = current.parsedStudioWidgets.map { existing ->
                                    if (existing.type == updatedItem.type) updatedItem.sanitized() else existing
                                }
                                current.copy(studioWidgetsJson = StudioWidgetItem.listToJson(nextList))
                            }
                        }
                    },
                    onUpdateWidgetsList = { nextList ->
                        lifecycleScope.launch {
                            store.updatePreferences(GuestLocalUserStore.GUEST_USER_ID) { current ->
                                current.copy(studioWidgetsJson = StudioWidgetItem.listToJson(nextList))
                            }
                        }
                    },
                    onCycleMediaSizeTier = { nextTier: MediaPlayerSizeTier ->
                        lifecycleScope.launch {
                            store.updatePreferences(GuestLocalUserStore.GUEST_USER_ID) { current ->
                                current.copy(mediaPlayerSizeTier = nextTier.name)
                            }
                        }
                    },
                    onExecuteShortcut = { shortcut ->
                        if (shortcut.action == LockShortcutAction.FLASHLIGHT) {
                            LockShortcutExecutor.toggleFlashlight(this@BackdropLockActivity)
                        } else if (isPreviewMode) {
                            LockShortcutExecutor.executeShortcut(this@BackdropLockActivity, shortcut)
                        } else {
                            requestUnlockAndRun {
                                LockShortcutExecutor.executeShortcut(this@BackdropLockActivity, shortcut)
                            }
                        }
                    },
                    onDismissToUnlock = {
                        if (isPreviewMode) {
                            finishLockSafely()
                        } else {
                            requestUnlockAndRun(onUnlocked = {})
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        if (dismissReceiverRegistered) {
            runCatching { unregisterReceiver(dismissReceiver) }
            dismissReceiverRegistered = false
        }
        super.onDestroy()
    }

    private fun registerDismissReceiver() {
        if (dismissReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(ACTION_DISMISS_LOCK_UI)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, dismissReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        dismissReceiverRegistered = true
    }

    private fun requestUnlockAndRun(onUnlocked: () -> Unit) {
        val kgm = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && kgm != null && kgm.isKeyguardLocked) {
            kgm.requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        onUnlocked()
                        finishLockSafely()
                    }

                    override fun onDismissCancelled() = Unit

                    override fun onDismissError() {
                        onUnlocked()
                        finishLockSafely()
                    }
                }
            )
        } else {
            onUnlocked()
            finishLockSafely()
        }
    }

    private fun finishLockSafely() {
        if (!isFinishing && !isDestroyed) {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, android.R.anim.fade_out)
        }
    }

    companion object {
        const val ACTION_DISMISS_LOCK_UI = "com.example.engine.overlay.ACTION_DISMISS_LOCK_UI"
        const val EXTRA_PREVIEW_MODE = "extra_preview_mode"

        fun createLaunchIntent(context: Context, previewMode: Boolean = false): Intent {
            return Intent(context.applicationContext, BackdropLockActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
                putExtra(EXTRA_PREVIEW_MODE, previewMode)
            }
        }
    }
}
