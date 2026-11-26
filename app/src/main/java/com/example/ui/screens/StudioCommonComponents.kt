package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint as AndroidPaint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.example.data.media.WallpaperTarget
import com.example.data.model.UiStyleMode
import com.example.ui.components.TactileSurface
import com.example.ui.theme.AppMotion
import com.example.ui.theme.LocalDarkThemeFlag
import com.example.ui.theme.LocalReducedMotionFlag
import com.example.ui.theme.LocalUiStyleMode
import com.example.ui.theme.rememberStudioHaptics
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Image Processing Engine V5 — Sub-Engine A: Hardware-accelerated 18-Look Color Matrix Pipeline.
 * Shared 1:1 between Jetpack Compose ColorFilter, rendered wallpaper bitmaps, and Liquid Glass refraction patches.
 */
internal object CanvasLookFilterSubEngine {
    fun colorMatrixArrayFor(filter: CanvasLookFilter): FloatArray? = when (filter) {
        CanvasLookFilter.ORIGINAL -> null
        CanvasLookFilter.WARM_FILM -> floatArrayOf(
            1.06f, 0.04f, 0f, 0f, 10f,
            0f, 1.01f, 0f, 0f, 4f,
            0f, 0f, 0.90f, 0f, -6f,
            0f, 0f, 0f, 1f, 0f
        )
        CanvasLookFilter.COOL_MIST -> floatArrayOf(
            0.92f, 0f, 0.04f, 0f, -4f,
            0f, 1.00f, 0.04f, 0f, 2f,
            0.03f, 0.04f, 1.08f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        )
        CanvasLookFilter.CRISP_MONO -> floatArrayOf(
            0.32f, 0.62f, 0.12f, 0f, -6f,
            0.32f, 0.62f, 0.12f, 0f, -6f,
            0.32f, 0.62f, 0.12f, 0f, -6f,
            0f, 0f, 0f, 1f, 0f
        )
        CanvasLookFilter.VELVET_MATTE -> floatArrayOf(
            0.88f, 0f, 0f, 0f, 14f,
            0f, 0.88f, 0f, 0f, 14f,
            0f, 0f, 0.90f, 0f, 16f,
            0f, 0f, 0f, 1f, 0f
        )
        CanvasLookFilter.SOLAR_AMBER -> floatArrayOf(
            1.14f, 0.06f, 0f, 0f, 12f,
            0.04f, 1.02f, 0f, 0f, 4f,
            0f, 0f, 0.78f, 0f, -14f,
            0f, 0f, 0f, 1f, 0f
        )
        CanvasLookFilter.ARCTIC_CYAN -> floatArrayOf(
            0.84f, 0.02f, 0.04f, 0f, -8f,
            0f, 1.06f, 0.06f, 0f, 6f,
            0.02f, 0.08f, 1.14f, 0f, 14f,
            0f, 0f, 0f, 1f, 0f
        )
        CanvasLookFilter.NEON_DUSK -> floatArrayOf(
            1.12f, 0f, 0.10f, 0f, 4f,
            0f, 0.88f, 0.04f, 0f, -6f,
            0.08f, 0f, 1.18f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        )
        CanvasLookFilter.EMERALD_CHROME -> floatArrayOf(
            0.88f, 0.06f, 0f, 0f, -4f,
            0.02f, 1.10f, 0.02f, 0f, 6f,
            0f, 0.04f, 0.98f, 0f, 2f,
            0f, 0f, 0f, 1f, 0f
        )
        CanvasLookFilter.DESERT_SEPIA -> floatArrayOf(
            0.44f, 0.66f, 0.14f, 0f, 12f,
            0.34f, 0.54f, 0.12f, 0f, 4f,
            0.22f, 0.36f, 0.10f, 0f, -6f,
            0f, 0f, 0f, 1f, 0f
        )
        CanvasLookFilter.PASTEL_BLOOM -> floatArrayOf(
            0.94f, 0.06f, 0.04f, 0f, 18f,
            0.04f, 0.94f, 0.06f, 0f, 16f,
            0.04f, 0.04f, 0.96f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        )
        CanvasLookFilter.MIDNIGHT_BLUE -> floatArrayOf(
            0.82f, 0f, 0.02f, 0f, -14f,
            0f, 0.90f, 0.04f, 0f, -8f,
            0.04f, 0.08f, 1.16f, 0f, 12f,
            0f, 0f, 0f, 1f, 0f
        )
        CanvasLookFilter.MOSS_OLIVE -> floatArrayOf(
            0.96f, 0.08f, 0f, 0f, -2f,
            0.06f, 1.02f, 0f, 0f, 4f,
            0f, 0f, 0.80f, 0f, -8f,
            0f, 0f, 0f, 1f, 0f
        )
        CanvasLookFilter.HIGH_KEY -> floatArrayOf(
            1.08f, 0f, 0f, 0f, 18f,
            0f, 1.08f, 0f, 0f, 18f,
            0f, 0f, 1.08f, 0f, 18f,
            0f, 0f, 0f, 1f, 0f
        )
        CanvasLookFilter.DARK_CINEMA -> floatArrayOf(
            1.10f, 0f, 0f, 0f, -18f,
            0f, 1.06f, 0.02f, 0f, -16f,
            0.02f, 0.04f, 1.08f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
        )
        CanvasLookFilter.CORAL_SUNSET -> floatArrayOf(
            1.14f, 0.04f, 0.02f, 0f, 12f,
            0.02f, 0.96f, 0.02f, 0f, 2f,
            0.04f, 0f, 0.92f, 0f, 4f,
            0f, 0f, 0f, 1f, 0f
        )
        CanvasLookFilter.VINTAGE_FADED -> floatArrayOf(
            0.90f, 0.06f, 0.02f, 0f, 14f,
            0.03f, 0.88f, 0.04f, 0f, 12f,
            0.02f, 0.04f, 0.84f, 0f, 16f,
            0f, 0f, 0f, 1f, 0f
        )
        CanvasLookFilter.PURE_CONTRAST -> floatArrayOf(
            1.24f, 0f, 0f, 0f, -24f,
            0f, 1.24f, 0f, 0f, -24f,
            0f, 0f, 1.24f, 0f, -24f,
            0f, 0f, 0f, 1f, 0f
        )
    }
}

