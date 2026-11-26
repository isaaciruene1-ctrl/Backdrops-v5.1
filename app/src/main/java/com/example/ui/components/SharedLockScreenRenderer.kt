package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChargingAnimationConfig
import com.example.data.model.ChargingAnimationPreset
import com.example.data.model.ChargingPosition
import com.example.data.model.GlanceCardType
import com.example.data.model.LockShortcutAction
import com.example.data.model.LockShortcutConfig
import com.example.data.model.MediaPlayerSizeTier
import com.example.data.model.SecondaryWidgetPagePosition
import com.example.data.model.StudioWidgetItem
import com.example.data.model.StudioWidgetType
import com.example.data.model.UiStyleMode
import com.example.data.model.UnlockButtonStyle
import com.example.data.model.UnlockButtonThemeConfig
import com.example.data.model.UserPreferencesDoc
import com.example.data.model.Wallpaper
import com.example.data.model.WallpaperMotionMode
import com.example.engine.notifications.LockNotificationGroup
import com.example.engine.notifications.LockNotificationItem
import com.example.engine.overlay.LockShortcutExecutor
import com.example.engine.performance.ZeroLagPerformanceEngine
import com.example.ui.screens.CanvasLookFilter
import com.example.ui.theme.AppMotion
import com.example.ui.theme.DotGothicFontFamily
import com.example.ui.theme.GlyphRedDot
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalReducedMotionFlag
import com.example.ui.theme.LocalThemeCustomization
import com.example.ui.theme.LocalUiStyleMode
import com.example.ui.theme.rememberStudioHaptics
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Unified Lock Screen Canvas Renderer shared identically by:
 * - Live Lock Screen Mode (`BackdropLockActivity` & `LockScreenOverlaySurface`)
 * - Lock Screen Mode interactive preview (`LockScreenModeScreen`)
 * - Preview Studio's Lock Screen tab (`PreviewStudioScreen`)
 *
 * Guarantees exact 1:1 visual parity across clock placement (`placementY`, `offsetXFraction`),
 * color customization, Glow & Liquid Glass materials, iOS-inspired stacked notifications,
 * charging animations, resizable Lock Screen widgets, full-bleed enlarged media player, and unlock bar.
 */
