package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.LockNotificationStyleConfig
import com.example.data.model.NotificationLayoutMode
import com.example.data.model.NotificationStylePreset
import com.example.engine.notifications.LockNotificationGroup
import com.example.engine.notifications.LockNotificationItem
import com.example.ui.theme.AppMotion
import com.example.ui.theme.LocalReducedMotionFlag
import com.example.ui.theme.LocalThemeCustomization
import com.example.ui.theme.LocalUiStyleMode
import com.example.ui.theme.rememberStudioHaptics
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
data class NotificationLayoutBounds(
    val topStartDp: Dp,
    val maxListHeightDp: Dp,
    val effectiveMaxGroups: Int,
    val forceCompact: Boolean
)

/**
 * Spatial lock-screen notification solver ensuring notification stacks never overlap clock glyphs,
 * date pills, active Studio widgets, or the bottom unlock button bar.
 */
object LockScreenNotificationLayoutSolver {

    fun solveAvailableZone(
        viewportHeightDp: Float,
        clockBottomFraction: Float,
        activeWidgetHeightDp: Float,
        bottomReserveDp: Float,
        styleConfig: LockNotificationStyleConfig
    ): NotificationLayoutBounds {
        val safeH = viewportHeightDp.coerceAtLeast(360f)
        val clockBottomDp = (safeH * clockBottomFraction.coerceIn(0.12f, 0.75f))
        val topStart = (clockBottomDp + activeWidgetHeightDp + 10f).coerceAtLeast(132f)
        val maxAvail = (safeH - topStart - bottomReserveDp).coerceAtLeast(72f)
        val approxRowHeight = when {
            styleConfig.layoutMode == NotificationLayoutMode.COUNT -> 44f
            styleConfig.compactTextHierarchy -> 64f
            else -> 76f
        }
        val fitGroups = (maxAvail / (approxRowHeight + styleConfig.cardSpacingDp))
            .toInt()
            .coerceIn(1, styleConfig.maxVisibleGroups)
        return NotificationLayoutBounds(
            topStartDp = topStart.dp,
            maxListHeightDp = maxAvail.dp,
            effectiveMaxGroups = fitGroups,
            forceCompact = maxAvail < 140f
        )
    }
}

