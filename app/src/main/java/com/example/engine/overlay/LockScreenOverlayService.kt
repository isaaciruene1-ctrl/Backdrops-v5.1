package com.example.engine.overlay

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.catalog.WallpaperCatalog
import com.example.data.media.WallpaperMediaService
import com.example.data.model.LockScreenSetDoc
import com.example.data.model.LockShortcutConfig
import com.example.data.model.MediaPlayerSizeTier
import com.example.data.model.StudioWidgetItem
import com.example.data.model.UserPreferencesDoc
import com.example.data.model.Wallpaper
import com.example.data.repository.GuestLocalUserStore
import com.example.data.weather.OpenMeteoWeatherRepository
import com.example.engine.depth.DepthDetectionEngine
import com.example.engine.notifications.LockNotificationGroup
import com.example.engine.notifications.LockNotificationItem
import com.example.engine.performance.ZeroLagPerformanceEngine
import com.example.ui.components.ClockDesignConfig
import com.example.ui.components.ClockWidgetTelemetry
import com.example.ui.components.SharedLockScreenRenderer
import com.example.ui.screens.CanvasLookFilter
import com.example.ui.theme.AppMotion
import com.example.ui.theme.LocalReducedMotionFlag
import com.example.ui.theme.LocalThemeCustomization
import com.example.ui.theme.LocalUiStyleMode
import com.example.widget.BackdropWidgetContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground wake & keyguard monitor service for Backdrop Lock Screen Mode.
 *
 * Replaces fragile `WindowManager` overlay attachment with reliable screen-off -> screen-on wake
 * tracking that presents [BackdropLockActivity] only after the display actually turns off, and
 * dismisses it immediately upon `ACTION_USER_PRESENT`.
 */
class LockScreenOverlayService : Service() {

    private var receiverRegistered = false
    private val screenWasOff = AtomicBoolean(false)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var lastLaunchElapsedMs: Long = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenWasOff.set(true)
                    warmLockAssetsInBackground()
                }
                Intent.ACTION_SCREEN_ON -> {
                    handleScreenWake()
                }
                Intent.ACTION_USER_PRESENT -> {
                    screenWasOff.set(false)
                    sendBroadcast(Intent(BackdropLockActivity.ACTION_DISMISS_LOCK_UI).setPackage(packageName))
                }
            }
        }
    }

    private fun warmLockAssetsInBackground() {
        val store = GuestLocalUserStore.getInstance(this)
        val prefs = store.getCurrentPreferencesSync()
        if (!prefs.lockScreenOverlayEnabled) return
        val sets = store.getLockScreenSetsSync()
        val activeSet = sets.firstOrNull { it.id == prefs.activeOverlaySetId } ?: sets.firstOrNull()
        serviceScope.launch {
            ZeroLagPerformanceEngine.InstantLockScreenEngine.warmLockAssets(
                context = applicationContext,
                preferences = prefs,
                activeSet = activeSet
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ZeroLagPerformanceEngine.init(applicationContext)
        val pm = getSystemService(POWER_SERVICE) as? PowerManager
        screenWasOff.set(pm?.isInteractive == false)
        ensureNotificationChannel()
        registerScreenLifecycleReceiver()
        warmLockAssetsInBackground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            screenWasOff.set(false)
            sendBroadcast(Intent(BackdropLockActivity.ACTION_DISMISS_LOCK_UI).setPackage(packageName))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val store = GuestLocalUserStore.getInstance(this)
        val prefs = store.getCurrentPreferencesSync()
        if (!prefs.lockScreenOverlayEnabled || !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildStatusNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        warmLockAssetsInBackground()

        if (intent?.action == ACTION_PREVIEW_TEST) {
            launchLockScreenActivity(previewMode = true)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            runCatching { unregisterReceiver(screenReceiver) }
            receiverRegistered = false
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun registerScreenLifecycleReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
    }

    internal fun handleScreenWake() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        val prefs = GuestLocalUserStore.getInstance(this).getCurrentPreferencesSync()
        if (!prefs.lockScreenOverlayEnabled) {
            stopSelf()
            return
        }
        // Only activate when transitioning from a genuinely off display state to avoid mid-session popups
        if (!screenWasOff.getAndSet(false)) return

        val kgm = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
        val shouldPresent = kgm == null || kgm.isKeyguardLocked || !kgm.isKeyguardSecure
        if (!shouldPresent) return

        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastLaunchElapsedMs < 650L) return
        lastLaunchElapsedMs = now

        launchLockScreenActivity(previewMode = false)
    }

    private fun launchLockScreenActivity(previewMode: Boolean) {
        try {
            val noAnimOptions = ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle()
            startActivity(BackdropLockActivity.createLaunchIntent(this, previewMode = previewMode), noAnimOptions)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to present BackdropLockActivity", e)
        }
    }

    private fun ensureNotificationChannel() {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Backdrop Lock Screen Mode",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active while Backdrop custom lock screen wake monitor is enabled"
                setShowBadge(false)
            }
            mgr.createNotificationChannel(channel)
        }
    }

    private fun buildStatusNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            10,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            11,
            Intent(this, LockScreenOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_splash_logo)
            .setContentTitle("Backdrop Lock Screen Ready")
            .setContentText("Shows your custom clock & notifications on wake")
            .setContentIntent(openIntent)
            .addAction(0, "Turn Off", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val TAG = "BackdropOverlaySvc"
        private const val CHANNEL_ID = "backdrop_lockscreen_overlay"
        private const val NOTIFICATION_ID = 4102
        const val ACTION_START = "com.example.engine.overlay.ACTION_START"
        const val ACTION_STOP = "com.example.engine.overlay.ACTION_STOP"
        const val ACTION_PREVIEW_TEST = "com.example.engine.overlay.ACTION_PREVIEW_TEST"

        fun start(context: Context, action: String = ACTION_START) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context.applicationContext, LockScreenOverlayService::class.java).setAction(action)
            runCatching {
                ContextCompat.startForegroundService(context.applicationContext, intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context.applicationContext, LockScreenOverlayService::class.java).setAction(ACTION_STOP)
            runCatching {
                context.applicationContext.startService(intent)
            }
        }
    }
}

