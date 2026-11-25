package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChargingAnimationConfig
import com.example.data.model.ChargingAnimationPreset
import com.example.data.model.ChargingPosition
import com.example.data.model.LockNotificationStyleConfig
import com.example.data.model.NotificationLayoutMode
import com.example.data.model.NotificationStylePreset
import com.example.data.model.UnlockButtonStyle
import com.example.data.model.UnlockButtonThemeConfig
import com.example.ui.components.StudioOptionPill
import com.example.ui.components.TactileSurface
import com.example.ui.theme.rememberStudioHaptics
import kotlin.math.roundToInt

@Composable
fun LockNotificationStyleCard(
    config: LockNotificationStyleConfig,
    onUpdate: (LockNotificationStyleConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberStudioHaptics()
    val safe = config.sanitized()
    val bgSwatches: List<Pair<String, Long?>> = listOf(
        "Preset" to null,
        "Obsidian" to 0xFF10131CL,
        "Slate" to 0xFF1C2333L,
        "Midnight" to 0xFF0E172BL,
        "Crimson" to 0xFF281219L,
        "Forest" to 0xFF10231BL
    )

    TactileSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("lockscreen_notif_style_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Notification Design & Layout", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(safe.preset.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("notif_preset_row")
            ) {
                items(NotificationStylePreset.entries, key = { it.name }) { preset ->
                    StudioOptionPill(
                        label = preset.label,
                        selected = safe.preset == preset,
                        onClick = {
                            haptics.performTick(22L)
                            onUpdate(
                                LockNotificationStyleConfig.fromPreset(preset).copy(
                                    layoutMode = safe.layoutMode,
                                    customBackgroundArgb = safe.customBackgroundArgb,
                                    compactTextHierarchy = safe.compactTextHierarchy,
                                    showTimestamp = safe.showTimestamp,
                                    maxVisibleGroups = safe.maxVisibleGroups
                                )
                            )
                        },
                        modifier = Modifier.testTag("notif_preset_${preset.name}")
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(NotificationLayoutMode.entries, key = { it.name }) { layout ->
                    StudioOptionPill(
                        label = layout.label,
                        selected = safe.layoutMode == layout,
                        onClick = {
                            haptics.performTick(18L)
                            onUpdate(safe.copy(layoutMode = layout))
                        },
                        modifier = Modifier.testTag("notif_layout_${layout.name}")
                    )
                }
            }

            // Custom background color pills
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(bgSwatches, key = { it.first }) { (label, argb) ->
                    val selected = safe.customBackgroundArgb == argb
                    Surface(
                        onClick = {
                            haptics.performTick(16L)
                            onUpdate(safe.copy(customBackgroundArgb = argb))
                        },
                        shape = RoundedCornerShape(50),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
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
                                    .background(argb?.let { Color(it.toInt()) } ?: MaterialTheme.colorScheme.primary)
                            )
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            StudioSliderDropdown(
                title = "Card Optics & Geometry",
                summary = "${(safe.transparency * 100).roundToInt()}% alpha • ${safe.cornerRadiusDp.roundToInt()}dp • ${safe.maxVisibleGroups} max",
                testTag = "notif_sliders_dropdown"
            ) {
                StudioParamSlider(
                    label = "Alpha",
                    valueText = "${(safe.transparency * 100).roundToInt()}%",
                    value = safe.transparency,
                    valueRange = 0.25f..0.98f,
                    onValueChange = { onUpdate(safe.copy(transparency = it)) },
                    sliderTag = "notif_alpha_slider"
                )
                StudioParamSlider(
                    label = "Blur",
                    valueText = "${(safe.blurIntensity * 100).roundToInt()}%",
                    value = safe.blurIntensity,
                    valueRange = 0f..1f,
                    onValueChange = { onUpdate(safe.copy(blurIntensity = it)) },
                    sliderTag = "notif_blur_slider"
                )
                StudioParamSlider(
                    label = "Radius",
                    valueText = "${safe.cornerRadiusDp.roundToInt()}dp",
                    value = safe.cornerRadiusDp,
                    valueRange = 8f..36f,
                    onValueChange = { onUpdate(safe.copy(cornerRadiusDp = it)) },
                    sliderTag = "notif_radius_slider"
                )
                StudioParamSlider(
                    label = "Gap",
                    valueText = "${safe.cardSpacingDp.roundToInt()}dp",
                    value = safe.cardSpacingDp,
                    valueRange = 4f..18f,
                    onValueChange = { onUpdate(safe.copy(cardSpacingDp = it)) },
                    sliderTag = "notif_spacing_slider"
                )
                StudioParamSlider(
                    label = "Icon",
                    valueText = "${safe.iconSizeDp.roundToInt()}dp",
                    value = safe.iconSizeDp,
                    valueRange = 22f..40f,
                    onValueChange = { onUpdate(safe.copy(iconSizeDp = it)) },
                    sliderTag = "notif_icon_size_slider"
                )
                StudioParamSlider(
                    label = "Shadow",
                    valueText = "${(safe.shadowIntensity * 100).roundToInt()}%",
                    value = safe.shadowIntensity,
                    valueRange = 0f..1f,
                    onValueChange = { onUpdate(safe.copy(shadowIntensity = it)) },
                    sliderTag = "notif_shadow_slider"
                )
                StudioParamSlider(
                    label = "Count",
                    valueText = "${safe.maxVisibleGroups}",
                    value = safe.maxVisibleGroups.toFloat(),
                    valueRange = 1f..8f,
                    onValueChange = { onUpdate(safe.copy(maxVisibleGroups = it.roundToInt().coerceIn(1, 8))) },
                    sliderTag = "notif_count_slider"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Compact Text Hierarchy", style = MaterialTheme.typography.labelLarge)
                Switch(
                    checked = safe.compactTextHierarchy,
                    onCheckedChange = { onUpdate(safe.copy(compactTextHierarchy = it)) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Notification Timestamps", style = MaterialTheme.typography.labelLarge)
                Switch(
                    checked = safe.showTimestamp,
                    onCheckedChange = { onUpdate(safe.copy(showTimestamp = it)) }
                )
            }
        }
    }
}

@Composable
fun ChargingAnimationCustomizerCard(
    config: ChargingAnimationConfig,
    onUpdate: (ChargingAnimationConfig) -> Unit,
    onTestChargingPreview: () -> Unit,
    bottomPortWaveEnabled: Boolean = false,
    onSetBottomPortWaveEnabled: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptics = rememberStudioHaptics()
    val accentSwatches = listOf(0xFF4CD964L, 0xFF64B5F6L, 0xFFB388FFL, 0xFFFF8A65L, 0xFFF48FB1L)

    TactileSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("lockscreen_charging_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Charging Overlay Animation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(config.preset.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { onUpdate(config.copy(enabled = it)) },
                    modifier = Modifier.testTag("charging_anim_switch")
                )
            }

            AnimatedVisibility(visible = config.enabled) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ChargingAnimationPreset.entries, key = { it.name }) { preset ->
                            StudioOptionPill(
                                label = preset.label,
                                selected = config.preset == preset,
                                onClick = {
                                    haptics.performTick(18L)
                                    onUpdate(config.copy(preset = preset))
                                },
                                modifier = Modifier.testTag("charging_preset_${preset.name}")
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChargingPosition.entries.forEach { pos ->
                            StudioOptionPill(
                                label = "${pos.label} Position",
                                selected = config.position == pos,
                                onClick = { onUpdate(config.copy(position = pos)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    StudioSliderDropdown(
                        title = "Speed & Scale Tuning",
                        summary = "${String.format(java.util.Locale.US, "%.1fx", config.speedMultiplier)} speed • ${(config.sizeScale * 100).roundToInt()}% size",
                        testTag = "charging_sliders_dropdown"
                    ) {
                        StudioParamSlider(
                            label = "Speed",
                            valueText = "${String.format(java.util.Locale.US, "%.1fx", config.speedMultiplier)}",
                            value = config.speedMultiplier,
                            valueRange = 0.5f..2.0f,
                            onValueChange = { onUpdate(config.copy(speedMultiplier = it)) },
                            sliderTag = "charging_speed_slider"
                        )
                        StudioParamSlider(
                            label = "Size",
                            valueText = "${(config.sizeScale * 100).roundToInt()}%",
                            value = config.sizeScale,
                            valueRange = 0.65f..1.5f,
                            onValueChange = { onUpdate(config.copy(sizeScale = it)) },
                            sliderTag = "charging_size_slider"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dynamic Wallpaper Accent", style = MaterialTheme.typography.labelLarge)
                        Switch(
                            checked = config.dynamicAccentColor,
                            onCheckedChange = { onUpdate(config.copy(dynamicAccentColor = it)) }
                        )
                    }

                    if (!config.dynamicAccentColor) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            accentSwatches.forEach { argb ->
                                val selected = config.customAccentArgb == argb
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(argb.toInt()))
                                        .clickable { onUpdate(config.copy(customAccentArgb = argb)) }
                                        .let {
                                            if (selected) it.padding(2.dp) else it
                                        }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Percentage Text", style = MaterialTheme.typography.labelLarge)
                        Switch(
                            checked = config.showPercentage,
                            onCheckedChange = { onUpdate(config.copy(showPercentage = it)) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Charging Status", style = MaterialTheme.typography.labelLarge)
                        Switch(
                            checked = config.showStatusText,
                            onCheckedChange = { onUpdate(config.copy(showStatusText = it)) }
                        )
                    }

                    FilledTonalButton(
                        onClick = onTestChargingPreview,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("test_charging_preview_btn")
                    ) {
                        Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Preview Charging Animation")
                    }
                }
            }

            if (onSetBottomPortWaveEnabled != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text("Bottom Port Wave", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Luminous surge rising from the bottom charging port",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = bottomPortWaveEnabled,
                        onCheckedChange = {
                            haptics.performTick(18L)
                            onSetBottomPortWaveEnabled(it)
                        },
                        modifier = Modifier.testTag("charging_bottom_port_wave_switch")
                    )
                }
            }
        }
    }
}

@Composable
fun UnlockButtonThemeCard(
    config: UnlockButtonThemeConfig,
    onUpdate: (UnlockButtonThemeConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberStudioHaptics()
    val customContainers = listOf(0xFF1F2430L, 0xFFFFFFFFL, 0xFF3F51B5L, 0xFF00695CL, 0xFF6A1B9AL)

    TactileSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("unlock_button_theme_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Unlock Button Appearance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Match Material 3, Frosted OS glass, or custom lock colours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(UnlockButtonStyle.entries, key = { it.name }) { style ->
                    StudioOptionPill(
                        label = style.label,
                        selected = config.style == style,
                        onClick = {
                            haptics.performTick(16L)
                            onUpdate(config.copy(style = style))
                        },
                        modifier = Modifier.testTag("unlock_style_${style.name}")
                    )
                }
            }

            if (config.style == UnlockButtonStyle.CUSTOM) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    customContainers.forEach { argb ->
                        val isLightBg = argb == 0xFFFFFFFFL
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(argb.toInt()))
                                .clickable {
                                    onUpdate(
                                        config.copy(
                                            customContainerArgb = argb,
                                            customContentArgb = if (isLightBg) 0xFF10131CL else 0xFFFFFFFFL
                                        )
                                    )
                                }
                        )
                    }
                }
            }

            StudioSliderDropdown(
                title = "Button Shape & Opacity",
                summary = "${(config.transparency * 100).roundToInt()}% alpha • ${config.cornerRadiusDp.roundToInt()}dp corner",
                testTag = "unlock_sliders_dropdown"
            ) {
                StudioParamSlider(
                    label = "Alpha",
                    valueText = "${(config.transparency * 100).roundToInt()}%",
                    value = config.transparency,
                    valueRange = 0.20f..1.0f,
                    onValueChange = { onUpdate(config.copy(transparency = it)) },
                    sliderTag = "unlock_alpha_slider"
                )
                StudioParamSlider(
                    label = "Corner",
                    valueText = "${config.cornerRadiusDp.roundToInt()}dp",
                    value = config.cornerRadiusDp,
                    valueRange = 8f..32f,
                    onValueChange = { onUpdate(config.copy(cornerRadiusDp = it)) },
                    sliderTag = "unlock_radius_slider"
                )
            }
        }
    }
}