enum class CanvasLookFilter(val label: String) {
    ORIGINAL("Original"),
    WARM_FILM("Warm Film"),
    COOL_MIST("Cool Mist"),
    CRISP_MONO("Noir Mono"),
    VELVET_MATTE("Velvet Matte"),
    SOLAR_AMBER("Solar Amber"),
    ARCTIC_CYAN("Arctic Cyan"),
    NEON_DUSK("Neon Dusk"),
    EMERALD_CHROME("Emerald Chrome"),
    DESERT_SEPIA("Desert Sepia"),
    PASTEL_BLOOM("Pastel Bloom"),
    MIDNIGHT_BLUE("Midnight Blue"),
    MOSS_OLIVE("Moss Olive"),
    HIGH_KEY("High Key"),
    DARK_CINEMA("Dark Cinema"),
    CORAL_SUNSET("Coral Sunset"),
    VINTAGE_FADED("Vintage Faded"),
    PURE_CONTRAST("Pure Contrast");

    fun toColorMatrixArray(): FloatArray? = CanvasLookFilterSubEngine.colorMatrixArrayFor(this)

    fun toComposeColorFilter(): ColorFilter? =
        toColorMatrixArray()?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
}

data class NormalizedCropRect(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
)

/**
 * Pure geometry solver for 1:1 Lock Screen centered framing and optional wider Home parallax crop.
 * Prevents Android WallpaperManager from drifting off-center when aspect ratios differ.
 */
