package com.example.data.repository

import android.content.Context
import androidx.core.content.edit
import com.example.data.model.AppLanguageOption
import com.example.data.model.DockLayoutMode
import com.example.data.model.FavoriteDoc
import com.example.data.model.ImageQualityPref
import com.example.data.model.LockScreenSetDoc
import com.example.data.model.MaterialNavStyle
import com.example.data.model.RecentlyViewedDoc
import com.example.data.model.SecondaryWidgetPagePosition
import com.example.data.model.ThemeMode
import com.example.data.model.UiStyleMode
import com.example.data.model.UserCollectionDoc
import com.example.data.model.UserPreferencesDoc
import com.example.ui.components.ClockDesignConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

interface UserSyncDataSource {
    fun observeFavorites(userId: String): Flow<List<FavoriteDoc>>
    suspend fun addFavorite(userId: String, wallpaperId: String): Result<Unit>
    suspend fun removeFavorite(userId: String, wallpaperId: String): Result<Unit>

    fun observeCollections(userId: String): Flow<List<UserCollectionDoc>>
    suspend fun createCollection(
        userId: String,
        name: String,
        description: String = "",
        initialWallpaperIds: List<String> = emptyList(),
        coverUrl: String? = null
    ): Result<String>

    suspend fun renameCollection(
        userId: String,
        collection: UserCollectionDoc,
        newName: String,
        newDescription: String = collection.description
    ): Result<Unit>

    suspend fun addWallpaperToCollection(
        userId: String,
        collection: UserCollectionDoc,
        wallpaperId: String,
        coverUrl: String?
    ): Result<Unit>

    suspend fun removeWallpaperFromCollection(
        userId: String,
        collection: UserCollectionDoc,
        wallpaperId: String
    ): Result<Unit>

    suspend fun deleteCollection(userId: String, collectionId: String): Result<Unit>

    fun observeRecentlyViewed(userId: String): Flow<List<RecentlyViewedDoc>>
    suspend fun recordRecentlyViewed(userId: String, wallpaperId: String): Result<Unit>

    fun observePreferences(userId: String): Flow<UserPreferencesDoc>
    suspend fun updatePreferences(
        userId: String,
        transform: (UserPreferencesDoc) -> UserPreferencesDoc
    ): Result<Unit>

    fun observeLockScreenSets(userId: String): Flow<List<LockScreenSetDoc>>
    suspend fun saveLockScreenSet(userId: String, set: LockScreenSetDoc): Result<LockScreenSetDoc>
    suspend fun toggleLockScreenSetFavorite(userId: String, setId: String): Result<Unit>
    suspend fun deleteLockScreenSet(userId: String, setId: String): Result<Unit>
    suspend fun deleteLockScreenSets(userId: String, setIds: Set<String>): Result<Unit>
}

/**
 * State Management & Synchronization Engine V5
 * Dual-sub-engine architecture:
 *  - [PreferenceCodecSubEngine]: schema-validated encoding/decoding and value sanitization
 *  - [AtomicLockStateSyncSubEngine]: synchronized single-writer queue with commit-flushed snapshots
 */
object PreferenceCodecSubEngine {
    fun decodeStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i)
                    if (v.isNotBlank()) add(v)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun sanitizePreferences(doc: UserPreferencesDoc): UserPreferencesDoc = doc.copy(
        glassDepthIntensity = doc.glassDepthIntensity.coerceIn(0.55f, 1.45f),
        m3SurfaceDepth = doc.m3SurfaceDepth.coerceIn(0.5f, 1.5f),
        m3CornerRadiusScale = doc.m3CornerRadiusScale.coerceIn(0.5f, 1.5f),
        m3ElevationScale = doc.m3ElevationScale.coerceIn(0f, 1.6f),
        m3DensityScale = doc.m3DensityScale.coerceIn(0.75f, 1.25f),
        m3TypographyScale = doc.m3TypographyScale.coerceIn(0.85f, 1.25f),
        m3MotionSpeed = doc.m3MotionSpeed.coerceIn(0.6f, 1.5f),
        glassBlurLevel = doc.glassBlurLevel.coerceIn(0.4f, 1.6f),
        glassRefractionLevel = doc.glassRefractionLevel.coerceIn(0.4f, 1.6f),
        glassTransparencyLevel = doc.glassTransparencyLevel.coerceIn(0.5f, 1.5f),
        glassLightResponse = doc.glassLightResponse.coerceIn(0.4f, 1.5f),
        glassSpecularHighlights = doc.glassSpecularHighlights.coerceIn(0.2f, 1.5f),
        glassBorderSoftness = doc.glassBorderSoftness.coerceIn(0.4f, 1.5f),
        glassAdaptiveContrast = doc.glassAdaptiveContrast.coerceIn(0.6f, 1.5f),
        offlinePackTargetCount = doc.offlinePackTargetCount.coerceIn(25, 300),
        lockScreenMaxWidgets = doc.lockScreenMaxWidgets.coerceIn(0, 6),
        widgetPageMaxWidgets = doc.widgetPageMaxWidgets.coerceIn(1, 12)
    )
}

internal class AtomicLockStateSyncSubEngine {
    val writeLock = Any()

    inline fun <T> commitFlowUpdate(
        flow: MutableStateFlow<T>,
        crossinline computeNext: (T) -> T,
        crossinline persist: (T) -> Unit
    ): T {
        synchronized(writeLock) {
            val next = computeNext(flow.value)
            flow.value = next
            persist(next)
            return next
        }
    }
}

class GuestLocalUserStore(context: Context) : UserSyncDataSource {

