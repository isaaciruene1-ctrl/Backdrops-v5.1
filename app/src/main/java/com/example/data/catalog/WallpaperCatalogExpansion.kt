package com.example.data.catalog

import com.example.data.model.ColorFamily
import com.example.data.model.DepthSubjectHint
import com.example.data.model.DiscoveryCollection
import com.example.data.model.ResolutionTier
import com.example.data.model.Wallpaper
import com.example.data.model.WallpaperCategory

/**
 * Curated Backdrop Expansion Pack:
 * - +990 unique photographic entries across the 11 core categories (90 per category), bringing catalog past 3,000 artworks.
 * - Zero procedural:// assets and zero removed Stock/Superhero categories.
 */
internal fun buildCatalogExpansion(): List<Wallpaper> {
    val result = ArrayList<Wallpaper>(1100)
    var order = 200

    val palettes: List<Pair<ColorFamily, List<Long>>> = listOf(
        ColorFamily.BLUE to listOf(0xFF14284B, 0xFF3E6BA8, 0xFF8FB8E8),
        ColorFamily.PURPLE to listOf(0xFF1F1638, 0xFF6B57A8, 0xFFC5B4F0),
        ColorFamily.GREEN to listOf(0xFF0E2923, 0xFF2F856B, 0xFF9CD9BE),
        ColorFamily.ORANGE to listOf(0xFF321810, 0xFFC95E4B, 0xFFF3B082),
        ColorFamily.MONOCHROME to listOf(0xFF101116, 0xFF3E424E, 0xFFC9CDD6),
        ColorFamily.WARM to listOf(0xFF341E1C, 0xFFB86B5E, 0xFFF0C6B2),
        ColorFamily.COOL to listOf(0xFF112633, 0xFF427A9B, 0xFF9FD6EA)
    )

    val abstractV2Descriptors = listOf(
        "Ribbon", "Prism", "Fold", "Sculpt", "Fluid", "Glass", "Contour", "Torsion", "Silk", "Vortex"
    )
    for (i in 1..50) {
        val idx = i - 1
        val (colorFamily, palette) = palettes[idx % palettes.size]
        val tier = if (idx % 2 == 0) ResolutionTier.UHD_4K else ResolutionTier.QHD_1440
        val ratio = if (idx % 5 == 0) "2:3" else "9:16"
        val (w, h) = when (tier) {
            ResolutionTier.UHD_4K -> if (ratio == "2:3") 2160 to 3240 else 2160 to 3840
            else -> if (ratio == "2:3") 1440 to 2160 else 1440 to 2560
        }
        val depthReady = idx % 2 == 0
        val id = "exp_abstract_v2_${i.toString().padStart(3, '0')}"
        val descriptor = abstractV2Descriptors[idx % abstractV2Descriptors.size]
        result += Wallpaper(
            id = id,
            title = "Abstract $descriptor Studio ${i.toString().padStart(2, '0')}",
            description = "Sculpted abstract light forms and layered geometric depth — ${tier.badge} edition.",
            category = WallpaperCategory.ABSTRACT,
            collections = listOfNotNull(
                DiscoveryCollection.ABSTRACT,
                DiscoveryCollection.ARTISTIC,
                DiscoveryCollection.NEW_ARRIVALS,
                if (tier == ResolutionTier.UHD_4K) DiscoveryCollection.ULTRA_HD_4K else null,
                if (depthReady) DiscoveryCollection.DEPTH_STUDIO else null,
                DiscoveryCollection.COLOUR_COLLECTIONS
            ).distinct(),
            thumbnailUrl = VerifiedMirrorPool.uniquePrimaryThumbUrl(id = id, category = WallpaperCategory.ABSTRACT),
            fullResUrl = VerifiedMirrorPool.uniquePrimaryFullUrl(
                id = id,
                category = WallpaperCategory.ABSTRACT,
                width = w,
                height = h,
                quality = if (tier == ResolutionTier.UHD_4K) 94 else 92
            ),
            fallbackResId = 0,
            width = w,
            height = h,
            resolutionTier = tier,
            aspectRatio = ratio,
            dominantColors = palette,
            colorFamily = colorFamily,
            tags = listOf("Abstract", descriptor, colorFamily.label, tier.badge, if (depthReady) "Depth Ready" else "Canvas"),
            creatorName = "Backdrops Curated",
            creatorSource = "Backdrops Abstract Studio",
            isDownloadable = true,
            supportsDepthEffect = depthReady,
            depthSubjectHint = DepthSubjectHint.entries[idx % DepthSubjectHint.entries.size],
            popularityScore = 82 + (idx % 18),
            addedOrder = 3200 + i
        )
    }

    val amoledV2Descriptors = listOf(
        "Void", "Obsidian", "Eclipse", "Carbon", "Nocturne", "Onyx", "Abyss", "Edge", "Ember", "Shadow"
    )
    for (i in 1..50) {
        val idx = i - 1
        val (colorFamily, basePalette) = palettes[idx % palettes.size]
        val amoledPalette = listOf(0xFF000000L, basePalette[1], basePalette[2])
        val tier = if (idx % 2 == 0) ResolutionTier.UHD_4K else ResolutionTier.QHD_1440
        val ratio = if (idx % 6 == 0) "2:3" else "9:16"
        val (w, h) = when (tier) {
            ResolutionTier.UHD_4K -> if (ratio == "2:3") 2160 to 3240 else 2160 to 3840
            else -> if (ratio == "2:3") 1440 to 2160 else 1440 to 2560
        }
        val depthReady = idx % 2 == 0
        val id = "exp_amoled_v2_${i.toString().padStart(3, '0')}"
        val descriptor = amoledV2Descriptors[idx % amoledV2Descriptors.size]
        result += Wallpaper(
            id = id,
            title = "AMOLED True Black $descriptor ${i.toString().padStart(2, '0')}",
            description = "Pure #000000 pixel-off obsidian background with subtle luminous contrast for OLED displays.",
            category = WallpaperCategory.AMOLED,
            collections = listOfNotNull(
                DiscoveryCollection.AMOLED,
                DiscoveryCollection.NEW_ARRIVALS,
                if (tier == ResolutionTier.UHD_4K) DiscoveryCollection.ULTRA_HD_4K else null,
                if (depthReady) DiscoveryCollection.DEPTH_STUDIO else null,
                DiscoveryCollection.COLOUR_COLLECTIONS
            ).distinct(),
            thumbnailUrl = VerifiedMirrorPool.uniquePrimaryThumbUrl(id = id, category = WallpaperCategory.AMOLED, grayscale = true),
            fullResUrl = VerifiedMirrorPool.uniquePrimaryFullUrl(
                id = id,
                category = WallpaperCategory.AMOLED,
                width = w,
                height = h,
                quality = if (tier == ResolutionTier.UHD_4K) 94 else 92,
                grayscale = true
            ),
            fallbackResId = 0,
            width = w,
            height = h,
            resolutionTier = tier,
            aspectRatio = ratio,
            dominantColors = amoledPalette,
            colorFamily = colorFamily,
            tags = listOf("AMOLED", "True Black", "#000000", descriptor, colorFamily.label, tier.badge),
            creatorName = "Backdrops Curated",
            creatorSource = "Backdrops Obsidian Archive",
            isDownloadable = true,
            supportsDepthEffect = depthReady,
            depthSubjectHint = DepthSubjectHint.entries[idx % DepthSubjectHint.entries.size],
            popularityScore = 84 + (idx % 16),
            addedOrder = 3250 + i
        )
    }

    val existingCategories = listOf(
        WallpaperCategory.NATURE,
        WallpaperCategory.ABSTRACT,
        WallpaperCategory.AMOLED,
        WallpaperCategory.FUTURISTIC,
        WallpaperCategory.ARCHITECTURE,
        WallpaperCategory.AUTOMOTIVE,
        WallpaperCategory.SPACE,
        WallpaperCategory.MINIMALIST,
        WallpaperCategory.ANIME_ARTISTIC,
        WallpaperCategory.CINEMATIC,
        WallpaperCategory.DEPTH_STUDIO
    )

    val descriptors = listOf(
        "Horizon", "Solstice", "Monolith", "Drift", "Canopy", "Silhouette", "Mirage",
        "Nocturne", "Crest", "Glacier", "Atlas", "Vesper", "Cascade", "Prism"
    )

    for (i in 1..90) {
        existingCategories.forEachIndexed { catIdx, category ->
            val idx = i - 1
            val (colorFamily, palette) = palettes[(idx + catIdx) % palettes.size]
            val tier = when (idx % 3) {
                0 -> ResolutionTier.UHD_4K
                1 -> ResolutionTier.QHD_1440
                else -> ResolutionTier.FHD_1080
            }
            val ratio = if (idx % 7 == 0) "2:3" else "9:16"
            val (w, h) = when (tier) {
                ResolutionTier.UHD_4K -> if (ratio == "2:3") 2160 to 3240 else 2160 to 3840
                ResolutionTier.QHD_1440 -> if (ratio == "2:3") 1440 to 2160 else 1440 to 2560
                ResolutionTier.FHD_1080 -> if (ratio == "2:3") 1080 to 1620 else 1080 to 1920
            }
            val depthEligible = category == WallpaperCategory.DEPTH_STUDIO || (idx % 3 == 0)
            val id = "exp_${category.name.lowercase()}_${i.toString().padStart(3, '0')}"

            val isGray = category == WallpaperCategory.AMOLED && idx % 3 == 0
            val quality = when (tier) {
                ResolutionTier.UHD_4K -> 94
                ResolutionTier.QHD_1440 -> 92
                ResolutionTier.FHD_1080 -> 90
            }
            val thumbUrl = VerifiedMirrorPool.uniquePrimaryThumbUrl(id = id, category = category, grayscale = isGray)
            val fullUrl = VerifiedMirrorPool.uniquePrimaryFullUrl(
                id = id,
                category = category,
                width = w,
                height = h,
                quality = quality,
                grayscale = isGray
            )

            val collections = buildList {
                if (idx % 8 == 0) add(DiscoveryCollection.FEATURED)
                if (idx % 6 == 0) add(DiscoveryCollection.EDITORS_PICKS)
                if (idx % 5 == 0) add(DiscoveryCollection.NEW_ARRIVALS)
                if (idx % 3 == 0) add(DiscoveryCollection.PHOTOGRAPHY)
                if (tier == ResolutionTier.UHD_4K) add(DiscoveryCollection.ULTRA_HD_4K)
                if (category == WallpaperCategory.AMOLED) add(DiscoveryCollection.AMOLED)
                if (depthEligible) add(DiscoveryCollection.DEPTH_STUDIO)
                if (category == WallpaperCategory.ANIME_ARTISTIC) add(DiscoveryCollection.ANIME)
                if (category == WallpaperCategory.FUTURISTIC) add(DiscoveryCollection.GAMING)
                if (category == WallpaperCategory.NATURE && idx % 4 == 0) add(DiscoveryCollection.ANIMALS)
                if (category == WallpaperCategory.ANIME_ARTISTIC || category == WallpaperCategory.ABSTRACT) add(DiscoveryCollection.ARTISTIC)
                add(DiscoveryCollection.COLOUR_COLLECTIONS)
            }.distinct()

            val fallbackRes = 0

            val descriptor = descriptors[(idx + catIdx) % descriptors.size]
            result += Wallpaper(
                id = id,
                title = "${category.displayName} $descriptor ${i.toString().padStart(2, '0')}",
                description = "${category.tagline} — Curated ${tier.badge} edition.",
                category = category,
                collections = collections,
                thumbnailUrl = thumbUrl,
                fullResUrl = fullUrl,
                fallbackResId = fallbackRes,
                width = w,
                height = h,
                resolutionTier = tier,
                aspectRatio = ratio,
                dominantColors = palette,
                colorFamily = colorFamily,
                tags = listOf(category.displayName, descriptor, colorFamily.label, tier.badge, if (depthEligible) "Depth Ready" else "Canvas"),
                creatorName = "Backdrops Curated",
                creatorSource = "Backdrops Photography Archive",
                isDownloadable = true,
                supportsDepthEffect = depthEligible,
                depthSubjectHint = DepthSubjectHint.entries[(idx + catIdx) % DepthSubjectHint.entries.size],
                popularityScore = 76 + ((idx * 7 + catIdx) % 24),
                addedOrder = order++
            )
        }
    }

    return result
}
