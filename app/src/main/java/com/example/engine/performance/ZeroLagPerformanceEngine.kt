package com.example.engine.performance

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.BatteryManager
import android.os.PowerManager
import android.util.LruCache
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.data.catalog.WallpaperCatalog
import com.example.data.media.BackdropImageLoader
import com.example.data.media.WallpaperMediaService
import com.example.data.model.DiscoveryCollection
import com.example.data.model.LiquidGlassTier
import com.example.data.model.LockScreenSetDoc
import com.example.data.model.UserPreferencesDoc
import com.example.data.model.Wallpaper
import com.example.data.model.WallpaperMotionMode
import com.example.data.repository.GuestLocalUserStore
import com.example.data.weather.OpenMeteoWeatherRepository
import com.example.data.weather.WeatherTelemetryState
import com.example.engine.crash.ZeroCrashEngine
import com.example.engine.depth.DepthDetectionEngine
import com.example.engine.depth.OnlineVisionDepthSolver
import com.example.engine.overlay.resolveActiveLockArtwork
import com.example.ui.components.ClockDesignConfig
import com.example.ui.components.StudioWidgetSystemBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

enum class PerformanceTier {
    LOW_RAM,
    BALANCED,
    HIGH_END
}

enum class PreloadPriority {
    IMMEDIATE_VISIBLE,
    NEIGHBOR_PREFETCH,
    BACKGROUND_TAB
}

data class PreloadedTabMetadata(
    val route: String,
    val titleList: List<String>,
    val wallpaperIds: List<String>,
    val preparedAtMs: Long
)

data class InstantLockWarmState(
    val activeSetId: String,
    val wallpaperId: String,
    val clockConfigJson: String,
    val wallpaperBitmap: Bitmap?,
    val cutoutBitmap: Bitmap?,
    val warmedAtMs: Long,
    val artworkKey: String = "",
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val weatherCompact: String = "READY",
    val nextCalendarEvent: String = "",
    val mediaTitle: String = "",
    val mediaArtist: String = "",
    val mediaIsPlaying: Boolean = false
)

/**
 * Hardware-guarded eligibility resolver for subtle wallpaper motion.
 * Automatically disables parallax/drift under Low-RAM, Power-Save, Reduced Motion, or missing gyro hardware.
 */
object WallpaperMotionEligibility {

    fun hasGyroscope(context: Context): Boolean {
        val sm = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return false
        return sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null ||
            sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
    }

    fun isPowerSaveEnabled(context: Context): Boolean {
        val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false
        return pm.isPowerSaveMode
    }

    fun resolveEffectiveMode(
        context: Context,
        requestedMode: WallpaperMotionMode,
        reducedMotionEnabled: Boolean
    ): WallpaperMotionMode {
        if (requestedMode == WallpaperMotionMode.STATIC || reducedMotionEnabled) return WallpaperMotionMode.STATIC
        if (isPowerSaveEnabled(context)) return WallpaperMotionMode.STATIC
        val tier = ZeroLagPerformanceEngine.classifyTier(context)
        if (tier == PerformanceTier.LOW_RAM) return WallpaperMotionMode.STATIC
        return when (requestedMode) {
            WallpaperMotionMode.SUBTLE_PARALLAX ->
                if (hasGyroscope(context)) WallpaperMotionMode.SUBTLE_PARALLAX else WallpaperMotionMode.AMBIENT_DRIFT
            WallpaperMotionMode.AMBIENT_DRIFT -> WallpaperMotionMode.AMBIENT_DRIFT
            WallpaperMotionMode.STATIC -> WallpaperMotionMode.STATIC
        }
    }
}

/**
 * Hardware-adaptive Zero Lag & Preloading Catcher Engine V5 (Backdrops 1.5):
 * - Sub-Engine A (Predictive Carousel, Tab & Neighbor Telemetry Warmer)
 * - Sub-Engine B (Tier-Guarded RAM & In-Flight Queue Cap)
 * - Instant Lock Screen Engine (pre-warms active lock wallpaper, cutout & configuration)
 * - Memory pressure listener releasing unnecessary caches cleanly
 */
