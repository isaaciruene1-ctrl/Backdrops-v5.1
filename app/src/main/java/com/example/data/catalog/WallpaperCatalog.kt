package com.example.data.catalog

import com.example.data.model.DiscoveryCollection
import com.example.data.model.SearchFilters
import com.example.data.model.SearchSort
import com.example.data.model.Wallpaper
import com.example.data.model.WallpaperCategory

object WallpaperCatalog {

    const val TARGET_CATALOG_SIZE = 3100

    // Ensure every wallpaper preserves a unique photographic URL without procedural generation or static fallbacks
    val allWallpapers: List<Wallpaper> by lazy {
        val coreParts = buildCatalogPartOne() +
            buildCatalogPartTwo() +
            buildCatalogPartThree() +
            buildCatalogPartFour() +
            buildCatalogPartFive() +
            buildCatalogPartSix() +
            buildCatalogPartSeven() +
            buildCatalogPartEight() +
            buildCatalogPartNine() +
            buildCatalogPartTen() +
            buildCatalogPartTwelve() +
            buildCatalogPartEleven()

        val seenThumbUrls = HashSet<String>(4096)
        val seenIds = HashSet<String>(4096)
        val uniqueCore = coreParts.filter { wp ->
            seenThumbUrls.add(wp.thumbnailUrl) && seenIds.add(wp.id)
        }
        val remainingNeeded = (TARGET_CATALOG_SIZE - uniqueCore.size).coerceAtLeast(0)
        val expansionTail = buildCatalogExpansion()
            .asSequence()
            .filter { wp -> seenThumbUrls.add(wp.thumbnailUrl) && seenIds.add(wp.id) }
            .take(remainingNeeded)
            .toList()

        (uniqueCore + expansionTail).take(TARGET_CATALOG_SIZE)
    }

    private val byId: Map<String, Wallpaper> by lazy {
        allWallpapers.associateBy { it.id }
    }

    fun getById(id: String): Wallpaper? = byId[id]

    fun getForCollection(collection: DiscoveryCollection): List<Wallpaper> = when (collection) {
        DiscoveryCollection.FEATURED -> allWallpapers.filter { DiscoveryCollection.FEATURED in it.collections }
        DiscoveryCollection.TRENDING -> allWallpapers.sortedByDescending { it.popularityScore }.take(24)
        DiscoveryCollection.EDITORS_PICKS -> allWallpapers.filter { DiscoveryCollection.EDITORS_PICKS in it.collections }
        DiscoveryCollection.NEW_ARRIVALS -> allWallpapers.sortedByDescending { it.addedOrder }.take(24)
        DiscoveryCollection.ULTRA_HD_4K -> allWallpapers.filter { DiscoveryCollection.ULTRA_HD_4K in it.collections }
        DiscoveryCollection.AMOLED -> allWallpapers.filter {
            it.category == WallpaperCategory.AMOLED || DiscoveryCollection.AMOLED in it.collections
        }
        DiscoveryCollection.DEPTH_STUDIO -> allWallpapers.filter { it.supportsDepthEffect }
        DiscoveryCollection.NATURE -> allWallpapers.filter { it.category == WallpaperCategory.NATURE }
        DiscoveryCollection.ABSTRACT -> allWallpapers.filter { it.category == WallpaperCategory.ABSTRACT }
        DiscoveryCollection.ARCHITECTURE -> allWallpapers.filter { it.category == WallpaperCategory.ARCHITECTURE }
        DiscoveryCollection.AUTOMOTIVE -> allWallpapers.filter { it.category == WallpaperCategory.AUTOMOTIVE }
        DiscoveryCollection.MINIMALIST -> allWallpapers.filter { it.category == WallpaperCategory.MINIMALIST }
        DiscoveryCollection.SPACE -> allWallpapers.filter { it.category == WallpaperCategory.SPACE }
        DiscoveryCollection.ANIME -> allWallpapers.filter { DiscoveryCollection.ANIME in it.collections }
        DiscoveryCollection.GAMING -> allWallpapers.filter { DiscoveryCollection.GAMING in it.collections }
        DiscoveryCollection.ANIMALS -> allWallpapers.filter { DiscoveryCollection.ANIMALS in it.collections }
        DiscoveryCollection.PHOTOGRAPHY -> allWallpapers.filter { DiscoveryCollection.PHOTOGRAPHY in it.collections }
        DiscoveryCollection.ARTISTIC -> allWallpapers.filter {
            it.category == WallpaperCategory.ANIME_ARTISTIC || DiscoveryCollection.ARTISTIC in it.collections
        }
        DiscoveryCollection.CINEMATIC -> allWallpapers.filter {
            it.category == WallpaperCategory.CINEMATIC || DiscoveryCollection.CINEMATIC in it.collections
        }
        DiscoveryCollection.COLOUR_COLLECTIONS -> allWallpapers.filter { it.dominantColors.isNotEmpty() }
    }

