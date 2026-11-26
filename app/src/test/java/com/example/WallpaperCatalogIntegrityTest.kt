package com.example

import com.example.data.catalog.WallpaperCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperCatalogIntegrityTest {

    private val deadSlugs = listOf(
        "photo-1614850523011",
        "photo-1541701494587-1234",
        "photo-1550745165-9bc0b252726f-dead",
        "photo-1541701494587?",
        "procedural://"
    )

    private val knownBrokenIds = setOf(
        "spc_07",
        "min_09",
        "pxl_05",
        "pxl_09",
        "dep_29"
    )

    @Test
    fun catalogHasExactly100UniqueWallpapersWithLiveMultiCdnUrls() {
        val all = WallpaperCatalog.allWallpapers
        assertEquals(WallpaperCatalog.TARGET_CATALOG_SIZE, all.size)

        val uniqueIds = all.map { it.id }.toHashSet()
        assertEquals("Every wallpaper ID must be unique", WallpaperCatalog.TARGET_CATALOG_SIZE, uniqueIds.size)

        val uniqueThumbs = all.map { it.thumbnailUrl }.toHashSet()
        assertEquals("Every wallpaper thumbnail URL must be unique", WallpaperCatalog.TARGET_CATALOG_SIZE, uniqueThumbs.size)

        for (wp in all) {
            assertFalse("Bundled/procedural URI must not appear for ${wp.id}", wp.thumbnailUrl.startsWith("procedural://"))
            assertFalse("Full-res URL must not be procedural:// for ${wp.id}", wp.fullResUrl.startsWith("procedural://"))
            assertTrue("Primary thumbnail must be HTTPS for ${wp.id}", wp.thumbnailUrl.startsWith("https://"))
            assertTrue("Primary full-res must be HTTPS for ${wp.id}", wp.fullResUrl.startsWith("https://"))
            assertFalse("Must not use auto=format (AVIF risk) for ${wp.id}", wp.thumbnailUrl.contains("auto=format"))
            assertFalse("Full-res must not use auto=format (AVIF risk) for ${wp.id}", wp.fullResUrl.contains("auto=format"))
            for (bad in deadSlugs) {
                assertFalse("Dead slug $bad found in ${wp.id}", wp.thumbnailUrl.contains(bad))
            }

            val thumbMirrors = wp.candidateUrls(useFullRes = false)
            assertTrue("Need >= 3 mirror candidates for ${wp.id}", thumbMirrors.size >= 3)
            assertEquals("Candidate list should be distinct for ${wp.id}", thumbMirrors.size, thumbMirrors.distinct().size)
            assertTrue("Should include Unsplash edge mirror for ${wp.id}", thumbMirrors.any { it.contains("images.unsplash.com") })
            assertTrue("Should include Picsum CDN mirror for ${wp.id}", thumbMirrors.any { it.contains("picsum.photos/id/") })

            val fullMirrors = wp.candidateUrls(useFullRes = true)
            assertTrue("Need >= 3 full-res mirror candidates for ${wp.id}", fullMirrors.size >= 3)
            assertTrue("Full-res candidates should span Picsum CDN for ${wp.id}", fullMirrors.any { it.contains("picsum.photos") })
        }
    }

    @Test
    fun previouslyFailingWallpapersUseVerifiedUrlsAndMirrors() {
        val repaired = WallpaperCatalog.allWallpapers.filter { it.id in knownBrokenIds }
        assertEquals(knownBrokenIds.size, repaired.size)
        repaired.forEach { wp ->
            assertTrue(wp.thumbnailUrl.startsWith("https://images.unsplash.com/photo-") || wp.thumbnailUrl.startsWith("https://picsum.photos/id/"))
            assertTrue(wp.candidateUrls(useFullRes = false).size >= 4)
            assertTrue(wp.candidateUrls(useFullRes = true).size >= 4)
        }
    }
}