internal fun computeCenteredViewportCropRect(
    sourceWidth: Int,
    sourceHeight: Int,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    targetScreenAspect: Float,
    allowParallaxWidth: Boolean = false
): NormalizedCropRect {
    val safeW = sourceWidth.coerceAtLeast(1)
    val safeH = sourceHeight.coerceAtLeast(1)
    val srcW = safeW.toFloat()
    val srcH = safeH.toFloat()
    val srcAspect = srcW / srcH

    val clampedTargetAspect = targetScreenAspect.coerceIn(0.36f, 2.2f)
    val fitAspect = if (allowParallaxWidth) {
        (clampedTargetAspect * 1.24f).coerceAtMost(max(srcAspect, clampedTargetAspect))
    } else {
        clampedTargetAspect
    }

    val safeZoom = zoomScale.coerceIn(1f, 4f)
    val baseBoxW: Float
    val baseBoxH: Float
    if (srcAspect > fitAspect) {
        baseBoxH = srcH
        baseBoxW = (srcH * fitAspect).coerceAtMost(srcW)
    } else {
        baseBoxW = srcW
        baseBoxH = (srcW / fitAspect).coerceAtMost(srcH)
    }

    val cropW = (baseBoxW / safeZoom).coerceIn(min(64f, srcW), srcW)
    val cropH = (baseBoxH / safeZoom).coerceIn(min(64f, srcH), srcH)

    val effectiveScale = if (viewportWidthPx > 0f && viewportHeightPx > 0f) {
        max(viewportWidthPx / srcW, viewportHeightPx / srcH) * safeZoom
    } else {
        1f
    }

    val rawCenterX = if (viewportWidthPx > 0f && effectiveScale > 0f) {
        srcW * 0.5f - (panOffsetX / effectiveScale)
    } else {
        srcW * 0.5f
    }
    val rawCenterY = if (viewportHeightPx > 0f && effectiveScale > 0f) {
        srcH * 0.5f - (panOffsetY / effectiveScale)
    } else {
        srcH * 0.5f
    }

    val minCenterX = cropW * 0.5f
    val maxCenterX = (srcW - cropW * 0.5f).coerceAtLeast(minCenterX)
    val minCenterY = cropH * 0.5f
    val maxCenterY = (srcH - cropH * 0.5f).coerceAtLeast(minCenterY)

    val clampedCenterX = rawCenterX.coerceIn(minCenterX, maxCenterX)
    val clampedCenterY = rawCenterY.coerceIn(minCenterY, maxCenterY)

    val intW = cropW.roundToInt().coerceIn(1, safeW)
    val intH = cropH.roundToInt().coerceIn(1, safeH)
    val maxLeft = (safeW - intW).coerceAtLeast(0)
    val maxTop = (safeH - intH).coerceAtLeast(0)
    val left = (clampedCenterX - intW * 0.5f).roundToInt().coerceIn(0, maxLeft)
    val top = (clampedCenterY - intH * 0.5f).roundToInt().coerceIn(0, maxTop)

    return NormalizedCropRect(
        left = left,
        top = top,
        width = intW,
        height = intH
    )
}

internal fun resolveDevicePortraitAspect(context: Context): Float {
    val metrics = context.resources.displayMetrics
    val minDim = min(metrics.widthPixels, metrics.heightPixels).coerceAtLeast(1)
    val maxDim = max(metrics.widthPixels, metrics.heightPixels).coerceAtLeast(1)
    return (minDim.toFloat() / maxDim.toFloat()).coerceIn(0.40f, 0.78f)
}