@Composable
fun LockScreenNotificationsSection(
    groups: List<LockNotificationGroup>,
    styleConfig: LockNotificationStyleConfig,
    expandedKeys: MutableMap<String, Boolean>,
    hideSensitive: Boolean,
    maxVisibleOverride: Int = styleConfig.maxVisibleGroups,
    forceCompact: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    dragToExpandEnabled: Boolean = true,
    onDragExpansionChange: ((Float) -> Unit)? = null,
    onOpenItem: (LockNotificationItem) -> Unit,
    onDismissItem: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Clean empty state: occupies 0.dp when there are no notifications
    if (groups.isEmpty()) return

    val safeCfg = remember(styleConfig) { styleConfig.sanitized() }
    val allItems = remember(groups) {
        val iconByPkg = groups.associate { it.packageName to it.appIconBitmap }
        groups.flatMap { grp ->
            grp.notifications.map { item ->
                if (item.appIconBitmap != null) item else item.copy(appIconBitmap = iconByPkg[grp.packageName])
            }
        }.sortedByDescending { it.postTime }
    }
    if (allItems.isEmpty()) return

    val totalCount = allItems.size
    var isStackExpanded by remember(safeCfg.layoutMode) { mutableStateOf(false) }
    var isCountExpanded by remember(safeCfg.layoutMode) { mutableStateOf(false) }
    var dragExpansionFraction by remember(safeCfg.layoutMode) { mutableFloatStateOf(0f) }

    fun setExpandedState(expanded: Boolean) {
        isStackExpanded = expanded
        isCountExpanded = expanded
        dragExpansionFraction = if (expanded) 1f else 0f
        onDragExpansionChange?.invoke(dragExpansionFraction)
    }

    BackHandler(enabled = isStackExpanded || isCountExpanded || dragExpansionFraction > 0.1f) {
        setExpandedState(false)
    }

    val effectiveMode = if (forceCompact && safeCfg.layoutMode == NotificationLayoutMode.LIST) {
        NotificationLayoutMode.STACK
    } else {
        safeCfg.layoutMode
    }

    val dragExpandGestureModifier = if (dragToExpandEnabled) {
        Modifier.pointerInput(totalCount, safeCfg.layoutMode) {
            detectVerticalDragGestures(
                onVerticalDrag = { change, dragAmount ->
                    change.consume()
                    val delta = (-dragAmount / 240f)
                    val next = (dragExpansionFraction + delta).coerceIn(0f, 1f)
                    dragExpansionFraction = next
                    onDragExpansionChange?.invoke(next)
                    if (next >= 0.45f && !isStackExpanded && !isCountExpanded) {
                        isStackExpanded = true
                        isCountExpanded = true
                    } else if (next < 0.25f && (isStackExpanded || isCountExpanded)) {
                        isStackExpanded = false
                        isCountExpanded = false
                    }
                },
                onDragEnd = {
                    setExpandedState(dragExpansionFraction >= 0.35f)
                },
                onDragCancel = {
                    setExpandedState(dragExpansionFraction >= 0.35f)
                }
            )
        }
    } else Modifier

    val uiStyle = LocalUiStyleMode.current
    val reducedMotion = LocalReducedMotionFlag.current
    val motionSpeed = LocalThemeCustomization.current.m3MotionSpeed

    Column(
        modifier = modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .then(dragExpandGestureModifier)
            .animateContentSize(AppMotion.lockNotificationDeckSpring(uiStyle, reducedMotion, motionSpeed)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (dragToExpandEnabled) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp, bottom = 2.dp)
                    .clickable { setExpandedState(!(isStackExpanded || isCountExpanded)) }
                    .testTag("lockscreen_notif_drag_handle"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 42.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = if (dragExpansionFraction > 0.3f) 0.72f else 0.42f))
                )
            }
        }

        AnimatedContent(
            targetState = effectiveMode to (isStackExpanded || isCountExpanded),
            transitionSpec = {
                AppMotion.lockNotifContentEnter(uiStyle, reducedMotion, motionSpeed)
                    .togetherWith(AppMotion.lockNotifContentExit(uiStyle, reducedMotion, motionSpeed))
            },
            label = "notif_presentation_mode"
        ) { (mode, expanded) ->
            when (mode) {
                NotificationLayoutMode.COUNT -> {
                    if (!expanded) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                onClick = { setExpandedState(true) },
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF161922).copy(alpha = (safeCfg.transparency * 0.92f).coerceIn(0.45f, 0.94f)),
                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.16f)),
                                shadowElevation = (safeCfg.shadowIntensity * 8f).dp,
                                modifier = Modifier.testTag("lockscreen_notif_count_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Notifications,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "$totalCount Notification${if (totalCount == 1) "" else "s"}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    } else {
                        ExpandedNotificationCollection(
                            items = allItems.take(maxVisibleOverride.coerceAtLeast(safeCfg.maxVisibleGroups)),
                            styleConfig = safeCfg,
                            hideSensitive = hideSensitive,
                            accentColor = accentColor,
                            onCollapse = { setExpandedState(false) },
                            onOpenItem = onOpenItem,
                            onDismissItem = onDismissItem,
                            onClearAll = onClearAll,
                            testTag = "lockscreen_notifications_count_expanded"
                        )
                    }
                }

                NotificationLayoutMode.LIST -> {
                    ExpandedNotificationCollection(
                        items = allItems.take(maxVisibleOverride.coerceAtLeast(safeCfg.maxVisibleGroups)),
                        styleConfig = safeCfg,
                        hideSensitive = hideSensitive,
                        accentColor = accentColor,
                        onCollapse = if (dragToExpandEnabled) ({ setExpandedState(false) }) else null,
                        onOpenItem = onOpenItem,
                        onDismissItem = onDismissItem,
                        onClearAll = onClearAll,
                        testTag = "lockscreen_notifications_list"
                    )
                }

                NotificationLayoutMode.STACK -> {
                    if (!expanded) {
                        IosOverlappingNotificationDeck(
                            items = allItems,
                            styleConfig = safeCfg,
                            hideSensitive = hideSensitive,
                            accentColor = accentColor,
                            onExpandStack = { setExpandedState(true) },
                            onDismissFront = { frontKey -> onDismissItem(frontKey) }
                        )
                    } else {
                        ExpandedNotificationCollection(
                            items = allItems,
                            styleConfig = safeCfg,
                            hideSensitive = hideSensitive,
                            accentColor = accentColor,
                            onCollapse = { setExpandedState(false) },
                            onOpenItem = onOpenItem,
                            onDismissItem = onDismissItem,
                            onClearAll = onClearAll,
                            testTag = "lockscreen_notifications_expanded_stack"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Native-inspired iOS Lock Screen overlapping deck:
 * - Newest card sits frontmost (index 0) with crisp charcoal translucency & thin subtle border
 * - Older cards (index 1, 2) peek behind with progressively smaller scale & natural vertical offset
 * - Zero cluttered chrome when collapsed; swipe horizontally to dismiss top card or tap to expand deck
 */
@Composable
private fun IosOverlappingNotificationDeck(
    items: List<LockNotificationItem>,
    styleConfig: LockNotificationStyleConfig,
    hideSensitive: Boolean,
    accentColor: Color,
    onExpandStack: () -> Unit,
    onDismissFront: (String) -> Unit
) {
    val visibleStack = remember(items) { items.take(3) }
    val deckDepthPadding = when (visibleStack.size) {
        1 -> 0.dp
        2 -> 14.dp
        else -> 26.dp
    }

    val uiStyle = LocalUiStyleMode.current
    val reducedMotion = LocalReducedMotionFlag.current
    val motionSpeed = LocalThemeCustomization.current.m3MotionSpeed
    val deckSpringDp = AppMotion.lockNotificationDeckSpring<Dp>(uiStyle, reducedMotion, motionSpeed)
    val deckSpringFloat = AppMotion.lockNotificationDeckSpring<Float>(uiStyle, reducedMotion, motionSpeed)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = deckDepthPadding)
            .testTag("lockscreen_notif_stacked_deck"),
        contentAlignment = Alignment.TopCenter
    ) {
        // Render back-to-front so frontmost (index 0) cleanly overlaps older cards
        for (layerIndex in visibleStack.indices.reversed()) {
            val item = visibleStack[layerIndex]
            key(item.key) {
                val targetOffsetY by animateDpAsState(
                    targetValue = (layerIndex * 13).dp,
                    animationSpec = deckSpringDp,
                    label = "stack_offset_y_$layerIndex"
                )
                val targetScale by animateFloatAsState(
                    targetValue = (1f - layerIndex * 0.055f).coerceIn(0.84f, 1f),
                    animationSpec = deckSpringFloat,
                    label = "stack_scale_$layerIndex"
                )
                val targetAlpha by animateFloatAsState(
                    targetValue = when (layerIndex) {
                        0 -> 1f
                        1 -> 0.88f
                        else -> 0.72f
                    },
                    animationSpec = AppMotion.floatEffectsSpec(uiStyle, reducedMotion, motionSpeed),
                    label = "stack_alpha_$layerIndex"
                )

                val isFront = layerIndex == 0
                val cardModifier = Modifier
                    .fillMaxWidth()
                    .zIndex((10 - layerIndex).toFloat())
                    .offset(y = targetOffsetY)
                    .graphicsLayer {
                        scaleX = targetScale
                        scaleY = targetScale
                        alpha = targetAlpha
                    }

                IosNotificationCardSurface(
                    item = item,
                    styleConfig = styleConfig,
                    hideSensitive = hideSensitive,
                    accentColor = accentColor,
                    isCompactDeckCard = true,
                    stackCountBadge = if (isFront && items.size > 1) items.size else null,
                    allowSwipeDismiss = isFront && item.isClearable,
                    showActions = false,
                    onClick = { onExpandStack() },
                    onDismiss = { onDismissFront(item.key) },
                    modifier = cardModifier.testTag(
                        if (isFront) "lockscreen_stack_front_card"
                        else "lockscreen_stack_back_card_$layerIndex"
                    )
                )
            }
        }
    }
}

@Composable
private fun ExpandedNotificationCollection(
    items: List<LockNotificationItem>,
    styleConfig: LockNotificationStyleConfig,
    hideSensitive: Boolean,
    accentColor: Color,
    onCollapse: (() -> Unit)?,
    onOpenItem: (LockNotificationItem) -> Unit,
    onDismissItem: (String) -> Unit,
    onClearAll: () -> Unit,
    testTag: String
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(styleConfig.cardSpacingDp.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        item(key = "notif_collection_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${items.size} Notification${if (items.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.90f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onCollapse != null) {
                        Surface(
                            onClick = onCollapse,
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF1A1D26).copy(alpha = 0.72f),
                            border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.16f)),
                            modifier = Modifier.testTag("lockscreen_stack_collapse_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ExpandLess,
                                    contentDescription = "Collapse notifications",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Show Less",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    if (items.any { it.isClearable }) {
                        Surface(
                            onClick = onClearAll,
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF1A1D26).copy(alpha = 0.72f),
                            border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.16f)),
                            modifier = Modifier.testTag("lockscreen_clear_notifications_btn")
                        ) {
                            Text(
                                text = "Clear All",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        items(items, key = { it.key }) { item ->
            IosNotificationCardSurface(
                item = item,
                styleConfig = styleConfig,
                hideSensitive = hideSensitive,
                accentColor = accentColor,
                isCompactDeckCard = false,
                stackCountBadge = null,
                allowSwipeDismiss = item.isClearable,
                showActions = true,
                onClick = { onOpenItem(item) },
                onDismiss = { onDismissItem(item.key) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lockscreen_notif_card_${item.key}")
            )
        }
    }
}

@Composable
private fun IosNotificationCardSurface(
    item: LockNotificationItem,
    styleConfig: LockNotificationStyleConfig,
    hideSensitive: Boolean,
    accentColor: Color,
    isCompactDeckCard: Boolean,
    stackCountBadge: Int?,
    allowSwipeDismiss: Boolean,
    showActions: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiStyle = LocalUiStyleMode.current
    val reducedMotion = LocalReducedMotionFlag.current
    val motionSpeed = LocalThemeCustomization.current.m3MotionSpeed
    val haptics = rememberStudioHaptics()
    val formattedTime = remember(item.postTime) {
        if (item.postTime > 0L) DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(item.postTime)) else ""
    }
    var dragOffsetX by remember(item.key) { mutableFloatStateOf(0f) }
    val animatedDragX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = AppMotion.lockNotificationDeckSpring(uiStyle, reducedMotion, motionSpeed),
        label = "notif_swipe_x"
    )

    val corner = styleConfig.cornerRadiusDp.dp
    val shape = RoundedCornerShape(corner)
    val baseBg = styleConfig.customBackgroundArgb?.let { Color(it.toInt()) } ?: when (styleConfig.preset) {
        NotificationStylePreset.APPLE_IOS -> Color(0xFF1A1D26)
        NotificationStylePreset.XIAOMI_HYPEROS -> Color(0xFF171D28)
        NotificationStylePreset.SAMSUNG_ONE_UI -> Color(0xFF141820)
        NotificationStylePreset.GOOGLE_PIXEL -> lerp(Color(0xFF1C2029), accentColor, 0.16f)
        NotificationStylePreset.MINIMAL -> Color(0xFF10131A)
        NotificationStylePreset.GLASS -> Color(0xFF1D2330)
        NotificationStylePreset.FLOATING -> Color(0xFF141822)
        NotificationStylePreset.COMPACT -> Color(0xFF12151E)
    }
    val containerColor = baseBg.copy(alpha = styleConfig.transparency.coerceIn(0.42f, 0.94f))
    val borderColor = Color.White.copy(alpha = (0.12f + styleConfig.blurIntensity * 0.08f).coerceIn(0.08f, 0.22f))
    val masked = hideSensitive && item.isSensitive

    val swipeModifier = if (allowSwipeDismiss) {
        Modifier.pointerInput(item.key) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (abs(dragOffsetX) > 150f) {
                        haptics.performTick(24L)
                        onDismiss()
                    }
                    dragOffsetX = 0f
                },
                onDragCancel = { dragOffsetX = 0f },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    dragOffsetX += dragAmount
                }
            )
        }
    } else {
        Modifier
    }

    val validPreviewBmp = item.previewImageBitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
    val validIconBmp = item.appIconBitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }

    Surface(
        shape = shape,
        color = containerColor,
        border = BorderStroke(0.75.dp, borderColor),
        shadowElevation = (styleConfig.shadowIntensity * 6f).dp,
        modifier = modifier
            .offset { IntOffset(animatedDragX.roundToInt(), 0) }
            .graphicsLayer {
                alpha = (1f - (abs(animatedDragX) / 420f)).coerceIn(0.25f, 1f)
            }
            .then(swipeModifier)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                val iconShape = when (styleConfig.preset) {
                    NotificationStylePreset.SAMSUNG_ONE_UI -> RoundedCornerShape(10.dp)
                    NotificationStylePreset.APPLE_IOS -> RoundedCornerShape(9.dp)
                    else -> CircleShape
                }
                if (validIconBmp != null) {
                    val iconImg = remember(validIconBmp) { validIconBmp.asImageBitmap() }
                    Image(
                        bitmap = iconImg,
                        contentDescription = item.appName,
                        modifier = Modifier
                            .size(styleConfig.iconSizeDp.dp)
                            .clip(iconShape)
                    )
                } else {
                    Surface(
                        color = accentColor.copy(alpha = 0.28f),
                        shape = iconShape,
                        modifier = Modifier.size(styleConfig.iconSizeDp.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = item.appName,
                                tint = Color.White,
                                modifier = Modifier.size((styleConfig.iconSizeDp * 0.52f).dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.appName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.68f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (styleConfig.showTimestamp && formattedTime.isNotBlank()) {
                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    color = Color.White.copy(alpha = 0.62f)
                                )
                            }
                            if (stackCountBadge != null && stackCountBadge > 1) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "+${stackCountBadge - 1} more",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.90f),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (!isCompactDeckCard && item.isClearable) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("dismiss_notif_${item.key}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Dismiss notification",
                                        tint = Color.White.copy(alpha = 0.72f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = item.senderOrTitle,
                        style = if (styleConfig.compactTextHierarchy) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (masked) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Sensitive content hidden",
                                tint = Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = if (masked) "Notification content hidden" else item.bodyPreview,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.80f),
                            maxLines = if (isCompactDeckCard) 2 else 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (validPreviewBmp != null && !masked) {
                    val previewImg = remember(validPreviewBmp) { validPreviewBmp.asImageBitmap() }
                    Image(
                        bitmap = previewImg,
                        contentDescription = "Notification image preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
            }

            if (showActions && !masked && item.actions.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(item.actions) { act ->
                        Surface(
                            onClick = { runCatching { act.actionIntent?.send() } },
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = act.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
