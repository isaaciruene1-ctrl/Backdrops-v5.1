package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LockShortcutAction
import com.example.data.model.LockShortcutConfig
import com.example.data.model.MediaPlayerSizeTier
import com.example.data.model.StudioWidgetItem
import com.example.data.model.StudioWidgetType
import com.example.data.model.UiStyleMode
import com.example.data.model.UserPreferencesDoc
import com.example.data.model.WidgetGeometry
import com.example.engine.overlay.LaunchableAppInfo
import com.example.engine.overlay.LockShortcutExecutor
import com.example.ui.components.ClockDesignConfig
import com.example.ui.components.HyperOsClockFace
import com.example.ui.components.HyperOsClockFinish
import com.example.ui.components.HyperOsClockFont
import com.example.ui.components.StudioOptionPill
import com.example.ui.components.StudioWidgetSystemBridge
import com.example.ui.theme.DotGothicFontFamily
import com.example.ui.theme.GlyphRedDot
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalUiStyleMode
import com.example.ui.theme.rememberStudioHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

enum class ShortcutSlotTarget(val label: String) {
    LEFT("Left Shortcut"),
    RIGHT("Right Shortcut")
}

internal fun iconForShortcutAction(action: LockShortcutAction): ImageVector = when (action) {
    LockShortcutAction.FLASHLIGHT -> Icons.Rounded.FlashlightOn
    LockShortcutAction.CAMERA -> Icons.Rounded.CameraAlt
    LockShortcutAction.ALARM -> Icons.Rounded.Alarm
    LockShortcutAction.TIMER -> Icons.Rounded.Timer
    LockShortcutAction.STOPWATCH -> Icons.Rounded.AvTimer
    LockShortcutAction.CALCULATOR -> Icons.Rounded.Calculate
    LockShortcutAction.QR_SCANNER -> Icons.Rounded.QrCodeScanner
    LockShortcutAction.SETTINGS -> Icons.Rounded.Settings
    LockShortcutAction.OPEN_APP -> Icons.Rounded.Apps
    LockShortcutAction.NONE -> Icons.Rounded.Close
}