@Composable
internal fun StudioWallpaperTargetDialog(
    title: String,
    syncHomeWithLockMode: Boolean = false,
    onSyncHomeWithLockModeChange: ((Boolean) -> Unit)? = null,
    onSaveToLockScreenMode: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSelectTarget: (WallpaperTarget, Boolean) -> Unit
) {
    var localSyncHomeWithLock by remember(syncHomeWithLockMode) { mutableStateOf(syncHomeWithLockMode) }
    val uiStyle = LocalUiStyleMode.current
    val isGlass = uiStyle == UiStyleMode.FROSTED_GLASS_OS
    val isGlyph = uiStyle == UiStyleMode.GLYPH_UI

    Dialog(onDismissRequest = onDismiss) {
        TactileSurface(
            containerColor = when {
                isGlyph -> MaterialTheme.colorScheme.surfaceContainerLow
                isGlass -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.86f)
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            },
            glassTintAlpha = if (isGlass) 0.78f else 1f,
            shape = RoundedCornerShape(
                when {
                    isGlyph -> 20.dp
                    isGlass -> 30.dp
                    else -> 28.dp
                }
            ),
            tonalElevation = if (isGlass || isGlyph) 0.dp else 6.dp,
            border = when {
                isGlyph -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                isGlass -> BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f))
                else -> null
            },
            modifier = Modifier
                .widthIn(max = 410.dp)
                .fillMaxWidth()
                .testTag("studio_wallpaper_target_dialog")
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isGlass) 0.72f else 1f),
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Wallpaper,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Apply Canvas",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "\"$title\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (isGlass) {
                        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    shape = RoundedCornerShape(20.dp),
                    border = if (isGlass) {
                        BorderStroke(0.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Sync with Lock Screen Mode",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Matches Home & Lock Mode canvas framing seamlessly",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = localSyncHomeWithLock,
                            onCheckedChange = { checked ->
                                localSyncHomeWithLock = checked
                                onSyncHomeWithLockModeChange?.invoke(checked)
                            },
                            modifier = Modifier.testTag("sync_home_lock_mode_switch")
                        )
                    }
                }

                Button(
                    onClick = {
                        if (localSyncHomeWithLock) {
                            onSaveToLockScreenMode?.invoke()
                        }
                        onSelectTarget(WallpaperTarget.HOME, false)
                    },
                    shape = if (isGlass) RoundedCornerShape(22.dp) else CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apply_target_home_button")
                ) {
                    Icon(Icons.Rounded.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Home Screen")
                }

                FilledTonalButton(
                    onClick = { onSelectTarget(WallpaperTarget.LOCK, false) },
                    shape = if (isGlass) RoundedCornerShape(22.dp) else CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apply_target_lock_button")
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lock Screen")
                }

                OutlinedButton(
                    onClick = {
                        if (localSyncHomeWithLock) {
                            onSaveToLockScreenMode?.invoke()
                        }
                        onSelectTarget(WallpaperTarget.BOTH, false)
                    },
                    shape = if (isGlass) RoundedCornerShape(22.dp) else CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apply_target_both_button")
                ) {
                    Icon(Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Home & Lock Screens")
                }

                if (onSaveToLockScreenMode != null) {
                    FilledTonalButton(
                        onClick = {
                            onSaveToLockScreenMode()
                            onDismiss()
                        },
                        shape = if (isGlass) RoundedCornerShape(22.dp) else CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_to_lock_mode_button")
                    ) {
                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save to Lock Screen Mode")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
internal fun StudioSliderDropdown(
    title: String,
    summary: String,
    testTag: String,
    modifier: Modifier = Modifier,
    expandedByDefault: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(expandedByDefault) }
    val uiStyle = LocalUiStyleMode.current
    val reducedMotion = LocalReducedMotionFlag.current
    val isGlass = uiStyle == UiStyleMode.FROSTED_GLASS_OS

    TactileSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = if (isGlass) 0.72f else 1f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("${testTag}_toggle"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (summary.isNotBlank()) {
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = AppMotion.drawerEnter(uiStyle, reducedMotion),
                exit = AppMotion.drawerExit(uiStyle, reducedMotion)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
internal fun AdaptiveDesignSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    thumbColorOverride: Color? = null,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val uiStyle = LocalUiStyleMode.current
    when (uiStyle) {
        UiStyleMode.FROSTED_GLASS_OS -> {
            LiquidGlassSlider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                thumbColorOverride = thumbColorOverride,
                onValueChangeFinished = onValueChangeFinished,
                modifier = modifier
            )
        }
        UiStyleMode.GLYPH_UI -> {
            GlyphSteppedDotSlider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                thumbColorOverride = thumbColorOverride,
                onValueChangeFinished = onValueChangeFinished,
                modifier = modifier
            )
        }
        UiStyleMode.MATERIAL_3 -> {
            val haptics = rememberStudioHaptics()
            val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-4f)
            val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
            var lastDetentBucket by remember { mutableIntStateOf((fraction * 10f).roundToInt()) }
            Slider(
                value = value.coerceIn(valueRange.start, valueRange.endInclusive),
                onValueChange = { next ->
                    val f = ((next - valueRange.start) / span).coerceIn(0f, 1f)
                    val bucket = (f * 10f).roundToInt()
                    if (bucket != lastDetentBucket) {
                        lastDetentBucket = bucket
                        haptics.performTick(45L)
                    }
                    onValueChange(next)
                },
                valueRange = valueRange,
                onValueChangeFinished = onValueChangeFinished,
                colors = SliderDefaults.colors(
                    thumbColor = thumbColorOverride ?: MaterialTheme.colorScheme.primary,
                    activeTrackColor = thumbColorOverride ?: MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ),
                modifier = modifier
            )
        }
    }
}

@Composable
internal fun GlyphSteppedDotSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    thumbColorOverride: Color? = null,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val haptics = rememberStudioHaptics()
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-4f)
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    var lastDetentBucket by remember { mutableIntStateOf((fraction * 12f).roundToInt()) }

    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnFinished by rememberUpdatedState(onValueChangeFinished)
    val latestValueRange by rememberUpdatedState(valueRange)

    BoxWithConstraints(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .height(34.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = value.coerceIn(valueRange.start, valueRange.endInclusive),
                    range = valueRange
                )
                setProgress { targetVal ->
                    onValueChange(targetVal.coerceIn(valueRange.start, valueRange.endInclusive))
                    true
                }
            }
            .pointerInput(valueRange) {
                val widthPx = size.width.toFloat().coerceAtLeast(1f)
                val thumbPx = 10.dp.toPx()
                val usableW = (widthPx - thumbPx * 2f).coerceAtLeast(1f)
                fun dispatch(xPx: Float) {
                    val f = ((xPx - thumbPx) / usableW).coerceIn(0f, 1f)
                    val rng = latestValueRange
                    val next = (rng.start + (rng.endInclusive - rng.start) * f).coerceIn(rng.start, rng.endInclusive)
                    val bucket = (f * 12f).roundToInt()
                    if (bucket != lastDetentBucket) {
                        lastDetentBucket = bucket
                        haptics.performTick(35L)
                    }
                    latestOnValueChange(next)
                }
                detectTapGestures { offset ->
                    dispatch(offset.x)
                    latestOnFinished?.invoke()
                }
            }
            .pointerInput(valueRange) {
                val widthPx = size.width.toFloat().coerceAtLeast(1f)
                val thumbPx = 10.dp.toPx()
                val usableW = (widthPx - thumbPx * 2f).coerceAtLeast(1f)
                fun dispatch(xPx: Float) {
                    val f = ((xPx - thumbPx) / usableW).coerceIn(0f, 1f)
                    val rng = latestValueRange
                    val next = (rng.start + (rng.endInclusive - rng.start) * f).coerceIn(rng.start, rng.endInclusive)
                    val bucket = (f * 12f).roundToInt()
                    if (bucket != lastDetentBucket) {
                        lastDetentBucket = bucket
                        haptics.performTick(35L)
                    }
                    latestOnValueChange(next)
                }
                detectDragGestures(
                    onDragStart = { dispatch(it.x) },
                    onDrag = { change, _ ->
                        change.consume()
                        dispatch(change.position.x)
                    },
                    onDragEnd = { latestOnFinished?.invoke() }
                )
            }
    ) {
        val density = LocalDensity.current
        val totalPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val thumbW = 14.dp
        val thumbPx = with(density) { thumbW.toPx() }
        val usablePx = (totalPx - thumbPx).coerceAtLeast(0f)
        val thumbX = (usablePx * fraction).roundToInt()
        val dotCount = 13

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(dotCount) { idx ->
                val dotFraction = idx.toFloat() / (dotCount - 1).toFloat()
                val filled = dotFraction <= fraction + 0.02f
                Box(
                    modifier = Modifier
                        .size(if (filled) 4.dp else 3.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) (thumbColorOverride ?: MaterialTheme.colorScheme.primary)
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(x = thumbX, y = 0) }
                .size(width = thumbW, height = 20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(thumbColorOverride ?: MaterialTheme.colorScheme.primary)
                .border(1.dp, MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(6.dp))
        )
    }
}

