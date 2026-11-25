package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import coil.memory.MemoryCache
import com.example.data.catalog.WallpaperCatalog
import com.example.data.media.BackdropImageLoader
import com.example.data.media.CanonicalSourceLockSubEngine
import com.example.data.media.ThumbnailToFullCrossfadeSubEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WallpaperIdentityConsistencyEngineTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        CanonicalSourceLockSubEngine.clearAllLocksForTesting(context)
    }

    @After
    fun tearDown() {
        CanonicalSourceLockSubEngine.clearAllLocksForTesting(context)
    }

    @Test
    fun canonicalSourceLockSubEngineKeepsExactSameUnsplashSlugAcrossThumbAndFullRes() {
        val sample = WallpaperCatalog.allWallpapers.first { !it.isBundledAsset }
        val thumbUrls = CanonicalSourceLockSubEngine.resolveLockedCandidates(context, sample, useFullRes = false)
        val fullUrls = CanonicalSourceLockSubEngine.resolveLockedCandidates(context, sample, useFullRes = true)
        val downloadUrls = CanonicalSourceLockSubEngine.resolveLockedCandidates(context, sample, useFullRes = true, forDownload = true)

        assertTrue(thumbUrls.isNotEmpty())
        assertTrue(fullUrls.isNotEmpty())
        assertTrue(downloadUrls.isNotEmpty())

        val thumbSlugs = thumbUrls.mapNotNull { CanonicalSourceLockSubEngine.extractUnsplashSlug(it) }.toSet()
        val fullSlugs = fullUrls.mapNotNull { CanonicalSourceLockSubEngine.extractUnsplashSlug(it) }.toSet()
        val downloadSlugs = downloadUrls.mapNotNull { CanonicalSourceLockSubEngine.extractUnsplashSlug(it) }.toSet()

        assertEquals("Thumbnail candidates must lock to exactly 1 photo slug", 1, thumbSlugs.size)
        assertEquals("Full-res candidates must lock to the exact same photo slug as thumbnail", thumbSlugs, fullSlugs)
        assertEquals("Download candidates must lock to the exact same photo slug as display", thumbSlugs, downloadSlugs)
        assertTrue(thumbUrls.all { it.contains("fm=jpg") && !it.contains("auto=format") })
        assertTrue(fullUrls.all { it.contains("fm=jpg") && !it.contains("auto=format") })
    }

    @Test
    fun canonicalFallbackLockSynchronizesBothThumbnailAndFullRes() {
        val sample = WallpaperCatalog.allWallpapers.first { !it.isBundledAsset }
        val promotedSlot = CanonicalSourceLockSubEngine.promoteDeterministicFallback(context, sample)
        assertEquals(1, promotedSlot)

        val thumbUrls = CanonicalSourceLockSubEngine.resolveLockedCandidates(context, sample, useFullRes = false)
        val fullUrls = CanonicalSourceLockSubEngine.resolveLockedCandidates(context, sample, useFullRes = true)

        assertEquals(1, thumbUrls.size)
        assertEquals(1, fullUrls.size)
        val thumbPicsumId = thumbUrls.first().substringAfter("/id/").substringBefore("/")
        val fullPicsumId = fullUrls.first().substringAfter("/id/").substringBefore("/")
        assertEquals("Fallback mirror ID must match between thumbnail and full-res", thumbPicsumId, fullPicsumId)
    }

    @Test
    fun thumbnailToFullCrossfadeSubEngineBridgesSameWallpaperFromMemoryCache() {
        val sample = WallpaperCatalog.allWallpapers.first { !it.isBundledAsset }
        val thumbBitmap = Bitmap.createBitmap(12, 18, Bitmap.Config.ARGB_8888)
        val thumbKey = MemoryCache.Key(ThumbnailToFullCrossfadeSubEngine.thumbMemoryCacheKey(sample.id))
        BackdropImageLoader.get(context).memoryCache?.set(thumbKey, MemoryCache.Value(thumbBitmap))

        val bridged = ThumbnailToFullCrossfadeSubEngine.findInMemorySameWallpaperBitmap(context, sample.id)
        assertNotNull("Full-res viewer must find exact same-wallpaper thumbnail in memory cache", bridged)
        assertEquals(12, bridged?.width)
        assertEquals(18, bridged?.height)
    }
}