@Composable
fun SharedLockScreenRenderer(
    wallpaper: Wallpaper,
    customBitmap: Bitmap?,
    backdropBitmap: Bitmap?,
    foregroundSubjectCutout: Bitmap? = null,
    clockConfig: ClockDesignConfig,
    telemetry: ClockWidgetTelemetry,
    preferences: UserPreferencesDoc = UserPreferencesDoc(),
    zoomScale: Float = 1.0f,
    panOffset: Offset = Offset.Zero,
    rotationDegrees: Float = 0f,
    dimFraction: Float = 0f,
    lookFilter: CanvasLookFilter = CanvasLookFilter.ORIGINAL,
    notificationGroups: List<LockNotificationGroup> = emptyList(),
    expandedNotificationKeys: MutableMap<String, Boolean> = remember { mutableMapOf() },
    hideSensitiveNotifications: Boolean = false,
    isCharging: Boolean = false,
    previewChargingOverlay: Boolean = false,
    showChromeControls: Boolean = true,
    isPreviewMode: Boolean = false,
    useLiveTime: Boolean = true,
    onUnlockClick: () -> Unit = {},
    onOpenNotification: (LockNotificationItem) -> Unit = {},
    onDismissNotification: (String) -> Unit = {},
    onClearAllNotifications: () -> Unit = {},
    onUpdateWidgetItem: ((StudioWidgetItem) -> Unit)? = null,
    onUpdateWidgetsList: ((List<StudioWidgetItem>) -> Unit)? = null,
    onCycleMediaSizeTier: ((MediaPlayerSizeTier) -> Unit)? = null,
    onExecuteShortcut: ((LockShortcutConfig) -> Unit)? = null,
    motionMode: WallpaperMotionMode = preferences.parsedWallpaperMotionMode,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val uiStyle = LocalUiStyleMode.current
    val reducedMotion = LocalReducedMotionFlag.current
    val motionSpeed = LocalThemeCustomization.current.m3MotionSpeed
    val haptics = rememberStudioHaptics()
    val isGlass = uiStyle == UiStyleMode.FROSTED_GLASS_OS
    val isGlyph = uiStyle == UiStyleMode.GLYPH_UI

    val primaryColor = clockConfig.customPrimaryArgb?.let { Color(it.toInt()) } ?: Color(0xFFF6F8FC)
    val secondaryColor = clockConfig.customSecondaryArgb?.let { Color(it.toInt()) }
        ?: wallpaper.dominantColors.firstOrNull()?.let { Color(it) }
        ?: MaterialTheme.colorScheme.primary

    val notificationConfig = remember(preferences.notificationStyleJson) {
        preferences.parsedNotificationStyle
    }
    val chargingConfig = remember(preferences.chargingAnimationJson) {
        preferences.parsedChargingAnimation
    }
    val unlockTheme = remember(preferences.unlockButtonThemeJson) {
        preferences.parsedUnlockButtonTheme
    }
    val studioWidgets = remember(preferences.studioWidgetsJson) {
        preferences.parsedStudioWidgets
    }
    val widgetPageWidgets = remember(preferences.widgetPageWidgetsJson) {
        preferences.parsedWidgetPageWidgets
    }
    val latestWidgets by rememberUpdatedState(studioWidgets)
    val latestOnUpdateWidgetItem by rememberUpdatedState(onUpdateWidgetItem)
    val latestOnUpdateWidgetsList by rememberUpdatedState(onUpdateWidgetsList)

    fun commitWidgetsList(updatedList: List<StudioWidgetItem>) {
        val listCb = latestOnUpdateWidgetsList
        if (listCb != null) {
            listCb(updatedList)
        } else {
            val singleCb = latestOnUpdateWidgetItem ?: return
            val prevByType = latestWidgets.associateBy { it.type }
            updatedList.forEach { item ->
                if (prevByType[item.type] != item) {
                    singleCb(item)
                }
            }
        }
    }

    val maxLockWidgets = preferences.safeLockScreenMaxWidgets
    val maxPageWidgets = preferences.safeWidgetPageMaxWidgets
    val widgetsEffective = preferences.areMainLockWidgetsEffective
    val visibleLockWidgets = remember(studioWidgets, maxLockWidgets, widgetsEffective) {
        if (!widgetsEffective) emptyList()
        else studioWidgets.filter { it.enabled }.sortedBy { it.sortOrder }.take(maxLockWidgets)
    }
    val stripWidgets = remember(visibleLockWidgets) {
        visibleLockWidgets.filter { !it.isFreePositioned }
    }
    val freeWidgets = remember(visibleLockWidgets) {
        visibleLockWidgets.filter { it.isFreePositioned }
    }
    val canEditWidgets = onUpdateWidgetItem != null || onUpdateWidgetsList != null

    var clockUnscaledSize by remember { mutableStateOf(IntSize(420, 180)) }
    var isWidgetResizeMode by remember { mutableStateOf(false) }
    var expandedMediaState by remember { mutableStateOf<ActiveMediaPlaybackState?>(null) }
    var notifDragExpansion by remember(notificationGroups.isEmpty()) { mutableFloatStateOf(0f) }

    val effectiveClockConfig = remember(
        clockConfig,
        notifDragExpansion,
        preferences.dragToExpandNotifications,
        notificationGroups.isNotEmpty()
    ) {
        if (preferences.dragToExpandNotifications && notificationGroups.isNotEmpty() && notifDragExpansion > 0.01f) {
            ClockSpatialMath.computeNotificationHeadroomMorph(clockConfig, notifDragExpansion)
        } else {
            clockConfig
        }
    }

    val needMediaTelemetry = preferences.glancePillEnabled || preferences.isStandaloneMediaPlayerActive
    var samsungPillMedia by remember {
        mutableStateOf(
            if (needMediaTelemetry) StudioWidgetSystemBridge.readActiveMediaState(context)
            else ActiveMediaPlaybackState()
        )
    }
    LaunchedEffect(needMediaTelemetry) {
        if (!needMediaTelemetry) {
            samsungPillMedia = ActiveMediaPlaybackState()
        } else {
            while (true) {
                samsungPillMedia = StudioWidgetSystemBridge.readActiveMediaState(context)
                delay(1000L)
            }
        }
    }

    var localMediaTier by remember(preferences.mediaPlayerSizeTier) {
        mutableStateOf(preferences.parsedMediaPlayerSizeTier)
    }
    var torchActive by remember { mutableStateOf(LockShortcutExecutor.isTorchOn()) }
    DisposableEffect(context) {
        val unregister = LockShortcutExecutor.registerTorchObserver(context) { enabled ->
            torchActive = enabled
        }
        onDispose { unregister() }
    }

    val effectiveMotionMode = remember(motionMode, reducedMotion) {
        com.example.engine.performance.WallpaperMotionEligibility.resolveEffectiveMode(
            context = context,
            requestedMode = motionMode,
            reducedMotionEnabled = reducedMotion
        )
    }
    var parallaxOffsetX by remember { mutableFloatStateOf(0f) }
    var parallaxOffsetY by remember { mutableFloatStateOf(0f) }
    DisposableEffect(effectiveMotionMode, context) {
        if (effectiveMotionMode != WallpaperMotionMode.SUBTLE_PARALLAX) {
            parallaxOffsetX = 0f
            parallaxOffsetY = 0f
            onDispose { }
        } else {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val rotSensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            val listener = object : SensorEventListener {
                private val rotMat = FloatArray(9)
                private val ori = FloatArray(3)
                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rotMat, event.values)
                    SensorManager.getOrientation(rotMat, ori)
                    val roll = ori[2].coerceIn(-0.45f, 0.45f)
                    val pitch = (ori[1] + 0.55f).coerceIn(-0.45f, 0.45f)
                    parallaxOffsetX = (roll / 0.45f) * 18f
                    parallaxOffsetY = (pitch / 0.45f) * 14f
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            if (sm != null && rotSensor != null) {
                sm.registerListener(listener, rotSensor, SensorManager.SENSOR_DELAY_UI)
            }
            onDispose { sm?.unregisterListener(listener) }
        }
    }
    val driftPhase = if (effectiveMotionMode == WallpaperMotionMode.AMBIENT_DRIFT) {
        val driftTrans = rememberInfiniteTransition(label = "wallpaper_ambient_drift")
        val p by driftTrans.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
            label = "ambient_drift_phase"
        )
        p
    } else {
        0f
    }

    LaunchedEffect(expandedMediaState != null) {
        while (expandedMediaState != null) {
            delay(1000L)
            val refreshed = StudioWidgetSystemBridge.readActiveMediaState(context)
            if (refreshed.hasSession) {
                expandedMediaState = refreshed
            }
        }
    }

    BackHandler(enabled = expandedMediaState != null || isWidgetResizeMode) {
        if (expandedMediaState != null) {
            expandedMediaState = null
        } else if (isWidgetResizeMode) {
            isWidgetResizeMode = false
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportMaxHeight = maxHeight
        val viewportPx = IntSize(
            width = with(density) { maxWidth.toPx().roundToInt().coerceAtLeast(1) },
            height = with(density) { viewportMaxHeight.toPx().roundToInt().coerceAtLeast(1) }
        )
        val marginPx = with(density) { 10.dp.toPx() }
        val resolvedClockBox = remember(clockUnscaledSize, viewportPx, effectiveClockConfig, marginPx) {
            ClockSpatialMath.computeClockBoxRect(
                unscaledWidthPx = clockUnscaledSize.width.toFloat(),
                unscaledHeightPx = clockUnscaledSize.height.toFloat(),
                viewportSize = viewportPx,
                config = effectiveClockConfig,
                minMarginPx = marginPx
            )
        }

        // 1. Background Wallpaper + Transform + Look Filter
        val validCustomBmp = customBitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
        val validPrimaryBmp = validCustomBmp ?: backdropBitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
        val motionOverscan = when (effectiveMotionMode) {
            WallpaperMotionMode.SUBTLE_PARALLAX -> 1.06f
            WallpaperMotionMode.AMBIENT_DRIFT -> 1.04f
            WallpaperMotionMode.STATIC -> 1.0f
        }
        val motionShiftX = when (effectiveMotionMode) {
            WallpaperMotionMode.SUBTLE_PARALLAX -> parallaxOffsetX
            WallpaperMotionMode.AMBIENT_DRIFT -> cos(driftPhase) * 10f
            WallpaperMotionMode.STATIC -> 0f
        }
        val motionShiftY = when (effectiveMotionMode) {
            WallpaperMotionMode.SUBTLE_PARALLAX -> parallaxOffsetY
            WallpaperMotionMode.AMBIENT_DRIFT -> sin(driftPhase) * 7f
            WallpaperMotionMode.STATIC -> 0f
        }
        val artworkModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val baseS = if (customBitmap == null) zoomScale.coerceIn(0.85f, 4.0f) else 1f
                val s = baseS * motionOverscan
                scaleX = s
                scaleY = s
                translationX = (if (customBitmap == null) panOffset.x else 0f) + motionShiftX
                translationY = (if (customBitmap == null) panOffset.y else 0f) + motionShiftY
                if (customBitmap == null) {
                    rotationZ = rotationDegrees
                }
            }

        if (validPrimaryBmp != null) {
            val primaryImg = remember(validPrimaryBmp) { validPrimaryBmp.asImageBitmap() }
            Image(
                bitmap = primaryImg,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = lookFilter.toComposeColorFilter(),
                modifier = artworkModifier
            )
        } else {
            WallpaperAsyncImage(
                wallpaper = wallpaper,
                useFullRes = true,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = lookFilter.toComposeColorFilter(),
                modifier = artworkModifier
            )
        }

        // Subtle vignette & legibility dim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = (0.24f + dimFraction * 0.5f).coerceAtMost(0.88f)),
                            Color.Black.copy(alpha = dimFraction.coerceAtMost(0.75f)),
                            Color.Black.copy(alpha = (0.48f + dimFraction * 0.45f).coerceAtMost(0.92f))
                        )
                    )
                )
        )

        val validCutout = foregroundSubjectCutout?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
        val hasEnabledWidgets = visibleLockWidgets.isNotEmpty() && effectiveClockConfig.widgetsEnabled
        val hasStripWidgets = stripWidgets.isNotEmpty() && effectiveClockConfig.widgetsEnabled
        val showChargingNow = (chargingConfig.enabled && chargingConfig.preset != ChargingAnimationPreset.OFF && (isCharging || previewChargingOverlay)) ||
            (preferences.pixelChargingWaveEnabled && (isCharging || previewChargingOverlay))
        val showUnlockBar = showChromeControls && preferences.showUnlockButtonOnLockScreen && expandedMediaState == null
        val showGlancePill = showChromeControls && preferences.glancePillEnabled && expandedMediaState == null
        val showStandaloneMediaPlayer = showChromeControls && preferences.isStandaloneMediaPlayerActive && expandedMediaState == null
        val leftShortcut = remember(preferences.leftShortcutJson) { preferences.parsedLeftShortcut }
        val rightShortcut = remember(preferences.rightShortcutJson) { preferences.parsedRightShortcut }
        val hasCornerShortcuts = showChromeControls && expandedMediaState == null &&
            (leftShortcut.action != LockShortcutAction.NONE || rightShortcut.action != LockShortcutAction.NONE)

        @Composable
        fun PrimaryLockCanvasPage() {
            var draggingWidget by remember { mutableStateOf<StudioWidgetItem?>(null) }
            var dragPointerRoot by remember { mutableStateOf(Offset.Zero) }
            var canvasBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
            var stripBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
            var dockBandBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
            val slotBoundsMap = remember { mutableStateMapOf<Int, Rect>() }

            val activeDragged = draggingWidget
            val previewInsideStrip = activeDragged != null && LockWidgetDragSpatialSolver.isInsideStripZone(
                pointerRoot = dragPointerRoot,
                stripBoundsInRoot = stripBoundsInRoot,
                dockBandBoundsInRoot = dockBandBoundsInRoot
            )
            val previewInsertIdx = if (previewInsideStrip) {
                LockWidgetDragSpatialSolver.computeStripSlotIndex(
                    pointerX = dragPointerRoot.x,
                    slotBoundsInRoot = slotBoundsMap,
                    remainingCount = stripWidgets.count { it.type != activeDragged?.type }
                )
            } else {
                null
            }

            fun beginWidgetDrag(item: StudioWidgetItem, startRootOffset: Offset) {
                haptics.performConfirm()
                draggingWidget = item
                dragPointerRoot = startRootOffset
            }

            fun updateWidgetDrag(pointerRoot: Offset) {
                dragPointerRoot = pointerRoot
            }

            fun finishWidgetDrag() {
                val target = draggingWidget
                val canvasRect = canvasBoundsInRoot
                if (target != null && canvasRect != null) {
                    val nextList = LockWidgetDragSpatialSolver.applyDragDrop(
                        allWidgets = latestWidgets,
                        draggedType = target.type,
                        pointerRoot = dragPointerRoot,
                        canvasBoundsInRoot = canvasRect,
                        stripBoundsInRoot = stripBoundsInRoot,
                        dockBandBoundsInRoot = dockBandBoundsInRoot,
                        slotBoundsInRoot = slotBoundsMap
                    )
                    haptics.performSnapDetent()
                    commitWidgetsList(nextList)
                }
                draggingWidget = null
            }

            fun cancelWidgetDrag() {
                draggingWidget = null
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        canvasBoundsInRoot = coords.boundsInRoot()
                    }
            ) {
                // Clock positioned accurately via ClockSpatialMath.computeClockBoxRect (with live headroom morph)
                val clockLeftPx = resolvedClockBox.leftPx.roundToInt()
                val clockTopPx = resolvedClockBox.topPx.roundToInt()

                if (preferences.inlineTopWidgetEnabled) {
                    val barY = (clockTopPx - with(density) { 26.dp.toPx() }.roundToInt()).coerceAtLeast(with(density) { 12.dp.toPx() }.roundToInt())
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(x = 0, y = barY) },
                        contentAlignment = Alignment.Center
                    ) {
                        StudioInlineTopWidgetBar(
                            widgetType = preferences.parsedInlineTopWidgetType ?: StudioWidgetType.QUICK_INFO,
                            telemetry = telemetry,
                            isCharging = isCharging,
                            is24HourFormat = effectiveClockConfig.is24HourFormat,
                            onDarkSurface = true
                        )
                    }
                }

                Box(
                    modifier = Modifier.offset { IntOffset(x = clockLeftPx, y = clockTopPx) }
                ) {
                    HyperOsLockClock(
                        face = effectiveClockConfig.face,
                        font = effectiveClockConfig.font,
                        finish = effectiveClockConfig.finish,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        weightLevel = effectiveClockConfig.weightLevel,
                        scaleFactor = effectiveClockConfig.scale,
                        widthScale = effectiveClockConfig.widthScale,
                        heightScale = effectiveClockConfig.heightScale,
                        opacity = effectiveClockConfig.opacity,
                        alignment = effectiveClockConfig.alignment,
                        useLiveTime = useLiveTime,
                        is24HourFormat = effectiveClockConfig.is24HourFormat,
                        showDateAndWeather = effectiveClockConfig.showDateAndWeather,
                        widgetsEnabled = effectiveClockConfig.widgetsEnabled,
                        showDateWidget = effectiveClockConfig.showDateWidget,
                        showWeatherWidget = effectiveClockConfig.showWeatherWidget,
                        showBatteryWidget = effectiveClockConfig.showBatteryWidget,
                        showDecorativeWidget = effectiveClockConfig.showDecorativeWidget,
                        telemetry = telemetry,
                        signatureMode = effectiveClockConfig.signatureMode,
                        signatureText = effectiveClockConfig.signatureText,
                        liquidGlassConfig = effectiveClockConfig.liquidGlass,
                        backdropBitmap = backdropBitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 },
                        viewportSize = viewportPx,
                        clockBoxRect = resolvedClockBox,
                        zoomScale = if (validCustomBmp == null) zoomScale else 1f,
                        panOffset = if (validCustomBmp == null) panOffset else Offset.Zero,
                        dimFraction = dimFraction,
                        lookFilter = lookFilter,
                        rotationDegrees = if (validCustomBmp == null) rotationDegrees else 0f,
                        scaleTextWithClock = effectiveClockConfig.scaleTextWithClock,
                        freeStretchEnabled = effectiveClockConfig.freeStretchEnabled,
                        glowConfig = effectiveClockConfig.glowConfig,
                        onMeasuredUnscaledSize = { measured ->
                            if (measured.width > 0 && measured.height > 0) {
                                clockUnscaledSize = measured
                            }
                        }
                    )
                }

                if (effectiveClockConfig.depthOverlapEnabled && validCutout != null) {
                    val cutoutImg = remember(validCutout) { validCutout.asImageBitmap() }
                    Image(
                        bitmap = cutoutImg,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = artworkModifier
                    )
                }

                // Top Lock Protection Status + Direct Corner Widget Resize Toggle
                if (showChromeControls) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (preferences.hasLockCredential) {
                            Row(
                                modifier = Modifier.testTag("lockscreen_status_bar"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Backdrop Lock Protected",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = preferences.parsedLockCredentialType.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.82f)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (hasEnabledWidgets && canEditWidgets) {
                                Surface(
                                    onClick = {
                                        haptics.performTick(22L)
                                        isWidgetResizeMode = !isWidgetResizeMode
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = if (isWidgetResizeMode) {
                                        secondaryColor.copy(alpha = 0.88f)
                                    } else {
                                        Color.Black.copy(alpha = 0.44f)
                                    },
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = if (isWidgetResizeMode) 0.72f else 0.28f)
                                    ),
                                    modifier = Modifier.testTag("lock_resize_widgets_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.OpenWith,
                                            contentDescription = "Resize Widgets",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isWidgetResizeMode) "Done Resizing" else "Resize Widgets",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            if (isPreviewMode && !showUnlockBar) {
                                Surface(
                                    onClick = onUnlockClick,
                                    shape = RoundedCornerShape(50),
                                    color = Color.Black.copy(alpha = 0.56f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f)),
                                    modifier = Modifier.testTag("lockscreen_exit_preview_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LockOpen,
                                            contentDescription = "Exit Preview",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Exit",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Compute clean non-overlapping bottom & top reserves (reflows automatically when Unlock Button is OFF)
                val clockBottomDp = with(density) { resolvedClockBox.bottomPx.toDp() }
                val maxTopForStrip = (viewportMaxHeight - 170.dp).coerceAtLeast(110.dp)
                val topClearanceDp = maxOf(clockBottomDp + 10.dp, viewportMaxHeight * 0.16f).coerceAtMost(maxTopForStrip)
                val widgetReserveDp = if (hasStripWidgets) 126f else 0f
                val unlockReserveDp = when {
                    showGlancePill && showUnlockBar -> 124f
                    showGlancePill || hasCornerShortcuts -> 74f
                    showUnlockBar -> 88f
                    else -> 12f
                }
                val mediaPillReserveDp = if (showStandaloneMediaPlayer) (localMediaTier.heightDp + 12f) else 0f
                val chargingReserveDp = if (showChargingNow && (chargingConfig.position == ChargingPosition.BOTTOM || preferences.pixelChargingWaveEnabled)) 62f else 0f
                val bottomReserveDp = unlockReserveDp + mediaPillReserveDp + chargingReserveDp + 14f

                val notifZone = LockScreenNotificationLayoutSolver.solveAvailableZone(
                    viewportHeightDp = viewportMaxHeight.value,
                    clockBottomFraction = (topClearanceDp / viewportMaxHeight.coerceAtLeast(1.dp)).coerceIn(0.14f, 0.85f),
                    activeWidgetHeightDp = widgetReserveDp,
                    bottomReserveDp = bottomReserveDp,
                    styleConfig = notificationConfig
                )

                // Symmetrically centered Widget Strip below the clock (plus automatic re-dock target during drag)
                if (effectiveClockConfig.widgetsEnabled && (hasStripWidgets || activeDragged != null)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = topClearanceDp, start = 16.dp, end = 16.dp)
                            .onGloballyPositioned { coords ->
                                dockBandBoundsInRoot = coords.boundsInRoot()
                            }
                    ) {
                        StudioWidgetsLockStrip(
                            widgets = visibleLockWidgets,
                            telemetry = telemetry,
                            isCharging = isCharging,
                            is24HourFormat = effectiveClockConfig.is24HourFormat,
                            isResizeMode = isWidgetResizeMode && canEditWidgets,
                            maxWidgets = maxLockWidgets,
                            draggingWidgetType = activeDragged?.type,
                            previewInsertIndex = previewInsertIdx,
                            onStripBoundsChanged = { rect -> stripBoundsInRoot = rect },
                            onSlotBoundsChanged = { idx, rect -> slotBoundsMap[idx] = rect },
                            onStartLongPressDrag = if (canEditWidgets) ::beginWidgetDrag else null,
                            onDragMove = if (canEditWidgets) ::updateWidgetDrag else null,
                            onDragEnd = if (canEditWidgets) ::finishWidgetDrag else null,
                            onDragCancel = if (canEditWidgets) ::cancelWidgetDrag else null,
                            onUpdateWidgetItem = onUpdateWidgetItem,
                            onExpandMediaPlayer = { media ->
                                isWidgetResizeMode = false
                                expandedMediaState = media
                            }
                        )
                    }
                }

                // Bottom-anchored Drag-to-Expand Notifications (or top-below-widgets if drag-to-expand OFF)
                if (notificationGroups.isNotEmpty()) {
                    val notifContainerModifier = if (preferences.dragToExpandNotifications) {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = (unlockReserveDp + mediaPillReserveDp + chargingReserveDp).dp
                            )
                            .heightIn(max = notifZone.maxListHeightDp)
                    } else {
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(
                                top = topClearanceDp + (if (hasStripWidgets) 128.dp else 4.dp),
                                start = 16.dp,
                                end = 16.dp
                            )
                            .heightIn(max = notifZone.maxListHeightDp)
                    }

                    LockScreenNotificationsSection(
                        groups = notificationGroups,
                        styleConfig = notificationConfig,
                        expandedKeys = expandedNotificationKeys,
                        hideSensitive = hideSensitiveNotifications,
                        maxVisibleOverride = notifZone.effectiveMaxGroups,
                        forceCompact = notifZone.forceCompact,
                        accentColor = secondaryColor,
                        dragToExpandEnabled = preferences.dragToExpandNotifications,
                        onDragExpansionChange = { frac -> notifDragExpansion = frac },
                        onOpenItem = onOpenNotification,
                        onDismissItem = onDismissNotification,
                        onClearAll = onClearAllNotifications,
                        modifier = notifContainerModifier
                    )
                }

                // Animated Charging Overlays: Bottom port charging wave + custom preset
                if (preferences.pixelChargingWaveEnabled && (isCharging || previewChargingOverlay) && expandedMediaState == null) {
                    BottomPortChargingWave(
                        batteryPercent = telemetry.batteryPercent,
                        dynamicAccent = secondaryColor,
                        speedMultiplier = chargingConfig.speedMultiplier,
                        showPercentage = chargingConfig.showPercentage,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    )
                }

                AnimatedVisibility(
                    visible = chargingConfig.enabled &&
                        chargingConfig.preset != ChargingAnimationPreset.OFF &&
                        (isCharging || previewChargingOverlay) &&
                        expandedMediaState == null &&
                        !(preferences.pixelChargingWaveEnabled && chargingConfig.position == ChargingPosition.BOTTOM),
                    enter = AppMotion.lockChargingOverlayEnter(uiStyle, reducedMotion, motionSpeed),
                    exit = AppMotion.lockChargingOverlayExit(uiStyle, reducedMotion, motionSpeed),
                    modifier = Modifier
                        .align(if (chargingConfig.position == ChargingPosition.CENTER) Alignment.Center else Alignment.BottomCenter)
                        .then(
                            if (chargingConfig.position == ChargingPosition.BOTTOM) {
                                Modifier.navigationBarsPadding().padding(bottom = (unlockReserveDp + mediaPillReserveDp + 8f).dp)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    LockScreenChargingAnimationOverlay(
                        config = chargingConfig,
                        batteryPercent = telemetry.batteryPercent,
                        dynamicAccent = secondaryColor
                    )
                }

                // Bottom Chrome Stack: Resizable Compact Media Player, Optional Unlock Button, and Bottom-Center Glance Pill + L/R Shortcuts
                if ((showStandaloneMediaPlayer || showGlancePill || showUnlockBar || hasCornerShortcuts) && expandedMediaState == null) {
                    val dispatchShortcut: (LockShortcutConfig) -> Unit = { cfg ->
                        haptics.performTick(20L)
                        if (onExecuteShortcut != null) {
                            onExecuteShortcut(cfg)
                        } else {
                            LockShortcutExecutor.executeShortcut(context, cfg)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(
                                start = 18.dp,
                                end = 18.dp,
                                bottom = if (preferences.pixelChargingWaveEnabled && (isCharging || previewChargingOverlay)) 32.dp else 14.dp
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (showStandaloneMediaPlayer) {
                            ResizableLockMediaPlayer(
                                mediaState = samsungPillMedia,
                                sizeTier = localMediaTier,
                                accentColor = secondaryColor,
                                uiStyle = uiStyle,
                                onCycleSizeTier = {
                                    haptics.performTick(16L)
                                    val tiers = MediaPlayerSizeTier.entries
                                    val next = tiers[(localMediaTier.ordinal + 1) % tiers.size]
                                    localMediaTier = next
                                    onCycleMediaSizeTier?.invoke(next)
                                },
                                onPreviousTrack = {
                                    haptics.performTick(16L)
                                    samsungPillMedia.controller?.transportControls?.skipToPrevious()
                                    val refreshed = StudioWidgetSystemBridge.readActiveMediaState(context)
                                    if (refreshed.hasSession) samsungPillMedia = refreshed
                                },
                                onTogglePlayPause = {
                                    haptics.performTick(18L)
                                    val tc = samsungPillMedia.controller?.transportControls
                                    if (samsungPillMedia.isPlaying) tc?.pause() else tc?.play()
                                    val refreshed = StudioWidgetSystemBridge.readActiveMediaState(context)
                                    samsungPillMedia = if (refreshed.hasSession) refreshed
                                    else samsungPillMedia.copy(isPlaying = !samsungPillMedia.isPlaying)
                                },
                                onNextTrack = {
                                    haptics.performTick(16L)
                                    samsungPillMedia.controller?.transportControls?.skipToNext()
                                    val refreshed = StudioWidgetSystemBridge.readActiveMediaState(context)
                                    if (refreshed.hasSession) samsungPillMedia = refreshed
                                },
                                onOpenExpanded = {
                                    haptics.performTick(18L)
                                    expandedMediaState = samsungPillMedia
                                }
                            )
                        }

                        if (showUnlockBar && (showGlancePill || !hasCornerShortcuts)) {
                            ThemedUnlockButton(
                                themeConfig = unlockTheme,
                                dynamicAccent = secondaryColor,
                                isGlassMode = isGlass,
                                isGlyphMode = isGlyph,
                                label = when {
                                    isPreviewMode -> "Exit Preview"
                                    preferences.hasLockCredential -> "Unlock with ${preferences.parsedLockCredentialType.label}"
                                    else -> "Unlock Device"
                                },
                                onClick = onUnlockClick
                            )
                        }

                        if (showGlancePill || hasCornerShortcuts) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (leftShortcut.action != LockShortcutAction.NONE) {
                                    LockCornerShortcutButton(
                                        config = leftShortcut,
                                        isTorchOn = torchActive && leftShortcut.action == LockShortcutAction.FLASHLIGHT,
                                        isGlyph = isGlyph,
                                        onClick = { dispatchShortcut(leftShortcut) },
                                        testTagName = "lockscreen_shortcut_left"
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(48.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        showGlancePill -> {
                                            BackdropGlancePill(
                                                mediaState = samsungPillMedia,
                                                telemetry = telemetry,
                                                isCharging = isCharging,
                                                accentColor = secondaryColor,
                                                uiStyle = uiStyle,
                                                onTogglePlayPause = {
                                                    haptics.performTick(18L)
                                                    val tc = samsungPillMedia.controller?.transportControls
                                                    if (samsungPillMedia.isPlaying) tc?.pause() else tc?.play()
                                                    val refreshed = StudioWidgetSystemBridge.readActiveMediaState(context)
                                                    samsungPillMedia = if (refreshed.hasSession) refreshed
                                                    else samsungPillMedia.copy(isPlaying = !samsungPillMedia.isPlaying)
                                                },
                                                onNextTrack = {
                                                    haptics.performTick(16L)
                                                    samsungPillMedia.controller?.transportControls?.skipToNext()
                                                    val refreshed = StudioWidgetSystemBridge.readActiveMediaState(context)
                                                    if (refreshed.hasSession) samsungPillMedia = refreshed
                                                },
                                                onOpenExpandedMedia = {
                                                    haptics.performTick(18L)
                                                    expandedMediaState = samsungPillMedia
                                                }
                                            )
                                        }
                                        showUnlockBar -> {
                                            ThemedUnlockButton(
                                                themeConfig = unlockTheme,
                                                dynamicAccent = secondaryColor,
                                                isGlassMode = isGlass,
                                                isGlyphMode = isGlyph,
                                                label = when {
                                                    isPreviewMode -> "Exit Preview"
                                                    preferences.hasLockCredential -> "Unlock"
                                                    else -> "Unlock"
                                                },
                                                onClick = onUnlockClick
                                            )
                                        }
                                    }
                                }

                                if (rightShortcut.action != LockShortcutAction.NONE) {
                                    LockCornerShortcutButton(
                                        config = rightShortcut,
                                        isTorchOn = torchActive && rightShortcut.action == LockShortcutAction.FLASHLIGHT,
                                        isGlyph = isGlyph,
                                        onClick = { dispatchShortcut(rightShortcut) },
                                        testTagName = "lockscreen_shortcut_right"
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(48.dp))
                                }
                            }
                        }
                    }
                }

                // Free-positioned Lock Screen widgets (tap, hold & drag outside the strip to place anywhere)
                if (effectiveClockConfig.widgetsEnabled) {
                    freeWidgets.forEach { freeItem ->
                        key("free_${freeItem.type.name}") {
                            var cardSize by remember { mutableStateOf(IntSize(220, 140)) }
                            var freeCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                            val isDragged = activeDragged?.type == freeItem.type
                            val targetOffset = remember(freeItem.freeXFraction, freeItem.freeYFraction, viewportPx, cardSize) {
                                val cx = (freeItem.freeXFraction * viewportPx.width).roundToInt() - (cardSize.width / 2)
                                val cy = (freeItem.freeYFraction * viewportPx.height).roundToInt() - (cardSize.height / 2)
                                IntOffset(
                                    x = cx.coerceIn(12, (viewportPx.width - cardSize.width - 12).coerceAtLeast(12)),
                                    y = cy.coerceIn(36, (viewportPx.height - cardSize.height - 36).coerceAtLeast(36))
                                )
                            }
                            val animatedOffset by animateIntOffsetAsState(
                                targetValue = targetOffset,
                                animationSpec = AppMotion.lockWidgetSnapSpring(uiStyle, reducedMotion, motionSpeed),
                                label = "free_widget_pos_${freeItem.type.name}"
                            )
                            val freeAlpha by animateFloatAsState(
                                targetValue = if (isDragged) 0.22f else 1f,
                                animationSpec = AppMotion.lockWidgetSnapSpring(uiStyle, reducedMotion, motionSpeed),
                                label = "free_widget_alpha_${freeItem.type.name}"
                            )
                            val freeDragMod = if (!isWidgetResizeMode && canEditWidgets) {
                                Modifier.pointerInput(freeItem.type, isWidgetResizeMode) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { startOffset ->
                                            freeCoords?.let { coords ->
                                                beginWidgetDrag(freeItem, coords.localToRoot(startOffset))
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            freeCoords?.let { coords ->
                                                updateWidgetDrag(coords.localToRoot(change.position))
                                            }
                                        },
                                        onDragEnd = { finishWidgetDrag() },
                                        onDragCancel = { cancelWidgetDrag() }
                                    )
                                }
                            } else {
                                Modifier
                            }

                            Box(
                                modifier = Modifier
                                    .offset { animatedOffset }
                                    .onSizeChanged { cardSize = it }
                                    .onGloballyPositioned { freeCoords = it }
                                    .then(freeDragMod)
                                    .graphicsLayer { alpha = freeAlpha }
                                    .testTag("lockscreen_free_widget_${freeItem.type.name.lowercase()}")
                            ) {
                                StudioWidgetCard(
                                    item = freeItem,
                                    telemetry = telemetry,
                                    isCharging = isCharging,
                                    is24HourFormat = effectiveClockConfig.is24HourFormat,
                                    isResizeMode = isWidgetResizeMode && canEditWidgets,
                                    onUpdateWidgetItem = onUpdateWidgetItem,
                                    onExpandMediaPlayer = { media ->
                                        isWidgetResizeMode = false
                                        expandedMediaState = media
                                    },
                                    onDarkSurface = true,
                                    maxCardWidthDp = (viewportPx.width / density.density - 24f).coerceIn(136f, 340f)
                                )
                            }
                        }
                    }
                }

                // Floating Drag Ghost with dual-mode lift spring & snap feedback pill
                if (activeDragged != null && canvasBoundsInRoot != null) {
                    var ghostSize by remember { mutableStateOf(IntSize(220, 140)) }
                    val localPointer = dragPointerRoot - canvasBoundsInRoot!!.topLeft
                    val ghostX = (localPointer.x.roundToInt() - ghostSize.width / 2)
                        .coerceIn(0, (viewportPx.width - ghostSize.width).coerceAtLeast(0))
                    val ghostY = (localPointer.y.roundToInt() - ghostSize.height / 2)
                        .coerceIn(0, (viewportPx.height - ghostSize.height).coerceAtLeast(0))
                    val liftScale by animateFloatAsState(
                        targetValue = 1.06f,
                        animationSpec = AppMotion.lockWidgetDragLiftSpring(uiStyle, reducedMotion, motionSpeed),
                        label = "drag_ghost_lift"
                    )

                    Column(
                        modifier = Modifier
                            .offset { IntOffset(ghostX, ghostY) }
                            .onSizeChanged { ghostSize = it }
                            .graphicsLayer {
                                scaleX = liftScale
                                scaleY = liftScale
                            }
                            .shadow(16.dp, RoundedCornerShape(24.dp))
                            .testTag("lockscreen_drag_ghost_${activeDragged.type.name.lowercase()}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = if (previewInsideStrip) secondaryColor.copy(alpha = 0.90f) else Color.Black.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = if (previewInsideStrip) "Snap to Strip" else "Free Placement",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                        StudioWidgetCard(
                            item = activeDragged,
                            telemetry = telemetry,
                            isCharging = isCharging,
                            is24HourFormat = effectiveClockConfig.is24HourFormat,
                            isResizeMode = false,
                            onDarkSurface = true,
                            maxCardWidthDp = 240f
                        )
                    }
                }
            }
        }

        val pagePosition = preferences.parsedSecondaryWidgetPagePosition
        when (pagePosition) {
            SecondaryWidgetPagePosition.LEFT,
            SecondaryWidgetPagePosition.RIGHT -> {
                key(pagePosition) {
                    val boardPageIdx = if (pagePosition == SecondaryWidgetPagePosition.LEFT) 0 else 1
                    val lockPageIdx = if (pagePosition == SecondaryWidgetPagePosition.LEFT) 1 else 0
                    val pagerState = rememberPagerState(initialPage = lockPageIdx, pageCount = { 2 })
                    Box(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            if (page == boardPageIdx) {
                                SecondaryWidgetsBoardPage(
                                    widgets = widgetPageWidgets,
                                    maxWidgets = maxPageWidgets,
                                    telemetry = telemetry,
                                    isCharging = isCharging,
                                    is24HourFormat = effectiveClockConfig.is24HourFormat,
                                    isResizeMode = isWidgetResizeMode && canEditWidgets,
                                    onUpdateWidgetItem = onUpdateWidgetItem,
                                    onExpandMediaPlayer = { expandedMediaState = it }
                                )
                            } else {
                                PrimaryLockCanvasPage()
                            }
                        }

                        // Subtle 2-dot / Liquid Glass pill page indicator
                        if (expandedMediaState == null) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(bottom = 4.dp)
                                    .testTag("lockscreen_pager_indicator"),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(2) { idx ->
                                    val selected = pagerState.currentPage == idx
                                    val dotWidth by animateDpAsState(
                                        targetValue = if (selected && isGlass) 18.dp else if (selected) 7.dp else 5.dp,
                                        animationSpec = AppMotion.lockWidgetSnapSpring(uiStyle, reducedMotion, motionSpeed),
                                        label = "pager_dot_w_$idx"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .height(if (selected) 6.dp else 5.dp)
                                            .width(dotWidth)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = if (selected) 0.88f else 0.34f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
            SecondaryWidgetPagePosition.OFF -> {
                PrimaryLockCanvasPage()
            }
        }

        // Enlarged Full-Bleed Media Player Overlay
        AnimatedVisibility(
            visible = expandedMediaState != null,
            enter = AppMotion.lockMediaOverlayEnter(uiStyle, reducedMotion, motionSpeed),
            exit = AppMotion.lockMediaOverlayExit(uiStyle, reducedMotion, motionSpeed)
        ) {
            val activeMedia = expandedMediaState ?: ActiveMediaPlaybackState()
            EnlargedFullBleedMediaPlayer(
                mediaState = activeMedia,
                fallbackWallpaper = wallpaper,
                fallbackBackdropBitmap = backdropBitmap,
                accentColor = secondaryColor,
                onUpdateMediaState = { expandedMediaState = it },
                onCollapse = { expandedMediaState = null }
            )
        }
    }
}

@Composable
private fun SecondaryWidgetsBoardPage(
    widgets: List<StudioWidgetItem>,
    maxWidgets: Int,
    telemetry: ClockWidgetTelemetry,
    isCharging: Boolean,
    is24HourFormat: Boolean,
    isResizeMode: Boolean,
    onUpdateWidgetItem: ((StudioWidgetItem) -> Unit)?,
    onExpandMediaPlayer: (ActiveMediaPlaybackState) -> Unit
) {
    val activeNative = remember(widgets, maxWidgets) {
        val enabled = widgets.filter { it.enabled }.sortedBy { it.sortOrder }
        enabled.ifEmpty { widgets }.take(maxWidgets.coerceIn(1, 12))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 22.dp)
            .testTag("lockscreen_secondary_widget_page"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Widgets,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.88f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Lock Screen Widgets (${activeNative.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(activeNative, key = { "sec_${it.type.name}" }) { item ->
                StudioWidgetCard(
                    item = item,
                    telemetry = telemetry,
                    isCharging = isCharging,
                    is24HourFormat = is24HourFormat,
                    isResizeMode = isResizeMode,
                    onUpdateWidgetItem = onUpdateWidgetItem,
                    onExpandMediaPlayer = onExpandMediaPlayer,
                    onDarkSurface = true,
                    maxCardWidthDp = 340f
                )
            }
        }
    }
}

@Composable
private fun LockCornerShortcutButton(
    config: LockShortcutConfig,
    isTorchOn: Boolean,
    isGlyph: Boolean,
    onClick: () -> Unit,
    testTagName: String
) {
    val icon = shortcutIconForAction(config.action, isTorchOn)
    val bgColor = when {
        isTorchOn && isGlyph -> GlyphRedDot
        isTorchOn -> Color.White
        isGlyph -> Color(0xFF141414).copy(alpha = 0.92f)
        else -> Color(0xFF141824).copy(alpha = 0.72f)
    }
    val fgColor = when {
        isTorchOn && !isGlyph -> Color(0xFF10131C)
        else -> Color.White
    }
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(1.dp, if (isTorchOn) Color.White.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.24f)),
        modifier = Modifier
            .size(48.dp)
            .testTag(testTagName)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = config.displayLabel,
                tint = fgColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

internal fun shortcutIconForAction(action: LockShortcutAction, isTorchOn: Boolean = false): ImageVector = when (action) {
    LockShortcutAction.FLASHLIGHT -> if (isTorchOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff
    LockShortcutAction.CAMERA -> Icons.Rounded.CameraAlt
    LockShortcutAction.ALARM -> Icons.Rounded.Alarm
    LockShortcutAction.TIMER -> Icons.Rounded.HourglassBottom
    LockShortcutAction.STOPWATCH -> Icons.Rounded.Timer
    LockShortcutAction.CALCULATOR -> Icons.Rounded.Calculate
    LockShortcutAction.QR_SCANNER -> Icons.Rounded.QrCodeScanner
    LockShortcutAction.SETTINGS -> Icons.Rounded.Settings
    LockShortcutAction.OPEN_APP -> Icons.Rounded.Apps
    LockShortcutAction.NONE -> Icons.Rounded.Apps
}

@Composable
private fun BackdropGlancePill(
    mediaState: ActiveMediaPlaybackState,
    telemetry: ClockWidgetTelemetry,
    isCharging: Boolean,
    accentColor: Color,
    uiStyle: UiStyleMode,
    onTogglePlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onOpenExpandedMedia: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val reducedMotion = LocalReducedMotionFlag.current
    val haptics = rememberStudioHaptics()
    val isGlyph = uiStyle == UiStyleMode.GLYPH_UI

    val cards = remember { GlanceCardType.entries }
    var cardIndex by remember { mutableIntStateOf(0) }
    var flipAccumulatedDeg by remember { mutableFloatStateOf(0f) }
    var dragAccumX by remember { mutableFloatStateOf(0f) }

    val activeCard = cards[cardIndex % cards.size]
    val animatedRotationY by animateFloatAsState(
        targetValue = flipAccumulatedDeg,
        animationSpec = if (reducedMotion) tween(0) else tween(durationMillis = if (isGlyph) 240 else 360),
        label = "glance_pill_3d_flip"
    )

    fun stepCard(delta: Int) {
        haptics.performTick(16L)
        cardIndex = ((cardIndex + delta) % cards.size + cards.size) % cards.size
        flipAccumulatedDeg += if (delta >= 0) 180f else -180f
    }

    val calendarEvent = remember(activeCard) {
        if (activeCard == GlanceCardType.CALENDAR_NEXT && StudioWidgetSystemBridge.hasCalendarPermission(context)) {
            StudioWidgetSystemBridge.loadUpcomingCalendarEvents(context, limit = 1).firstOrNull()
        } else null
    }

    val pillBg = if (isGlyph) Color(0xFF141414).copy(alpha = 0.94f) else Color(0xFF141824).copy(alpha = 0.84f)
    val pillBorder = if (isGlyph) Color.White.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.26f)
    val dotAccent = if (isGlyph) GlyphRedDot else accentColor

    Surface(
        onClick = {
            if (activeCard == GlanceCardType.NOW_PLAYING) onOpenExpandedMedia()
            else stepCard(1)
        },
        shape = RoundedCornerShape(50),
        color = pillBg,
        border = BorderStroke(1.dp, pillBorder),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                if (!reducedMotion) {
                    val normalizedAngle = ((animatedRotationY % 180f) + 180f) % 180f
                    val displayAngle = if (normalizedAngle > 90f) normalizedAngle - 180f else normalizedAngle
                    rotationY = displayAngle
                    cameraDistance = 14f * density.density
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumX = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumX += dragAmount
                    },
                    onDragEnd = {
                        if (dragAccumX > 36f) stepCard(-1)
                        else if (dragAccumX < -36f) stepCard(1)
                        dragAccumX = 0f
                    }
                )
            }
            .testTag("backdrop_glance_pill")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { stepCard(-1) },
                modifier = Modifier
                    .size(32.dp)
                    .testTag("glance_pill_prev_card")
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = "Previous Glance card",
                    tint = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedContent(
                targetState = activeCard,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(120)) },
                modifier = Modifier.weight(1f),
                label = "glance_card_content"
            ) { card ->
                when (card) {
                    GlanceCardType.NOW_PLAYING -> {
                        val validArt = mediaState.albumArt?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (validArt != null) {
                                val bmp = remember(validArt) { validArt.asImageBitmap() }
                                Image(
                                    bitmap = bmp,
                                    contentDescription = mediaState.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(30.dp).clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(dotAccent.copy(alpha = 0.28f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mediaState.title,
                                    fontFamily = if (isGlyph) DotGothicFontFamily else MaterialTheme.typography.labelMedium.fontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = mediaState.artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.68f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("glance_pill_play_pause")
                            ) {
                                Icon(
                                    imageVector = if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (mediaState.isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onNextTrack,
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("glance_pill_next_track")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Next track",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    GlanceCardType.WEATHER_BRIEF -> {
                        val todayStr = remember {
                            SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = if (isGlyph) GlyphRedDot else Color(0xFFFFCA62), modifier = Modifier.size(18.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = telemetry.weatherText,
                                    fontFamily = if (isGlyph) DotGothicFontFamily else MaterialTheme.typography.labelMedium.fontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$todayStr • Glance Brief",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.68f),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    GlanceCardType.CALENDAR_NEXT -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = dotAccent, modifier = Modifier.size(18.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = calendarEvent?.title ?: "Clear Schedule",
                                    fontFamily = if (isGlyph) DotGothicFontFamily else MaterialTheme.typography.labelMedium.fontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = calendarEvent?.timeLabel ?: "No upcoming events",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.68f),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    GlanceCardType.BATTERY_BRIEF -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = if (isGlyph) GlyphRedDot else Color(0xFF5CF28E), modifier = Modifier.size(18.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${telemetry.batteryPercent.coerceIn(0, 100)}% ${if (isCharging) "• Charging" else "• Ready"}",
                                    fontFamily = if (isGlyph) DotGothicFontFamily else JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Swipe pill to flip card",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.65f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = { stepCard(1) },
                modifier = Modifier
                    .size(32.dp)
                    .testTag("glance_pill_next_card")
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Next Glance card",
                    tint = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ResizableLockMediaPlayer(
    mediaState: ActiveMediaPlaybackState,
    sizeTier: MediaPlayerSizeTier,
    accentColor: Color,
    uiStyle: UiStyleMode,
    onCycleSizeTier: () -> Unit,
    onPreviousTrack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onOpenExpanded: () -> Unit
) {
    val validArt = mediaState.albumArt?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
    val isGlyph = uiStyle == UiStyleMode.GLYPH_UI
    val pillShape = if (sizeTier == MediaPlayerSizeTier.EXPANDED_MINI) RoundedCornerShape(22.dp) else RoundedCornerShape(50)
    val animatedHeight by animateDpAsState(targetValue = sizeTier.heightDp.dp, label = "media_tier_h")
    val progress = if (mediaState.durationMs > 0L) {
        (mediaState.positionMs.toFloat() / mediaState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        onClick = onOpenExpanded,
        shape = pillShape,
        color = if (isGlyph) Color(0xFF141414).copy(alpha = 0.94f) else Color(0xFF141824).copy(alpha = 0.82f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
        modifier = Modifier
            .fillMaxWidth(sizeTier.widthScale)
            .height(animatedHeight)
            .testTag("samsung_bottom_media_pill")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = if (sizeTier == MediaPlayerSizeTier.EXPANDED_MINI) 8.dp else 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (validArt != null) {
                    val bmp = remember(validArt) { validArt.asImageBitmap() }
                    Image(
                        bitmap = bmp,
                        contentDescription = mediaState.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(if (sizeTier == MediaPlayerSizeTier.EXPANDED_MINI) 40.dp else 34.dp)
                            .clip(if (isGlyph) RoundedCornerShape(8.dp) else CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(if (sizeTier == MediaPlayerSizeTier.EXPANDED_MINI) 40.dp else 34.dp)
                            .clip(if (isGlyph) RoundedCornerShape(8.dp) else CircleShape)
                            .background((if (isGlyph) GlyphRedDot else accentColor).copy(alpha = 0.28f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaState.title,
                        fontFamily = if (isGlyph) DotGothicFontFamily else MaterialTheme.typography.labelLarge.fontFamily,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mediaState.artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (sizeTier != MediaPlayerSizeTier.COMPACT) {
                    IconButton(
                        onClick = onPreviousTrack,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous track",
                            tint = Color.White.copy(alpha = 0.88f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("samsung_media_pill_play_pause")
                ) {
                    Icon(
                        imageVector = if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (mediaState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onNextTrack,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("samsung_media_pill_next")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next track",
                        tint = Color.White.copy(alpha = 0.88f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onCycleSizeTier,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("media_player_cycle_size_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AspectRatio,
                        contentDescription = "Resize media player (${sizeTier.label})",
                        tint = Color.White.copy(alpha = 0.76f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (sizeTier == MediaPlayerSizeTier.EXPANDED_MINI) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (isGlyph) GlyphRedDot else Color.White,
                    trackColor = Color.White.copy(alpha = 0.22f)
                )
            }
        }
    }
}

@Composable
private fun BottomPortChargingWave(
    batteryPercent: Int,
    dynamicAccent: Color,
    speedMultiplier: Float,
    showPercentage: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = dynamicAccent
    val durationMs = (1650f / speedMultiplier.coerceIn(0.5f, 2f)).roundToInt().coerceIn(700, 3600)
    val transition = rememberInfiniteTransition(label = "bottom_port_wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing), RepeatMode.Restart),
        label = "bottom_port_wave_phase"
    )

    Box(
        modifier = modifier
            .height(108.dp)
            .testTag("bottom_port_charging_wave"),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val portOrigin = Offset(size.width * 0.5f, size.height)
            val maxR = size.minDimension * 1.38f

            // Luminous port surge aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.42f),
                        accent.copy(alpha = 0.62f),
                        accent.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = portOrigin,
                    radius = maxR * 0.85f
                ),
                radius = maxR * 0.85f,
                center = portOrigin
            )

            // Dual harmonic expanding port waves
            for (ring in 0..3) {
                val p = (phase + ring * 0.25f) % 1f
                val r = (maxR * (0.18f + p * 0.96f)).coerceAtLeast(8f)
                val alpha = ((1f - p) * (1f - p) * 0.72f).coerceIn(0f, 0.78f)
                drawCircle(
                    color = if (ring % 2 == 0) accent.copy(alpha = alpha) else Color.White.copy(alpha = alpha * 0.65f),
                    radius = r,
                    center = portOrigin,
                    style = Stroke(width = (4.2f * (1f - p * 0.5f)).dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Rising energy motes from bottom port
            for (i in 0 until 10) {
                val particlePhase = (phase + i * 0.10f) % 1f
                val spreadAngle = (-PI * 0.82 + (i / 9.0) * PI * 0.64)
                val dist = particlePhase * size.height * 0.92f
                val px = portOrigin.x + (cos(spreadAngle) * dist * 1.1f).toFloat()
                val py = portOrigin.y + (sin(spreadAngle) * dist).toFloat()
                val pAlpha = ((1f - particlePhase) * 0.85f).coerceIn(0f, 0.85f)
                drawCircle(
                    color = Color.White.copy(alpha = pAlpha),
                    radius = (2.6f * (1f - particlePhase * 0.45f)).dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
        if (showPercentage) {
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.BatteryChargingFull,
                    contentDescription = "Fast Charging",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${batteryPercent.coerceIn(0, 100)}% • Fast Charging",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.94f)
                )
            }
        }
    }
}

@Composable
private fun EnlargedFullBleedMediaPlayer(
    mediaState: ActiveMediaPlaybackState,
    fallbackWallpaper: Wallpaper,
    fallbackBackdropBitmap: Bitmap?,
    accentColor: Color,
    onUpdateMediaState: (ActiveMediaPlaybackState) -> Unit,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberStudioHaptics()
    val validAlbumArt = mediaState.albumArt?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
    val validFallbackBmp = fallbackBackdropBitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D14))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCollapse
            )
            .testTag("enlarged_media_player_overlay")
    ) {
        // Edge-to-edge full-bleed album art filling the entire screen
        when {
            validAlbumArt != null -> {
                val artImg = remember(validAlbumArt) { validAlbumArt.asImageBitmap() }
                Image(
                    bitmap = artImg,
                    contentDescription = "Full-screen album artwork for ${mediaState.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            validFallbackBmp != null -> {
                val bgImg = remember(validFallbackBmp) { validFallbackBmp.asImageBitmap() }
                Image(
                    bitmap = bgImg,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                WallpaperAsyncImage(
                    wallpaper = fallbackWallpaper,
                    useFullRes = true,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Subtle ambient center vinyl/music emblem when artwork isn't supplied by external player
        if (validAlbumArt == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 120.dp)
                    .size(164.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.Black.copy(alpha = 0.42f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.86f),
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        // Bottom gradient scrim smoothly elevating controls & typography over full-bleed artwork
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.28f),
                        0.45f to Color.Transparent,
                        0.72f to Color.Black.copy(alpha = 0.68f),
                        1.0f to Color.Black.copy(alpha = 0.94f)
                    )
                )
        )

        // Top minimize affordance pill
        Surface(
            onClick = {
                haptics.performTick(16L)
                onCollapse()
            },
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.48f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
                .testTag("enlarged_media_collapse_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Minimize full-screen media player",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Bottom Gradient Controls Dock (intercept clicks so tapping buttons won't collapse view)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = mediaState.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = mediaState.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.76f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val progress = if (mediaState.durationMs > 0L) {
                (mediaState.positionMs.toFloat() / mediaState.durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.24f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatPlaybackDuration(mediaState.positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    Text(
                        text = if (mediaState.durationMs > 0L) formatPlaybackDuration(mediaState.durationMs) else "--:--",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptics.performTick(18L)
                        mediaState.controller?.transportControls?.skipToPrevious()
                        val refreshed = StudioWidgetSystemBridge.readActiveMediaState(context)
                        if (refreshed.hasSession) onUpdateMediaState(refreshed)
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("enlarged_media_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous track",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Surface(
                    onClick = {
                        haptics.performTick(24L)
                        val tc = mediaState.controller?.transportControls
                        if (mediaState.isPlaying) tc?.pause() else tc?.play()
                        val refreshed = StudioWidgetSystemBridge.readActiveMediaState(context)
                        onUpdateMediaState(
                            if (refreshed.hasSession) refreshed
                            else mediaState.copy(isPlaying = !mediaState.isPlaying)
                        )
                    },
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier
                        .size(64.dp)
                        .testTag("enlarged_media_play_pause_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (mediaState.isPlaying) "Pause" else "Play",
                            tint = Color(0xFF0E121B),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        haptics.performTick(18L)
                        mediaState.controller?.transportControls?.skipToNext()
                        val refreshed = StudioWidgetSystemBridge.readActiveMediaState(context)
                        if (refreshed.hasSession) onUpdateMediaState(refreshed)
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("enlarged_media_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next track",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}

private fun formatPlaybackDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%d:%02d", mins, secs)
}

@Composable
fun ThemedUnlockButton(
    themeConfig: UnlockButtonThemeConfig,
    dynamicAccent: Color,
    isGlassMode: Boolean,
    label: String,
    onClick: () -> Unit,
    isGlyphMode: Boolean = LocalUiStyleMode.current == UiStyleMode.GLYPH_UI,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isGlyphMode && themeConfig.style != UnlockButtonStyle.CUSTOM -> Color(0xFF141414).copy(alpha = 0.92f)
        else -> when (themeConfig.style) {
            UnlockButtonStyle.DYNAMIC_ACCENT -> dynamicAccent.copy(alpha = themeConfig.transparency)
            UnlockButtonStyle.SOLID -> Color.White.copy(alpha = themeConfig.transparency.coerceAtLeast(0.85f))
            UnlockButtonStyle.FROSTED -> Color(0xFF141824).copy(alpha = (themeConfig.transparency * 0.68f).coerceIn(0.32f, 0.85f))
            UnlockButtonStyle.TRANSPARENT -> Color.Black.copy(alpha = 0.24f)
            UnlockButtonStyle.M3_TONAL -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = themeConfig.transparency)
            UnlockButtonStyle.CUSTOM -> Color(themeConfig.customContainerArgb.toInt()).copy(alpha = themeConfig.transparency)
        }
    }
    val contentColor = when {
        isGlyphMode && themeConfig.style != UnlockButtonStyle.CUSTOM -> Color.White
        else -> when (themeConfig.style) {
            UnlockButtonStyle.DYNAMIC_ACCENT -> Color.White
            UnlockButtonStyle.SOLID -> Color(0xFF10131C)
            UnlockButtonStyle.FROSTED,
            UnlockButtonStyle.TRANSPARENT -> Color.White
            UnlockButtonStyle.M3_TONAL -> MaterialTheme.colorScheme.onSecondaryContainer
            UnlockButtonStyle.CUSTOM -> Color(themeConfig.customContentArgb.toInt())
        }
    }
    val border = when {
        isGlyphMode -> BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
        themeConfig.style == UnlockButtonStyle.FROSTED || (isGlassMode && themeConfig.style != UnlockButtonStyle.SOLID) ->
            BorderStroke(1.dp, Color.White.copy(alpha = 0.44f))
        themeConfig.style == UnlockButtonStyle.TRANSPARENT ->
            BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
        else -> null
    }

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(themeConfig.cornerRadiusDp.dp),
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = modifier
            .height(48.dp)
            .testTag("lockscreen_unlock_button")
    ) {
        Icon(Icons.Rounded.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun LockScreenChargingAnimationOverlay(
    config: ChargingAnimationConfig,
    batteryPercent: Int,
    dynamicAccent: Color,
    modifier: Modifier = Modifier
) {
    val uiStyle = LocalUiStyleMode.current
    val isGlyph = uiStyle == UiStyleMode.GLYPH_UI
    val accent = when {
        !config.dynamicAccentColor -> Color(config.customAccentArgb.toInt())
        isGlyph -> GlyphRedDot
        else -> dynamicAccent
    }
    val safePct = batteryPercent.coerceIn(0, 100)
    val durationMs = (1900f / config.speedMultiplier.coerceIn(0.5f, 2f)).roundToInt().coerceIn(700, 4200)
    val transition = rememberInfiniteTransition(label = "lockscreen_charging")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing), RepeatMode.Restart),
        label = "charging_phase"
    )
    val breathScale = 0.96f + 0.05f * sin(phase * 2.0 * PI).toFloat()
    val scale = config.sizeScale.coerceIn(0.65f, 1.5f)
    val circleSizeDp = (116f * scale).dp

    when (config.preset) {
        ChargingAnimationPreset.OFF -> Unit

        // 1. Minimal Energy Halo: subtle breathing luminous ring matched to battery %
        ChargingAnimationPreset.MINIMAL_ENERGY_HALO -> {
            Box(
                modifier = modifier
                    .size(circleSizeDp)
                    .testTag("lockscreen_charging_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width * 0.5f, size.height * 0.5f)
                    val r = size.minDimension * 0.40f * breathScale
                    drawCircle(
                        color = Color(0xFF0C1018).copy(alpha = 0.72f),
                        radius = r,
                        center = c
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.16f),
                        radius = r,
                        center = c,
                        style = Stroke(width = 3.5.dp.toPx())
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(accent.copy(alpha = 0.45f), Color.White, accent)),
                        startAngle = -90f,
                        sweepAngle = (safePct / 100f) * 360f,
                        useCenter = false,
                        topLeft = Offset(c.x - r, c.y - r),
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.BatteryChargingFull,
                        contentDescription = "Charging",
                        tint = accent,
                        modifier = Modifier.size((17f * scale).dp)
                    )
                    if (config.showPercentage) {
                        Text(
                            text = "$safePct%",
                            fontFamily = if (isGlyph) DotGothicFontFamily else JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = (15f * scale).sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 2. Liquid Energy Flow: sleek capsule reservoir with dual-layer fluid charge wave
        ChargingAnimationPreset.LIQUID_ENERGY_FLOW -> {
            val capsuleW = (176f * scale).dp
            val capsuleH = (58f * scale).dp
            Box(
                modifier = modifier
                    .size(width = capsuleW, height = capsuleH)
                    .clip(RoundedCornerShape(50))
                    .testTag("lockscreen_charging_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(
                        color = Color(0xFF0C1018).copy(alpha = 0.82f),
                        cornerRadius = CornerRadius(size.height * 0.5f, size.height * 0.5f)
                    )
                    val fillRight = size.width * (safePct / 100f).coerceAtLeast(0.10f)
                    val frontWave = Path().apply {
                        moveTo(0f, size.height)
                        lineTo(0f, 0f)
                        lineTo(fillRight, 0f)
                        val steps = 14
                        for (i in 0..steps) {
                            val t = i / steps.toFloat()
                            val y = size.height * t
                            val xOff = sin(t * 2.0 * PI + phase * 2.0 * PI).toFloat() * 4.dp.toPx()
                            lineTo((fillRight + xOff).coerceIn(0f, size.width), y)
                        }
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(frontWave, color = accent.copy(alpha = 0.36f))
                    drawRoundRect(
                        color = accent.copy(alpha = 0.72f),
                        cornerRadius = CornerRadius(size.height * 0.5f, size.height * 0.5f),
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                }
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BatteryChargingFull,
                        contentDescription = "Charging",
                        tint = accent,
                        modifier = Modifier.size((18f * scale).dp)
                    )
                    if (config.showPercentage) {
                        Text(
                            text = "$safePct%",
                            fontFamily = if (isGlyph) DotGothicFontFamily else JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = (15f * scale).sp,
                            color = Color.White
                        )
                    }
                    if (config.showStatusText) {
                        Text(
                            text = "Charging",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.84f)
                        )
                    }
                }
            }
        }

        // 3. Vertical Light Pulse: slim vertical beam rising cleanly from the charging port
        ChargingAnimationPreset.VERTICAL_LIGHT_PULSE -> {
            Box(
                modifier = modifier
                    .size(width = (154f * scale).dp, height = (72f * scale).dp)
                    .testTag("lockscreen_charging_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val beamX = size.width * 0.5f
                    val travelY = size.height * (1f - phase)
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, accent.copy(alpha = 0.85f), Color.White)
                        ),
                        start = Offset(beamX, (travelY - 22.dp.toPx()).coerceAtLeast(0f)),
                        end = Offset(beamX, size.height),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawRoundRect(
                        color = Color(0xFF0E131D).copy(alpha = 0.76f),
                        topLeft = Offset(0f, size.height * 0.20f),
                        size = Size(size.width, size.height * 0.60f),
                        cornerRadius = CornerRadius(size.height * 0.3f, size.height * 0.3f)
                    )
                    drawRoundRect(
                        color = accent.copy(alpha = 0.65f),
                        topLeft = Offset(0f, size.height * 0.20f),
                        size = Size(size.width, size.height * 0.60f),
                        cornerRadius = CornerRadius(size.height * 0.3f, size.height * 0.3f),
                        style = Stroke(width = 1.1.dp.toPx())
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.BatteryChargingFull, contentDescription = "Charging", tint = accent, modifier = Modifier.size(16.dp))
                    if (config.showPercentage) {
                        Text(
                            text = "$safePct%",
                            fontFamily = if (isGlyph) DotGothicFontFamily else JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    if (config.showStatusText) {
                        Text("Pulse", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.82f))
                    }
                }
            }
        }

        // 4. Precision Battery Meter: 10-segment technical gauge with tabular % readout
        ChargingAnimationPreset.PRECISION_BATTERY_METER -> {
            val activeSegs = ((safePct / 10f).roundToInt()).coerceIn(1, 10)
            val pulseSeg = ((phase * activeSegs).toInt()).coerceIn(0, activeSegs - 1)
            Surface(
                shape = RoundedCornerShape(if (isGlyph) 14.dp else 20.dp),
                color = Color(0xFF10131A).copy(alpha = 0.88f),
                border = BorderStroke(1.dp, if (isGlyph) Color.White.copy(alpha = 0.24f) else accent.copy(alpha = 0.62f)),
                modifier = modifier.testTag("lockscreen_charging_overlay")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.BatteryChargingFull, contentDescription = "Charging", tint = accent, modifier = Modifier.size(15.dp))
                        if (config.showPercentage) {
                            Text(
                                text = "$safePct%",
                                fontFamily = if (isGlyph) DotGothicFontFamily else JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = (15f * scale).sp,
                                color = Color.White
                            )
                        }
                        if (config.showStatusText) {
                            Text(
                                text = "POWER",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.66f)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(10) { idx ->
                            val lit = idx < activeSegs
                            val highlight = lit && idx == pulseSeg
                            Box(
                                modifier = Modifier
                                    .size(width = (10f * scale).dp, height = (6f * scale).dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        when {
                                            highlight -> Color.White
                                            lit -> accent
                                            else -> Color.White.copy(alpha = 0.16f)
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }

        // 5. Soft Charging Ripple: concentric damped waves radiating smoothly from bottom port
        ChargingAnimationPreset.SOFT_CHARGING_RIPPLE -> {
            Box(
                modifier = modifier
                    .size(circleSizeDp)
                    .testTag("lockscreen_charging_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width * 0.5f, size.height * 0.5f)
                    val baseR = size.minDimension * 0.24f
                    val maxR = size.minDimension * 0.48f
                    drawCircle(
                        color = Color(0xFF0C1018).copy(alpha = 0.72f),
                        radius = baseR * 1.15f,
                        center = c
                    )
                    for (ring in 0..2) {
                        val rp = (phase + ring * 0.33f) % 1f
                        val r = baseR + (maxR - baseR) * rp
                        val a = ((1f - rp) * (1f - rp) * 0.72f).coerceIn(0f, 0.75f)
                        drawCircle(
                            color = accent.copy(alpha = a),
                            radius = r,
                            center = c,
                            style = Stroke(width = (2.4f * (1f - rp * 0.4f)).dp.toPx())
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.BatteryChargingFull, contentDescription = "Charging", tint = accent, modifier = Modifier.size((16f * scale).dp))
                    if (config.showPercentage) {
                        Text(
                            text = "$safePct%",
                            fontFamily = if (isGlyph) DotGothicFontFamily else JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = (14f * scale).sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