object ZeroLagPerformanceEngine {

    private const val PRELOAD_FAILURE_COOLDOWN_MS = 45_000L
    private val initialized = AtomicBoolean(false)
    private val inFlightPreloads = ConcurrentHashMap.newKeySet<String>()
    private val failedPreloadCooldowns = ConcurrentHashMap<String, Long>()
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lowRamSemaphore = Semaphore(2)
    private val balancedSemaphore = Semaphore(4)
    private val highEndSemaphore = Semaphore(6)

    internal fun isUrlOnCooldown(url: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        val until = failedPreloadCooldowns[url] ?: return false
        if (nowMs >= until) {
            failedPreloadCooldowns.remove(url)
            return false
        }
        return true
    }

    internal fun clearCooldown(url: String) {
        failedPreloadCooldowns.remove(url)
    }

    internal fun recordFailureCooldown(url: String, nowMs: Long = System.currentTimeMillis()) {
        failedPreloadCooldowns[url] = nowMs + PRELOAD_FAILURE_COOLDOWN_MS
    }

    @Volatile
    private var cachedTier: PerformanceTier? = null

    private fun semaphoreForTier(tier: PerformanceTier): Semaphore = when (tier) {
        PerformanceTier.LOW_RAM -> lowRamSemaphore
        PerformanceTier.BALANCED -> balancedSemaphore
        PerformanceTier.HIGH_END -> highEndSemaphore
    }

    internal fun maxQueueBudgetForTier(tier: PerformanceTier): Int = when (tier) {
        PerformanceTier.LOW_RAM -> 8
        PerformanceTier.BALANCED -> 16
        PerformanceTier.HIGH_END -> 28
    }

    fun init(context: Context) {
        val appContext = context.applicationContext
        classifyTier(appContext)
        if (initialized.compareAndSet(false, true)) {
            ZeroCrashEngine.registerMemoryCleanupHook("ZeroLagPerformanceEngine") {
                handleTrimMemory(appContext, ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
            }
            appContext.registerComponentCallbacks(object : ComponentCallbacks2 {
                override fun onTrimMemory(level: Int) {
                    handleTrimMemory(appContext, level)
                }

                override fun onConfigurationChanged(newConfig: Configuration) = Unit

                @Deprecated("Deprecated in Android API")
                override fun onLowMemory() {
                    handleTrimMemory(appContext, ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
                }
            })
        }
    }

    fun classifyTier(context: Context): PerformanceTier {
        cachedTier?.let { return it }
        val am = context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isLowRam = am?.isLowRamDevice == true
        val memClass = am?.memoryClass ?: 128
        val cores = Runtime.getRuntime().availableProcessors()
        val tier = when {
            isLowRam || memClass <= 128 || cores <= 4 -> PerformanceTier.LOW_RAM
            memClass >= 384 && cores >= 8 -> PerformanceTier.HIGH_END
            else -> PerformanceTier.BALANCED
        }
        cachedTier = tier
        return tier
    }

    fun recommendLiquidGlassTier(context: Context): LiquidGlassTier {
        val am = context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isLowRam = am?.isLowRamDevice == true
        val memClass = am?.memoryClass ?: 192
        val tier = LiquidGlassTier.recommendForDevice(
            ramMb = memClass * 16,
            sdkInt = android.os.Build.VERSION.SDK_INT,
            isLowRamDevice = isLowRam
        )
        return tier
    }

    fun resolveEffectiveLiquidGlassTier(context: Context, requested: LiquidGlassTier): LiquidGlassTier =
        if (requested == LiquidGlassTier.AUTO_RECOMMENDED) recommendLiquidGlassTier(context) else requested

    fun memoryCachePercent(tier: PerformanceTier): Double = when (tier) {
        PerformanceTier.LOW_RAM -> 0.12
        PerformanceTier.BALANCED -> 0.18
        PerformanceTier.HIGH_END -> 0.24
    }

    fun diskCacheLimitBytes(tier: PerformanceTier): Long = when (tier) {
        PerformanceTier.LOW_RAM -> 80L * 1024L * 1024L
        PerformanceTier.BALANCED -> 150L * 1024L * 1024L
        PerformanceTier.HIGH_END -> 240L * 1024L * 1024L
    }

    fun preferredThumbnailBitmapConfig(context: Context): Bitmap.Config =
        when (classifyTier(context)) {
            PerformanceTier.LOW_RAM -> Bitmap.Config.RGB_565
            PerformanceTier.BALANCED, PerformanceTier.HIGH_END -> Bitmap.Config.ARGB_8888
        }

    fun handleTrimMemory(context: Context, level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                runCatching { BackdropImageLoader.get(context).memoryCache?.clear() }
                inFlightPreloads.clear()
                TabPreloadingEngine.trimAll()
                InstantLockScreenEngine.trimBitmaps()
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                runCatching {
                    BackdropImageLoader.get(context).memoryCache?.let { mc ->
                        mc.trimMemory(mc.maxSize / 2)
                    }
                }
                TabPreloadingEngine.trimToBudget(maxTabs = 2)
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                runCatching {
                    BackdropImageLoader.get(context).memoryCache?.let { mc ->
                        mc.trimMemory((mc.maxSize * 3) / 4)
                    }
                }
                // Retain InstantLockScreenEngine bitmaps across UI_HIDDEN so power-button wake stays instant.
            }
        }
    }