    private class StoreBacking(appContext: Context) {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val syncEngine = AtomicLockStateSyncSubEngine()
        val writeLock get() = syncEngine.writeLock
        val favoritesFlow = MutableStateFlow(loadFavoritesFrom(prefs))
        val collectionsFlow = MutableStateFlow(loadCollectionsFrom(prefs))
        val recentlyViewedFlow = MutableStateFlow(loadRecentlyViewedFrom(prefs))
        val preferencesFlow = MutableStateFlow(loadPreferencesFrom(prefs))
        val lockScreenSetsFlow = MutableStateFlow(loadLockScreenSetsFrom(prefs))
    }

    private val backing: StoreBacking = getOrCreateBacking(context.applicationContext)
    private val prefs get() = backing.prefs
    private val favoritesFlow get() = backing.favoritesFlow
    private val collectionsFlow get() = backing.collectionsFlow
    private val recentlyViewedFlow get() = backing.recentlyViewedFlow
    private val preferencesFlow get() = backing.preferencesFlow
    private val lockScreenSetsFlow get() = backing.lockScreenSetsFlow

    override fun observeFavorites(userId: String): Flow<List<FavoriteDoc>> = favoritesFlow

    override suspend fun addFavorite(userId: String, wallpaperId: String): Result<Unit> {
        backing.syncEngine.commitFlowUpdate(
            flow = favoritesFlow,
            computeNext = { existing ->
                val current = existing.filterNot { it.wallpaperId == wallpaperId }
                listOf(
                    FavoriteDoc(
                        id = wallpaperId,
                        userId = GUEST_USER_ID,
                        wallpaperId = wallpaperId,
                        createdAt = System.currentTimeMillis()
                    )
                ) + current
            },
            persist = ::saveFavorites
        )
        return Result.success(Unit)
    }

    override suspend fun removeFavorite(userId: String, wallpaperId: String): Result<Unit> {
        backing.syncEngine.commitFlowUpdate(
            flow = favoritesFlow,
            computeNext = { existing -> existing.filterNot { it.wallpaperId == wallpaperId } },
            persist = ::saveFavorites
        )
        return Result.success(Unit)
    }

    override fun observeCollections(userId: String): Flow<List<UserCollectionDoc>> = collectionsFlow