/**
 * Tactile optical capsule slider unique to Liquid Glass mode:
 * - Translucent glass capsule track that gently swells vertically while actively dragged.
 * - Refracted gradient active span + specular top rim.
 * - Morphing frosted droplet thumb with subtle stretch & haptic detents across notches.
 */
@Composable
internal fun LiquidGlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    thumbColorOverride: Color? = null,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val isDark = LocalDarkThemeFlag.current
    val reducedMotion = LocalReducedMotionFlag.current
    val customTokens = com.example.ui.theme.LocalThemeCustomization.current
    val haptics = rememberStudioHaptics()

    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-4f)
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    var isPressedOrDragging by remember { mutableStateOf(false) }
    var lastDetentBucket by remember { mutableIntStateOf((fraction * 10f).roundToInt()) }

    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnFinished by rememberUpdatedState(onValueChangeFinished)
    val latestValueRange by rememberUpdatedState(valueRange)
    val latestHaptics by rememberUpdatedState(haptics)

    val trackHeight by animateDpAsState(
        targetValue = if (isPressedOrDragging) 14.dp else 10.dp,
        animationSpec = AppMotion.spatialSpring(UiStyleMode.FROSTED_GLASS_OS, reducedMotion, customTokens.m3MotionSpeed),
        label = "liquid_slider_track_h"
    )
    val dropletScaleX by animateFloatAsState(
        targetValue = if (isPressedOrDragging && !reducedMotion) 1.24f else 1.0f,
        animationSpec = AppMotion.emphasisSpring(UiStyleMode.FROSTED_GLASS_OS, reducedMotion, customTokens.m3MotionSpeed),
        label = "liquid_slider_droplet_sx"
    )
    val dropletScaleY by animateFloatAsState(
        targetValue = if (isPressedOrDragging && !reducedMotion) 1.12f else 1.0f,
        animationSpec = AppMotion.emphasisSpring(UiStyleMode.FROSTED_GLASS_OS, reducedMotion, customTokens.m3MotionSpeed),
        label = "liquid_slider_droplet_sy"
    )

    val trackBgAlpha = (0.70f + (1f - customTokens.glassTransparencyLevel) * 0.24f).coerceIn(0.42f, 0.94f)
    val trackBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = trackBgAlpha)
    } else {
        Color.White.copy(alpha = trackBgAlpha)
    }
    val rimMul = customTokens.glassSpecularHighlights.coerceIn(0.35f, 1.5f)
    val rimBrush = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = ((if (isDark) 0.45f else 0.92f) * rimMul).coerceIn(0.18f, 0.98f)),
            MaterialTheme.colorScheme.primary.copy(alpha = (0.36f * rimMul).coerceIn(0.15f, 0.72f))
        )
    )
    val activeFillBrush = Brush.horizontalGradient(
        listOf(
            (thumbColorOverride ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.88f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.92f)
        )
    )

    BoxWithConstraints(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .height(34.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = value.coerceIn(valueRange.start, valueRange.endInclusive),
                    range = valueRange
                )
                setProgress { targetVal ->
                    val clamped = targetVal.coerceIn(valueRange.start, valueRange.endInclusive)
                    onValueChange(clamped)
                    true
                }
            }
            .pointerInput(valueRange) {
                val widthPx = size.width.toFloat().coerceAtLeast(1f)
                val thumbRadiusPx = 11.dp.toPx()
                val usableW = (widthPx - thumbRadiusPx * 2f).coerceAtLeast(1f)

                fun dispatchX(xPx: Float) {
                    val f = ((xPx - thumbRadiusPx) / usableW).coerceIn(0f, 1f)
                    val rng = latestValueRange
                    val newVal = (rng.start + (rng.endInclusive - rng.start) * f)
                        .coerceIn(rng.start, rng.endInclusive)
                    val bucket = (f * 10f).roundToInt()
                    if (bucket != lastDetentBucket) {
                        lastDetentBucket = bucket
                        latestHaptics.performTick(45L)
                    }
                    latestOnValueChange(newVal)
                }

                detectTapGestures { offset ->
                    isPressedOrDragging = true
                    dispatchX(offset.x)
                    isPressedOrDragging = false
                    latestOnFinished?.invoke()
                }
            }
            .pointerInput(valueRange) {
                val widthPx = size.width.toFloat().coerceAtLeast(1f)
                val thumbRadiusPx = 11.dp.toPx()
                val usableW = (widthPx - thumbRadiusPx * 2f).coerceAtLeast(1f)

                fun dispatchX(xPx: Float) {
                    val f = ((xPx - thumbRadiusPx) / usableW).coerceIn(0f, 1f)
                    val rng = latestValueRange
                    val newVal = (rng.start + (rng.endInclusive - rng.start) * f)
                        .coerceIn(rng.start, rng.endInclusive)
                    val bucket = (f * 10f).roundToInt()
                    if (bucket != lastDetentBucket) {
                        lastDetentBucket = bucket
                        latestHaptics.performTick(45L)
                    }
                    latestOnValueChange(newVal)
                }

                detectDragGestures(
                    onDragStart = { startOffset ->
                        isPressedOrDragging = true
                        dispatchX(startOffset.x)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        dispatchX(change.position.x)
                    },
                    onDragEnd = {
                        isPressedOrDragging = false
                        latestOnFinished?.invoke()
                    },
                    onDragCancel = {
                        isPressedOrDragging = false
                    }
                )
            }
    ) {
        val density = LocalDensity.current
        val totalPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val thumbSizeDp = 22.dp
        val thumbPx = with(density) { thumbSizeDp.toPx() }
        val usablePx = (totalPx - thumbPx).coerceAtLeast(0f)
        val thumbX = (usablePx * fraction).roundToInt()
        val fillWidthDp = with(density) { ((usablePx * fraction) + thumbPx * 0.5f).coerceIn(8f, totalPx).toDp() }

        // Outer glass capsule track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(50))
                .background(trackBg)
                .border(BorderStroke(1.dp, rimBrush), RoundedCornerShape(50))
                .testTag("liquid_glass_slider_track")
        ) {
            // Active refracted span
            Box(
                modifier = Modifier
                    .width(fillWidthDp)
                    .height(trackHeight)
                    .clip(RoundedCornerShape(50))
                    .background(activeFillBrush)
            )
        }

        // Morphing frosted droplet thumb
        Surface(
            color = (thumbColorOverride ?: Color.White).copy(alpha = if (isPressedOrDragging) 0.96f else 0.92f),
            shape = CircleShape,
            border = BorderStroke(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White,
                        (thumbColorOverride ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.72f)
                    )
                )
            ),
            shadowElevation = if (isPressedOrDragging) 10.dp else 5.dp,
            modifier = Modifier
                .offset { IntOffset(x = thumbX, y = 0) }
                .size(thumbSizeDp)
                .graphicsLayer {
                    scaleX = dropletScaleX
                    scaleY = dropletScaleY
                }
        ) {}
    }
}