    /**
     * V6 Preloading Catcher: warms upcoming neighbor items around the current scroll/detail index.
     */
    fun prewarmNeighbors(context: Context, wallpapers: List<Wallpaper>, centerIndex: Int, radius: Int = 3) {
        if (wallpapers.isEmpty()) return
        val start = (centerIndex - radius).coerceAtLeast(0)
        val end = (centerIndex + radius).coerceAtMost(wallpapers.lastIndex)
        if (start > end) return
        preloadThumbnails(
            context = context,
            wallpapers = wallpapers.subList(start, end + 1),
            maxCount = (radius * 2 + 1).coerceAtMost(10),
            priority = PreloadPriority.NEIGHBOR_PREFETCH
        )
    }

    /**
     * Warm up visible + ahead-of-scroll thumbnails using priority tiers, CDN mirror awareness, and cooldown backoff.
     */
    fun preloadThumbnails(
        context: Context,
        wallpapers: List<Wallpaper>,
        maxCount: Int = 8,
        priority: PreloadPriority = PreloadPriority.NEIGHBOR_PREFETCH
    ) {
        if (wallpapers.isEmpty()) return
        val appContext = context.applicationContext
        init(appContext)
        val tier = classifyTier(appContext)
        val semaphore = semaphoreForTier(tier)
        val count = when (tier) {
            PerformanceTier.LOW_RAM -> maxCount.coerceAtMost(6)
            PerformanceTier.BALANCED -> maxCount.coerceAtMost(10)
            PerformanceTier.HIGH_END -> maxCount.coerceAtMost(16)
        }
        val baseQueue = maxQueueBudgetForTier(tier)
        val effectiveQueueLimit = when (priority) {
            PreloadPriority.IMMEDIATE_VISIBLE -> baseQueue + 8
            PreloadPriority.NEIGHBOR_PREFETCH -> baseQueue + 4
            PreloadPriority.BACKGROUND_TAB -> baseQueue
        }
        if (priority == PreloadPriority.BACKGROUND_TAB && inFlightPreloads.size >= baseQueue) return
        if (inFlightPreloads.size >= effectiveQueueLimit) return

        val isOnline = OnlineVisionDepthSolver.isOnlineNetworkAvailable(appContext)
        val candidates = wallpapers.take(count).filter { wp ->
            !wp.isBundledAsset && wp.thumbnailUrl.isNotBlank() && isOnline
        }
        if (candidates.isEmpty()) return
        preloadScope.launch {
            val loader = BackdropImageLoader.get(appContext)
            val useRgb565 = tier == PerformanceTier.LOW_RAM
            candidates.forEach { wp ->
                if (inFlightPreloads.size >= effectiveQueueLimit) return@forEach
                val cacheKey = "${wp.id}_thumb"
                if (!inFlightPreloads.add(cacheKey)) return@forEach
                val now = System.currentTimeMillis()
                val mirrorUrls = com.example.data.media.CanonicalSourceLockSubEngine
                    .resolveLockedCandidates(appContext, wp, useFullRes = false)
                    .filter { !isUrlOnCooldown(it, now) }
                if (mirrorUrls.isEmpty()) {
                    inFlightPreloads.remove(cacheKey)
                    return@forEach
                }
                launch {
                    semaphore.withPermit {
                        try {
                            val maxAttempts = if (priority == PreloadPriority.IMMEDIATE_VISIBLE) 2 else 1
                            for (attemptIdx in 0 until minOf(maxAttempts, mirrorUrls.size)) {
                                val targetUrl = mirrorUrls[attemptIdx]
                                val request = ImageRequest.Builder(appContext)
                                    .data(targetUrl)
                                    .memoryCacheKey(cacheKey)
                                    .diskCacheKey(cacheKey)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .allowRgb565(useRgb565)
                                    .size(540, 840)
                                    .build()
                                val result = loader.execute(request)
                                if (result is SuccessResult) {
                                    clearCooldown(targetUrl)
                                    break
                                } else if (result is ErrorResult) {
                                    recordFailureCooldown(targetUrl)
                                }
                            }
                        } catch (_: Exception) {
                        } finally {
                            inFlightPreloads.remove(cacheKey)
                        }
                    }
                }
            }
        }
    }