    override suspend fun createCollection(
        userId: String,
        name: String,
        description: String,
        initialWallpaperIds: List<String>,
        coverUrl: String?
    ): Result<String> {
        val trimmedName = name.trim().take(60)
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Collection name cannot be empty."))
        }
        val collectionId = "guest_col_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val now = System.currentTimeMillis()
        val created = UserCollectionDoc(
            id = collectionId,
            userId = GUEST_USER_ID,
            name = trimmedName,
            description = description.trim().take(240),
            wallpaperIds = initialWallpaperIds.distinct().take(150),
            coverUrl = coverUrl.orEmpty().take(500),
            createdAt = now,
            updatedAt = now
        )
        backing.syncEngine.commitFlowUpdate(
            flow = collectionsFlow,
            computeNext = { existing -> listOf(created) + existing },
            persist = ::saveCollections
        )
        return Result.success(collectionId)
    }

    override suspend fun renameCollection(
        userId: String,
        collection: UserCollectionDoc,
        newName: String,
        newDescription: String
    ): Result<Unit> {
        val trimmedName = newName.trim().take(60)
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Collection name cannot be empty."))
        }
        backing.syncEngine.commitFlowUpdate(
            flow = collectionsFlow,
            computeNext = { existing ->
                existing.map { item ->
                    if (item.id == collection.id) {
                        item.copy(
                            name = trimmedName,
                            description = newDescription.trim().take(240),
                            updatedAt = System.currentTimeMillis()
                        )
                    } else item
                }
            },
            persist = ::saveCollections
        )
        return Result.success(Unit)
    }

    override suspend fun addWallpaperToCollection(
        userId: String,
        collection: UserCollectionDoc,
        wallpaperId: String,
        coverUrl: String?
    ): Result<Unit> {
        backing.syncEngine.commitFlowUpdate(
            flow = collectionsFlow,
            computeNext = { existing ->
                existing.map { item ->
                    if (item.id == collection.id) {
                        item.copy(
                            wallpaperIds = (item.wallpaperIds + wallpaperId).distinct().take(150),
                            coverUrl = item.coverUrl.ifBlank { coverUrl.orEmpty().take(500) },
                            updatedAt = System.currentTimeMillis()
                        )
                    } else item
                }
            },
            persist = ::saveCollections
        )
        return Result.success(Unit)
    }

    override suspend fun removeWallpaperFromCollection(
        userId: String,
        collection: UserCollectionDoc,
        wallpaperId: String
    ): Result<Unit> {
        backing.syncEngine.commitFlowUpdate(
            flow = collectionsFlow,
            computeNext = { existing ->
                existing.map { item ->
                    if (item.id == collection.id) {
                        item.copy(
                            wallpaperIds = item.wallpaperIds.filterNot { it == wallpaperId },
                            updatedAt = System.currentTimeMillis()
                        )
                    } else item
                }
            },
            persist = ::saveCollections
        )
        return Result.success(Unit)
    }

    override suspend fun deleteCollection(userId: String, collectionId: String): Result<Unit> {
        backing.syncEngine.commitFlowUpdate(
            flow = collectionsFlow,
            computeNext = { existing -> existing.filterNot { it.id == collectionId } },
            persist = ::saveCollections
        )
        return Result.success(Unit)
    }

    override fun observeRecentlyViewed(userId: String): Flow<List<RecentlyViewedDoc>> = recentlyViewedFlow

    override suspend fun recordRecentlyViewed(userId: String, wallpaperId: String): Result<Unit> {
        backing.syncEngine.commitFlowUpdate(
            flow = recentlyViewedFlow,
            computeNext = { existing ->
                val filtered = existing.filterNot { it.wallpaperId == wallpaperId }
                (listOf(
                    RecentlyViewedDoc(
                        id = wallpaperId,
                        userId = GUEST_USER_ID,
                        wallpaperId = wallpaperId,
                        viewedAt = System.currentTimeMillis()
                    )
                ) + filtered).take(24)
            },
            persist = ::saveRecentlyViewed
        )
        return Result.success(Unit)
    }

    override fun observePreferences(userId: String): Flow<UserPreferencesDoc> = preferencesFlow

    override suspend fun updatePreferences(
        userId: String,
        transform: (UserPreferencesDoc) -> UserPreferencesDoc
    ): Result<Unit> {
        backing.syncEngine.commitFlowUpdate(
            flow = preferencesFlow,
            computeNext = { current ->
                PreferenceCodecSubEngine.sanitizePreferences(
                    transform(current).copy(
                        id = "settings",
                        userId = GUEST_USER_ID,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            },
            persist = ::savePreferences
        )
        return Result.success(Unit)
    }

    override fun observeLockScreenSets(userId: String): Flow<List<LockScreenSetDoc>> = lockScreenSetsFlow

    override suspend fun saveLockScreenSet(userId: String, set: LockScreenSetDoc): Result<LockScreenSetDoc> {
        val trimmedName = set.name.trim().take(60)
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Set name cannot be empty."))
        }
        if (set.wallpaperId.isBlank() && set.customPhotoUri.isBlank()) {
            return Result.failure(IllegalArgumentException("Wallpaper source missing for set."))
        }
        val id = set.id.ifBlank { "set_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
        var normalizedResult: LockScreenSetDoc? = null
        backing.syncEngine.commitFlowUpdate(
            flow = lockScreenSetsFlow,
            computeNext = { currentList ->
                val now = System.currentTimeMillis()
                val existing = currentList.firstOrNull { it.id == id }
                val normalized = set.copy(
                    id = id,
                    name = trimmedName,
                    createdAt = existing?.createdAt ?: if (set.createdAt > 0L) set.createdAt else now,
                    updatedAt = now
                )
                normalizedResult = normalized
                (listOf(normalized) + currentList.filterNot { it.id == id }).take(60)
            },
            persist = ::saveLockScreenSets
        )
        return Result.success(normalizedResult!!)
    }

    override suspend fun toggleLockScreenSetFavorite(userId: String, setId: String): Result<Unit> {
        var found = false
        backing.syncEngine.commitFlowUpdate(
            flow = lockScreenSetsFlow,
            computeNext = { currentList ->
                found = currentList.any { it.id == setId }
                if (!found) return@commitFlowUpdate currentList
                val now = System.currentTimeMillis()
                currentList.map { item ->
                    if (item.id == setId) item.copy(isFavorite = !item.isFavorite, updatedAt = now) else item
                }
            },
            persist = { updated -> if (found) saveLockScreenSets(updated) }
        )
        return if (found) {
            Result.success(Unit)
        } else {
            Result.failure(NoSuchElementException("Lock-screen set not found."))
        }
    }

    override suspend fun deleteLockScreenSet(userId: String, setId: String): Result<Unit> {
        backing.syncEngine.commitFlowUpdate(
            flow = lockScreenSetsFlow,
            computeNext = { currentList -> currentList.filterNot { it.id == setId } },
            persist = ::saveLockScreenSets
        )
        return Result.success(Unit)
    }

    override suspend fun deleteLockScreenSets(userId: String, setIds: Set<String>): Result<Unit> {
        if (setIds.isEmpty()) return Result.success(Unit)
        backing.syncEngine.commitFlowUpdate(
            flow = lockScreenSetsFlow,
            computeNext = { currentList -> currentList.filterNot { it.id in setIds } },
            persist = ::saveLockScreenSets
        )
        return Result.success(Unit)
    }

    fun getCurrentPreferencesSync(): UserPreferencesDoc = preferencesFlow.value

    fun getLockScreenSetsSync(): List<LockScreenSetDoc> = lockScreenSetsFlow.value

    fun getFavoritesSync(): List<FavoriteDoc> = favoritesFlow.value

    fun getCollectionsSync(): List<UserCollectionDoc> = collectionsFlow.value

    fun getRecentlyViewedSync(): List<RecentlyViewedDoc> = recentlyViewedFlow.value

    private fun saveFavorites(list: List<FavoriteDoc>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.wallpaperId) }
        prefs.edit { putString(KEY_FAVORITES, arr.toString()) }
    }

    private fun loadCollections(): List<UserCollectionDoc> = loadCollectionsFrom(prefs)

    private fun saveCollections(list: List<UserCollectionDoc>) {
        val arr = JSONArray()
        list.forEach { col ->
            val obj = JSONObject().apply {
                put("id", col.id)
                put("name", col.name)
                put("description", col.description)
                put("coverUrl", col.coverUrl)
                put("wallpaperIds", JSONArray(col.wallpaperIds))
            }
            arr.put(obj)
        }
        prefs.edit { putString(KEY_COLLECTIONS, arr.toString()) }
    }

    private fun saveRecentlyViewed(list: List<RecentlyViewedDoc>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.wallpaperId) }
        prefs.edit { putString(KEY_RECENTLY_VIEWED, arr.toString()) }
    }

    private fun savePreferences(doc: UserPreferencesDoc) {
        prefs.edit(commit = true) {
            putString(KEY_THEME_MODE, doc.themeMode)
            putBoolean(KEY_AMOLED_DARK, doc.amoledDark)
            putString(KEY_IMAGE_QUALITY, doc.imageQuality)
            putBoolean(KEY_WIFI_ONLY, doc.wifiOnlyDownloads)
            putString(KEY_SEARCHES, JSONArray(doc.recentSearches.take(15)).toString())
            putBoolean(KEY_HOME_CATEGORY_CHIPS, doc.homeCategoryChipsEnabled)
            putString(KEY_UI_STYLE, doc.uiStyle)
            putBoolean(KEY_HAPTICS_ENABLED, doc.hapticFeedbackEnabled)
            putBoolean(KEY_REDUCED_MOTION, doc.reducedMotionEnabled)
            putFloat(KEY_GLASS_DEPTH, doc.glassDepthIntensity.coerceIn(0.55f, 1.45f))
            putString(KEY_LANGUAGE_CODE, doc.languageCode)
            putBoolean(KEY_ONBOARDING_DONE, doc.onboardingCompleted)
            putBoolean(KEY_DYNAMIC_THEMING, doc.dynamicThemingEnabled)
            putLong(KEY_CUSTOM_ACCENT_HEX, doc.customAccentHex)
            putFloat(KEY_M3_SURFACE_DEPTH, doc.m3SurfaceDepth)
            putFloat(KEY_M3_CORNER_RADIUS, doc.m3CornerRadiusScale)
            putFloat(KEY_M3_ELEVATION, doc.m3ElevationScale)
            putFloat(KEY_M3_DENSITY, doc.m3DensityScale)
            putString(KEY_M3_NAV_STYLE, doc.m3NavStyle)
            putFloat(KEY_M3_TYPO_SCALE, doc.m3TypographyScale)
            putFloat(KEY_M3_MOTION_SPEED, doc.m3MotionSpeed)
            putFloat(KEY_GLASS_BLUR, doc.glassBlurLevel)
            putFloat(KEY_GLASS_REFRACTION, doc.glassRefractionLevel)
            putFloat(KEY_GLASS_TRANSPARENCY, doc.glassTransparencyLevel)
            putFloat(KEY_GLASS_LIGHT_RESP, doc.glassLightResponse)
            putFloat(KEY_GLASS_SPECULAR, doc.glassSpecularHighlights)
            putFloat(KEY_GLASS_BORDER_SOFT, doc.glassBorderSoftness)
            putFloat(KEY_GLASS_CONTRAST, doc.glassAdaptiveContrast)
            putString(KEY_DOCK_LAYOUT, doc.dockLayoutMode)
            putBoolean(KEY_CLASSIC_HANDLES, doc.useClassicClockHandles)
            putString(KEY_WEATHER_CITY, doc.weatherCity)
            putBoolean(KEY_WEATHER_USE_LOC, doc.weatherUseLocation)
            putBoolean(KEY_OVERLAY_ENABLED, doc.lockScreenOverlayEnabled)
            putString(KEY_OVERLAY_SET_ID, doc.activeOverlaySetId)
            putString(KEY_ACTIVE_HOME_WP_ID, doc.activeHomeWallpaperId)
            putString(KEY_ACTIVE_LOCK_WP_ID, doc.activeLockWallpaperId)
            putString(KEY_ACTIVE_LOCK_CUSTOM_PATH, doc.activeLockCustomBitmapPath)
            putBoolean(KEY_LOCK_NOTIFS_ENABLED, doc.lockNotificationsEnabled)
            putBoolean(KEY_HIDE_SENSITIVE_NOTIFS, doc.hideSensitiveNotifications)
            putBoolean(KEY_MANUAL_DEPTH_ENABLED, doc.manualDepthEffectEnabled)
            putString(KEY_LOCK_CRED_TYPE, doc.lockCredentialType)
            putString(KEY_LOCK_CRED_HASH, doc.lockCredentialHash)
            putString(KEY_LOCK_CRED_SALT, doc.lockCredentialSalt)
            putBoolean(KEY_REMEMBER_PREV_EDIT, doc.rememberPreviousEditingSettings)
            putBoolean(KEY_SCALE_TEXT_WITH_CLOCK_DEF, doc.scaleTextWithClockDefault)
            putString(KEY_STUDIO_FONT, doc.selectedStudioFont)
            putString(KEY_LIQUID_GLASS_TIER, doc.liquidGlassTier)
            putString(KEY_GLOW_MATERIAL_JSON, doc.glowMaterialJson)
            putString(KEY_NOTIF_STYLE_JSON, doc.notificationStyleJson)
            putString(KEY_CHARGING_ANIM_JSON, doc.chargingAnimationJson)
            putString(KEY_UNLOCK_BTN_JSON, doc.unlockButtonThemeJson)
            putBoolean(KEY_ON_DEVICE_ENABLED, doc.onDeviceCollectionEnabled)
            putString(KEY_ON_DEVICE_FOLDERS, JSONArray(doc.onDeviceFolderUris).toString())
            putString(KEY_ON_DEVICE_PHOTOS, JSONArray(doc.onDevicePhotoUris).toString())
            putInt(KEY_OFFLINE_TARGET_COUNT, doc.offlinePackTargetCount)
            putString(KEY_OFFLINE_DOWNLOADED_IDS, JSONArray(doc.offlineDownloadedIds).toString())
            putString(KEY_OFFLINE_STATUS, doc.offlinePackStatus)
            putBoolean(KEY_SYNC_HOME_WITH_LOCK, doc.syncHomeWithLockMode)
            putString(KEY_STUDIO_WIDGETS_JSON, doc.studioWidgetsJson)
            putBoolean(KEY_DRAG_EXPAND_NOTIFS, doc.dragToExpandNotifications)
            putBoolean(KEY_SHOW_UNLOCK_BTN, doc.showUnlockButtonOnLockScreen)
            val resolvedPos = doc.parsedSecondaryWidgetPagePosition
            putBoolean(KEY_SECONDARY_WIDGET_PAGE_LEFT, resolvedPos == SecondaryWidgetPagePosition.LEFT)
            putString(KEY_SECONDARY_WIDGET_PAGE_POSITION, resolvedPos.name)
            putInt(KEY_LOCK_SCREEN_MAX_WIDGETS, doc.lockScreenMaxWidgets.coerceIn(0, 6))
            putInt(KEY_WIDGET_PAGE_MAX_WIDGETS, doc.widgetPageMaxWidgets.coerceIn(1, 12))
            putBoolean(KEY_SAMSUNG_MEDIA_PILL, doc.samsungMediaPillEnabled)
            putBoolean(KEY_PIXEL_CHARGING_WAVE, doc.pixelChargingWaveEnabled)
        }
    }

    private fun saveLockScreenSets(list: List<LockScreenSetDoc>) {
        val arr = JSONArray()
        list.take(60).forEach { set ->
            arr.put(
                JSONObject().apply {
                    put("id", set.id)
                    put("name", set.name)
                    put("wallpaperId", set.wallpaperId)
                    put("customPhotoUri", set.customPhotoUri)
                    put("clockConfigJson", set.clockConfigJson)
                    put("zoomScale", set.zoomScale.toDouble())
                    put("panOffsetX", set.panOffsetX.toDouble())
                    put("panOffsetY", set.panOffsetY.toDouble())
                    put("rotationDegrees", set.rotationDegrees.toDouble())
                    put("dimFraction", set.dimFraction.toDouble())
                    put("lookFilterName", set.lookFilterName)
                    put("isFavorite", set.isFavorite)
                    put("createdAt", set.createdAt)
                    put("updatedAt", set.updatedAt)
                }
            )
        }
        prefs.edit(commit = true) { putString(KEY_LOCK_SCREEN_SETS, arr.toString()) }
    }

    fun loadClockDesignConfig(): ClockDesignConfig? =
        ClockDesignConfig.fromJsonString(prefs.getString(KEY_DEPTH_CLOCK_CONFIG, null))

    fun saveClockDesignConfig(config: ClockDesignConfig) {
        prefs.edit(commit = true) {
            putString(KEY_DEPTH_CLOCK_CONFIG, config.toJsonString())
        }
    }

    companion object {
        const val GUEST_USER_ID = "guest_local"
        private const val PREFS_NAME = "backdrop_guest_store"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_COLLECTIONS = "collections"
        private const val KEY_RECENTLY_VIEWED = "recently_viewed"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_AMOLED_DARK = "amoled_dark"
        private const val KEY_IMAGE_QUALITY = "image_quality"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_SEARCHES = "recent_searches"
        private const val KEY_HOME_CATEGORY_CHIPS = "home_category_chips_enabled"
        private const val KEY_UI_STYLE = "ui_style"
        private const val KEY_HAPTICS_ENABLED = "haptic_feedback_enabled"
        private const val KEY_REDUCED_MOTION = "reduced_motion_enabled"
        private const val KEY_GLASS_DEPTH = "glass_depth_intensity"
        private const val KEY_LOCK_SCREEN_SETS = "lock_screen_sets_json"
        private const val KEY_DEPTH_CLOCK_CONFIG = "depth_clock_design_json"
        private const val KEY_LANGUAGE_CODE = "app_language_code"
        private const val KEY_ONBOARDING_DONE = "onboarding_completed"
        private const val KEY_DYNAMIC_THEMING = "dynamic_theming_enabled"
        private const val KEY_CUSTOM_ACCENT_HEX = "custom_accent_hex"
        private const val KEY_M3_SURFACE_DEPTH = "m3_surface_depth"
        private const val KEY_M3_CORNER_RADIUS = "m3_corner_radius"
        private const val KEY_M3_ELEVATION = "m3_elevation"
        private const val KEY_M3_DENSITY = "m3_density"
        private const val KEY_M3_NAV_STYLE = "m3_nav_style"
        private const val KEY_M3_TYPO_SCALE = "m3_typo_scale"
        private const val KEY_M3_MOTION_SPEED = "m3_motion_speed"
        private const val KEY_GLASS_BLUR = "glass_blur"
        private const val KEY_GLASS_REFRACTION = "glass_refraction"
        private const val KEY_GLASS_TRANSPARENCY = "glass_transparency"
        private const val KEY_GLASS_LIGHT_RESP = "glass_light_resp"
        private const val KEY_GLASS_SPECULAR = "glass_specular"
        private const val KEY_GLASS_BORDER_SOFT = "glass_border_soft"
        private const val KEY_GLASS_CONTRAST = "glass_contrast"
        private const val KEY_DOCK_LAYOUT = "dock_layout_mode"
        private const val KEY_CLASSIC_HANDLES = "use_classic_clock_handles"
        private const val KEY_WEATHER_CITY = "weather_city"
        private const val KEY_WEATHER_USE_LOC = "weather_use_location"
        private const val KEY_OVERLAY_ENABLED = "lock_screen_overlay_enabled"
        private const val KEY_OVERLAY_SET_ID = "active_overlay_set_id"
        private const val KEY_ACTIVE_HOME_WP_ID = "active_home_wallpaper_id"
        private const val KEY_ACTIVE_LOCK_WP_ID = "active_lock_wallpaper_id"
        private const val KEY_ACTIVE_LOCK_CUSTOM_PATH = "active_lock_custom_bitmap_path"
        private const val KEY_LOCK_NOTIFS_ENABLED = "lock_notifications_enabled"
        private const val KEY_HIDE_SENSITIVE_NOTIFS = "hide_sensitive_notifications"
        private const val KEY_MANUAL_DEPTH_ENABLED = "manual_depth_effect_enabled"
        private const val KEY_LOCK_CRED_TYPE = "lock_credential_type"
        private const val KEY_LOCK_CRED_HASH = "lock_credential_hash"
        private const val KEY_LOCK_CRED_SALT = "lock_credential_salt"
        private const val KEY_REMEMBER_PREV_EDIT = "remember_previous_editing_settings"
        private const val KEY_SCALE_TEXT_WITH_CLOCK_DEF = "scale_text_with_clock_default"
        private const val KEY_STUDIO_FONT = "selected_studio_font"
        private const val KEY_LIQUID_GLASS_TIER = "liquid_glass_tier"
        private const val KEY_GLOW_MATERIAL_JSON = "glow_material_json"
        private const val KEY_NOTIF_STYLE_JSON = "notification_style_json"
        private const val KEY_CHARGING_ANIM_JSON = "charging_animation_json"
        private const val KEY_UNLOCK_BTN_JSON = "unlock_button_theme_json"
        private const val KEY_ON_DEVICE_ENABLED = "on_device_collection_enabled"
        private const val KEY_ON_DEVICE_FOLDERS = "on_device_folder_uris"
        private const val KEY_ON_DEVICE_PHOTOS = "on_device_photo_uris"
        private const val KEY_OFFLINE_TARGET_COUNT = "offline_pack_target_count"
        private const val KEY_OFFLINE_DOWNLOADED_IDS = "offline_downloaded_ids"
        private const val KEY_OFFLINE_STATUS = "offline_pack_status"
        private const val KEY_SYNC_HOME_WITH_LOCK = "sync_home_with_lock_mode"
        private const val KEY_STUDIO_WIDGETS_JSON = "studio_widgets_json"
        private const val KEY_DRAG_EXPAND_NOTIFS = "drag_to_expand_notifications"
        private const val KEY_SHOW_UNLOCK_BTN = "show_unlock_button_on_lock_screen"
        private const val KEY_SECONDARY_WIDGET_PAGE_LEFT = "secondary_widget_page_left"
        private const val KEY_SECONDARY_WIDGET_PAGE_POSITION = "secondary_widget_page_position"
        private const val KEY_LOCK_SCREEN_MAX_WIDGETS = "lock_screen_max_widgets"
        private const val KEY_WIDGET_PAGE_MAX_WIDGETS = "widget_page_max_widgets"
        private const val KEY_SAMSUNG_MEDIA_PILL = "samsung_media_pill_enabled"
        private const val KEY_PIXEL_CHARGING_WAVE = "pixel_charging_wave_enabled"

        @Volatile
        private var sharedBacking: StoreBacking? = null

        fun getInstance(context: Context): GuestLocalUserStore = GuestLocalUserStore(context.applicationContext)

        private fun getOrCreateBacking(appContext: Context): StoreBacking {
            return sharedBacking ?: synchronized(this) {
                sharedBacking ?: StoreBacking(appContext).also { sharedBacking = it }
            }
        }

        private fun loadFavoritesFrom(prefs: android.content.SharedPreferences): List<FavoriteDoc> {
            val raw = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val wpId = arr.optString(i)
                        if (wpId.isNotBlank()) {
                            add(FavoriteDoc(id = wpId, userId = GUEST_USER_ID, wallpaperId = wpId))
                        }
                    }
                }
            }.getOrDefault(emptyList())
        }

        private fun loadCollectionsFrom(prefs: android.content.SharedPreferences): List<UserCollectionDoc> {
            val raw = prefs.getString(KEY_COLLECTIONS, null) ?: return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val idsArr = obj.optJSONArray("wallpaperIds")
                        val ids = buildList {
                            if (idsArr != null) {
                                for (j in 0 until idsArr.length()) {
                                    val v = idsArr.optString(j)
                                    if (v.isNotBlank()) add(v)
                                }
                            }
                        }
                        add(
                            UserCollectionDoc(
                                id = obj.optString("id"),
                                userId = GUEST_USER_ID,
                                name = obj.optString("name"),
                                description = obj.optString("description"),
                                wallpaperIds = ids,
                                coverUrl = obj.optString("coverUrl")
                            )
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }

        private fun loadRecentlyViewedFrom(prefs: android.content.SharedPreferences): List<RecentlyViewedDoc> {
            val raw = prefs.getString(KEY_RECENTLY_VIEWED, null) ?: return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val wpId = arr.optString(i)
                        if (wpId.isNotBlank()) {
                            add(RecentlyViewedDoc(id = wpId, userId = GUEST_USER_ID, wallpaperId = wpId))
                        }
                    }
                }
            }.getOrDefault(emptyList())
        }

        private fun loadPreferencesFrom(prefs: android.content.SharedPreferences): UserPreferencesDoc {
            val searches = runCatching {
                val arr = JSONArray(prefs.getString(KEY_SEARCHES, "[]") ?: "[]")
                buildList {
                    for (i in 0 until arr.length()) {
                        val s = arr.optString(i)
                        if (s.isNotBlank()) add(s)
                    }
                }
            }.getOrDefault(emptyList())

            return UserPreferencesDoc(
                id = "settings",
                userId = GUEST_USER_ID,
                themeMode = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name,
                amoledDark = prefs.getBoolean(KEY_AMOLED_DARK, false),
                imageQuality = prefs.getString(KEY_IMAGE_QUALITY, ImageQualityPref.ORIGINAL.name)
                    ?: ImageQualityPref.ORIGINAL.name,
                wifiOnlyDownloads = prefs.getBoolean(KEY_WIFI_ONLY, false),
                recentSearches = searches,
                homeCategoryChipsEnabled = prefs.getBoolean(KEY_HOME_CATEGORY_CHIPS, false),
                uiStyle = prefs.getString(KEY_UI_STYLE, UiStyleMode.MATERIAL_3.name)
                    ?: UiStyleMode.MATERIAL_3.name,
                hapticFeedbackEnabled = prefs.getBoolean(KEY_HAPTICS_ENABLED, true),
                reducedMotionEnabled = prefs.getBoolean(KEY_REDUCED_MOTION, false),
                glassDepthIntensity = prefs.getFloat(KEY_GLASS_DEPTH, 1.0f).coerceIn(0.55f, 1.45f),
                languageCode = prefs.getString(KEY_LANGUAGE_CODE, AppLanguageOption.ENGLISH.code)
                    ?: AppLanguageOption.ENGLISH.code,
                onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_DONE, false),
                dynamicThemingEnabled = prefs.getBoolean(KEY_DYNAMIC_THEMING, true),
                customAccentHex = prefs.getLong(KEY_CUSTOM_ACCENT_HEX, 0xFF6B57A8L),
                m3SurfaceDepth = prefs.getFloat(KEY_M3_SURFACE_DEPTH, 1.0f).coerceIn(0.6f, 1.4f),
                m3CornerRadiusScale = prefs.getFloat(KEY_M3_CORNER_RADIUS, 1.0f).coerceIn(0.5f, 1.5f),
                m3ElevationScale = prefs.getFloat(KEY_M3_ELEVATION, 1.0f).coerceIn(0f, 1.6f),
                m3DensityScale = prefs.getFloat(KEY_M3_DENSITY, 1.0f).coerceIn(0.85f, 1.25f),
                m3NavStyle = prefs.getString(KEY_M3_NAV_STYLE, MaterialNavStyle.PILL_TONAL.name)
                    ?: MaterialNavStyle.PILL_TONAL.name,
                m3TypographyScale = prefs.getFloat(KEY_M3_TYPO_SCALE, 1.0f).coerceIn(0.85f, 1.25f),
                m3MotionSpeed = prefs.getFloat(KEY_M3_MOTION_SPEED, 1.0f).coerceIn(0.6f, 1.5f),
                glassBlurLevel = prefs.getFloat(KEY_GLASS_BLUR, 1.0f).coerceIn(0.4f, 1.5f),
                glassRefractionLevel = prefs.getFloat(KEY_GLASS_REFRACTION, 1.0f).coerceIn(0.4f, 1.5f),
                glassTransparencyLevel = prefs.getFloat(KEY_GLASS_TRANSPARENCY, 1.0f).coerceIn(0.5f, 1.4f),
                glassLightResponse = prefs.getFloat(KEY_GLASS_LIGHT_RESP, 1.0f).coerceIn(0.4f, 1.5f),
                glassSpecularHighlights = prefs.getFloat(KEY_GLASS_SPECULAR, 1.0f).coerceIn(0.3f, 1.5f),
                glassBorderSoftness = prefs.getFloat(KEY_GLASS_BORDER_SOFT, 1.0f).coerceIn(0.4f, 1.5f),
                glassAdaptiveContrast = prefs.getFloat(KEY_GLASS_CONTRAST, 1.0f).coerceIn(0.7f, 1.5f),
                dockLayoutMode = prefs.getString(KEY_DOCK_LAYOUT, DockLayoutMode.AUTOMATIC.name)
                    ?: DockLayoutMode.AUTOMATIC.name,
                useClassicClockHandles = prefs.getBoolean(KEY_CLASSIC_HANDLES, false),
                weatherCity = prefs.getString(KEY_WEATHER_CITY, "") ?: "",
                weatherUseLocation = prefs.getBoolean(KEY_WEATHER_USE_LOC, false),
                lockScreenOverlayEnabled = prefs.getBoolean(KEY_OVERLAY_ENABLED, false),
                activeOverlaySetId = prefs.getString(KEY_OVERLAY_SET_ID, "") ?: "",
                activeHomeWallpaperId = prefs.getString(KEY_ACTIVE_HOME_WP_ID, "") ?: "",
                activeLockWallpaperId = prefs.getString(KEY_ACTIVE_LOCK_WP_ID, "") ?: "",
                activeLockCustomBitmapPath = prefs.getString(KEY_ACTIVE_LOCK_CUSTOM_PATH, "") ?: "",
                lockNotificationsEnabled = prefs.getBoolean(KEY_LOCK_NOTIFS_ENABLED, true),
                hideSensitiveNotifications = prefs.getBoolean(KEY_HIDE_SENSITIVE_NOTIFS, false),
                manualDepthEffectEnabled = prefs.getBoolean(KEY_MANUAL_DEPTH_ENABLED, true),
                lockCredentialType = prefs.getString(KEY_LOCK_CRED_TYPE, com.example.data.model.LockCredentialType.NONE.name)
                    ?: com.example.data.model.LockCredentialType.NONE.name,
                lockCredentialHash = prefs.getString(KEY_LOCK_CRED_HASH, "") ?: "",
                lockCredentialSalt = prefs.getString(KEY_LOCK_CRED_SALT, "") ?: "",
                rememberPreviousEditingSettings = prefs.getBoolean(KEY_REMEMBER_PREV_EDIT, false),
                scaleTextWithClockDefault = prefs.getBoolean(KEY_SCALE_TEXT_WITH_CLOCK_DEF, true),
                selectedStudioFont = prefs.getString(KEY_STUDIO_FONT, com.example.data.model.BackdropStudioFont.BACKDROP_DEFAULT.name)
                    ?: com.example.data.model.BackdropStudioFont.BACKDROP_DEFAULT.name,
                liquidGlassTier = prefs.getString(KEY_LIQUID_GLASS_TIER, com.example.data.model.LiquidGlassTier.AUTO_RECOMMENDED.name)
                    ?: com.example.data.model.LiquidGlassTier.AUTO_RECOMMENDED.name,
                glowMaterialJson = prefs.getString(KEY_GLOW_MATERIAL_JSON, "") ?: "",
                notificationStyleJson = prefs.getString(KEY_NOTIF_STYLE_JSON, "") ?: "",
                chargingAnimationJson = prefs.getString(KEY_CHARGING_ANIM_JSON, "") ?: "",
                unlockButtonThemeJson = prefs.getString(KEY_UNLOCK_BTN_JSON, "") ?: "",
                onDeviceCollectionEnabled = prefs.getBoolean(KEY_ON_DEVICE_ENABLED, true),
                onDeviceFolderUris = parseStringArray(prefs.getString(KEY_ON_DEVICE_FOLDERS, "[]")),
                onDevicePhotoUris = parseStringArray(prefs.getString(KEY_ON_DEVICE_PHOTOS, "[]")),
                offlinePackTargetCount = prefs.getInt(KEY_OFFLINE_TARGET_COUNT, 60).coerceIn(25, 300),
                offlineDownloadedIds = parseStringArray(prefs.getString(KEY_OFFLINE_DOWNLOADED_IDS, "[]")),
                offlinePackStatus = prefs.getString(KEY_OFFLINE_STATUS, "IDLE") ?: "IDLE",
                syncHomeWithLockMode = prefs.getBoolean(KEY_SYNC_HOME_WITH_LOCK, false),
                studioWidgetsJson = prefs.getString(KEY_STUDIO_WIDGETS_JSON, "") ?: "",
                dragToExpandNotifications = prefs.getBoolean(KEY_DRAG_EXPAND_NOTIFS, true),
                showUnlockButtonOnLockScreen = prefs.getBoolean(KEY_SHOW_UNLOCK_BTN, false),
                secondaryWidgetPageLeft = prefs.getBoolean(KEY_SECONDARY_WIDGET_PAGE_LEFT, true),
                secondaryWidgetPagePosition = prefs.getString(KEY_SECONDARY_WIDGET_PAGE_POSITION, null)
                    ?: if (prefs.getBoolean(KEY_SECONDARY_WIDGET_PAGE_LEFT, true)) {
                        SecondaryWidgetPagePosition.LEFT.name
                    } else {
                        SecondaryWidgetPagePosition.OFF.name
                    },
                lockScreenMaxWidgets = prefs.getInt(KEY_LOCK_SCREEN_MAX_WIDGETS, 4).coerceIn(0, 6),
                widgetPageMaxWidgets = prefs.getInt(KEY_WIDGET_PAGE_MAX_WIDGETS, 8).coerceIn(1, 12),
                samsungMediaPillEnabled = prefs.getBoolean(KEY_SAMSUNG_MEDIA_PILL, true),
                pixelChargingWaveEnabled = prefs.getBoolean(KEY_PIXEL_CHARGING_WAVE, true)
            ).let { PreferenceCodecSubEngine.sanitizePreferences(it) }
        }

        private fun parseStringArray(raw: String?): List<String> =
            PreferenceCodecSubEngine.decodeStringList(raw)

        private fun loadLockScreenSetsFrom(prefs: android.content.SharedPreferences): List<LockScreenSetDoc> {
            val raw = prefs.getString(KEY_LOCK_SCREEN_SETS, null) ?: return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val id = obj.optString("id")
                        val name = obj.optString("name")
                        if (id.isNotBlank() && name.isNotBlank()) {
                            add(
                                LockScreenSetDoc(
                                    id = id,
                                    name = name,
                                    wallpaperId = obj.optString("wallpaperId"),
                                    customPhotoUri = obj.optString("customPhotoUri"),
                                    clockConfigJson = obj.optString("clockConfigJson"),
                                    zoomScale = obj.optDouble("zoomScale", 1.0).toFloat().coerceIn(0.85f, 4f),
                                    panOffsetX = obj.optDouble("panOffsetX", 0.0).toFloat(),
                                    panOffsetY = obj.optDouble("panOffsetY", 0.0).toFloat(),
                                    rotationDegrees = obj.optDouble("rotationDegrees", 0.0).toFloat(),
                                    dimFraction = obj.optDouble("dimFraction", 0.0).toFloat().coerceIn(0f, 0.5f),
                                    lookFilterName = obj.optString("lookFilterName", "ORIGINAL"),
                                    isFavorite = obj.optBoolean("isFavorite", false),
                                    createdAt = obj.optLong("createdAt", 0L),
                                    updatedAt = obj.optLong("updatedAt", 0L)
                                )
                            )
                        }
                    }
                }
            }.getOrDefault(emptyList())
        }
    }
}
