package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.catalog.WallpaperCatalog
import com.example.data.media.OfflinePackProgress
import com.example.data.model.AppLanguageOption
import com.example.data.model.BackdropStudioFont
import com.example.data.model.DockLayoutMode
import com.example.data.model.GlowMaterialConfig
import com.example.data.model.ImageQualityPref
import com.example.data.model.LiquidGlassTier
import com.example.data.model.MaterialNavStyle
import com.example.data.model.OfflinePackSizeOption
import com.example.data.model.ThemeMode
import com.example.data.model.UiStyleMode
import com.example.data.model.UserPreferencesDoc
import com.example.ui.components.BackdropLogoMark
import com.example.ui.components.TactileSurface
import com.example.ui.components.glassSpecularBorder
import com.example.ui.theme.AppMotion
import com.example.ui.theme.LocalReducedMotionFlag
import com.example.ui.theme.LocalUiStyleMode
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanelScreen(
    preferences: UserPreferencesDoc,
    cacheBytes: Long,
    onBack: () -> Unit,
    onUiStyleChange: (UiStyleMode) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAmoledDarkChange: (Boolean) -> Unit,
    onHomeCategoryChipsChange: (Boolean) -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit = {},
    onReducedMotionChange: (Boolean) -> Unit = {},
    onGlassDepthIntensityChange: (Float) -> Unit = {},
    onImageQualityChange: (ImageQualityPref) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onRefreshCacheSize: () -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier,
    // Backdrop 1.1 expanded controls
    onDynamicThemingChange: (Boolean) -> Unit = {},
    onCustomAccentChange: (Long) -> Unit = {},
    onM3SurfaceDepthChange: (Float) -> Unit = {},
    onM3CornerRadiusChange: (Float) -> Unit = {},
    onM3ElevationChange: (Float) -> Unit = {},
    onM3DensityChange: (Float) -> Unit = {},
    onM3NavStyleChange: (MaterialNavStyle) -> Unit = {},
    onM3TypographyScaleChange: (Float) -> Unit = {},
    onM3MotionSpeedChange: (Float) -> Unit = {},
    onGlassBlurChange: (Float) -> Unit = {},
    onGlassRefractionChange: (Float) -> Unit = {},
    onGlassTransparencyChange: (Float) -> Unit = {},
    onGlassLightResponseChange: (Float) -> Unit = {},
    onGlassSpecularHighlightsChange: (Float) -> Unit = {},
    onGlassBorderSoftnessChange: (Float) -> Unit = {},
    onGlassAdaptiveContrastChange: (Float) -> Unit = {},
    onDockLayoutChange: (DockLayoutMode) -> Unit = {},
    onClassicClockHandlesChange: (Boolean) -> Unit = {},
    onManualDepthEffectChange: (Boolean) -> Unit = {},
    onLanguageChange: (String) -> Unit = {},
    onWeatherCityChange: (String) -> Unit = {},
    onWeatherUseLocationChange: (Boolean) -> Unit = {},
    onOpenLockScreenMode: () -> Unit = {},
    onOpenCompatibilityManager: () -> Unit = {},
    onReplayOnboarding: () -> Unit = {},
    onResetMaterial3Defaults: () -> Unit = {},
    onResetGlassDefaults: () -> Unit = {},
    onRestoreAllDefaults: () -> Unit = {},
    // Backdrop 1.3 Typography, Studio Materials, On-Device & Offline Packs
    onStudioFontChange: (BackdropStudioFont) -> Unit = {},
    onLiquidGlassTierChange: (LiquidGlassTier) -> Unit = {},
    onGlowMaterialChange: (GlowMaterialConfig) -> Unit = {},
    onRememberEditingChange: (Boolean) -> Unit = {},
    onScaleTextDefaultChange: (Boolean) -> Unit = {},
    onDeviceCount: Int = 0,
    onToggleOnDeviceEnabled: (Boolean) -> Unit = {},
    onAddOnDevicePhotos: (List<String>) -> Unit = {},
    onAddOnDeviceFolder: (String) -> Unit = {},
    onRemoveOnDeviceFolder: (String) -> Unit = {},
    onRefreshOnDevice: () -> Unit = {},
    offlinePackProgress: OfflinePackProgress = OfflinePackProgress(),
    onStartOfflinePack: (OfflinePackSizeOption) -> Unit = {},
    onPauseOfflinePack: () -> Unit = {},
    onCancelOfflinePack: () -> Unit = {},
    onClearOfflinePack: () -> Unit = {}
) {
    val isGlass = LocalUiStyleMode.current == UiStyleMode.FROSTED_GLASS_OS
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var selectedOfflineOption by remember(preferences.offlinePackTargetCount) {
        mutableStateOf(OfflinePackSizeOption.fromCount(preferences.offlinePackTargetCount))
    }

    LaunchedEffect(Unit) {
        onRefreshCacheSize()
    }

    val cacheLabel = remember(cacheBytes) {
        when {
            cacheBytes < 1024L -> "$cacheBytes B"
            cacheBytes < 1024L * 1024L -> "${cacheBytes / 1024L} KB"
            else -> String.format(Locale.US, "%.1f MB", cacheBytes / (1024f * 1024f))
        }
    }
    val totalCount = remember { WallpaperCatalog.allWallpapers.size }

    Scaffold(
        containerColor = if (isGlass) Color.Transparent else MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = stringResource(id = R.string.settings_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${preferences.parsedUiStyle.label} • ${preferences.parsedLanguage.nativeLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_panel_screen")
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.displayCutout)),
            contentAlignment = Alignment.TopCenter
        ) {
            val isWide = maxWidth >= 720.dp
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 980.dp)
                    .fillMaxSize()
                    .testTag("settings_lazy_column"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 52.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Interface Style Switcher + Granular M3 / Frosted Glass Tuning Sliders
                item {
                    InterfaceStyleSwitcherCard(
                        preferences = preferences,
                        onUiStyleChange = onUiStyleChange,
                        onGlassDepthIntensityChange = onGlassDepthIntensityChange,
                        onDynamicThemingChange = onDynamicThemingChange,
                        onCustomAccentChange = onCustomAccentChange,
                        onM3SurfaceDepthChange = onM3SurfaceDepthChange,
                        onM3CornerRadiusChange = onM3CornerRadiusChange,
                        onM3ElevationChange = onM3ElevationChange,
                        onM3DensityChange = onM3DensityChange,
                        onM3NavStyleChange = onM3NavStyleChange,
                        onM3TypographyScaleChange = onM3TypographyScaleChange,
                        onM3MotionSpeedChange = onM3MotionSpeedChange,
                        onGlassBlurChange = onGlassBlurChange,
                        onGlassRefractionChange = onGlassRefractionChange,
                        onGlassTransparencyChange = onGlassTransparencyChange,
                        onGlassLightResponseChange = onGlassLightResponseChange,
                        onGlassSpecularHighlightsChange = onGlassSpecularHighlightsChange,
                        onGlassBorderSoftnessChange = onGlassBorderSoftnessChange,
                        onGlassAdaptiveContrastChange = onGlassAdaptiveContrastChange,
                        onResetMaterial3Defaults = onResetMaterial3Defaults,
                        onResetGlassDefaults = onResetGlassDefaults
                    )
                }

                // 2. Universal Studio Typography & Scale (Material 3 + Frosted OS)
                item {
                    StudioTypographyCard(
                        preferences = preferences,
                        onStudioFontChange = onStudioFontChange,
                        onTypographyScaleChange = onM3TypographyScaleChange
                    )
                }

                // 3. Appearance & Layout (Light/Dark, Dock & Navigation Bar Style, Handles, Motion — ALWAYS Card #3)
                item {
                    AppearanceLayoutCard(
                        preferences = preferences,
                        onThemeModeChange = onThemeModeChange,
                        onAmoledDarkChange = onAmoledDarkChange,
                        onHomeCategoryChipsChange = onHomeCategoryChipsChange,
                        onHapticFeedbackChange = onHapticFeedbackChange,
                        onReducedMotionChange = onReducedMotionChange,
                        onDockLayoutChange = onDockLayoutChange,
                        onM3NavStyleChange = onM3NavStyleChange,
                        onClassicClockHandlesChange = onClassicClockHandlesChange,
                        onManualDepthEffectChange = onManualDepthEffectChange
                    )
                }

                if (isWide) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                CollapsibleSettingsDrawerCard(
                                    title = "Liquid Glass Tier & Studio Behavior",
                                    summary = "${preferences.parsedLiquidGlassTier.label} • Glow & remember studio state",
                                    icon = Icons.Rounded.AutoAwesome,
                                    testTag = "settings_drawer_materials_tier"
                                ) {
                                    StudioMaterialsAndTierCard(
                                        preferences = preferences,
                                        onLiquidGlassTierChange = onLiquidGlassTierChange,
                                        onGlowMaterialChange = onGlowMaterialChange,
                                        onRememberEditingChange = onRememberEditingChange,
                                        onScaleTextDefaultChange = onScaleTextDefaultChange
                                    )
                                }
                                CollapsibleSettingsDrawerCard(
                                    title = "Language, RTL & Weather",
                                    summary = "${preferences.parsedLanguage.nativeLabel} • ${preferences.weatherCity.ifBlank { "Location / Auto" }}",
                                    icon = Icons.Rounded.Language,
                                    testTag = "settings_drawer_language_weather"
                                ) {
                                    LanguageAndWeatherCard(
                                        preferences = preferences,
                                        onLanguageChange = onLanguageChange,
                                        onWeatherCityChange = onWeatherCityChange,
                                        onWeatherUseLocationChange = onWeatherUseLocationChange
                                    )
                                }
                                CollapsibleSettingsDrawerCard(
                                    title = "On-Device Gallery Collection",
                                    summary = if (preferences.onDeviceCollectionEnabled) "$onDeviceCount device photos indexed" else "Scoped photo & folder sources",
                                    icon = Icons.Rounded.Devices,
                                    testTag = "settings_drawer_on_device"
                                ) {
                                    OnDeviceCollectionSettingsCard(
                                        preferences = preferences,
                                        onDeviceCount = onDeviceCount,
                                        onToggleEnabled = onToggleOnDeviceEnabled,
                                        onAddPhotoUris = onAddOnDevicePhotos,
                                        onAddFolderUri = onAddOnDeviceFolder,
                                        onRemoveFolderUri = onRemoveOnDeviceFolder,
                                        onRefreshScan = onRefreshOnDevice
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                CollapsibleSettingsDrawerCard(
                                    title = "Downloads & Image Quality",
                                    summary = "${preferences.parsedImageQuality.label}${if (preferences.wifiOnlyDownloads) " • Wi-Fi only" else ""}",
                                    icon = Icons.Rounded.HighQuality,
                                    testTag = "settings_drawer_downloads"
                                ) {
                                    DownloadsResolutionCard(
                                        preferences = preferences,
                                        isGlass = isGlass,
                                        onImageQualityChange = onImageQualityChange,
                                        onWifiOnlyChange = onWifiOnlyChange
                                    )
                                }
                                CollapsibleSettingsDrawerCard(
                                    title = "Offline Wallpaper Pack",
                                    summary = "${OfflinePackSizeOption.fromCount(preferences.offlinePackTargetCount).label} • Cache local 4K sets",
                                    icon = Icons.Rounded.Wifi,
                                    testTag = "settings_drawer_offline_pack"
                                ) {
                                    OfflinePackSettingsCard(
                                        progress = offlinePackProgress,
                                        selectedOption = selectedOfflineOption,
                                        onSelectOption = { selectedOfflineOption = it },
                                        onStartOrResume = { onStartOfflinePack(selectedOfflineOption) },
                                        onPause = onPauseOfflinePack,
                                        onCancel = onCancelOfflinePack,
                                        onClearPack = onClearOfflinePack
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        CollapsibleSettingsDrawerCard(
                            title = "Liquid Glass Tier & Studio Behavior",
                            summary = "${preferences.parsedLiquidGlassTier.label} • Glow & remember studio state",
                            icon = Icons.Rounded.AutoAwesome,
                            testTag = "settings_drawer_materials_tier"
                        ) {
                            StudioMaterialsAndTierCard(
                                preferences = preferences,
                                onLiquidGlassTierChange = onLiquidGlassTierChange,
                                onGlowMaterialChange = onGlowMaterialChange,
                                onRememberEditingChange = onRememberEditingChange,
                                onScaleTextDefaultChange = onScaleTextDefaultChange
                            )
                        }
                    }
                    item {
                        CollapsibleSettingsDrawerCard(
                            title = "Language, RTL & Weather",
                            summary = "${preferences.parsedLanguage.nativeLabel} • ${preferences.weatherCity.ifBlank { "Location / Auto" }}",
                            icon = Icons.Rounded.Language,
                            testTag = "settings_drawer_language_weather"
                        ) {
                            LanguageAndWeatherCard(
                                preferences = preferences,
                                onLanguageChange = onLanguageChange,
                                onWeatherCityChange = onWeatherCityChange,
                                onWeatherUseLocationChange = onWeatherUseLocationChange
                            )
                        }
                    }
                    item {
                        CollapsibleSettingsDrawerCard(
                            title = "On-Device Gallery Collection",
                            summary = if (preferences.onDeviceCollectionEnabled) "$onDeviceCount device photos indexed" else "Scoped photo & folder sources",
                            icon = Icons.Rounded.Devices,
                            testTag = "settings_drawer_on_device"
                        ) {
                            OnDeviceCollectionSettingsCard(
                                preferences = preferences,
                                onDeviceCount = onDeviceCount,
                                onToggleEnabled = onToggleOnDeviceEnabled,
                                onAddPhotoUris = onAddOnDevicePhotos,
                                onAddFolderUri = onAddOnDeviceFolder,
                                onRemoveFolderUri = onRemoveOnDeviceFolder,
                                onRefreshScan = onRefreshOnDevice
                            )
                        }
                    }
                    item {
                        CollapsibleSettingsDrawerCard(
                            title = "Downloads & Image Quality",
                            summary = "${preferences.parsedImageQuality.label}${if (preferences.wifiOnlyDownloads) " • Wi-Fi only" else ""}",
                            icon = Icons.Rounded.HighQuality,
                            testTag = "settings_drawer_downloads"
                        ) {
                            DownloadsResolutionCard(
                                preferences = preferences,
                                isGlass = isGlass,
                                onImageQualityChange = onImageQualityChange,
                                onWifiOnlyChange = onWifiOnlyChange
                            )
                        }
                    }
                    item {
                        CollapsibleSettingsDrawerCard(
                            title = "Offline Wallpaper Pack",
                            summary = "${OfflinePackSizeOption.fromCount(preferences.offlinePackTargetCount).label} • Cache local 4K sets",
                            icon = Icons.Rounded.Wifi,
                            testTag = "settings_drawer_offline_pack"
                        ) {
                            OfflinePackSettingsCard(
                                progress = offlinePackProgress,
                                selectedOption = selectedOfflineOption,
                                onSelectOption = { selectedOfflineOption = it },
                                onStartOrResume = { onStartOfflinePack(selectedOfflineOption) },
                                onPause = onPauseOfflinePack,
                                onCancel = onCancelOfflinePack,
                                onClearPack = onClearOfflinePack
                            )
                        }
                    }
                }

                // Pinned bottom-most section across all screen sizes: Storage, Tutorial & Defaults
                item {
                    CollapsibleSettingsDrawerCard(
                        title = "Storage, Tutorial & Defaults",
                        summary = "Backdrop 1.5 • $cacheLabel cache",
                        icon = Icons.Rounded.CleaningServices,
                        testTag = "settings_drawer_storage_about"
                    ) {
                        StorageAboutCard(
                            cacheLabel = cacheLabel,
                            totalCount = totalCount,
                            onClearCache = onClearCache,
                            onReplayOnboarding = onReplayOnboarding,
                            onRequestRestoreDefaults = { showRestoreConfirm = true }
                        )
                    }
                }
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore All Defaults?") },
            text = {
                Text(
                    "Resets UI style sliders, theme, dock layout, and studio interface preferences back to defaults. " +
                        "Your saved Lock-Screen Sets, Collections, and Favorites will NOT be touched."
                )
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        onRestoreAllDefaults()
                    },
                    modifier = Modifier.testTag("settings_confirm_restore_defaults")
                ) {
                    Text("Restore")
                }
            }
        )
    }
}

