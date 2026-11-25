package com.example.ui.screens

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.media.OfflinePackProgress
import com.example.data.model.BackdropStudioFont
import com.example.data.model.GlowMaterialConfig
import com.example.data.model.LiquidGlassTier
import com.example.data.model.OfflinePackSizeOption
import com.example.data.model.UserPreferencesDoc
import com.example.data.repository.OnDeviceWallpaperRepository
import com.example.engine.performance.ZeroLagPerformanceEngine
import com.example.ui.components.TactileSurface
import com.example.ui.components.glassSpecularBorder
import com.example.ui.theme.BackdropCustomAccentSwatches
import com.example.ui.theme.composeColorToHsl
import com.example.ui.theme.evaluateSchemeContrast
import com.example.ui.theme.hslToComposeColor
import com.example.ui.theme.studioDisplayFontFamily
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Custom HSL + Swatch Dynamic Color picker with live hex readout and WCAG AA contrast validation.
 */
@Composable
internal fun CustomDynamicColorPickerCard(
    selectedHex: Long,
    onSelectHex: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialHsl = remember(selectedHex) { composeColorToHsl(Color(selectedHex.toInt())) }
    var hue by remember(selectedHex) { mutableFloatStateOf(initialHsl[0]) }
    var saturation by remember(selectedHex) { mutableFloatStateOf(initialHsl[1].coerceIn(0.15f, 0.95f)) }
    var lightness by remember(selectedHex) { mutableFloatStateOf(initialHsl[2].coerceIn(0.20f, 0.80f)) }

    val liveColor = remember(hue, saturation, lightness) { hslToComposeColor(hue, saturation, lightness) }
    val hexString = remember(liveColor) {
        String.format(Locale.US, "#%06X", liveColor.toArgb() and 0xFFFFFF)
    }
    val contrastReport = evaluateSchemeContrast(MaterialTheme.colorScheme)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("custom_dynamic_color_picker"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            items(BackdropCustomAccentSwatches) { (name, argb) ->
                val selected = selectedHex == argb
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectHex(argb) }
                        .padding(4.dp)
                        .testTag("settings_accent_swatch_$name")
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(argb.toInt()))
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.45f),
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = name.substringBefore(" "),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
            shape = RoundedCornerShape(18.dp),
            border = glassSpecularBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(liveColor)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                        Text(
                            text = "Custom Seed $hexString",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("custom_color_hex_readout")
                        )
                    }
                    Surface(
                        color = if (contrastReport.meetsWcagAa) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("custom_color_wcag_badge")
                    ) {
                        Text(
                            text = if (contrastReport.meetsWcagAa) {
                                String.format(Locale.US, "WCAG AA %.1f:1", contrastReport.primaryContrast)
                            } else {
                                "Contrast Adjusted"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (contrastReport.meetsWcagAa) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                StudioSliderDropdown(
                    title = "HSL Color Tuning",
                    summary = "${hue.roundToInt()}° • ${(saturation * 100).roundToInt()}% sat • ${(lightness * 100).roundToInt()}% light",
                    testTag = "custom_color_hsl_dropdown"
                ) {
                    StudioParamSlider(
                        label = "Hue",
                        valueText = "${hue.roundToInt()}°",
                        value = hue,
                        valueRange = 0f..359f,
                        onValueChange = { nextHue ->
                            hue = nextHue
                            val c = hslToComposeColor(nextHue, saturation, lightness)
                            onSelectHex(c.toArgb().toLong() and 0xFFFFFFFFL)
                        },
                        sliderTag = "custom_color_hue_slider"
                    )
                    StudioParamSlider(
                        label = "Saturation",
                        valueText = "${(saturation * 100).roundToInt()}%",
                        value = saturation,
                        valueRange = 0.15f..0.95f,
                        onValueChange = { nextSat ->
                            saturation = nextSat
                            val c = hslToComposeColor(hue, nextSat, lightness)
                            onSelectHex(c.toArgb().toLong() and 0xFFFFFFFFL)
                        },
                        sliderTag = "custom_color_sat_slider"
                    )
                    StudioParamSlider(
                        label = "Lightness",
                        valueText = "${(lightness * 100).roundToInt()}%",
                        value = lightness,
                        valueRange = 0.20f..0.80f,
                        onValueChange = { nextL ->
                            lightness = nextL
                            val c = hslToComposeColor(hue, saturation, nextL)
                            onSelectHex(c.toArgb().toLong() and 0xFFFFFFFFL)
                        },
                        sliderTag = "custom_color_light_slider"
                    )
                }
            }
        }
    }
}

/**
 * Standalone 10-Font Studio Typography & Scale Card (works across Material 3 & Frosted OS).
 */
