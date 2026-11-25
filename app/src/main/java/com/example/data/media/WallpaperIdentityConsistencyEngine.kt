package com.example.data.media

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import coil.memory.MemoryCache
import com.example.data.catalog.VerifiedMirrorPool
import com.example.data.model.Wallpaper
import com.example.data.model.WallpaperCategory
import com.example.engine.performance.ZeroLagPerformanceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Wallpaper Identity Consistency Engine:
 * Guarantees that every card thumbnail, loading placeholder, Detail Screen, Preview Studio,
 * and Lock Screen Mode always renders the EXACT SAME photograph for a given `wallpaper.id`.
 *
 * Dual-sub-engine architecture:
 *  - [CanonicalSourceLockSubEngine]: Locks one deterministic photograph per `wallpaper.id`
 *    across all resolution tiers (`thumb`, `display_full`, `download_4k`). Retries identical-photo
 *    JPEG delivery parameters before ever promoting a persisted deterministic fallback.
 *  - [ThumbnailToFullCrossfadeSubEngine]: Bridges full-resolution viewers directly from the
 *    already-decoded thumbnail bitmap (`${id}_thumb`) of the exact same wallpaper and performs
 *    silent background recovery without showing blocking Reload overlays.
 */
object CanonicalSourceLockSubEngine {

    private const val PREFS_NAME = "backdrop_canonical_source_locks"
    private const val KEY_PREFIX = "lock_slot_"

    // Slot 0 = Primary canonical photo (Unsplash slug in Wallpaper.thumbnailUrl / fullResUrl).
    // Slots >= 1 = Persisted deterministic fallback index for that specific wallpaper.id.
    private val inMemorySlotLocks = ConcurrentHashMap<String, Int>()

    @Volatile
    private var prefsInstance: SharedPreferences? = null

    private fun prefs(context: Context): SharedPreferences {
        return prefsInstance ?: synchronized(this) {
            prefsInstance ?: context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .also { prefsInstance = it }
        }
    }

    fun getLockedSlot(context: Context, wallpaperId: String): Int {
        inMemorySlotLocks[wallpaperId]?.let { return it }
        val persisted = prefs(context).getInt("$KEY_PREFIX$wallpaperId", 0).coerceAtLeast(0)
        inMemorySlotLocks[wallpaperId] = persisted
        return persisted
    }

    fun lockFallbackSlot(context: Context, wallpaperId: String, slotIndex: Int) {
        val safeSlot = slotIndex.coerceAtLeast(0)
        val prev = inMemorySlotLocks.put(wallpaperId, safeSlot)
        prefs(context).edit().putInt("$KEY_PREFIX$wallpaperId", safeSlot).apply()
        if (prev != safeSlot) {
            runCatching { BackdropImageLoader.evictWallpaperCache(context.applicationContext, wallpaperId) }
        }
    }

    fun clearAllLocksForTesting(context: Context? = null) {
        inMemorySlotLocks.clear()
        context?.let {
            prefs(it).edit().clear().apply()
        }
    }

    /**
     * Extracts the canonical Unsplash photo slug (e.g. `1506744038136-46273834b3fb`) if the
     * wallpaper uses Unsplash, preserving 1-to-1 photo identity across every resolution tier.
     */
    internal fun extractUnsplashSlug(url: String): String? {
        val marker = "images.unsplash.com/photo-"
        val idx = url.indexOf(marker)
        if (idx < 0) return null
        val start = idx + marker.length
        val end = url.indexOf('?', start).let { if (it < 0) url.length else it }
        return url.substring(start, end).takeIf { it.isNotBlank() }
    }

    private fun buildDisplayDimensions(wallpaper: Wallpaper, useFullRes: Boolean, forDownload: Boolean): Pair<Int, Int> {
        if (!useFullRes) return 720 to 1120
        val targetW = if (forDownload) wallpaper.width else minOf(wallpaper.width, 1280)
        val targetH = if (forDownload) wallpaper.height else minOf(wallpaper.height, 2240)
        return targetW.coerceIn(720, 2160) to targetH.coerceIn(1080, 3840)
    }

    private fun buildSamePhotoUnsplashCandidates(
        wallpaper: Wallpaper,
        slug: String,
        useFullRes: Boolean,
        forDownload: Boolean
    ): List<String> {
        val primaryRaw = if (useFullRes) wallpaper.fullResUrl else wallpaper.thumbnailUrl
        val primaryJpeg = primaryRaw.replace("auto=format", "fm=jpg")
        val (w, h) = buildDisplayDimensions(wallpaper, useFullRes, forDownload)
        val q = if (useFullRes) 90 else 82
        val grayscale = wallpaper.category == WallpaperCategory.AMOLED &&
            (primaryJpeg.contains("sat=-85") || primaryJpeg.contains("grayscale"))
        val satParam = if (grayscale) "&sat=-85" else ""

        // Extract original focal crop query if present so displayFull uses identical framing to thumb.
        val queryStart = primaryJpeg.indexOf('?')
        val lockedPrimaryUrl = if (queryStart > 0 && !forDownload && useFullRes) {
            val existingQuery = primaryJpeg.substring(queryStart + 1)
                .replace(Regex("(&|^)w=\\d+"), "$1w=$w")
                .replace(Regex("(&|^)h=\\d+"), "$1h=$h")
                .replace(Regex("(&|^)q=\\d+"), "$1q=$q")
            "https://images.unsplash.com/photo-$slug?$existingQuery"
        } else {
            primaryJpeg
        }

        // Tier 1 & Tier 2 point to the EXACT SAME Unsplash photograph (`photo-$slug`),
        // varying only CDN crop/size flags so retries NEVER substitute a different image.
        val standardCropSamePhoto = "https://images.unsplash.com/photo-$slug?fm=jpg&fit=crop&w=$w&h=$h&q=$q$satParam"
        val compactSamePhoto = "https://images.unsplash.com/photo-$slug?fm=jpg&w=${minOf(w, 1080)}&q=84$satParam"

        return buildList {
            if (lockedPrimaryUrl.startsWith("https://")) add(lockedPrimaryUrl)
            add(standardCropSamePhoto)
            add(compactSamePhoto)
        }.distinct()
    }

