package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MotionPhotosOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.catalog.WallpaperCatalog
import com.example.data.model.ChargingAnimationConfig
import com.example.data.model.LockCredentialType
import com.example.data.model.LockNotificationStyleConfig
import com.example.data.model.LockScreenSetDoc
import com.example.data.model.LockShortcutAction
import com.example.data.model.LockShortcutConfig
import com.example.data.model.MediaPlayerSizeTier
import com.example.data.model.SecondaryWidgetPagePosition
import com.example.data.model.StudioWidgetItem
import com.example.data.model.StudioWidgetType
import com.example.data.model.UiStyleMode
import com.example.data.model.UnlockButtonThemeConfig
import com.example.data.model.UserPreferencesDoc
import com.example.data.model.WallpaperMotionMode
import com.example.data.weather.OpenMeteoWeatherRepository
import com.example.engine.notifications.LockNotificationGroup
import com.example.engine.notifications.LockNotificationItem
import com.example.engine.notifications.LockScreenNotificationRepository
import com.example.engine.overlay.BackdropLockActivity
import com.example.engine.overlay.CompatibilityStatus
import com.example.engine.overlay.LockScreenCompatibilityManager
import com.example.engine.overlay.LockScreenOverlayService
import com.example.engine.overlay.LockScreenOverlaySurface
import com.example.ui.components.ClockDesignConfig
import com.example.ui.components.ClockWidgetTelemetry
import com.example.ui.components.SharedLockScreenRenderer
import com.example.ui.components.StudioDestination
import com.example.ui.components.StudioOptionPill
import com.example.ui.components.StudioSwitchBar
import com.example.ui.components.StudioWidgetsConfiguratorCard
import com.example.ui.components.TactileSurface
import com.example.ui.theme.AppMotion
import com.example.ui.theme.DotGothicFontFamily
import com.example.ui.theme.GlyphRedDot
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalReducedMotionFlag
import com.example.ui.theme.LocalUiStyleMode
import com.example.ui.theme.rememberStudioHaptics
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreenModeScreen(
    preferences: UserPreferencesDoc,
    savedSets: List<LockScreenSetDoc>,
    currentClockConfig: ClockDesignConfig,
    onBack: () -> Unit,
    onOpenCompatibility: () -> Unit,
    onSelectOverlaySetId: (String) -> Unit,
    onSetOverlayEnabled: (Boolean) -> Unit,
    onSetLockNotificationsEnabled: (Boolean) -> Unit = {},
    onSetHideSensitiveNotifications: (Boolean) -> Unit = {},
    onSetLockCredential: (LockCredentialType, String) -> Unit = { _, _ -> },
    onClearLockCredential: () -> Unit = {},
    onSetNotificationStyleConfig: (LockNotificationStyleConfig) -> Unit = {},
    onSetChargingAnimationConfig: (ChargingAnimationConfig) -> Unit = {},
    onSetUnlockButtonThemeConfig: (UnlockButtonThemeConfig) -> Unit = {},
    onSetStudioWidgets: (List<StudioWidgetItem>) -> Unit = {},
    onSetDragToExpandNotifications: (Boolean) -> Unit = {},
    onSetShowUnlockButtonOnLockScreen: (Boolean) -> Unit = {},
    onSetSecondaryWidgetPageLeft: (Boolean) -> Unit = {},
    onSetSecondaryWidgetPagePosition: (SecondaryWidgetPagePosition) -> Unit = {},
    onSetLockScreenMaxWidgets: (Int) -> Unit = {},
    onSetWidgetPageMaxWidgets: (Int) -> Unit = {},
    onSetSamsungMediaPillEnabled: (Boolean) -> Unit = {},
    onSetPixelChargingWaveEnabled: (Boolean) -> Unit = {},
    onNavigateStudioDestination: ((StudioDestination) -> Unit)? = null,
    onSetGlancePillEnabled: (Boolean) -> Unit = {},
    onSetGlancePillCoexistWithWidgets: (Boolean) -> Unit = {},
    onSetMediaPlayerSizeTier: (MediaPlayerSizeTier) -> Unit = {},
    onSetWidgetPageWidgets: (List<StudioWidgetItem>) -> Unit = {},
    onSetInlineTopWidget: (Boolean, StudioWidgetType?) -> Unit = { _, _ -> },
    onSetLeftShortcut: (LockShortcutConfig) -> Unit = {},
    onSetRightShortcut: (LockShortcutConfig) -> Unit = {},
    onSetWallpaperMotionMode: (WallpaperMotionMode) -> Unit = {},
    onCommitClockConfig: ((ClockDesignConfig) -> ClockDesignConfig) -> Unit = {},
    onResetLockSetup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = rememberStudioHaptics()
    val uiStyle = LocalUiStyleMode.current
    val isGlyph = uiStyle == UiStyleMode.GLYPH_UI

    var diagnostics by remember { mutableStateOf(LockScreenCompatibilityManager.inspectDevice(context)) }
    var hasPostNotif by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var showFullPreview by rememberSaveable { mutableStateOf(false) }
    var previewChargingOverlay by rememberSaveable { mutableStateOf(false) }
    var showEditFrames by rememberSaveable { mutableStateOf(true) }
    var activeShortcutSheetTarget by rememberSaveable { mutableStateOf<ShortcutSlotTarget?>(null) }
    var showInlineTopSheet by rememberSaveable { mutableStateOf(false) }
    var showClockSheet by rememberSaveable { mutableStateOf(false) }
    var showWidgetsSheet by rememberSaveable { mutableStateOf(false) }
    var showResetConfirm by rememberSaveable { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPostNotif = granted
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                diagnostics = LockScreenCompatibilityManager.inspectDevice(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasPostNotif = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val activeSet = savedSets.firstOrNull { it.id == preferences.activeOverlaySetId } ?: savedSets.firstOrNull()
    val previewClock = activeSet?.let { ClockDesignConfig.fromJsonString(it.clockConfigJson) } ?: currentClockConfig
    val previewWallpaper = remember(activeSet?.wallpaperId, preferences.activeLockWallpaperId) {
        val wpId = activeSet?.wallpaperId?.takeIf { it.isNotBlank() } ?: preferences.activeLockWallpaperId
        WallpaperCatalog.getById(wpId)
            ?: WallpaperCatalog.allWallpapers.firstOrNull { it.supportsDepthEffect }
            ?: WallpaperCatalog.allWallpapers.first()
    }
    val notificationGroups by LockScreenNotificationRepository.groups.collectAsState()
    val weatherState by OpenMeteoWeatherRepository.state.collectAsState()
    val weatherReady = weatherState as? com.example.data.weather.WeatherTelemetryState.Ready
    val telemetry = ClockWidgetTelemetry(
        weatherText = weatherReady?.compactReadout ?: "21° Clear",
        batteryPercent = OpenMeteoWeatherRepository.readBatteryPercent(context),
        isWeatherAvailable = weatherReady != null
    )
    val samplePreviewGroups = remember(preferences.notificationStyleJson) {
        listOf(
            LockNotificationGroup(
                packageName = "com.backdrop.preview",
                appName = "Backdrop Studio",
                notifications = listOf(
                    LockNotificationItem(
                        key = "lock_mode_sample",
                        packageName = "com.backdrop.preview",
                        appName = "Backdrop Studio",
                        senderOrTitle = "${preferences.parsedNotificationStyle.preset.label} Notification",
                        bodyPreview = "Expand to preview stacked cards & privacy masking.",
                        postTime = System.currentTimeMillis(),
                        isClearable = false
                    )
                ),
                latestTimestamp = System.currentTimeMillis()
            )
        )
    }
    val previewNotifications = if (!preferences.lockNotificationsEnabled) {
        emptyList()
    } else if (notificationGroups.isNotEmpty()) {
        notificationGroups
    } else {
        samplePreviewGroups
    }

    @Composable
    fun InteractiveEditorCanvas(
        canvasHeight: Dp? = null,
        modifierCanvas: Modifier = Modifier
    ) {
        TactileSurface(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(26.dp),
            modifier = modifierCanvas
                .fillMaxWidth()
                .testTag("lockscreen_inline_preview_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // iOS-style Cancel / Title / Done bar + quick preview toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onBack,
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.testTag("lock_studio_cancel_button")
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        StudioOptionPill(
                            label = if (showEditFrames) "Editing" else "Clean",
                            selected = showEditFrames,
                            onClick = {
                                haptics.performTick(14L)
                                showEditFrames = !showEditFrames
                            },
                            modifier = Modifier.testTag("lock_studio_edit_frames_toggle")
                        )
                        StudioOptionPill(
                            label = if (previewChargingOverlay) "Charge" else "Battery",
                            selected = previewChargingOverlay,
                            onClick = {
                                haptics.performTick(18L)
                                previewChargingOverlay = !previewChargingOverlay
                            },
                            modifier = Modifier.testTag("lockscreen_inline_charging_toggle")
                        )
                        IconButton(
                            onClick = { showFullPreview = true },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("lockscreen_inline_expand_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fullscreen,
                                contentDescription = "Open fullscreen lock preview",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            haptics.performConfirm()
                            onBack()
                        },
                        shape = RoundedCornerShape(50),
                        color = if (isGlyph) GlyphRedDot else MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("lock_studio_done_button")
                    ) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                // Device-Aspect Interactive Canvas with 4 Framed Edit Zones
                val frameModifier = if (canvasHeight != null) {
                    Modifier
                        .height(canvasHeight)
                        .aspectRatio(9f / 19f)
                } else {
                    Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth(0.92f)
                        .aspectRatio(9f / 19f)
                }

                Box(
                    modifier = frameModifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color.Black)
                ) {
                    SharedLockScreenRenderer(
                        wallpaper = previewWallpaper,
                        customBitmap = null,
                        backdropBitmap = null,
                        clockConfig = previewClock,
                        telemetry = telemetry,
                        preferences = preferences,
                        notificationGroups = if (showEditFrames) emptyList() else previewNotifications,
                        hideSensitiveNotifications = preferences.hideSensitiveNotifications,
                        isCharging = previewChargingOverlay,
                        previewChargingOverlay = previewChargingOverlay,
                        showChromeControls = !showEditFrames,
                        isPreviewMode = true,
                        useLiveTime = previewClock.useLiveTime,
                        onUpdateWidgetItem = { updatedItem ->
                            val nextList = preferences.parsedStudioWidgets.map { existing ->
                                if (existing.type == updatedItem.type) updatedItem.sanitized() else existing
                            }
                            onSetStudioWidgets(nextList)
                        },
                        onUpdateWidgetsList = onSetStudioWidgets,
                        onCycleMediaSizeTier = onSetMediaPlayerSizeTier,
                        motionMode = preferences.parsedWallpaperMotionMode
                    )

                    if (showEditFrames) {
                        val borderColor = if (isGlyph) Color.White.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.62f)
                        val activeMainWidgetsCount = preferences.parsedStudioWidgets.count { it.enabled }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                // ZONE 1: Top Inline Date/Metric Bar Frame
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.88f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.Black.copy(alpha = 0.24f))
                                        .clickable {
                                            haptics.performTick(16L)
                                            showInlineTopSheet = true
                                        }
                                        .padding(1.dp)
                                        .testTag("lock_edit_zone_inline_top")
                                ) {
                                    DashedFrameOverlay(borderColor = borderColor, cornerRadiusDp = 14f)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (preferences.inlineTopWidgetEnabled) {
                                                "INLINE: ${(preferences.parsedInlineTopWidgetType ?: StudioWidgetType.QUICK_INFO).title.uppercase()}"
                                            } else {
                                                "+ ADD INLINE METRIC"
                                            },
                                            fontFamily = if (isGlyph) DotGothicFontFamily else JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // ZONE 2: Clock Edit Frame
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .height(104.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.Black.copy(alpha = 0.14f))
                                        .clickable {
                                            haptics.performTick(18L)
                                            showClockSheet = true
                                        }
                                        .testTag("lock_edit_zone_clock")
                                ) {
                                    DashedFrameOverlay(borderColor = borderColor, cornerRadiusDp = 18f)
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .background(Color.Black.copy(alpha = 0.56f), RoundedCornerShape(50))
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                        Text(
                                            text = previewClock.face.label,
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // ZONE 3: + ADD WIDGETS Framed Dashed Well
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .height(62.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.Black.copy(alpha = 0.24f))
                                        .clickable {
                                            haptics.performTick(18L)
                                            showWidgetsSheet = true
                                        }
                                        .testTag("lock_edit_zone_widgets")
                                ) {
                                    DashedFrameOverlay(borderColor = borderColor, cornerRadiusDp = 18f)
                                    Row(
                                        modifier = Modifier.align(Alignment.Center),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (activeMainWidgetsCount > 0) Icons.Rounded.Widgets else Icons.Rounded.Add,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (preferences.glancePillEnabled && !preferences.glancePillCoexistWithWidgets) {
                                                "GLANCE PILL ACTIVE • TAP WIDGETS"
                                            } else if (activeMainWidgetsCount > 0) {
                                                "$activeMainWidgetsCount WIDGETS • TAP TO EDIT"
                                            } else {
                                                "+ ADD WIDGETS"
                                            },
                                            fontFamily = if (isGlyph) DotGothicFontFamily else JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // ZONE 4: Bottom Corners (-)/(+) Shortcuts + Bottom-Center Glance Pill Zone
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CornerShortcutEditSlot(
                                    config = preferences.parsedLeftShortcut,
                                    onClick = {
                                        haptics.performTick(16L)
                                        activeShortcutSheetTarget = ShortcutSlotTarget.LEFT
                                    },
                                    modifier = Modifier.testTag("lock_edit_zone_left_shortcut")
                                )

                                Surface(
                                    onClick = {
                                        haptics.performTick(18L)
                                        onSetGlancePillEnabled(!preferences.glancePillEnabled)
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = if (preferences.glancePillEnabled) {
                                        (if (isGlyph) GlyphRedDot else Color(0xFF4C8CFF)).copy(alpha = 0.85f)
                                    } else {
                                        Color.Black.copy(alpha = 0.45f)
                                    },
                                    border = BorderStroke(1.dp, borderColor),
                                    modifier = Modifier.testTag("lock_edit_zone_glance_pill")
                                ) {
                                    Text(
                                        text = if (preferences.glancePillEnabled) "Glance Pill ON" else "Glance Pill OFF",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }

                                CornerShortcutEditSlot(
                                    config = preferences.parsedRightShortcut,
                                    onClick = {
                                        haptics.performTick(16L)
                                        activeShortcutSheetTarget = ShortcutSlotTarget.RIGHT
                                    },
                                    modifier = Modifier.testTag("lock_edit_zone_right_shortcut")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ControlsListItems(modifierList: Modifier = Modifier) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifierList.testTag("lockscreen_mode_scroll_list")
        ) {
            if (!modifierList.toString().contains("WIDE_PANE_MARKER")) {
                // Included inline at top for portrait scroll flows; handled explicitly below
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideLandscape = maxWidth >= 700.dp && maxWidth > maxHeight
        val portraitCanvasHeight = (maxHeight * 0.54f).coerceIn(320.dp, 500.dp)

        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        text = if (isGlyph) "LOCK SCREEN STUDIO" else "Lock Screen Mode",
                        fontFamily = if (isGlyph) DotGothicFontFamily else null,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("lockscreen_mode_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenCompatibility,
                        modifier = Modifier.testTag("lockscreen_mode_compat_action")
                    ) {
                        Icon(Icons.Rounded.Devices, contentDescription = "Compatibility Diagnostics")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            if (onNavigateStudioDestination != null) {
                StudioSwitchBar(
                    currentDestination = StudioDestination.LOCK,
                    onSelectDestination = onNavigateStudioDestination,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 6.dp)
                )
            }

            if (isWideLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.46f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        InteractiveEditorCanvas(canvasHeight = null)
                    }

                    LockScreenModeControlsList(
                        includePreviewItem = false,
                        previewContent = {},
                        preferences = preferences,
                        diagnostics = diagnostics,
                        hasPostNotif = hasPostNotif,
                        savedSets = savedSets,
                        activeSet = activeSet,
                        currentClockConfig = currentClockConfig,
                        previewChargingOverlay = previewChargingOverlay,
                        onSetOverlayEnabled = onSetOverlayEnabled,
                        onOpenFullPreview = { charging ->
                            previewChargingOverlay = charging
                            showFullPreview = true
                        },
                        onRequestNotifPerm = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        onSetLockNotificationsEnabled = onSetLockNotificationsEnabled,
                        onSetHideSensitiveNotifications = onSetHideSensitiveNotifications,
                        onOpenCompatibility = onOpenCompatibility,
                        onSetNotificationStyleConfig = onSetNotificationStyleConfig,
                        onSetChargingAnimationConfig = onSetChargingAnimationConfig,
                        onSetPixelChargingWaveEnabled = onSetPixelChargingWaveEnabled,
                        onSetUnlockButtonThemeConfig = onSetUnlockButtonThemeConfig,
                        onSetDragToExpandNotifications = onSetDragToExpandNotifications,
                        onSetShowUnlockButtonOnLockScreen = onSetShowUnlockButtonOnLockScreen,
                        onSetSecondaryWidgetPageLeft = onSetSecondaryWidgetPageLeft,
                        onSetSecondaryWidgetPagePosition = onSetSecondaryWidgetPagePosition,
                        onSetLockScreenMaxWidgets = onSetLockScreenMaxWidgets,
                        onSetWidgetPageMaxWidgets = onSetWidgetPageMaxWidgets,
                        onSetSamsungMediaPillEnabled = onSetSamsungMediaPillEnabled,
                        onSetStudioWidgets = onSetStudioWidgets,
                        onSelectOverlaySetId = onSelectOverlaySetId,
                        onSetLockCredential = onSetLockCredential,
                        onClearLockCredential = onClearLockCredential,
                        onSetGlancePillEnabled = onSetGlancePillEnabled,
                        onSetGlancePillCoexistWithWidgets = onSetGlancePillCoexistWithWidgets,
                        onSetMediaPlayerSizeTier = onSetMediaPlayerSizeTier,
                        onSetWallpaperMotionMode = onSetWallpaperMotionMode,
                        onOpenShortcutPicker = { activeShortcutSheetTarget = it },
                        onOpenInlineBarPicker = { showInlineTopSheet = true },
                        onOpenWidgetsSheet = { showWidgetsSheet = true },
                        onRequestResetLockSetup = { showResetConfirm = true },
                        modifier = Modifier.weight(0.54f).fillMaxHeight()
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    LockScreenModeControlsList(
                        includePreviewItem = true,
                        previewContent = { InteractiveEditorCanvas(canvasHeight = portraitCanvasHeight) },
                        preferences = preferences,
                        diagnostics = diagnostics,
                        hasPostNotif = hasPostNotif,
                        savedSets = savedSets,
                        activeSet = activeSet,
                        currentClockConfig = currentClockConfig,
                        previewChargingOverlay = previewChargingOverlay,
                        onSetOverlayEnabled = onSetOverlayEnabled,
                        onOpenFullPreview = { charging ->
                            previewChargingOverlay = charging
                            showFullPreview = true
                        },
                        onRequestNotifPerm = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        onSetLockNotificationsEnabled = onSetLockNotificationsEnabled,
                        onSetHideSensitiveNotifications = onSetHideSensitiveNotifications,
                        onOpenCompatibility = onOpenCompatibility,
                        onSetNotificationStyleConfig = onSetNotificationStyleConfig,
                        onSetChargingAnimationConfig = onSetChargingAnimationConfig,
                        onSetPixelChargingWaveEnabled = onSetPixelChargingWaveEnabled,
                        onSetUnlockButtonThemeConfig = onSetUnlockButtonThemeConfig,
                        onSetDragToExpandNotifications = onSetDragToExpandNotifications,
                        onSetShowUnlockButtonOnLockScreen = onSetShowUnlockButtonOnLockScreen,
                        onSetSecondaryWidgetPageLeft = onSetSecondaryWidgetPageLeft,
                        onSetSecondaryWidgetPagePosition = onSetSecondaryWidgetPagePosition,
                        onSetLockScreenMaxWidgets = onSetLockScreenMaxWidgets,
                        onSetWidgetPageMaxWidgets = onSetWidgetPageMaxWidgets,
                        onSetSamsungMediaPillEnabled = onSetSamsungMediaPillEnabled,
                        onSetStudioWidgets = onSetStudioWidgets,
                        onSelectOverlaySetId = onSelectOverlaySetId,
                        onSetLockCredential = onSetLockCredential,
                        onClearLockCredential = onClearLockCredential,
                        onSetGlancePillEnabled = onSetGlancePillEnabled,
                        onSetGlancePillCoexistWithWidgets = onSetGlancePillCoexistWithWidgets,
                        onSetMediaPlayerSizeTier = onSetMediaPlayerSizeTier,
                        onSetWallpaperMotionMode = onSetWallpaperMotionMode,
                        onOpenShortcutPicker = { activeShortcutSheetTarget = it },
                        onOpenInlineBarPicker = { showInlineTopSheet = true },
                        onOpenWidgetsSheet = { showWidgetsSheet = true },
                        onRequestResetLockSetup = { showResetConfirm = true },
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxSize()
                    )
                }
            }
        }
    }

    // Modal 1: Dark Translucent Search Controls + Open App Picker
    activeShortcutSheetTarget?.let { target ->
        val currentShortcut = if (target == ShortcutSlotTarget.LEFT) {
            preferences.parsedLeftShortcut
        } else {
            preferences.parsedRightShortcut
        }
        SearchControlsModalSheet(
            slot = target,
            currentConfig = currentShortcut,
            onSelectConfig = { next ->
                if (target == ShortcutSlotTarget.LEFT) onSetLeftShortcut(next)
                else onSetRightShortcut(next)
            },
            onDismiss = { activeShortcutSheetTarget = null }
        )
    }

    // Modal 2: Inline Top Widget Selector
    if (showInlineTopSheet) {
        InlineTopWidgetModalSheet(
            enabled = preferences.inlineTopWidgetEnabled,
            activeType = preferences.parsedInlineTopWidgetType ?: StudioWidgetType.QUICK_INFO,
            onSelect = onSetInlineTopWidget,
            onDismiss = { showInlineTopSheet = false }
        )
    }

    // Modal 3: Direct Clock Customizer inside Lock Screen Studio
    if (showClockSheet) {
        LockClockCustomizerModalSheet(
            clockConfig = previewClock,
            onUpdateClockConfig = { updated ->
                onCommitClockConfig { updated }
            },
            onDismiss = { showClockSheet = false }
        )
    }

    // Modal 4: Add & Shape Widgets (Decoupled Lock Strip vs Widget Page + Geometry Switcher)
    if (showWidgetsSheet) {
        LockWidgetsGeometryModalSheet(
            preferences = preferences,
            onSetStudioWidgets = onSetStudioWidgets,
            onSetWidgetPageWidgets = onSetWidgetPageWidgets,
            onSetGlancePillEnabled = onSetGlancePillEnabled,
            onSetGlancePillCoexistWithWidgets = onSetGlancePillCoexistWithWidgets,
            onSetMediaPlayerSizeTier = onSetMediaPlayerSizeTier,
            onDismiss = { showWidgetsSheet = false }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Lock Screen Setup?") },
            text = { Text("Restores clock widgets, Glance Pill, media tier, shortcuts, notifications style, and charging preset to Backdrop V2 defaults.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        haptics.performConfirm()
                        onResetLockSetup()
                    },
                    modifier = Modifier.testTag("confirm_reset_lock_setup")
                ) {
                    Text("Reset Setup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFullPreview) {
        Dialog(
            onDismissRequest = { showFullPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            LockScreenOverlaySurface(
                selectedSet = activeSet,
                clockConfig = previewClock,
                telemetry = telemetry,
                preferences = preferences,
                notificationGroups = if (preferences.lockNotificationsEnabled) notificationGroups else emptyList(),
                hideSensitiveNotifications = preferences.hideSensitiveNotifications,
                isPreviewMode = true,
                previewChargingOverlay = previewChargingOverlay,
                onDismissNotification = { LockScreenNotificationRepository.dismissNotification(it) },
                onClearAllNotifications = { LockScreenNotificationRepository.clearAllClearable() },
                onUpdateWidgetItem = { updatedItem ->
                    val nextList = preferences.parsedStudioWidgets.map { existing ->
                        if (existing.type == updatedItem.type) updatedItem.sanitized() else existing
                    }
                    onSetStudioWidgets(nextList)
                },
                onUpdateWidgetsList = onSetStudioWidgets,
                onCycleMediaSizeTier = onSetMediaPlayerSizeTier,
                onDismissToUnlock = {
                    previewChargingOverlay = false
                    showFullPreview = false
                }
            )
        }
    }
}

@Composable
private fun DashedFrameOverlay(
    borderColor: Color,
    cornerRadiusDp: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRoundRect(
            color = borderColor,
            cornerRadius = CornerRadius(cornerRadiusDp.dp.toPx(), cornerRadiusDp.dp.toPx()),
            style = Stroke(
                width = 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            )
        )
    }
}

@Composable
private fun CornerShortcutEditSlot(
    config: LockShortcutConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = config.action != LockShortcutAction.NONE
    Box(
        modifier = modifier
            .size(46.dp)
            .clickable(onClick = onClick)
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.50f),
            shape = CircleShape,
            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.78f)),
            modifier = Modifier
                .align(Alignment.Center)
                .size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isActive) iconForShortcutAction(config.action) else Icons.Rounded.Add,
                    contentDescription = config.displayLabel,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Surface(
            color = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 1.dp, y = (-1).dp)
                .size(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isActive) Icons.Rounded.Remove else Icons.Rounded.Add,
                    contentDescription = if (isActive) "Edit or remove shortcut" else "Add shortcut",
                    tint = Color.Black,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
private fun LockScreenModeControlsList(
    includePreviewItem: Boolean,
    previewContent: @Composable () -> Unit,
    preferences: UserPreferencesDoc,
    diagnostics: com.example.engine.overlay.OverlayDiagnosticsReport,
    hasPostNotif: Boolean,
    savedSets: List<LockScreenSetDoc>,
    activeSet: LockScreenSetDoc?,
    currentClockConfig: ClockDesignConfig,
    previewChargingOverlay: Boolean,
    onSetOverlayEnabled: (Boolean) -> Unit,
    onOpenFullPreview: (Boolean) -> Unit,
    onRequestNotifPerm: () -> Unit,
    onSetLockNotificationsEnabled: (Boolean) -> Unit,
    onSetHideSensitiveNotifications: (Boolean) -> Unit,
    onOpenCompatibility: () -> Unit,
    onSetNotificationStyleConfig: (LockNotificationStyleConfig) -> Unit,
    onSetChargingAnimationConfig: (ChargingAnimationConfig) -> Unit,
    onSetPixelChargingWaveEnabled: (Boolean) -> Unit,
    onSetUnlockButtonThemeConfig: (UnlockButtonThemeConfig) -> Unit,
    onSetDragToExpandNotifications: (Boolean) -> Unit,
    onSetShowUnlockButtonOnLockScreen: (Boolean) -> Unit,
    onSetSecondaryWidgetPageLeft: (Boolean) -> Unit,
    onSetSecondaryWidgetPagePosition: (SecondaryWidgetPagePosition) -> Unit,
    onSetLockScreenMaxWidgets: (Int) -> Unit,
    onSetWidgetPageMaxWidgets: (Int) -> Unit,
    onSetSamsungMediaPillEnabled: (Boolean) -> Unit,
    onSetStudioWidgets: (List<StudioWidgetItem>) -> Unit,
    onSelectOverlaySetId: (String) -> Unit,
    onSetLockCredential: (LockCredentialType, String) -> Unit,
    onClearLockCredential: () -> Unit,
    onSetGlancePillEnabled: (Boolean) -> Unit,
    onSetGlancePillCoexistWithWidgets: (Boolean) -> Unit,
    onSetMediaPlayerSizeTier: (MediaPlayerSizeTier) -> Unit,
    onSetWallpaperMotionMode: (WallpaperMotionMode) -> Unit,
    onOpenShortcutPicker: (ShortcutSlotTarget) -> Unit,
    onOpenInlineBarPicker: () -> Unit,
    onOpenWidgetsSheet: () -> Unit,
    onRequestResetLockSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberStudioHaptics()

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.testTag("lockscreen_mode_scroll_list")
    ) {
        if (includePreviewItem) {
            item { previewContent() }
        }

        // V2 Glance Pill, Media Player, Shortcuts & Wallpaper Motion Card
        item {
            LockScreenV2FeaturesCard(
                preferences = preferences,
                onSetGlancePillEnabled = onSetGlancePillEnabled,
                onSetGlancePillCoexistWithWidgets = onSetGlancePillCoexistWithWidgets,
                onSetMediaPlayerSizeTier = onSetMediaPlayerSizeTier,
                onSetWallpaperMotionMode = onSetWallpaperMotionMode,
                onOpenShortcutPicker = onOpenShortcutPicker,
                onOpenInlineBarPicker = onOpenInlineBarPicker,
                onRequestResetLockSetup = onRequestResetLockSetup
            )
        }

        // Enable / Disable Banner
        item {
            TactileSurface(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            Text(
                                text = "Enable Backdrop Lock Screen",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (preferences.lockScreenOverlayEnabled && diagnostics.canDrawOverlays) {
                                    "Active • Launches custom lock screen on display wake"
                                } else {
                                    "Displays custom clock & notifications on display wake"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.lockScreenOverlayEnabled,
                            onCheckedChange = { enabled ->
                                haptics.performConfirm()
                                if (enabled && !diagnostics.canDrawOverlays) {
                                    runCatching {
                                        context.startActivity(
                                            LockScreenCompatibilityManager.buildOverlayPermissionIntent(context)
                                        )
                                    }
                                } else {
                                    onSetOverlayEnabled(enabled)
                                    if (enabled) LockScreenOverlayService.start(context)
                                    else LockScreenOverlayService.stop(context)
                                }
                            },
                            modifier = Modifier.testTag("lockscreen_enable_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onOpenFullPreview(false) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f).testTag("lockscreen_preview_button")
                        ) {
                            Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preview", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        if (!preferences.lockScreenOverlayEnabled) {
                            Button(
                                onClick = {
                                    haptics.performConfirm()
                                    if (!diagnostics.canDrawOverlays) {
                                        runCatching {
                                            context.startActivity(
                                                LockScreenCompatibilityManager.buildOverlayPermissionIntent(context)
                                            )
                                        }
                                    } else {
                                        onSetOverlayEnabled(true)
                                        LockScreenOverlayService.start(context)
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f).testTag("lockscreen_apply_button")
                            ) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Apply Mode", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    haptics.performTick(25L)
                                    onSetOverlayEnabled(false)
                                    LockScreenOverlayService.stop(context)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f).testTag("lockscreen_disable_button")
                            ) {
                                Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Disable", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            haptics.performTick(20L)
                            runCatching {
                                context.startActivity(BackdropLockActivity.createLaunchIntent(context, previewMode = true))
                            }
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().testTag("lockscreen_test_activity_button")
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Full-Screen Lock Experience", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Backdrop Lock Protection & Security
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LockCredentialSetupCard(
                    preferences = preferences,
                    onSaveCredential = onSetLockCredential,
                    onClearCredential = onClearLockCredential
                )
                TactileSurface(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Security & Keyguard Coexistence",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Backdrop presents an official full-screen lock experience on wake over unsecured keyguards. Your PIN, pattern, password & biometrics are never bypassed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Permissions Checklist
        item {
            TactileSurface(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth().testTag("lockscreen_permissions_checklist_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Grant Required Permissions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    PermissionChecklistRow(
                        icon = Icons.Rounded.Layers,
                        title = "Display Over Other Apps",
                        description = "Allows launching Backdrop's lock screen when the display wakes.",
                        isGranted = diagnostics.canDrawOverlays,
                        actionLabel = if (diagnostics.canDrawOverlays) "Granted" else "Grant",
                        onClick = { runCatching { context.startActivity(LockScreenCompatibilityManager.buildOverlayPermissionIntent(context)) } },
                        rowTag = "perm_overlay_row"
                    )
                    PermissionChecklistRow(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "Notification Access",
                        description = "Lets Backdrop display live, expandable notification cards on the lock screen.",
                        isGranted = diagnostics.isNotificationAccessGranted,
                        actionLabel = if (diagnostics.isNotificationAccessGranted) "Enabled" else "Allow",
                        onClick = { runCatching { context.startActivity(LockScreenNotificationRepository.buildNotificationAccessIntent(context)) } },
                        rowTag = "perm_notification_access_row"
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionChecklistRow(
                            icon = Icons.Rounded.NotificationsActive,
                            title = "Foreground Status Notification",
                            description = "Required by Android 14+ to keep wake detection active reliably.",
                            isGranted = hasPostNotif,
                            actionLabel = if (hasPostNotif) "Granted" else "Allow",
                            onClick = onRequestNotifPerm,
                            rowTag = "perm_notif_row"
                        )
                    }
                    PermissionChecklistRow(
                        icon = Icons.Rounded.BatteryChargingFull,
                        title = "Unrestricted Battery Usage",
                        description = "Recommended so OEM battery savers do not kill screen-on wake detection.",
                        isGranted = diagnostics.isBatteryOptimizationIgnored,
                        actionLabel = if (diagnostics.isBatteryOptimizationIgnored) "Enabled" else "Configure",
                        onClick = { runCatching { context.startActivity(LockScreenCompatibilityManager.buildBatteryOptimizationIntent(context)) } },
                        rowTag = "perm_battery_row"
                    )
                }
            }
        }

        // Notifications Toggle Card
        item {
            TactileSurface(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Lock Screen Notifications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            Text("Show Notifications on Lock Screen", style = MaterialTheme.typography.labelLarge)
                            Text("Grouped by app with smooth drag-to-expand cards", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = preferences.lockNotificationsEnabled,
                            onCheckedChange = {
                                haptics.performTick(22L)
                                onSetLockNotificationsEnabled(it)
                            },
                            modifier = Modifier.testTag("lockscreen_notifications_switch")
                        )
                    }
                    if (preferences.lockNotificationsEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text("Hide Sensitive Content", style = MaterialTheme.typography.labelLarge)
                                Text("Keep sender visible while masking private text until unlocked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = preferences.hideSensitiveNotifications,
                                onCheckedChange = {
                                    haptics.performTick(22L)
                                    onSetHideSensitiveNotifications(it)
                                },
                                modifier = Modifier.testTag("lockscreen_hide_sensitive_switch")
                            )
                        }
                    }
                }
            }
        }

        // OEM Compatibility Card
        item {
            TactileSurface(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onOpenCompatibility)
                    .testTag("open_compat_diagnostics_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${diagnostics.detectedProfile.displayName} • ${diagnostics.effectiveStatus.label}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "View full OEM compatibility & device diagnostics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (diagnostics.effectiveStatus == CompatibilityStatus.FULLY_SUPPORTED) {
                            Icons.Rounded.CheckCircle
                        } else {
                            Icons.Rounded.WarningAmber
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Collapsible Drawers
        if (preferences.lockNotificationsEnabled) {
            item {
                LockModeCollapsibleDrawerCard(
                    title = "Notification Style & Presets",
                    summary = "${preferences.parsedNotificationStyle.preset.label} • Corner radius & translucency",
                    icon = Icons.Rounded.NotificationsActive,
                    testTag = "lockscreen_drawer_notif_style"
                ) {
                    LockNotificationStyleCard(
                        config = preferences.parsedNotificationStyle,
                        onUpdate = onSetNotificationStyleConfig
                    )
                }
            }
        }

        item {
            LockModeCollapsibleDrawerCard(
                title = "Charging Customization",
                summary = "${preferences.parsedChargingAnimation.preset.label} • 5 redesigned overlays",
                icon = Icons.Rounded.BatteryChargingFull,
                testTag = "lockscreen_drawer_charging"
            ) {
                ChargingAnimationCustomizerCard(
                    config = preferences.parsedChargingAnimation,
                    onUpdate = onSetChargingAnimationConfig,
                    onTestChargingPreview = { onOpenFullPreview(true) },
                    bottomPortWaveEnabled = preferences.pixelChargingWaveEnabled,
                    onSetBottomPortWaveEnabled = onSetPixelChargingWaveEnabled
                )
            }
        }

        item {
            LockModeCollapsibleDrawerCard(
                title = "Unlock Button Appearance",
                summary = "${preferences.parsedUnlockButtonTheme.style.label} • Opacity & accent",
                icon = Icons.Rounded.TouchApp,
                testTag = "lockscreen_drawer_unlock_theme"
            ) {
                UnlockButtonThemeCard(
                    config = preferences.parsedUnlockButtonTheme,
                    onUpdate = onSetUnlockButtonThemeConfig
                )
            }
        }

        item {
            LockModeCollapsibleDrawerCard(
                title = "Lock Screen Layout & Gestures",
                summary = "${preferences.parsedSecondaryWidgetPagePosition.label} Page • ${preferences.safeLockScreenMaxWidgets} Lock / ${preferences.safeWidgetPageMaxWidgets} Board",
                icon = Icons.Rounded.Swipe,
                testTag = "lockscreen_drawer_v5_layout"
            ) {
                LockScreenV5InteractionsCard(
                    preferences = preferences,
                    onSetDragToExpandNotifications = onSetDragToExpandNotifications,
                    onSetShowUnlockButtonOnLockScreen = onSetShowUnlockButtonOnLockScreen,
                    onSetSecondaryWidgetPageLeft = onSetSecondaryWidgetPageLeft,
                    onSetSecondaryWidgetPagePosition = onSetSecondaryWidgetPagePosition,
                    onSetLockScreenMaxWidgets = onSetLockScreenMaxWidgets,
                    onSetWidgetPageMaxWidgets = onSetWidgetPageMaxWidgets,
                    onSetSamsungMediaPillEnabled = onSetSamsungMediaPillEnabled,
                    onOpenWidgetGeometrySheet = onOpenWidgetsSheet
                )
            }
        }

        item {
            LockModeCollapsibleDrawerCard(
                title = "Lock Screen Widgets Suite",
                summary = "${preferences.parsedStudioWidgets.count { it.enabled }} / ${StudioWidgetType.entries.size} active widgets",
                icon = Icons.Rounded.Widgets,
                testTag = "lockscreen_drawer_widgets"
            ) {
                StudioWidgetsConfiguratorCard(
                    widgets = preferences.parsedStudioWidgets,
                    onUpdateWidgets = onSetStudioWidgets
                )
            }
        }

        item {
            LockModeCollapsibleDrawerCard(
                title = "Select Clock Design Set",
                summary = activeSet?.name ?: "${currentClockConfig.face.label} (Studio active)",
                icon = Icons.Rounded.Palette,
                testTag = "lockscreen_drawer_sets"
            ) {
                TactileSurface(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Select Clock Design", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (savedSets.isEmpty()) {
                            Text(
                                text = "Using active Studio clock (${currentClockConfig.face.label}). Tap the Clock Frame in preview or save Sets in Depth Studio.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(savedSets, key = { it.id }) { setDoc ->
                                    StudioOptionPill(
                                        label = setDoc.name,
                                        selected = (activeSet?.id == setDoc.id),
                                        onClick = {
                                            haptics.performTick(24L)
                                            onSelectOverlaySetId(setDoc.id)
                                        },
                                        modifier = Modifier.testTag("lockscreen_set_pill_${setDoc.id}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun LockScreenV2FeaturesCard(
    preferences: UserPreferencesDoc,
    onSetGlancePillEnabled: (Boolean) -> Unit,
    onSetGlancePillCoexistWithWidgets: (Boolean) -> Unit,
    onSetMediaPlayerSizeTier: (MediaPlayerSizeTier) -> Unit,
    onSetWallpaperMotionMode: (WallpaperMotionMode) -> Unit,
    onOpenShortcutPicker: (ShortcutSlotTarget) -> Unit,
    onOpenInlineBarPicker: () -> Unit,
    onRequestResetLockSetup: () -> Unit
) {
    val haptics = rememberStudioHaptics()
    val mediaTier = preferences.parsedMediaPlayerSizeTier
    val motionMode = preferences.parsedWallpaperMotionMode

    TactileSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lockscreen_v2_glance_motion_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.ViewCarousel, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Glance Pill, Media & Shortcuts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("3D flip hub, resizable player, corner actions & canvas motion", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            LockV5ToggleRow(
                title = "Backdrop Glance Pill",
                subtitle = "Anchored bottom-center with 3D swipe cards (Now Playing, Weather, Calendar, Battery). Overrules standalone player & widgets by default.",
                checked = preferences.glancePillEnabled,
                onCheckedChange = {
                    haptics.performConfirm()
                    onSetGlancePillEnabled(it)
                },
                switchTag = "glance_pill_switch"
            )

            AnimatedVisibility(visible = preferences.glancePillEnabled) {
                LockV5ToggleRow(
                    title = "Coexist With Main Lock Widgets",
                    subtitle = "Keep main lock screen widgets visible alongside the Glance Pill",
                    checked = preferences.glancePillCoexistWithWidgets,
                    onCheckedChange = {
                        haptics.performTick(16L)
                        onSetGlancePillCoexistWithWidgets(it)
                    },
                    switchTag = "glance_pill_coexist_switch"
                )
            }

            AnimatedVisibility(visible = !preferences.glancePillEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Media Player Size Tier", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        MediaPlayerSizeTier.entries.forEach { tier ->
                            StudioOptionPill(
                                label = tier.label,
                                selected = mediaTier == tier,
                                onClick = {
                                    haptics.performTick(16L)
                                    onSetMediaPlayerSizeTier(tier)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("media_tier_${tier.name}")
                            )
                        }
                    }
                }
            }

            // Corner Shortcuts & Inline Top Metric Quick Launchers
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bottom Corner Shortcuts & Top Bar", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = { onOpenShortcutPicker(ShortcutSlotTarget.LEFT) },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f).testTag("configure_left_shortcut_button")
                    ) {
                        Icon(iconForShortcutAction(preferences.parsedLeftShortcut.action), contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("L: ${preferences.parsedLeftShortcut.displayLabel}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = { onOpenShortcutPicker(ShortcutSlotTarget.RIGHT) },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f).testTag("configure_right_shortcut_button")
                    ) {
                        Icon(iconForShortcutAction(preferences.parsedRightShortcut.action), contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("R: ${preferences.parsedRightShortcut.displayLabel}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedButton(
                    onClick = onOpenInlineBarPicker,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("configure_inline_top_button")
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (preferences.inlineTopWidgetEnabled) {
                            "Top Bar: ${(preferences.parsedInlineTopWidgetType ?: StudioWidgetType.QUICK_INFO).title}"
                        } else {
                            "Top Inline Metric: Hidden"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Wallpaper Motion Mode
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.MotionPhotosOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text("Wallpaper Motion Mode", style = MaterialTheme.typography.labelLarge)
                }
                Text(motionMode.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(WallpaperMotionMode.entries, key = { it.name }) { mode ->
                        StudioOptionPill(
                            label = mode.label,
                            selected = motionMode == mode,
                            onClick = {
                                haptics.performTick(16L)
                                onSetWallpaperMotionMode(mode)
                            },
                            modifier = Modifier.testTag("wallpaper_motion_${mode.name}")
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onRequestResetLockSetup,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().testTag("reset_lock_setup_button")
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Lock Setup")
            }
        }
    }
}

@Composable
private fun LockModeCollapsibleDrawerCard(
    title: String,
    summary: String,
    icon: ImageVector,
    testTag: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable(testTag) { mutableStateOf(initiallyExpanded) }
    val currentStyle = LocalUiStyleMode.current
    val reducedMotion = LocalReducedMotionFlag.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TactileSurface(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable { expanded = !expanded }
                .minimumInteractiveComponentSize()
                .testTag("${testTag}_toggle")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(summary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = AppMotion.drawerEnter(currentStyle, reducedMotion),
            exit = AppMotion.drawerExit(currentStyle, reducedMotion)
        ) {
            content()
        }
    }
}

@Composable
private fun PermissionChecklistRow(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onClick: () -> Unit,
    rowTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag(rowTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (isGranted) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = "Granted", tint = MaterialTheme.colorScheme.primary)
        } else {
            FilledTonalButton(onClick = onClick, shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                Text(actionLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LockCredentialSetupCard(
    preferences: UserPreferencesDoc,
    onSaveCredential: (LockCredentialType, String) -> Unit,
    onClearCredential: () -> Unit
) {
    val haptics = rememberStudioHaptics()
    val configuredType = preferences.parsedLockCredentialType
    var selectedType by remember(configuredType) { mutableStateOf(configuredType) }
    var isEditing by remember(configuredType, preferences.hasLockCredential) {
        mutableStateOf(configuredType != LockCredentialType.NONE && !preferences.hasLockCredential)
    }
    var secretInput by remember { mutableStateOf("") }
    var confirmInput by remember { mutableStateOf("") }
    var showSecret by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    TactileSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth().testTag("lockscreen_credential_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text("Backdrop Lock Protection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (preferences.hasLockCredential) "${configuredType.label} lock enabled for Backdrop overlay" else "Optional PIN or Password before dismissing Backdrop Lock",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFB74D).copy(alpha = 0.14f),
                border = BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth().testTag("lockscreen_security_warning_banner")
            ) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(18.dp))
                    Text(
                        text = "Visual convenience lock only: This PIN/Password is NOT as secure as your native Android lock screen, hardware keystore, or biometrics. Always keep your device's system lock enabled.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LockCredentialType.entries.forEach { type ->
                    StudioOptionPill(
                        label = type.label,
                        selected = selectedType == type,
                        onClick = {
                            haptics.performTick(20L)
                            validationError = null
                            secretInput = ""
                            confirmInput = ""
                            selectedType = type
                            if (type == LockCredentialType.NONE) {
                                isEditing = false
                                if (preferences.hasLockCredential) onClearCredential()
                            } else {
                                isEditing = !(preferences.hasLockCredential && configuredType == type)
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("lock_cred_type_${type.name}")
                    )
                }
            }

            if (selectedType != LockCredentialType.NONE) {
                if (preferences.hasLockCredential && configuredType == selectedType && !isEditing) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { secretInput = ""; confirmInput = ""; validationError = null; isEditing = true },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f).testTag("lock_cred_change_button")
                        ) { Text("Change ${selectedType.label}") }
                        OutlinedButton(
                            onClick = { haptics.performTick(24L); selectedType = LockCredentialType.NONE; isEditing = false; onClearCredential() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f).testTag("lock_cred_clear_button")
                        ) { Text("Remove") }
                    }
                } else {
                    val isPin = selectedType == LockCredentialType.PIN
                    OutlinedTextField(
                        value = secretInput,
                        onValueChange = { raw ->
                            secretInput = if (isPin) raw.filter { it.isDigit() }.take(8) else raw.take(32)
                            validationError = null
                        },
                        label = { Text(if (isPin) "Enter PIN (4–8 digits)" else "Enter Password (min 4 chars)") },
                        singleLine = true,
                        visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = if (isPin) KeyboardType.NumberPassword else KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showSecret = !showSecret }) {
                                Icon(if (showSecret) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("lock_cred_input")
                    )
                    OutlinedTextField(
                        value = confirmInput,
                        onValueChange = { raw ->
                            confirmInput = if (isPin) raw.filter { it.isDigit() }.take(8) else raw.take(32)
                            validationError = null
                        },
                        label = { Text("Confirm ${selectedType.label}") },
                        singleLine = true,
                        visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = if (isPin) KeyboardType.NumberPassword else KeyboardType.Password),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("lock_cred_confirm_input")
                    )
                    if (!validationError.isNullOrBlank()) {
                        Text(validationError.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("lock_cred_validation_error"))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val minValid = if (isPin) secretInput.length in 4..8 && secretInput.all { it.isDigit() } else secretInput.length >= 4
                                when {
                                    !minValid -> validationError = if (isPin) "PIN must be 4 to 8 digits." else "Password must be at least 4 characters."
                                    secretInput != confirmInput -> validationError = "${selectedType.label} entries do not match."
                                    else -> {
                                        haptics.performConfirm()
                                        onSaveCredential(selectedType, secretInput)
                                        secretInput = ""; confirmInput = ""; isEditing = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f).testTag("lock_cred_save_button")
                        ) { Text("Save ${selectedType.label}") }
                        if (preferences.hasLockCredential) {
                            OutlinedButton(
                                onClick = { secretInput = ""; confirmInput = ""; validationError = null; selectedType = configuredType; isEditing = false },
                                shape = RoundedCornerShape(50)
                            ) { Text("Cancel") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LockScreenV5InteractionsCard(
    preferences: UserPreferencesDoc,
    onSetDragToExpandNotifications: (Boolean) -> Unit,
    onSetShowUnlockButtonOnLockScreen: (Boolean) -> Unit,
    onSetSecondaryWidgetPageLeft: (Boolean) -> Unit,
    onSetSecondaryWidgetPagePosition: (SecondaryWidgetPagePosition) -> Unit,
    onSetLockScreenMaxWidgets: (Int) -> Unit,
    onSetWidgetPageMaxWidgets: (Int) -> Unit,
    onSetSamsungMediaPillEnabled: (Boolean) -> Unit,
    onOpenWidgetGeometrySheet: () -> Unit
) {
    val haptics = rememberStudioHaptics()
    val currentPos = preferences.parsedSecondaryWidgetPagePosition
    val lockMax = preferences.safeLockScreenMaxWidgets
    val pageMax = preferences.safeWidgetPageMaxWidgets

    TactileSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth().testTag("lockscreen_v5_interactions_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LockV5ToggleRow(
                title = "Drag-to-Expand Notifications",
                subtitle = "Drag notifications up smoothly; height commits on release",
                checked = preferences.dragToExpandNotifications,
                onCheckedChange = { haptics.performTick(18L); onSetDragToExpandNotifications(it) },
                switchTag = "lock_v5_drag_notifs_switch"
            )
            LockV5ToggleRow(
                title = "Show Unlock Button",
                subtitle = "Swipe-up to unlock stays active while hiding the bottom button",
                checked = preferences.showUnlockButtonOnLockScreen,
                onCheckedChange = { haptics.performTick(18L); onSetShowUnlockButtonOnLockScreen(it) },
                switchTag = "lock_v5_unlock_btn_switch"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Secondary Widget Page Side", style = MaterialTheme.typography.labelLarge)
                Text("Swipe Left, Right, or disable the dedicated widget board page", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryWidgetPagePosition.entries.forEach { pos ->
                        StudioOptionPill(
                            label = pos.label,
                            selected = currentPos == pos,
                            onClick = {
                                haptics.performSnapDetent()
                                onSetSecondaryWidgetPagePosition(pos)
                                onSetSecondaryWidgetPageLeft(pos == SecondaryWidgetPagePosition.LEFT)
                            },
                            modifier = Modifier.weight(1f).testTag("lock_v5_page_pos_${pos.name}")
                        )
                    }
                }
                OutlinedButton(
                    onClick = onOpenWidgetGeometrySheet,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().testTag("configure_widget_page_widgets_btn")
                ) {
                    Icon(Icons.Rounded.Widgets, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Configure Widget Page & Geometry (${preferences.parsedWidgetPageWidgets.count { it.enabled }} active)")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Direct Lock Screen Widgets", style = MaterialTheme.typography.labelLarge)
                    Text(if (lockMax == 0) "Hidden (0)" else "$lockMax max", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
                AdaptiveDesignSlider(
                    value = lockMax.toFloat(),
                    onValueChange = { raw ->
                        val next = raw.roundToInt().coerceIn(0, 6)
                        if (next != lockMax) { haptics.performTick(16L); onSetLockScreenMaxWidgets(next) }
                    },
                    valueRange = 0f..6f,
                    modifier = Modifier.fillMaxWidth().testTag("lock_v5_direct_widget_count_slider")
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Widget Page Widgets", style = MaterialTheme.typography.labelLarge)
                    Text("$pageMax max", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
                AdaptiveDesignSlider(
                    value = pageMax.toFloat(),
                    onValueChange = { raw ->
                        val next = raw.roundToInt().coerceIn(1, 12)
                        if (next != pageMax) { haptics.performTick(16L); onSetWidgetPageMaxWidgets(next) }
                    },
                    valueRange = 1f..12f,
                    modifier = Modifier.fillMaxWidth().testTag("lock_v5_page_widget_count_slider")
                )
            }

            LockV5ToggleRow(
                title = "Bottom Media Player",
                subtitle = "Resizable compact media card when Glance Pill is inactive",
                checked = preferences.samsungMediaPillEnabled,
                onCheckedChange = { haptics.performTick(18L); onSetSamsungMediaPillEnabled(it) },
                switchTag = "lock_v5_samsung_media_switch"
            )
        }
    }
}

@Composable
private fun LockV5ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTag: String
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag(switchTag))
    }
}