@Composable
internal fun StudioTypographyCard(
    preferences: UserPreferencesDoc,
    onStudioFontChange: (BackdropStudioFont) -> Unit,
    onTypographyScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var fontDropdownExpanded by remember { mutableStateOf(false) }
    val selectedFont = preferences.parsedStudioFont

    TactileSurface(
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("studio_typography_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Studio Typography & Scale",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "10 curated system fonts + universal display & body scaling",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { fontDropdownExpanded = true },
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(18.dp),
                    border = glassSpecularBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_font_dropdown_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedFont.displayName,
                                fontFamily = studioDisplayFontFamily(selectedFont),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${selectedFont.subtitle} • \"${selectedFont.sampleHeadline}\"",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(Icons.Rounded.ExpandMore, contentDescription = "Choose font")
                    }
                }

                DropdownMenu(
                    expanded = fontDropdownExpanded,
                    onDismissRequest = { fontDropdownExpanded = false },
                    modifier = Modifier.testTag("studio_font_dropdown_menu")
                ) {
                    BackdropStudioFont.entries.forEach { fontOption ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = fontOption.displayName,
                                        fontFamily = studioDisplayFontFamily(fontOption),
                                        fontWeight = if (selectedFont == fontOption) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        text = fontOption.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            trailingIcon = {
                                if (selectedFont == fontOption) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            onClick = {
                                fontDropdownExpanded = false
                                onStudioFontChange(fontOption)
                            },
                            modifier = Modifier.testTag("studio_font_option_${fontOption.name}")
                        )
                    }
                }
            }

            StudioSliderDropdown(
                title = "Font Size",
                summary = "${(preferences.m3TypographyScale * 100).roundToInt()}% scale",
                testTag = "studio_typography_scale_dropdown"
            ) {
                StudioParamSlider(
                    label = "Font Size",
                    valueText = "${(preferences.m3TypographyScale * 100).roundToInt()}%",
                    value = preferences.m3TypographyScale,
                    valueRange = 0.85f..1.25f,
                    onValueChange = onTypographyScaleChange,
                    sliderTag = "settings_m3_typography"
                )
            }
        }
    }
}

/**
 * Hardware-aware Liquid Glass Tier, Glow Material customization, and Studio defaults.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StudioMaterialsAndTierCard(
    preferences: UserPreferencesDoc,
    onLiquidGlassTierChange: (LiquidGlassTier) -> Unit,
    onGlowMaterialChange: (GlowMaterialConfig) -> Unit,
    onRememberEditingChange: (Boolean) -> Unit,
    onScaleTextDefaultChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recommendedTier = remember { ZeroLagPerformanceEngine.recommendLiquidGlassTier(context) }
    val glow = preferences.parsedGlowMaterial
    var pendingHeavyTier by remember { mutableStateOf<LiquidGlassTier?>(null) }

    TactileSurface(
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("studio_materials_tier_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Liquid Glass Tier & Studio Behavior",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Recommended for this hardware: ${recommendedTier.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidGlassTier.entries.forEach { tier ->
                    val selected = preferences.parsedLiquidGlassTier == tier
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (tier.requiresHardwareWarning && !selected) {
                                pendingHeavyTier = tier
                            } else {
                                onLiquidGlassTierChange(tier)
                            }
                        },
                        label = { Text(tier.label) },
                        modifier = Modifier.testTag("settings_glass_tier_${tier.name}")
                    )
                }
            }

            if (preferences.parsedLiquidGlassTier != LiquidGlassTier.AUTO_RECOMMENDED) {
                OutlinedButton(
                    onClick = { onLiquidGlassTierChange(LiquidGlassTier.AUTO_RECOMMENDED) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("settings_restore_recommended_tier")
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore Recommended Tier (${recommendedTier.label})")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            StudioSliderDropdown(
                title = "Studio Glow Material Defaults",
                summary = "${(glow.intensity * 100).roundToInt()}% • ${glow.radiusDp.roundToInt()}dp radius",
                testTag = "studio_glow_defaults_dropdown"
            ) {
                StudioParamSlider(
                    label = "Glow Intensity",
                    valueText = "${(glow.intensity * 100).roundToInt()}%",
                    value = glow.intensity,
                    valueRange = 0.1f..1.5f,
                    onValueChange = { onGlowMaterialChange(glow.copy(intensity = it).sanitized()) },
                    sliderTag = "settings_glow_intensity"
                )
                StudioParamSlider(
                    label = "Glow Radius",
                    valueText = "${glow.radiusDp.roundToInt()}dp",
                    value = glow.radiusDp,
                    valueRange = 4f..42f,
                    onValueChange = { onGlowMaterialChange(glow.copy(radiusDp = it).sanitized()) },
                    sliderTag = "settings_glow_radius"
                )
                StudioParamSlider(
                    label = "Edge Halo",
                    valueText = "${(glow.edgeIllumination * 100).roundToInt()}%",
                    value = glow.edgeIllumination,
                    valueRange = 0f..1.0f,
                    onValueChange = { onGlowMaterialChange(glow.copy(edgeIllumination = it).sanitized()) },
                    sliderTag = "settings_glow_edge"
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("Remember Previous Editing Settings", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Carry over crop, zoom & filter edits when switching studio wallpapers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = preferences.rememberPreviousEditingSettings,
                    onCheckedChange = onRememberEditingChange,
                    modifier = Modifier.testTag("settings_remember_edits_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("Scale Text with Clock (Default)", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Scale date, weather & signature inside clock boxes by default",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = preferences.scaleTextWithClockDefault,
                    onCheckedChange = onScaleTextDefaultChange,
                    modifier = Modifier.testTag("settings_scale_text_clock_switch")
                )
            }
        }
    }

    pendingHeavyTier?.let { warningTier ->
        AlertDialog(
            onDismissRequest = { pendingHeavyTier = null },
            icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Enable ${warningTier.label}?") },
            text = {
                Text(
                    "Multi-pass high-precision optical shaders increase GPU workload, battery draw, and thermal output compared to your device's recommended tier (${recommendedTier.label})."
                )
            },
            dismissButton = {
                TextButton(onClick = { pendingHeavyTier = null }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLiquidGlassTierChange(warningTier)
                        pendingHeavyTier = null
                    },
                    modifier = Modifier.testTag("confirm_heavy_glass_tier")
                ) {
                    Text("Enable Anyway")
                }
            }
        )
    }
}

/**
 * Play-compliant On-Device Collection manager (Photo Picker + SAF OpenDocumentTree; zero storage permissions).
 */