    /**
     * Returns the ordered URL list for the wallpaper's currently locked canonical photograph.
     * For slot 0, every URL points to the exact same Unsplash photograph (varying only JPEG flags).
     * For fallback slots (>= 1), returns the exact locked Picsum mirror across all screens.
     */
    fun resolveLockedCandidates(
        context: Context,
        wallpaper: Wallpaper,
        useFullRes: Boolean,
        forDownload: Boolean = false
    ): List<String> {
        val slot = getLockedSlot(context, wallpaper.id)
        val (w, h) = buildDisplayDimensions(wallpaper, useFullRes, forDownload)
        val grayscale = wallpaper.category == WallpaperCategory.AMOLED &&
            (wallpaper.thumbnailUrl.contains("sat=-85") || wallpaper.thumbnailUrl.contains("grayscale"))

        if (slot == 0) {
            val slug = extractUnsplashSlug(wallpaper.thumbnailUrl)
                ?: extractUnsplashSlug(wallpaper.fullResUrl)
            if (slug != null) {
                return buildSamePhotoUnsplashCandidates(wallpaper, slug, useFullRes, forDownload)
            }
            val primary = (if (useFullRes) wallpaper.fullResUrl else wallpaper.thumbnailUrl)
                .replace("auto=format", "fm=jpg")
            return listOf(primary)
        }

        val salt = if (slot == 1) 31L else 73L
        val lockedPicsum = VerifiedMirrorPool.verifiedPicsumUrl(
            id = wallpaper.id,
            salt = salt,
            width = w,
            height = h,
            grayscale = grayscale
        )
        return listOf(lockedPicsum)
    }

    /**
     * Promotes to a deterministic fallback slot ONLY when every query tier of the locked primary
     * photograph failed cleanly at the HTTP origin. Calling this locks the fallback for BOTH
     * thumbnail and full-resolution screens so no visual mismatch can ever occur.
     */
    fun promoteDeterministicFallback(context: Context, wallpaper: Wallpaper): Int {
        val current = getLockedSlot(context, wallpaper.id)
        val next = (current + 1).coerceAtMost(2)
        if (next != current) {
            lockFallbackSlot(context, wallpaper.id, next)
        }
        return next
    }
}

object ThumbnailToFullCrossfadeSubEngine {

    fun thumbMemoryCacheKey(wallpaperId: String): String = "${wallpaperId}_thumb"

    fun fullMemoryCacheKey(wallpaperId: String): String = "${wallpaperId}_full"

    /**
     * Synchronously resolves an already-decoded bitmap of the EXACT SAME `wallpaper.id`
     * from Coil's MemoryCache or InstantLockScreenEngine so full-res views render zero blank flash.
     */
    fun findInMemorySameWallpaperBitmap(context: Context, wallpaperId: String): Bitmap? {
        val warmState = ZeroLagPerformanceEngine.InstantLockScreenEngine.warmState.value
        if (warmState != null && warmState.wallpaperId == wallpaperId) {
            warmState.wallpaperBitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }?.let {
                return it
            }
        }
        val memCache = runCatching { BackdropImageLoader.get(context).memoryCache }.getOrNull()
        val fullHit = memCache?.get(MemoryCache.Key(fullMemoryCacheKey(wallpaperId)))
            ?.bitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
        if (fullHit != null) return fullHit

        return memCache?.get(MemoryCache.Key(thumbMemoryCacheKey(wallpaperId)))
            ?.bitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
    }

    /**
     * Reads the cached same-wallpaper thumbnail or offline file from disk on IO so even after
     * RAM cache trimming, Detail View and Lock Screen bridge seamlessly from the identical photo.
     */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    suspend fun loadDiskSameWallpaperBitmap(context: Context, wallpaper: Wallpaper): Bitmap? =
        withContext(Dispatchers.IO) {
            findInMemorySameWallpaperBitmap(context, wallpaper.id)?.let { return@withContext it }

            if (wallpaper.isBundledAsset && wallpaper.fallbackResId != 0) {
                return@withContext runCatching {
                    BitmapFactory.decodeResource(context.resources, wallpaper.fallbackResId)
                }.getOrNull()
            }
            if (wallpaper.thumbnailUrl.startsWith("content://") || wallpaper.thumbnailUrl.startsWith("file://")) {
                return@withContext runCatching {
                    context.contentResolver.openInputStream(Uri.parse(wallpaper.thumbnailUrl))?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.getOrNull()
            }
            OfflinePackDownloadManager.getDownloadedFile(context, wallpaper.id)?.let { file ->
                val bmp = runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
                if (bmp != null && !bmp.isRecycled) return@withContext bmp
            }

            val diskCache = runCatching { BackdropImageLoader.get(context).diskCache }.getOrNull()
            for (key in listOf(fullMemoryCacheKey(wallpaper.id), thumbMemoryCacheKey(wallpaper.id))) {
                val bmp = runCatching {
                    diskCache?.openSnapshot(key)?.use { snap ->
                        BitmapFactory.decodeFile(snap.data.toFile().absolutePath)
                    }
                }.getOrNull()
                if (bmp != null && !bmp.isRecycled && bmp.width > 0 && bmp.height > 0) {
                    return@withContext bmp
                }
            }
            null
        }
}