/**
 * Resolves the exact active lock-screen wallpaper + optional custom bitmap path without silently
 * snapping back to catalog index 0 when a custom edit or lock wallpaper has been set.
 */
internal data class ResolvedLockArtwork(
    val wallpaper: Wallpaper,
    val customBitmapPath: String,
    val customPhotoUri: String
)

internal fun resolveActiveLockArtwork(
    context: Context,
    preferences: UserPreferencesDoc,
    selectedSet: LockScreenSetDoc?
): ResolvedLockArtwork {
    val setWallpaper = selectedSet?.wallpaperId?.takeIf { it.isNotBlank() }?.let { WallpaperCatalog.getById(it) }
    val activeLockWp = preferences.activeLockWallpaperId.takeIf { it.isNotBlank() }?.let { WallpaperCatalog.getById(it) }
    val widgetWp = BackdropWidgetContract.getCurrentWallpaperId(context).let { WallpaperCatalog.getById(it) }
    val fallbackWp = WallpaperCatalog.allWallpapers.first()
    val resolvedWallpaper = setWallpaper ?: activeLockWp ?: widgetWp ?: fallbackWp

    val setPhotoUri = selectedSet?.customPhotoUri.orEmpty()
    val savedBitmapPath = preferences.activeLockCustomBitmapPath
        .takeIf { it.isNotBlank() && File(it).exists() }
        .orEmpty()

    return ResolvedLockArtwork(
        wallpaper = resolvedWallpaper,
        customBitmapPath = if (setPhotoUri.isBlank()) savedBitmapPath else "",
        customPhotoUri = setPhotoUri
    )
}

