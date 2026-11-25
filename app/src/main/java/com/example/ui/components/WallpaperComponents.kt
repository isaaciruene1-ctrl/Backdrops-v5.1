package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.media.BackdropImageLoader
import com.example.data.media.CanonicalSourceLockSubEngine
import com.example.data.media.PooledHttpRetrySubEngine
import com.example.data.media.ThumbnailToFullCrossfadeSubEngine
import com.example.data.model.UiStyleMode
import com.example.data.model.Wallpaper
import com.example.engine.depth.OnlineVisionDepthSolver
import com.example.ui.theme.AppMotion
import com.example.ui.theme.LocalReducedMotionFlag
import com.example.ui.theme.LocalUiStyleMode
import com.example.ui.theme.rememberStudioHaptics
import kotlinx.coroutines.delay

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.62f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha)
        )
    )
}

@Composable
fun WallpaperAsyncImage(
    wallpaper: Wallpaper,
    useFullRes: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    colorFilter: ColorFilter? = null
) {
    if (wallpaper.isBundledAsset && wallpaper.fallbackResId != 0) {
        Image(
            painter = painterResource(id = wallpaper.fallbackResId),
            contentDescription = contentDescription,
            contentScale = contentScale,
            colorFilter = colorFilter,
            modifier = modifier
        )
        return
    }
    val context = LocalContext.current
    val candidates = remember(wallpaper.id, useFullRes) {
        CanonicalSourceLockSubEngine.resolveLockedCandidates(context, wallpaper, useFullRes)
    }
    var candidateIndex by remember(wallpaper.id, useFullRes) { mutableIntStateOf(0) }
    var retryNonce by remember(wallpaper.id, useFullRes) { mutableIntStateOf(0) }

    val offlineFile = remember(context, wallpaper.id) {
        com.example.data.media.OfflinePackDownloadManager.getDownloadedFile(context, wallpaper.id)
    }
    val activeUrl = candidates.getOrElse(candidateIndex) {
        CanonicalSourceLockSubEngine.resolveLockedCandidates(context, wallpaper, useFullRes).first()
    }
    val dataSource: Any = if (retryNonce == 0 && offlineFile != null) offlineFile else activeUrl
    val thumbCacheKey = ThumbnailToFullCrossfadeSubEngine.thumbMemoryCacheKey(wallpaper.id)
    val fullCacheKey = ThumbnailToFullCrossfadeSubEngine.fullMemoryCacheKey(wallpaper.id)
    val cacheKey = if (useFullRes) fullCacheKey else thumbCacheKey
    val tier = remember(context) {
        com.example.engine.performance.ZeroLagPerformanceEngine.classifyTier(context)
    }

    val immediateThumbBitmap = remember(wallpaper.id, useFullRes) {
        if (useFullRes) {
            ThumbnailToFullCrossfadeSubEngine.findInMemorySameWallpaperBitmap(context, wallpaper.id)
        } else null
    }
    val bridgedThumbBitmap by produceState<Bitmap?>(
        initialValue = immediateThumbBitmap,
        key1 = wallpaper.id,
        key2 = useFullRes
    ) {
        if (useFullRes && value == null) {
            value = ThumbnailToFullCrossfadeSubEngine.loadDiskSameWallpaperBitmap(context, wallpaper)
        }
    }

    val imageRequest = remember(wallpaper.id, useFullRes, dataSource, tier, retryNonce, candidateIndex) {
        ImageRequest.Builder(context)
            .data(dataSource)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .setParameter("retryNonce", retryNonce, memoryCacheKey = null)
            .setParameter("candidateIdx", candidateIndex, memoryCacheKey = null)
            .apply {
                if (useFullRes) {
                    placeholderMemoryCacheKey(MemoryCache.Key(thumbCacheKey))
                    size(1280, 2240)
                } else {
                    size(540, 840)
                }
                if (retryNonce > 0) {
                    addHeader(PooledHttpRetrySubEngine.FORCE_RELOAD_HEADER, "1")
                    memoryCachePolicy(CachePolicy.WRITE_ONLY)
                    diskCachePolicy(CachePolicy.WRITE_ONLY)
                }
            }
            .allowRgb565(!useFullRes && tier == com.example.engine.performance.PerformanceTier.LOW_RAM)
            .crossfade(220)
            .build()
    }
    val paletteColors = remember(wallpaper.id) {
        val raw = wallpaper.dominantColors.map { Color(it) }
        when {
            raw.size >= 2 -> raw
            raw.size == 1 -> listOf(raw.first(), raw.first().copy(alpha = 0.72f), Color(0xFF141820))
            else -> listOf(Color(0xFF1E2634), Color(0xFF33415C), Color(0xFF10131A))
        }
    }

    @Composable
    fun SameIdentityBridgeSurface(showAmbientShimmer: Boolean) {
        val validThumb = bridgedThumbBitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
        if (validThumb != null) {
            val bmp = remember(validThumb) { validThumb.asImageBitmap() }
            Image(
                bitmap = bmp,
                contentDescription = contentDescription,
                contentScale = contentScale,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(paletteColors))
            ) {
                if (showAmbientShimmer) {
                    ShimmerBox(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    SubcomposeAsyncImage(
        model = imageRequest,
        imageLoader = BackdropImageLoader.get(context),
        contentDescription = contentDescription,
        contentScale = contentScale,
        colorFilter = colorFilter,
        modifier = modifier,
        loading = {
            SameIdentityBridgeSurface(showAmbientShimmer = true)
        },
        error = {
            val hasNextSamePhotoTier = candidateIndex < candidates.lastIndex
            LaunchedEffect(wallpaper.id, candidateIndex, retryNonce) {
                if (hasNextSamePhotoTier) {
                    candidateIndex++
                } else {
                    val backoffMs = when {
                        retryNonce == 0 -> 900L
                        retryNonce == 1 -> 2400L
                        else -> 5500L
                    }
                    delay(backoffMs)
                    if (OnlineVisionDepthSolver.isOnlineNetworkAvailable(context) || retryNonce < 4) {
                        candidateIndex = 0
                        retryNonce++
                    }
                }
            }
            SameIdentityBridgeSurface(showAmbientShimmer = hasNextSamePhotoTier || bridgedThumbBitmap == null)
        }
    )
}

@Composable
fun FavoriteHeartButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Black.copy(alpha = 0.36f)
) {
    val uiStyle = LocalUiStyleMode.current
    val reducedMotion = LocalReducedMotionFlag.current
    val haptics = rememberStudioHaptics()
    val scale by animateFloatAsState(
        targetValue = if (isFavorite && !reducedMotion) {
            if (uiStyle == UiStyleMode.FROSTED_GLASS_OS) 1.22f else 1.12f
        } else 1f,
        animationSpec = AppMotion.emphasisSpring(uiStyle, reducedMotion),
        label = "heart_scale"
    )
    val tint by animateColorAsState(
        targetValue = if (isFavorite) Color(0xFFFF8095) else Color.White,
        animationSpec = AppMotion.colorSpec(uiStyle, reducedMotion),
        label = "heart_tint"
    )
    Surface(
        color = containerColor,
        shape = CircleShape,
        modifier = modifier.size(48.dp)
    ) {
        IconButton(
            onClick = {
                haptics.performTick(25L)
                onToggle()
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = tint,
                modifier = Modifier
                    .size(22.dp)
                    .scale(scale)
            )
        }
    }
}

@Composable
fun WallpaperGridCard(
    wallpaper: Wallpaper,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 0.66f,
    cornerRadius: Dp = 24.dp
) {
    val isGlass = LocalUiStyleMode.current == UiStyleMode.FROSTED_GLASS_OS
    Surface(
        shape = RoundedCornerShape(cornerRadius),
        border = glassSpecularBorder(),
        tonalElevation = if (isGlass) 0.dp else 2.dp,
        shadowElevation = if (isGlass) 6.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(onClick = onClick)
            .testTag("wallpaper_card_${wallpaper.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            WallpaperAsyncImage(
                wallpaper = wallpaper,
                useFullRes = false,
                contentDescription = "${wallpaper.category.displayName} wallpaper",
                modifier = Modifier.fillMaxSize()
            )

            // Bottom editorial gradient scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                        )
                    )
            )

            // Top badges row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (wallpaper.supportsDepthEffect) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Layers,
                                contentDescription = "Depth Studio compatible",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "DEPTH",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                } else {
                    Surface(
                        color = Color.Black.copy(alpha = 0.42f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = wallpaper.resolutionTier.badge.substringBefore(" "),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                FavoriteHeartButton(
                    isFavorite = isFavorite,
                    onToggle = onFavoriteToggle,
                    modifier = Modifier.testTag("favorite_btn_${wallpaper.id}")
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = wallpaper.category.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = wallpaper.aspectRatio,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun EmptyStatePanel(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    TactileSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BackdropLogoMark(size = 44.dp)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("empty_state_action_button")
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