    /**
     * Intelligent Tab Preloading Engine:
     * Preloads lightweight UI metadata & prioritized thumbnails across Home, Explore & Favorites
     * based on current location and user navigation affinity.
     */
    object TabPreloadingEngine {
        private val metadataCache = LruCache<String, PreloadedTabMetadata>(6)
        private val _preparedRoutes = MutableStateFlow<Set<String>>(emptySet())
        val preparedRoutes: StateFlow<Set<String>> = _preparedRoutes.asStateFlow()

        fun preloadLikelyDestinations(
            context: Context,
            currentRoute: String,
            favoriteWallpapers: List<Wallpaper> = emptyList(),
            exploreResults: List<Wallpaper> = WallpaperCatalog.allWallpapers
        ) {
            val appContext = context.applicationContext
            val priorityOrder = when (currentRoute) {
                "home" -> listOf("explore", "favorites", "collections", "profile")
                "explore" -> listOf("home", "favorites", "collections", "profile")
                "favorites" -> listOf("explore", "home", "collections")
                "collections" -> listOf("favorites", "explore", "home")
                else -> listOf("home", "explore", "favorites")
            }
            // Stagger adjacent tab preparation so visible viewport cards own the initial socket pool.
            priorityOrder.firstOrNull()?.let { firstTarget ->
                prepareDestination(
                    context = appContext,
                    route = firstTarget,
                    favoriteWallpapers = favoriteWallpapers,
                    exploreResults = exploreResults,
                    maxPreloadCount = 4,
                    priority = PreloadPriority.BACKGROUND_TAB
                )
            }
            val secondaryTargets = priorityOrder.drop(1).take(2)
            if (secondaryTargets.isNotEmpty()) {
                preloadScope.launch {
                    delay(450L)
                    secondaryTargets.forEach { target ->
                        prepareDestination(
                            context = appContext,
                            route = target,
                            favoriteWallpapers = favoriteWallpapers,
                            exploreResults = exploreResults,
                            maxPreloadCount = 4,
                            priority = PreloadPriority.BACKGROUND_TAB
                        )
                    }
                }
            }
        }