@Composable
internal fun LockScreenOverlaySurface(
    selectedSet: LockScreenSetDoc?,
    clockConfig: ClockDesignConfig,
    telemetry: ClockWidgetTelemetry,
    onDismissToUnlock: () -> Unit,
    preferences: UserPreferencesDoc = UserPreferencesDoc(),
    notificationGroups: List<LockNotificationGroup> = emptyList(),
    hideSensitiveNotifications: Boolean = false,
    isPreviewMode: Boolean = false,
    previewChargingOverlay: Boolean = false,
    onOpenNotification: (LockNotificationItem) -> Unit = {},
    onDismissNotification: (String) -> Unit = {},
    onClearAllNotifications: () -> Unit = {},
    onUpdateWidgetItem: ((StudioWidgetItem) -> Unit)? = null,
    onUpdateWidgetsList: ((List<StudioWidgetItem>) -> Unit)? = null,
    onCycleMediaSizeTier: ((MediaPlayerSizeTier) -> Unit)? = null,
    onExecuteShortcut: ((LockShortcutConfig) -> Unit)? = null
) {
    val context = LocalContext.current
    val resolved = remember(selectedSet, preferences.activeLockWallpaperId, preferences.activeLockCustomBitmapPath) {
        resolveActiveLockArtwork(context, preferences, selectedSet)
    }
    val lookFilter = remember(selectedSet?.lookFilterName) {
        runCatching {
            CanvasLookFilter.valueOf(selectedSet?.lookFilterName ?: CanvasLookFilter.ORIGINAL.name)
        }.getOrDefault(CanvasLookFilter.ORIGINAL)
    }
    val dimFraction = (selectedSet?.dimFraction ?: 0f).coerceIn(0f, 0.72f)
    val artworkKey = "${selectedSet?.id.orEmpty()}|${resolved.wallpaper.id}|${resolved.customBitmapPath}|${resolved.customPhotoUri}"
    val warmSnapshot = remember(artworkKey) {
        ZeroLagPerformanceEngine.InstantLockScreenEngine.warmState.value?.takeIf {
            it.artworkKey == artworkKey || (it.artworkKey.isBlank() && it.wallpaperId == resolved.wallpaper.id)
        }
    }
    val warmBackdropBmp = warmSnapshot?.wallpaperBitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
    val warmCutoutBmp = warmSnapshot?.cutoutBitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
    val hasCustomAsset = resolved.customBitmapPath.isNotBlank() || resolved.customPhotoUri.isNotBlank()

    val customBitmapState = produceState<Bitmap?>(
        initialValue = if (hasCustomAsset) warmBackdropBmp else null,
        key1 = resolved.customBitmapPath,
        key2 = resolved.customPhotoUri
    ) {
        if (!hasCustomAsset) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            when {
                resolved.customBitmapPath.isNotBlank() ->
                    runCatching { BitmapFactory.decodeFile(resolved.customBitmapPath) }.getOrNull()
                resolved.customPhotoUri.isNotBlank() ->
                    runCatching {
                        context.contentResolver.openInputStream(Uri.parse(resolved.customPhotoUri))?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }.getOrNull()
                else -> null
            }
        } ?: value
    }
    val resolvedBackdropBitmap by produceState<Bitmap?>(
        initialValue = customBitmapState.value ?: warmBackdropBmp,
        key1 = customBitmapState.value,
        key2 = resolved.wallpaper.id,
        key3 = resolved.wallpaper.fallbackResId
    ) {
        val custom = customBitmapState.value
        if (custom != null) {
            value = custom
            return@produceState
        }
        if (value == null) {
            val localRes = if (resolved.wallpaper.isBundledAsset && resolved.wallpaper.fallbackResId != 0) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        BitmapFactory.decodeResource(context.resources, resolved.wallpaper.fallbackResId)
                    }.getOrNull()
                }
            } else null
            if (localRes != null) {
                value = localRes
            }
            if (!resolved.wallpaper.isBundledAsset) {
                val remote = runCatching {
                    WallpaperMediaService(context.applicationContext).loadFullBitmap(resolved.wallpaper)
                }.getOrNull()
                if (remote != null) {
                    value = remote
                }
            }
        }
    }
    val resolvedCutoutBitmap by produceState<Bitmap?>(
        initialValue = warmCutoutBmp,
        key1 = resolvedBackdropBitmap,
        key2 = preferences.manualDepthEffectEnabled,
        key3 = clockConfig.depthOverlapEnabled
    ) {
        if (!preferences.manualDepthEffectEnabled || !clockConfig.depthOverlapEnabled) {
            value = null
            return@produceState
        }
        val bmp = resolvedBackdropBitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
            ?: return@produceState
        if (value == null) {
            val extracted = runCatching {
                DepthDetectionEngine().detectAndExtract(
                    sourceBitmap = bmp,
                    titleHint = resolved.wallpaper.title,
                    cacheKey = "${resolved.wallpaper.id}_${resolved.customBitmapPath.hashCode()}_${resolved.customPhotoUri.hashCode()}",
                    preferPortraitPrior = resolved.customPhotoUri.isNotBlank() ||
                        (resolved.wallpaper.width.toFloat() / resolved.wallpaper.height.coerceAtLeast(1)) < 0.8f,
                    context = context.applicationContext
                ).cutoutBitmap
            }.getOrNull()
            if (extracted != null) {
                value = extracted
            }
        }
        ZeroLagPerformanceEngine.InstantLockScreenEngine.updateWarmBitmaps(
            wallpaperId = resolved.wallpaper.id,
            wallpaperBitmap = bmp,
            cutoutBitmap = value,
            artworkKey = artworkKey,
            activeSetId = selectedSet?.id.orEmpty()
        )
    }

    LaunchedEffect(artworkKey, preferences.manualDepthEffectEnabled) {
        if (warmBackdropBmp == null) {
            ZeroLagPerformanceEngine.InstantLockScreenEngine.warmLockAssets(
                context = context.applicationContext,
                preferences = preferences,
                activeSet = selectedSet
            )
        }
    }

    val uiStyle = LocalUiStyleMode.current
    val reducedMotion = LocalReducedMotionFlag.current
    val motionSpeed = LocalThemeCustomization.current.m3MotionSpeed
    val secondaryColor = clockConfig.customSecondaryArgb?.let { Color(it.toInt()) }
        ?: resolved.wallpaper.dominantColors.firstOrNull()?.let { Color(it) }
        ?: MaterialTheme.colorScheme.primary

    var liveBatteryPercent by remember {
        mutableIntStateOf(
            if (telemetry.batteryPercent > 0) telemetry.batteryPercent
            else OpenMeteoWeatherRepository.readBatteryPercent(context)
        )
    }
    var isCharging by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (level >= 0 && scale > 0) {
                    liveBatteryPercent = ((level * 100f) / scale).toInt().coerceIn(0, 100)
                }
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        val sticky = runCatching {
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                ContextCompat.RECEIVER_EXPORTED
            )
        }.getOrNull()
        if (sticky != null) {
            val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (level >= 0 && scale > 0) {
                liveBatteryPercent = ((level * 100f) / scale).toInt().coerceIn(0, 100)
            }
            val status = sticky.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    val effectiveTelemetry = remember(telemetry, liveBatteryPercent) {
        telemetry.copy(batteryPercent = liveBatteryPercent)
    }

    var showCredentialGate by remember(preferences.hasLockCredential) {
        mutableStateOf(false)
    }

    BackHandler(enabled = showCredentialGate) {
        showCredentialGate = false
    }

    fun requestUnlockOrChallenge() {
        if (preferences.hasLockCredential) {
            showCredentialGate = true
        } else {
            onDismissToUnlock()
        }
    }

    var dragAccumulatedPx by remember { mutableFloatStateOf(0f) }
    val expandedKeys = remember { mutableStateMapOf<String, Boolean>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C12))
            .pointerInput(preferences.hasLockCredential, showCredentialGate) {
                if (!showCredentialGate) {
                    detectVerticalDragGestures(
                        onDragStart = { dragAccumulatedPx = 0f },
                        onDragEnd = {
                            if (dragAccumulatedPx < -160f) {
                                requestUnlockOrChallenge()
                            }
                            dragAccumulatedPx = 0f
                        },
                        onVerticalDrag = { _, dragAmount ->
                            dragAccumulatedPx += dragAmount
                        }
                    )
                }
            }
            .testTag("lockscreen_overlay_root")
    ) {
        val applySetTransform = selectedSet != null && resolved.customBitmapPath.isBlank()
        val lockZoom = if (applySetTransform) selectedSet!!.zoomScale.coerceIn(0.85f, 4.0f) else 1.0f
        val lockPan = if (applySetTransform) Offset(selectedSet!!.panOffsetX, selectedSet.panOffsetY) else Offset.Zero
        val lockRotation = if (applySetTransform) selectedSet!!.rotationDegrees else 0f

        SharedLockScreenRenderer(
            wallpaper = resolved.wallpaper,
            customBitmap = (customBitmapState.value ?: if (hasCustomAsset) resolvedBackdropBitmap else null)?.takeIf { !it.isRecycled },
            backdropBitmap = resolvedBackdropBitmap?.takeIf { !it.isRecycled },
            foregroundSubjectCutout = resolvedCutoutBitmap?.takeIf { !it.isRecycled },
            clockConfig = clockConfig,
            telemetry = effectiveTelemetry,
            preferences = preferences,
            zoomScale = lockZoom,
            panOffset = lockPan,
            rotationDegrees = lockRotation,
            dimFraction = dimFraction,
            lookFilter = lookFilter,
            notificationGroups = if (showCredentialGate) emptyList() else notificationGroups,
            expandedNotificationKeys = expandedKeys,
            hideSensitiveNotifications = hideSensitiveNotifications,
            isCharging = isCharging,
            previewChargingOverlay = previewChargingOverlay,
            showChromeControls = !showCredentialGate,
            isPreviewMode = isPreviewMode,
            useLiveTime = true,
            onUnlockClick = { requestUnlockOrChallenge() },
            onOpenNotification = onOpenNotification,
            onDismissNotification = onDismissNotification,
            onClearAllNotifications = onClearAllNotifications,
            onUpdateWidgetItem = onUpdateWidgetItem,
            onUpdateWidgetsList = onUpdateWidgetsList,
            onCycleMediaSizeTier = onCycleMediaSizeTier,
            onExecuteShortcut = onExecuteShortcut,
            motionMode = preferences.parsedWallpaperMotionMode
        )

        AnimatedVisibility(
            visible = showCredentialGate,
            enter = AppMotion.screenEnter(uiStyle, reducedMotion, motionSpeed),
            exit = AppMotion.screenExit(uiStyle, reducedMotion, motionSpeed)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.64f))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                LockScreenCredentialGate(
                    preferences = preferences,
                    onUnlockSuccess = {
                        showCredentialGate = false
                        onDismissToUnlock()
                    },
                    onCancelToClock = { showCredentialGate = false }
                )
            }
        }
    }
}