    fun getRelated(wallpaper: Wallpaper, limit: Int = 10): List<Wallpaper> =
        allWallpapers
            .asSequence()
            .filter { it.id != wallpaper.id }
            .sortedByDescending { candidate ->
                var score = 0
                if (candidate.category == wallpaper.category) score += 5
                if (candidate.colorFamily == wallpaper.colorFamily) score += 3
                score += candidate.tags.count { it in wallpaper.tags } * 2
                score
            }
            .take(limit)
            .toList()

    fun search(filters: SearchFilters): List<Wallpaper> {
        val q = filters.query.trim().lowercase()
        val filtered = allWallpapers.filter { wp ->
            val matchesQuery = if (q.isEmpty()) {
                true
            } else {
                wp.title.lowercase().contains(q) ||
                    wp.description.lowercase().contains(q) ||
                    wp.category.displayName.lowercase().contains(q) ||
                    wp.colorFamily.label.lowercase().contains(q) ||
                    wp.resolutionTier.badge.lowercase().contains(q) ||
                    wp.creatorName.lowercase().contains(q) ||
                    wp.tags.any { it.lowercase().contains(q) }
            }
            val matchesRes = filters.resolutionTier?.let { wp.resolutionTier == it } ?: true
            val matchesAmoled = if (filters.amoledOnly) {
                wp.category == WallpaperCategory.AMOLED || DiscoveryCollection.AMOLED in wp.collections
            } else true
            val matchesPortrait = if (filters.portraitOnly) wp.height >= wp.width else true
            val matchesDepth = if (filters.depthStudioOnly) wp.supportsDepthEffect else true
            val matchesColor = filters.colorFamily?.let { wp.colorFamily == it } ?: true
            val matchesCategory = filters.category?.let { cat ->
                if (cat == WallpaperCategory.DEPTH_STUDIO) wp.supportsDepthEffect else wp.category == cat
            } ?: true
            val matchesCollection = filters.collection?.let { col ->
                when (col) {
                    DiscoveryCollection.DEPTH_STUDIO -> wp.supportsDepthEffect
                    DiscoveryCollection.AMOLED -> wp.category == WallpaperCategory.AMOLED || col in wp.collections
                    else -> col in wp.collections || wp.category.displayName.equals(col.title, ignoreCase = true)
                }
            } ?: true

            matchesQuery && matchesRes && matchesAmoled && matchesPortrait &&
                matchesDepth && matchesColor && matchesCategory && matchesCollection
        }

        return when (filters.sort) {
            SearchSort.RELEVANT -> if (q.isEmpty()) filtered else filtered.sortedByDescending { wp ->
                var score = wp.popularityScore / 10
                if (wp.title.lowercase().contains(q)) score += 20
                if (wp.category.displayName.lowercase().contains(q)) score += 12
                if (wp.tags.any { it.lowercase().contains(q) }) score += 10
                score
            }
            SearchSort.RECENTLY_ADDED -> filtered.sortedByDescending { it.addedOrder }
            SearchSort.MOST_POPULAR -> filtered.sortedByDescending { it.popularityScore }
        }
    }

    val searchSuggestions: List<String> = listOf(
        "Pastel", "AMOLED", "Depth Studio", "4K", "Mountains", "Minimalist",
        "Space", "Neon", "Architecture", "Automotive", "Kyoto", "Cinematic"
    )
}