/**
 * Dark translucent iOS-inspired "Search Controls" 4-column picker modal
 * + real PackageManager `MAIN`+`LAUNCHER` Open App sub-picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchControlsModalSheet(
    slot: ShortcutSlotTarget,
    currentConfig: LockShortcutConfig,
    onSelectConfig: (LockShortcutConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberStudioHaptics()
    val uiStyle = LocalUiStyleMode.current
    val isGlyph = uiStyle == UiStyleMode.GLYPH_UI

    var query by remember { mutableStateOf("") }
    var pickingApp by remember { mutableStateOf(false) }
    var appQuery by remember { mutableStateOf("") }

    val installedApps by produceState<List<LaunchableAppInfo>>(initialValue = emptyList(), key1 = pickingApp) {
        if (pickingApp) {
            value = withContext(Dispatchers.IO) {
                LockShortcutExecutor.queryLaunchableApps(context)
            }
        }
    }

    val sheetBg = if (isGlyph) Color(0xFF0E0E0E) else Color(0xFF151923)
    val tileBg = if (isGlyph) Color(0xFF1A1A1A) else Color(0xFF232938)
    val accent = if (isGlyph) GlyphRedDot else Color(0xFF6BA6FF)

    val filteredActions = remember(query) {
        val clean = query.trim().lowercase()
        LockShortcutAction.entries.filter { act ->
            clean.isEmpty() ||
                act.label.lowercase().contains(clean) ||
                act.category.lowercase().contains(clean) ||
                act.subtitle.lowercase().contains(clean)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetBg.copy(alpha = 0.96f),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(horizontal = 18.dp, vertical = 6.dp)
                .testTag("search_controls_modal_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!pickingApp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isGlyph) slot.label.uppercase() else slot.label,
                            fontFamily = if (isGlyph) DotGothicFontFamily else null,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Current: ${currentConfig.displayLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.68f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search Controls", color = Color.White.copy(alpha = 0.56f)) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.72f))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = tileBg,
                        unfocusedContainerColor = tileBg,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_controls_input")
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 380.dp)
                ) {
                    items(filteredActions, key = { it.name }) { action ->
                        val isSelected = currentConfig.action == action
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    haptics.performConfirm()
                                    if (action == LockShortcutAction.OPEN_APP) {
                                        pickingApp = true
                                    } else {
                                        onSelectConfig(LockShortcutConfig(action = action))
                                        onDismiss()
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 2.dp)
                                .testTag("search_control_item_${action.name}")
                        ) {
                            Surface(
                                color = if (isSelected) accent.copy(alpha = 0.28f) else tileBg,
                                shape = CircleShape,
                                border = if (isSelected) BorderStroke(1.5.dp, accent) else BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = iconForShortcutAction(action),
                                        contentDescription = action.label,
                                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.90f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.92f),
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { pickingApp = false }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to controls", tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Open App",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Choose any installed launchable app",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.68f)
                        )
                    }
                }

                OutlinedTextField(
                    value = appQuery,
                    onValueChange = { appQuery = it },
                    placeholder = { Text("Filter apps", color = Color.White.copy(alpha = 0.56f)) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.72f))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = tileBg,
                        unfocusedContainerColor = tileBg,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_app_search_input")
                )

                val visibleApps = remember(installedApps, appQuery) {
                    val clean = appQuery.trim().lowercase()
                    installedApps.filter {
                        clean.isEmpty() || it.label.lowercase().contains(clean) || it.packageName.lowercase().contains(clean)
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 380.dp)
                        .testTag("open_app_picker_list")
                ) {
                    if (visibleApps.isEmpty()) {
                        item {
                            Text(
                                text = "No matching launchable apps found on this device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.70f),
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        items(visibleApps, key = { it.packageName }) { app ->
                            val selected = currentConfig.action == LockShortcutAction.OPEN_APP &&
                                currentConfig.packageName == app.packageName
                            Surface(
                                onClick = {
                                    haptics.performConfirm()
                                    onSelectConfig(
                                        LockShortcutConfig(
                                            action = LockShortcutAction.OPEN_APP,
                                            packageName = app.packageName,
                                            customLabel = app.label
                                        )
                                    )
                                    onDismiss()
                                },
                                color = if (selected) accent.copy(alpha = 0.24f) else tileBg,
                                shape = RoundedCornerShape(16.dp),
                                border = if (selected) BorderStroke(1.dp, accent) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_app_entry_${app.packageName}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = app.packageName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.56f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (selected) {
                                        Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = accent)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InlineTopWidgetModalSheet(
    enabled: Boolean,
    activeType: StudioWidgetType,
    onSelect: (Boolean, StudioWidgetType?) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = rememberStudioHaptics()
    val options = remember {
        listOf(
            StudioWidgetType.WEATHER to Icons.Rounded.WbSunny,
            StudioWidgetType.BATTERY to Icons.Rounded.BatteryChargingFull,
            StudioWidgetType.CALENDAR to Icons.Rounded.CalendarToday,
            StudioWidgetType.SUNRISE_SUNSET to Icons.Rounded.NightsStay,
            StudioWidgetType.DAY_PROGRESS to Icons.Rounded.Schedule,
            StudioWidgetType.STEP_COUNTER to Icons.Rounded.DirectionsRun
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("inline_top_widget_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Top Inline Date Bar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose a live metric displayed cleanly above the lock clock.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Top Inline Metric", style = MaterialTheme.typography.labelLarge)
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        haptics.performTick(18L)
                        onSelect(it, activeType)
                    },
                    modifier = Modifier.testTag("inline_top_bar_enable_switch")
                )
            }

            options.forEach { (type, icon) ->
                val selected = enabled && activeType == type
                Surface(
                    onClick = {
                        haptics.performConfirm()
                        onSelect(true, type)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (selected) BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inline_top_option_${type.name}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(type.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                Text(type.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                        if (selected) {
                            Icon(Icons.Rounded.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockClockCustomizerModalSheet(
    clockConfig: ClockDesignConfig,
    onUpdateClockConfig: (ClockDesignConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = rememberStudioHaptics()
    val colorSwatches = remember {
        listOf(
            "Ice" to 0xFFF6F8FCL,
            "Cyan" to 0xFF7BE3FFL,
            "Coral" to 0xFFFF8F75L,
            "Crimson" to 0xFFD92B2BL,
            "Mint" to 0xFF6CF29AL,
            "Gold" to 0xFFFFD166L
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("lock_clock_customizer_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Customize Lock Clock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StudioOptionPill(label = "Done", selected = true, onClick = onDismiss)
            }

            Text("Clock Face", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HyperOsClockFace.entries, key = { it.name }) { face ->
                    StudioOptionPill(
                        label = face.label,
                        selected = clockConfig.face == face,
                        onClick = {
                            haptics.performTick(18L)
                            onUpdateClockConfig(
                                clockConfig.copy(
                                    face = face,
                                    font = face.recommendedFont
                                )
                            )
                        },
                        modifier = Modifier.testTag("lock_clock_face_${face.name}")
                    )
                }
            }

            Text("Finish & Typography", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HyperOsClockFinish.entries, key = { it.name }) { fin ->
                    StudioOptionPill(
                        label = fin.label,
                        selected = clockConfig.finish == fin,
                        onClick = {
                            haptics.performTick(16L)
                            onUpdateClockConfig(clockConfig.copy(finish = fin))
                        }
                    )
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HyperOsClockFont.entries, key = { it.name }) { font ->
                    StudioOptionPill(
                        label = font.label,
                        selected = clockConfig.font == font,
                        onClick = {
                            haptics.performTick(16L)
                            onUpdateClockConfig(clockConfig.copy(font = font))
                        }
                    )
                }
            }

            Text("Color Tint", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(colorSwatches, key = { it.first }) { (label, argb) ->
                    val selected = (clockConfig.customPrimaryArgb ?: 0xFFF6F8FCL) == argb
                    Surface(
                        onClick = {
                            haptics.performTick(16L)
                            onUpdateClockConfig(clockConfig.copy(customPrimaryArgb = argb))
                        },
                        shape = RoundedCornerShape(50),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (selected) BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb.toInt()))
                            )
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            StudioParamSlider(
                label = "Size",
                valueText = "${(clockConfig.scale * 100).roundToInt()}%",
                value = clockConfig.scale,
                valueRange = 0.65f..1.45f,
                onValueChange = { onUpdateClockConfig(clockConfig.copy(scale = it)) },
                sliderTag = "lock_clock_scale_slider"
            )
            StudioParamSlider(
                label = "Alpha",
                valueText = "${(clockConfig.opacity * 100).roundToInt()}%",
                value = clockConfig.opacity,
                valueRange = 0.30f..1.0f,
                onValueChange = { onUpdateClockConfig(clockConfig.copy(opacity = it)) },
                sliderTag = "lock_clock_alpha_slider"
            )
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockWidgetsGeometryModalSheet(
    preferences: UserPreferencesDoc,
    onSetStudioWidgets: (List<StudioWidgetItem>) -> Unit,
    onSetWidgetPageWidgets: (List<StudioWidgetItem>) -> Unit,
    onSetGlancePillEnabled: (Boolean) -> Unit,
    onSetGlancePillCoexistWithWidgets: (Boolean) -> Unit,
    onSetMediaPlayerSizeTier: (MediaPlayerSizeTier) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberStudioHaptics()
    var targetBoard by remember { mutableIntStateOf(0) } // 0 = Main Lock Strip, 1 = Secondary Widget Page

    val calendarPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val baseList = if (targetBoard == 0) preferences.parsedStudioWidgets else preferences.parsedWidgetPageWidgets
            val next = baseList.map {
                if (it.type == StudioWidgetType.PHOTO_FRAME) it.copy(customPhotoUri = uri.toString(), enabled = true)
                else it
            }
            if (targetBoard == 0) onSetStudioWidgets(next) else onSetWidgetPageWidgets(next)
        }
    }

    val activeList = if (targetBoard == 0) preferences.parsedStudioWidgets else preferences.parsedWidgetPageWidgets

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .testTag("lock_widgets_geometry_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add & Shape Widgets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StudioOptionPill(label = "Done", selected = true, onClick = onDismiss)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StudioOptionPill(
                    label = "Lock Screen Strip",
                    selected = targetBoard == 0,
                    onClick = { targetBoard = 0 },
                    modifier = Modifier.weight(1f).testTag("widget_board_tab_lock")
                )
                StudioOptionPill(
                    label = "Widget Page (${preferences.parsedSecondaryWidgetPagePosition.label})",
                    selected = targetBoard == 1,
                    onClick = { targetBoard = 1 },
                    modifier = Modifier.weight(1f).testTag("widget_board_tab_page")
                )
            }

            if (targetBoard == 0 && preferences.glancePillEnabled && !preferences.glancePillCoexistWithWidgets) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Glance Pill hides main lock widgets by default.",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StudioOptionPill(
                            label = "Coexist",
                            selected = preferences.glancePillCoexistWithWidgets,
                            onClick = { onSetGlancePillCoexistWithWidgets(true) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 380.dp)
            ) {
                items(activeList, key = { it.type.name }) { item ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(item.type.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    Text(item.type.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (item.type == StudioWidgetType.PHOTO_FRAME && item.enabled) {
                                    StudioOptionPill(
                                        label = "Photo",
                                        selected = false,
                                        onClick = {
                                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        }
                                    )
                                }
                                Switch(
                                    checked = item.enabled,
                                    onCheckedChange = { enable ->
                                        haptics.performTick(16L)
                                        if (enable && item.type == StudioWidgetType.CALENDAR && !StudioWidgetSystemBridge.hasCalendarPermission(context)) {
                                            calendarPermLauncher.launch(Manifest.permission.READ_CALENDAR)
                                        }
                                        val next = activeList.map {
                                            if (it.type == item.type) it.copy(enabled = enable) else it
                                        }
                                        if (targetBoard == 0) onSetStudioWidgets(next) else onSetWidgetPageWidgets(next)
                                    }
                                )
                            }

                            if (item.enabled) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    WidgetGeometry.entries.forEach { geom ->
                                        StudioOptionPill(
                                            label = geom.label,
                                            selected = item.geometry == geom,
                                            onClick = {
                                                haptics.performTick(14L)
                                                val next = activeList.map {
                                                    if (it.type == item.type) it.copy(geometry = geom) else it
                                                }
                                                if (targetBoard == 0) onSetStudioWidgets(next) else onSetWidgetPageWidgets(next)
                                            },
                                            modifier = Modifier.testTag("widget_geom_${item.type.name}_${geom.name}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