        fun prepareDestination(
            context: Context,
            route: String,
            favoriteWallpapers: List<Wallpaper> = emptyList(),
            exploreResults: List<Wallpaper> = WallpaperCatalog.allWallpapers,
            maxPreloadCount: Int = 6,
            priority: PreloadPriority = PreloadPriority.NEIGHBOR_PREFETCH
        ): PreloadedTabMetadata {
            val existing = metadataCache.get(route)
            val now = System.currentTimeMillis()
            if (existing != null && (now - existing.preparedAtMs) < 30_000L) {
                return existing
            }
            val list = when (route) {
                "home" -> WallpaperCatalog.getForCollection(DiscoveryCollection.FEATURED).take(12) +
                    WallpaperCatalog.getForCollection(DiscoveryCollection.TRENDING).take(8)
                "explore" -> exploreResults.take(16)
                "favorites" -> favoriteWallpapers.take(16).ifEmpty {
                    WallpaperCatalog.getForCollection(DiscoveryCollection.EDITORS_PICKS).take(8)
                }
                "collections" -> WallpaperCatalog.getForCollection(DiscoveryCollection.DEPTH_STUDIO).take(8)
                else -> WallpaperCatalog.getForCollection(DiscoveryCollection.FEATURED).take(6)
            }
            val meta = PreloadedTabMetadata(
                route = route,
                titleList = list.map { it.title },
                wallpaperIds = list.map { it.id },
                preparedAtMs = now
            )
            metadataCache.put(route, meta)
            _preparedRoutes.value = (_preparedRoutes.value + route)
            preloadThumbnails(
                context = context,
                wallpapers = list,
                maxCount = maxPreloadCount,
                priority = priority
            )
            return meta
        }

        fun getCachedMetadata(route: String): PreloadedTabMetadata? = metadataCache.get(route)

        fun trimToBudget(maxTabs: Int) {
            metadataCache.trimToSize(maxTabs.coerceAtLeast(1))
        }

        fun trimAll() {
            metadataCache.evictAll()
            _preparedRoutes.value = emptySet()
        }
    }

    /**
     * Predictive Floating Pill Navigation Engine:
     * Hooks directly into Frosted OS floating pill press/drag gestures so the hovered tab
     * begins preparing before finger release.
     */
    object PredictivePillNavigationEngine {
        private val _activePreloadRoute = MutableStateFlow<String?>(null)
        val activePreloadRoute: StateFlow<String?> = _activePreloadRoute.asStateFlow()

        private val _preloadTriggerCount = MutableStateFlow(0)
        val preloadTriggerCount: StateFlow<Int> = _preloadTriggerCount.asStateFlow()

        @Volatile
        private var lastHoveredRoute: String? = null

        fun onPillHoverChanged(
            context: Context,
            hoveredRoute: String,
            favoriteWallpapers: List<Wallpaper> = emptyList(),
            exploreResults: List<Wallpaper> = WallpaperCatalog.allWallpapers
        ) {
            if (hoveredRoute.isBlank() || hoveredRoute == lastHoveredRoute) return
            lastHoveredRoute = hoveredRoute
            _activePreloadRoute.value = hoveredRoute
            _preloadTriggerCount.value += 1
            TabPreloadingEngine.prepareDestination(
                context = context,
                route = hoveredRoute,
                favoriteWallpapers = favoriteWallpapers,
                exploreResults = exploreResults
            )
        }

        fun onPillDragEnded() {
            lastHoveredRoute = null
            _activePreloadRoute.value = null
        }
    }

    /**
     * Instant Lock Screen Engine:
     * Pre-warms the active lock wallpaper, subject cutout, and clock/notification state so
     * waking Lock Screen Mode or opening Preview displays without blank flash or lag.
     */
    object InstantLockScreenEngine {
        private val _warmState = MutableStateFlow<InstantLockWarmState?>(null)
        val warmState: StateFlow<InstantLockWarmState?> = _warmState.asStateFlow()

        fun warmLockConfiguration(
            context: Context,
            preferences: UserPreferencesDoc,
            activeSet: LockScreenSetDoc?
        ) {
            val resolved = resolveActiveLockArtwork(context.applicationContext, preferences, activeSet)
            val wallpaperId = resolved.wallpaper.id
            val activeSetId = activeSet?.id.orEmpty()
            val artworkKey = "$activeSetId|$wallpaperId|${resolved.customBitmapPath}|${resolved.customPhotoUri}"
            val current = _warmState.value
            val configJson = activeSet?.clockConfigJson.orEmpty()
            if (current != null &&
                current.activeSetId == activeSetId &&
                current.wallpaperId == wallpaperId &&
                current.clockConfigJson == configJson &&
                current.artworkKey == artworkKey
            ) {
                return
            }
            preloadThumbnails(context, listOf(resolved.wallpaper), maxCount = 2, priority = PreloadPriority.IMMEDIATE_VISIBLE)
            val sameArtwork = current != null && current.artworkKey == artworkKey
            _warmState.value = InstantLockWarmState(
                activeSetId = activeSetId,
                wallpaperId = wallpaperId,
                clockConfigJson = configJson,
                wallpaperBitmap = if (sameArtwork) current?.wallpaperBitmap?.takeIf { !it.isRecycled } else null,
                cutoutBitmap = if (sameArtwork) current?.cutoutBitmap?.takeIf { !it.isRecycled } else null,
                warmedAtMs = System.currentTimeMillis(),
                artworkKey = artworkKey
            )
        }