@Composable
internal fun StudioParamSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    sliderTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(56.dp)
        )
        AdaptiveDesignSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .weight(1f)
                .testTag(sliderTag)
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(42.dp)
        )
    }
}

internal fun parseHexOrNull(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        Color(hex.trim().toColorInt())
    } catch (_: Exception) {
        null
    }
}

/**
 * Image Processing Engine V5 — Sub-Engine B: Viewport Crop & ARGB_8888 Look Export.
 * Bakes centered crop, optional Home parallax width, rotation, dim vignette, and look matrix cleanly.
 */
internal object ViewportCropExportSubEngine {
    fun renderEditedBitmap(
        source: Bitmap,
        zoomScale: Float,
        panOffset: Offset,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        dimFraction: Float,
        lookFilter: CanvasLookFilter,
        targetScreenAspect: Float,
        allowParallaxWidth: Boolean,
        rotationDegrees: Float
    ): Bitmap {
        if (source.isRecycled || source.width <= 0 || source.height <= 0) {
            return createBitmap(64, 64)
        }
        val crop = computeCenteredViewportCropRect(
            sourceWidth = source.width,
            sourceHeight = source.height,
            zoomScale = zoomScale,
            panOffsetX = panOffset.x,
            panOffsetY = panOffset.y,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
            targetScreenAspect = targetScreenAspect,
            allowParallaxWidth = allowParallaxWidth
        )

        val normRot = ((rotationDegrees % 360f) + 360f) % 360f
        val hasRotation = normRot > 0.25f && normRot < 359.75f

        val cropped = if (!hasRotation && crop.left == 0 && crop.top == 0 && crop.width == source.width && crop.height == source.height) {
            source
        } else if (!hasRotation) {
            Bitmap.createBitmap(source, crop.left, crop.top, crop.width, crop.height)
        } else {
            val out = createBitmap(crop.width, crop.height)
            val canvas = AndroidCanvas(out)
            val matrix = android.graphics.Matrix()
            val scale = max(crop.width.toFloat() / source.width.toFloat(), crop.height.toFloat() / source.height.toFloat()) *
                zoomScale.coerceIn(1f, 4f)
            val panRatioX = if (viewportWidthPx > 0f) panOffset.x * (crop.width / viewportWidthPx) else 0f
            val panRatioY = if (viewportHeightPx > 0f) panOffset.y * (crop.height / viewportHeightPx) else 0f
            matrix.postTranslate(-source.width * 0.5f, -source.height * 0.5f)
            matrix.postScale(scale, scale)
            matrix.postRotate(rotationDegrees)
            matrix.postTranslate(crop.width * 0.5f + panRatioX, crop.height * 0.5f + panRatioY)
            val rotPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(source, matrix, rotPaint)
            out
        }

        val matrixValues = lookFilter.toColorMatrixArray()
        val safeDim = dimFraction.coerceIn(0f, 0.6f)
        if (matrixValues == null && safeDim <= 0.01f) {
            return cropped
        }

        val out = createBitmap(cropped.width, cropped.height)
        val canvas = AndroidCanvas(out)
        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG)
        if (matrixValues != null) {
            paint.colorFilter = ColorMatrixColorFilter(AndroidColorMatrix(matrixValues))
        }
        canvas.drawBitmap(cropped, 0f, 0f, paint)
        if (safeDim > 0.01f) {
            canvas.drawColor(android.graphics.Color.argb((safeDim * 255f).roundToInt().coerceIn(0, 200), 0, 0, 0))
        }
        return out
    }
}

internal fun renderEditedWallpaperBitmap(
    source: Bitmap,
    zoomScale: Float,
    panOffset: Offset,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    dimFraction: Float = 0f,
    lookFilter: CanvasLookFilter = CanvasLookFilter.ORIGINAL,
    targetScreenAspect: Float = if (viewportWidthPx > 0f && viewportHeightPx > 0f) {
        (viewportWidthPx / viewportHeightPx).coerceIn(0.40f, 0.78f)
    } else {
        9f / 19.5f
    },
    allowParallaxWidth: Boolean = false,
    rotationDegrees: Float = 0f
): Bitmap = ViewportCropExportSubEngine.renderEditedBitmap(
    source = source,
    zoomScale = zoomScale,
    panOffset = panOffset,
    viewportWidthPx = viewportWidthPx,
    viewportHeightPx = viewportHeightPx,
    dimFraction = dimFraction,
    lookFilter = lookFilter,
    targetScreenAspect = targetScreenAspect,
    allowParallaxWidth = allowParallaxWidth,
    rotationDegrees = rotationDegrees
)