@Composable
private fun InterfaceStyleSwitcherCard(
    preferences: UserPreferencesDoc,
    onUiStyleChange: (UiStyleMode) -> Unit,
    onGlassDepthIntensityChange: (Float) -> Unit,
    onDynamicThemingChange: (Boolean) -> Unit,
    onCustomAccentChange: (Long) -> Unit,
    onM3SurfaceDepthChange: (Float) -> Unit,
    onM3CornerRadiusChange: (Float) -> Unit,
    onM3ElevationChange: (Float) -> Unit,
    onM3DensityChange: (Float) -> Unit,
    onM3NavStyleChange: (MaterialNavStyle) -> Unit,
    onM3TypographyScaleChange: (Float) -> Unit,
    onM3MotionSpeedChange: (Float) -> Unit,
    onGlassBlurChange: (Float) -> Unit,
    onGlassRefractionChange: (Float) -> Unit,
    onGlassTransparencyChange: (Float) -> Unit,
    onGlassLightResponseChange: (Float) -> Unit,
    onGlassSpecularHighlightsChange: (Float) -> Unit,
    onGlassBorderSoftnessChange: (Float) -> Unit,
    onGlassAdaptiveContrastChange: (Float) -> Unit,
    onResetMaterial3Defaults: () -> Unit,
    onResetGlassDefaults: () -> Unit
) {
    val currentStyle = preferences.parsedUiStyle
    val reducedMotion = LocalReducedMotionFlag.current
    TactileSurface(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Interface Design System",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Applies across Home, Preview & Depth Studio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UiStyleMode.entries.forEach { mode ->
                    UiStyleOptionCard(
                        mode = mode,
                        selected = currentStyle == mode,
                        onClick = { onUiStyleChange(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Collapsed-by-default Expandable Dropdown Panels per Active UI Mode
            var m3TuningExpanded by rememberSaveable { mutableStateOf(false) }
            var glassTuningExpanded by rememberSaveable { mutableStateOf(false) }

            AnimatedVisibility(
                visible = currentStyle == UiStyleMode.MATERIAL_3,
                enter = AppMotion.drawerEnter(currentStyle, reducedMotion),
                exit = AppMotion.drawerExit(currentStyle, reducedMotion)
            ) {
                TactileSurface(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { m3TuningExpanded = !m3TuningExpanded }
                                .minimumInteractiveComponentSize()
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                .testTag("settings_m3_dropdown_header"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Material 3 Expressive Tuning",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (m3TuningExpanded) {
                                        "Tap to collapse nav style, depth & motion sliders"
                                    } else {
                                        "${preferences.parsedMaterialNavStyle.label} • Expand to tune surface depth, radii & scale"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (m3TuningExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (m3TuningExpanded) "Collapse Material 3 tuning" else "Expand Material 3 tuning",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(
                            visible = m3TuningExpanded,
                            enter = AppMotion.drawerEnter(currentStyle, reducedMotion),
                            exit = AppMotion.drawerExit(currentStyle, reducedMotion)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = onResetMaterial3Defaults,
                                        modifier = Modifier.testTag("settings_m3_reset_button")
                                    ) {
                                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Reset Section")
                                    }
                                }

                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    val navStyles = MaterialNavStyle.entries
                                    navStyles.forEachIndexed { idx, style ->
                                        SegmentedButton(
                                            selected = preferences.parsedMaterialNavStyle == style,
                                            onClick = { onM3NavStyleChange(style) },
                                            shape = SegmentedButtonDefaults.itemShape(idx, navStyles.size),
                                            modifier = Modifier.testTag("settings_m3_nav_${style.name}")
                                        ) {
                                            Text(style.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }

                                StudioParamSlider(
                                    label = "Surface Depth",
                                    valueText = "${(preferences.m3SurfaceDepth * 100).roundToInt()}%",
                                    value = preferences.m3SurfaceDepth,
                                    valueRange = 0.6f..1.4f,
                                    onValueChange = onM3SurfaceDepthChange,
                                    sliderTag = "settings_m3_surface_depth"
                                )
                                StudioParamSlider(
                                    label = "Corner Radius",
                                    valueText = "${(preferences.m3CornerRadiusScale * 100).roundToInt()}%",
                                    value = preferences.m3CornerRadiusScale,
                                    valueRange = 0.5f..1.5f,
                                    onValueChange = onM3CornerRadiusChange,
                                    sliderTag = "settings_m3_corner_radius"
                                )
                                StudioParamSlider(
                                    label = "Elevation",
                                    valueText = "${(preferences.m3ElevationScale * 100).roundToInt()}%",
                                    value = preferences.m3ElevationScale,
                                    valueRange = 0f..1.6f,
                                    onValueChange = onM3ElevationChange,
                                    sliderTag = "settings_m3_elevation"
                                )
                                StudioParamSlider(
                                    label = "Density",
                                    valueText = "${(preferences.m3DensityScale * 100).roundToInt()}%",
                                    value = preferences.m3DensityScale,
                                    valueRange = 0.85f..1.25f,
                                    onValueChange = onM3DensityChange,
                                    sliderTag = "settings_m3_density"
                                )
                                StudioParamSlider(
                                    label = "Motion Speed",
                                    valueText = "${(preferences.m3MotionSpeed * 100).roundToInt()}%",
                                    value = preferences.m3MotionSpeed,
                                    valueRange = 0.6f..1.5f,
                                    onValueChange = onM3MotionSpeedChange,
                                    sliderTag = "settings_m3_motion"
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = currentStyle == UiStyleMode.FROSTED_GLASS_OS,
                enter = AppMotion.drawerEnter(currentStyle, reducedMotion),
                exit = AppMotion.drawerExit(currentStyle, reducedMotion)
            ) {
                TactileSurface(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { glassTuningExpanded = !glassTuningExpanded }
                                .minimumInteractiveComponentSize()
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                .testTag("settings_glass_dropdown_header"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Frosted Glass OS Optics",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (glassTuningExpanded) {
                                        "Tap to collapse blur, refraction & specular sliders"
                                    } else {
                                        "Depth ${(preferences.glassDepthIntensity * 100).roundToInt()}% • Expand optical parameters"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (glassTuningExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (glassTuningExpanded) "Collapse Frosted Glass tuning" else "Expand Frosted Glass tuning",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(
                            visible = glassTuningExpanded,
                            enter = AppMotion.drawerEnter(currentStyle, reducedMotion),
                            exit = AppMotion.drawerExit(currentStyle, reducedMotion)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = onResetGlassDefaults,
                                        modifier = Modifier.testTag("settings_glass_reset_button")
                                    ) {
                                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Reset Section")
                                    }
                                }
                                StudioParamSlider(
                                    label = "Glass Depth",
                                    valueText = "${(preferences.glassDepthIntensity * 100).roundToInt()}%",
                                    value = preferences.glassDepthIntensity,
                                    valueRange = 0.55f..1.45f,
                                    onValueChange = onGlassDepthIntensityChange,
                                    sliderTag = "settings_glass_depth_slider"
                                )
                                StudioParamSlider(
                                    label = "Glass Blur",
                                    valueText = "${(preferences.glassBlurLevel * 100).roundToInt()}%",
                                    value = preferences.glassBlurLevel,
                                    valueRange = 0.4f..1.5f,
                                    onValueChange = onGlassBlurChange,
                                    sliderTag = "settings_glass_blur"
                                )
                                StudioParamSlider(
                                    label = "Refraction",
                                    valueText = "${(preferences.glassRefractionLevel * 100).roundToInt()}%",
                                    value = preferences.glassRefractionLevel,
                                    valueRange = 0.4f..1.5f,
                                    onValueChange = onGlassRefractionChange,
                                    sliderTag = "settings_glass_refraction"
                                )
                                StudioParamSlider(
                                    label = "Transparency",
                                    valueText = "${(preferences.glassTransparencyLevel * 100).roundToInt()}%",
                                    value = preferences.glassTransparencyLevel,
                                    valueRange = 0.5f..1.4f,
                                    onValueChange = onGlassTransparencyChange,
                                    sliderTag = "settings_glass_transparency"
                                )
                                StudioParamSlider(
                                    label = "Light Response",
                                    valueText = "${(preferences.glassLightResponse * 100).roundToInt()}%",
                                    value = preferences.glassLightResponse,
                                    valueRange = 0.4f..1.5f,
                                    onValueChange = onGlassLightResponseChange,
                                    sliderTag = "settings_glass_light"
                                )
                                StudioParamSlider(
                                    label = "Specular Highlights",
                                    valueText = "${(preferences.glassSpecularHighlights * 100).roundToInt()}%",
                                    value = preferences.glassSpecularHighlights,
                                    valueRange = 0.3f..1.5f,
                                    onValueChange = onGlassSpecularHighlightsChange,
                                    sliderTag = "settings_glass_specular"
                                )
                                StudioParamSlider(
                                    label = "Border Softness",
                                    valueText = "${(preferences.glassBorderSoftness * 100).roundToInt()}%",
                                    value = preferences.glassBorderSoftness,
                                    valueRange = 0.4f..1.5f,
                                    onValueChange = onGlassBorderSoftnessChange,
                                    sliderTag = "settings_glass_border"
                                )
                                StudioParamSlider(
                                    label = "Adaptive Contrast",
                                    valueText = "${(preferences.glassAdaptiveContrast * 100).roundToInt()}%",
                                    value = preferences.glassAdaptiveContrast,
                                    valueRange = 0.7f..1.5f,
                                    onValueChange = onGlassAdaptiveContrastChange,
                                    sliderTag = "settings_glass_contrast"
                                )
                            }
                        }
                    }
                }
            }

            // Dynamic Theming toggle + Accent Swatches when Dynamic Theming is OFF
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Dynamic Theming", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Sync palette to wallpaper",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = preferences.dynamicThemingEnabled,
                    onCheckedChange = onDynamicThemingChange,
                    modifier = Modifier.testTag("settings_dynamic_theming_switch")
                )
            }

            AnimatedVisibility(
                visible = !preferences.dynamicThemingEnabled,
                enter = AppMotion.drawerEnter(currentStyle, reducedMotion),
                exit = AppMotion.drawerExit(currentStyle, reducedMotion)
            ) {
                CustomDynamicColorPickerCard(
                    selectedHex = preferences.customAccentHex,
                    onSelectHex = onCustomAccentChange
                )
            }
        }
    }
}

@Composable
private fun AppearanceLayoutCard(
    preferences: UserPreferencesDoc,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAmoledDarkChange: (Boolean) -> Unit,
    onHomeCategoryChipsChange: (Boolean) -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit,
    onReducedMotionChange: (Boolean) -> Unit,
    onDockLayoutChange: (DockLayoutMode) -> Unit,
    onM3NavStyleChange: (MaterialNavStyle) -> Unit,
    onClassicClockHandlesChange: (Boolean) -> Unit,
    onManualDepthEffectChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    TactileSurface(
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("settings_appearance_layout_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Appearance & Layout",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = listOf(
                    ThemeMode.SYSTEM to "System",
                    ThemeMode.LIGHT to "Light",
                    ThemeMode.DARK to "Dark"
                )
                modes.forEachIndexed { idx, (mode, label) ->
                    SegmentedButton(
                        selected = preferences.parsedThemeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = modes.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
                            inactiveContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.testTag("settings_theme_mode_${mode.name}")
                    ) {
                        Text(label)
                    }
                }
            }

            // Navigation Bar Position works in both M3 & Frosted Glass OS; M3 Navigation Bar Style is M3-exclusive
            val isGlassStyle = preferences.parsedUiStyle == UiStyleMode.FROSTED_GLASS_OS
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Dock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = if (isGlassStyle) "Navigation Bar Position" else "Navigation Bar Position & Style",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_dock_layout_row")
                ) {
                    val dockModes = DockLayoutMode.entries
                    dockModes.forEachIndexed { idx, mode ->
                        SegmentedButton(
                            selected = preferences.parsedDockLayoutMode == mode,
                            onClick = { onDockLayoutChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(idx, dockModes.size),
                            modifier = Modifier.testTag("settings_dock_layout_${mode.name}")
                        ) {
                            Text(mode.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (!isGlassStyle) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_material_nav_style_row")
                    ) {
                        val navStyles = MaterialNavStyle.entries
                        navStyles.forEachIndexed { idx, style ->
                            SegmentedButton(
                                selected = preferences.parsedMaterialNavStyle == style,
                                onClick = { onM3NavStyleChange(style) },
                                shape = SegmentedButtonDefaults.itemShape(idx, navStyles.size),
                                modifier = Modifier.testTag("settings_appearance_nav_style_${style.name}")
                            ) {
                                Text(style.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            SettingsSwitchRow(
                icon = Icons.Rounded.DarkMode,
                title = "AMOLED Pitch Black",
                subtitle = "True #000000 backdrop when dark theme is active",
                checked = preferences.amoledDark,
                onCheckedChange = onAmoledDarkChange,
                tag = "settings_amoled_switch"
            )

            SettingsSwitchRow(
                icon = Icons.Rounded.OpenWith,
                title = "Use Classic Resize Handles",
                subtitle = "Classic square corners vs adaptive handles",
                checked = preferences.useClassicClockHandles,
                onCheckedChange = onClassicClockHandlesChange,
                tag = "settings_classic_handles_switch"
            )

            SettingsSwitchRow(
                icon = Icons.Rounded.Brush,
                title = "Manual Depth Effect",
                subtitle = "Show Keep/Erase brush & magnifier tools in Depth Studio",
                checked = preferences.manualDepthEffectEnabled,
                onCheckedChange = onManualDepthEffectChange,
                tag = "settings_manual_depth_switch"
            )

            SettingsSwitchRow(
                icon = Icons.Rounded.FilterList,
                title = "Home Category Chips",
                subtitle = "Pin category pills below the Home greeting",
                checked = preferences.homeCategoryChipsEnabled,
                onCheckedChange = onHomeCategoryChipsChange,
                tag = "settings_chips_switch"
            )

            SettingsSwitchRow(
                icon = Icons.Rounded.TouchApp,
                title = "Tactile Haptics",
                subtitle = "Haptic detents on sliders, nav & snaps",
                checked = preferences.hapticFeedbackEnabled,
                onCheckedChange = onHapticFeedbackChange,
                tag = "settings_haptics_switch"
            )

            SettingsSwitchRow(
                icon = Icons.Rounded.Animation,
                title = "Reduced Motion",
                subtitle = "Minimal transitions across tabs & drawers",
                checked = preferences.reducedMotionEnabled,
                onCheckedChange = onReducedMotionChange,
                tag = "settings_reduced_motion_switch"
            )
        }
    }
}

@Composable
private fun LanguageAndWeatherCard(
    preferences: UserPreferencesDoc,
    onLanguageChange: (String) -> Unit,
    onWeatherCityChange: (String) -> Unit,
    onWeatherUseLocationChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var cityDraft by remember(preferences.weatherCity) { mutableStateOf(preferences.weatherCity) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onWeatherUseLocationChange(granted)
    }

    TactileSurface(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Language & RTL Layout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Supports 9 languages including Arabic right-to-left mirroring",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AppLanguageOption.entries) { lang ->
                    val selected = preferences.parsedLanguage == lang
                    FilterChip(
                        selected = selected,
                        onClick = { onLanguageChange(lang.code) },
                        label = {
                            Text(if (lang.isRtl) "${lang.nativeLabel} (RTL)" else lang.nativeLabel)
                        },
                        modifier = Modifier.testTag("settings_language_${lang.code}")
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Text("Live Lock-Screen Weather (Open-Meteo)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = cityDraft,
                    onValueChange = { cityDraft = it },
                    label = { Text("City name (e.g. Tokyo, London)") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_weather_city_input")
                )
                OutlinedButton(
                    onClick = { onWeatherCityChange(cityDraft) },
                    modifier = Modifier.testTag("settings_weather_city_save")
                ) {
                    Text("Save")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = null)
                    Column {
                        Text("Optional Coarse Location", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Fallback automatically to local coordinates when city is blank",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = preferences.weatherUseLocation,
                    onCheckedChange = { enable ->
                        if (enable) {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                onWeatherUseLocationChange(true)
                            } else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        } else {
                            onWeatherUseLocationChange(false)
                        }
                    },
                    modifier = Modifier.testTag("settings_weather_location_switch")
                )
            }
        }
    }
}

@Composable
private fun DownloadsResolutionCard(
    preferences: UserPreferencesDoc,
    isGlass: Boolean,
    onImageQualityChange: (ImageQualityPref) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    TactileSurface(
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Downloads & Resolution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            ImageQualityPref.entries.forEach { pref ->
                val selected = preferences.parsedImageQuality == pref
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (isGlass) 0.84f else 1f),
                    contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = glassSpecularBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onImageQualityChange(pref) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.HighQuality,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                pref.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                pref.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            SettingsSwitchRow(
                icon = Icons.Rounded.Wifi,
                title = "Wi-Fi Only Downloads",
                subtitle = "Protect mobile data on 4K architecture packs",
                checked = preferences.wifiOnlyDownloads,
                onCheckedChange = onWifiOnlyChange,
                tag = "settings_wifi_only_switch"
            )
        }
    }
}

@Composable
private fun StorageAboutCard(
    cacheLabel: String,
    totalCount: Int,
    onClearCache: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onRequestRestoreDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    TactileSurface(
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.CleaningServices, contentDescription = null)
                    Column {
                        Text("Artwork Preview Cache", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Using $cacheLabel on device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(
                    onClick = onClearCache,
                    modifier = Modifier.testTag("settings_clear_cache_button")
                ) {
                    Text("Clear")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BackdropLogoMark(size = 28.dp)
                Column {
                    Text(
                        text = "Backdrop 1.5 • $totalCount Canvases",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "37 Clocks • 10 Studio Fonts • Depth Engine V5 • 22 Widgets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReplayOnboarding,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_replay_onboarding_button")
                ) {
                    Icon(Icons.Rounded.School, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Tutorial", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = onRequestRestoreDefaults,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_restore_defaults_button")
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Defaults", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null)
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun UiStyleOptionCard(
    mode: UiStyleMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "ui_style_border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.84f)
        },
        label = "ui_style_bg"
    )
    val labelColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val subColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.84f) else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = bgColor,
        contentColor = labelColor,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .testTag("ui_style_${mode.name}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (mode == UiStyleMode.FROSTED_GLASS_OS) {
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.36f)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            )
                        }
                    )
                    .padding(8.dp)
            ) {
                if (mode == UiStyleMode.MATERIAL_3) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(18.dp)
                                .align(Alignment.Start)
                        ) {}
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .height(16.dp)
                        ) {}
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = labelColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = mode.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = subColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CollapsibleSettingsDrawerCard(
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
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable { expanded = !expanded }
                .minimumInteractiveComponentSize()
                .testTag("${testTag}_toggle")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