        /**
         * Asynchronously pre-decodes the active lock backdrop bitmap (custom file / URI / bundled / network full-res)
         * and extracts the foreground depth cutout on [Dispatchers.IO] so Frame-0 of Lock Screen wake has zero delay.
         */
        suspend fun warmLockAssets(
            context: Context,
            preferences: UserPreferencesDoc,
            activeSet: LockScreenSetDoc?
        ) = withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            warmLockConfiguration(appContext, preferences, activeSet)
            val resolved = resolveActiveLockArtwork(appContext, preferences, activeSet)
            val activeSetId = activeSet?.id.orEmpty()
            val wallpaperId = resolved.wallpaper.id
            val artworkKey = "$activeSetId|$wallpaperId|${resolved.customBitmapPath}|${resolved.customPhotoUri}"

            val existing = _warmState.value
            val existingBmp = existing?.takeIf { it.artworkKey == artworkKey }
                ?.wallpaperBitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
            val existingCutout = existing?.takeIf { it.artworkKey == artworkKey }
                ?.cutoutBitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }

            val decodedBmp = existingBmp ?: when {
                resolved.customBitmapPath.isNotBlank() ->
                    runCatching { BitmapFactory.decodeFile(resolved.customBitmapPath) }.getOrNull()
                resolved.customPhotoUri.isNotBlank() ->
                    runCatching {
                        appContext.contentResolver.openInputStream(Uri.parse(resolved.customPhotoUri))?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }.getOrNull()
                resolved.wallpaper.isBundledAsset && resolved.wallpaper.fallbackResId != 0 ->
                    runCatching {
                        BitmapFactory.decodeResource(appContext.resources, resolved.wallpaper.fallbackResId)
                    }.getOrNull()
                else ->
                    runCatching {
                        WallpaperMediaService(appContext).loadFullBitmap(resolved.wallpaper)
                    }.getOrNull()
            }

            if (decodedBmp == null || decodedBmp.isRecycled) return@withContext

            val latestBeforeCutout = _warmState.value
            if (latestBeforeCutout != null && latestBeforeCutout.artworkKey != artworkKey) return@withContext

            val activeClockCfg = activeSet?.clockConfigJson?.let { ClockDesignConfig.fromJsonString(it) }
                ?: GuestLocalUserStore.getInstance(appContext).loadClockDesignConfig()
                ?: ClockDesignConfig()
            val shouldWarmCutout = preferences.manualDepthEffectEnabled && activeClockCfg.depthOverlapEnabled

            val resolvedCutout = if (!shouldWarmCutout) {
                existingCutout
            } else {
                existingCutout ?: runCatching {
                    DepthDetectionEngine().detectAndExtract(
                        sourceBitmap = decodedBmp,
                        titleHint = resolved.wallpaper.title,
                        cacheKey = "${wallpaperId}_${resolved.customBitmapPath.hashCode()}_${resolved.customPhotoUri.hashCode()}",
                        preferPortraitPrior = resolved.customPhotoUri.isNotBlank() ||
                            (resolved.wallpaper.width.toFloat() / resolved.wallpaper.height.coerceAtLeast(1)) < 0.8f,
                        context = appContext
                    ).cutoutBitmap
                }.getOrNull()
            }