@Composable
internal fun OnDeviceCollectionSettingsCard(
    preferences: UserPreferencesDoc,
    onDeviceCount: Int,
    onToggleEnabled: (Boolean) -> Unit,
    onAddPhotoUris: (List<String>) -> Unit,
    onAddFolderUri: (String) -> Unit,
    onRemoveFolderUri: (String) -> Unit,
    onRefreshScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { OnDeviceWallpaperRepository.takePersistableReadPermission(context, it, isTree = false) }
            onAddPhotoUris(uris.map { it.toString() })
        }
    }
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            OnDeviceWallpaperRepository.takePersistableReadPermission(context, treeUri, isTree = true)
            onAddFolderUri(treeUri.toString())
        }
    }

    TactileSurface(
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("on_device_collection_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                    Icon(Icons.Rounded.FolderCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(
                            text = "On Device Collection ($onDeviceCount)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Zero storage permissions • Hiding never deletes files from your device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = preferences.onDeviceCollectionEnabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag("on_device_collection_switch")
                )
            }

            AnimatedVisibility(visible = preferences.onDeviceCollectionEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("on_device_pick_photos_btn")
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Photos", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("on_device_add_folder_btn")
                        ) {
                            Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Folder", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(
                            onClick = onRefreshScan,
                            modifier = Modifier.testTag("on_device_refresh_btn")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh On-Device scan")
                        }
                    }

                    if (preferences.onDeviceFolderUris.isNotEmpty()) {
                        preferences.onDeviceFolderUris.forEach { folderUri ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = OnDeviceWallpaperRepository.formatUriLabel(folderUri),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(onClick = { onRemoveFolderUri(folderUri) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove folder")
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

/**
 * Offline Wallpaper Pack selector & download manager card inside Settings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OfflinePackSettingsCard(
    progress: OfflinePackProgress,
    selectedOption: OfflinePackSizeOption,
    onSelectOption: (OfflinePackSizeOption) -> Unit,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onCancel: () -> Unit,
    onClearPack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    TactileSurface(
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("offline_pack_settings_card")
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
                    imageVector = if (progress.isComplete) Icons.Rounded.CloudDone else Icons.Rounded.DownloadForOffline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Offline Wallpaper Pack",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val formattedSize = if (progress.bytesWritten > 0L) {
                        Formatter.formatShortFileSize(context, progress.bytesWritten)
                    } else {
                        "~${selectedOption.estimatedMb} MB estimated"
                    }
                    Text(
                        text = "${progress.completedCount}/${selectedOption.count} cached locally • $formattedSize",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OfflinePackSizeOption.entries.forEach { opt ->
                    FilterChip(
                        selected = selectedOption == opt,
                        onClick = { onSelectOption(opt) },
                        label = { Text("${opt.count} (~${opt.estimatedMb} MB)") },
                        modifier = Modifier.testTag("offline_pack_option_${opt.count}")
                    )
                }
            }

            if (progress.isDownloading || progress.completedCount > 0) {
                LinearProgressIndicator(
                    progress = { progress.fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                )
                if (progress.currentTitle.isNotBlank()) {
                    Text(
                        text = progress.currentTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (progress.isDownloading) {
                    FilledTonalButton(
                        onClick = if (progress.isPaused) onStartOrResume else onPause,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (progress.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (progress.isPaused) "Resume" else "Pause")
                    }
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                } else {
                    Button(
                        onClick = onStartOrResume,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("offline_pack_start_button")
                    ) {
                        Icon(Icons.Rounded.DownloadForOffline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (progress.completedCount > 0) "Sync / Top Up" else "Download Pack")
                    }
                    if (progress.completedCount > 0) {
                        OutlinedButton(
                            onClick = onClearPack,
                            modifier = Modifier.testTag("offline_pack_clear_button")
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }
        }
    }
}