            val latestAfterCutout = _warmState.value
            if (latestAfterCutout != null && latestAfterCutout.artworkKey != artworkKey) return@withContext
            updateWarmBitmaps(
                wallpaperId = wallpaperId,
                wallpaperBitmap = decodedBmp,
                cutoutBitmap = resolvedCutout,
                artworkKey = artworkKey,
                activeSetId = activeSetId
            )
            warmTelemetrySnapshot(appContext)
        }

        /**
         * Pre-warms real device telemetry (battery, weather summary, active media, next calendar event)
         * for Backdrop Glance Pill & lock-screen widgets on wake.
         */
        fun warmTelemetrySnapshot(context: Context) {
            val appContext = context.applicationContext
            val batterySticky = runCatching {
                appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            }.getOrNull()
            val batteryPct = if (batterySticky != null) {
                val lvl = batterySticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scl = batterySticky.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (lvl >= 0 && scl > 0) ((lvl * 100f) / scl).toInt().coerceIn(0, 100)
                else OpenMeteoWeatherRepository.readBatteryPercent(appContext)
            } else {
                OpenMeteoWeatherRepository.readBatteryPercent(appContext)
            }
            val batteryStatus = batterySticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val chargingNow = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                batteryStatus == BatteryManager.BATTERY_STATUS_FULL

            val weatherReady = OpenMeteoWeatherRepository.state.value as? WeatherTelemetryState.Ready
            val weatherLabel = weatherReady?.compactReadout ?: "READY"
            val nextEvent = StudioWidgetSystemBridge.loadUpcomingCalendarEvents(appContext, limit = 1)
                .firstOrNull()
                ?.let { "${it.title} • ${it.timeLabel}" }
                .orEmpty()
            val mediaState = StudioWidgetSystemBridge.readActiveMediaState(appContext)

            val existing = _warmState.value ?: InstantLockWarmState(
                activeSetId = "",
                wallpaperId = "",
                clockConfigJson = "",
                wallpaperBitmap = null,
                cutoutBitmap = null,
                warmedAtMs = System.currentTimeMillis()
            )
            _warmState.value = existing.copy(
                batteryPercent = batteryPct,
                isCharging = chargingNow,
                weatherCompact = weatherLabel,
                nextCalendarEvent = nextEvent,
                mediaTitle = if (mediaState.hasSession) mediaState.title else "",
                mediaArtist = if (mediaState.hasSession) mediaState.artist else "",
                mediaIsPlaying = mediaState.hasSession && mediaState.isPlaying,
                warmedAtMs = System.currentTimeMillis()
            )
        }

        fun updateWarmBitmaps(
            wallpaperId: String,
            wallpaperBitmap: Bitmap?,
            cutoutBitmap: Bitmap?,
            artworkKey: String = "",
            activeSetId: String = ""
        ) {
            val existing = _warmState.value
            val validNewWallpaper = wallpaperBitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
            val validNewCutout = cutoutBitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
            val matchesExisting = existing != null &&
                existing.wallpaperId == wallpaperId &&
                (artworkKey.isBlank() || existing.artworkKey == artworkKey)
            val fallbackWallpaper = existing?.takeIf { matchesExisting }?.wallpaperBitmap
                ?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
            val fallbackCutout = existing?.takeIf { matchesExisting }?.cutoutBitmap
                ?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
            _warmState.value = (existing ?: InstantLockWarmState(
                activeSetId = activeSetId,
                wallpaperId = wallpaperId,
                clockConfigJson = "",
                wallpaperBitmap = null,
                cutoutBitmap = null,
                warmedAtMs = System.currentTimeMillis(),
                artworkKey = artworkKey
            )).copy(
                activeSetId = activeSetId.ifBlank { existing?.activeSetId.orEmpty() },
                wallpaperId = wallpaperId,
                wallpaperBitmap = validNewWallpaper ?: fallbackWallpaper,
                cutoutBitmap = validNewCutout ?: fallbackCutout,
                warmedAtMs = System.currentTimeMillis(),
                artworkKey = artworkKey.ifBlank { existing?.artworkKey.orEmpty() }
            )
        }

        fun trimBitmaps() {
            val existing = _warmState.value ?: return
            _warmState.value = existing.copy(wallpaperBitmap = null, cutoutBitmap = null)
        }
    }
}
