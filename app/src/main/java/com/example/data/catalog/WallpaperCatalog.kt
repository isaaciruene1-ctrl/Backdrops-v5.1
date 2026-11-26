package com.example.data.catalog

import com.example.data.model.DiscoveryCollection
import com.example.data.model.SearchFilters
import com.example.data.model.SearchSort
import com.example.data.model.Wallpaper
import com.example.data.model.WallpaperCategory

object WallpaperCatalog {

    const val TARGET_CATALOG_SIZE = 100
    const val CURATED_ONLINE_LIMIT = 100

    private val priorityShowcaseIds = setOf(
        "abs_01", "spc_07", "min_09", "pxl_05", "pxl_09", "dep_29"
    )

    // Ensure every wallpaper preserves a unique photographic URL without procedural generation or bloat
    val allWallpapers: List<Wallpaper> by lazy {
        val coreParts = buildCatalogPartOne() +
            buildCatalogPartTwo() +
            buildCatalogPartThree() +
            buildCatalogPartFour()

        val seenThumbUrls = HashSet<String>(256)
        val seenIds = HashSet<String>(256)
        val uniqueCore = coreParts.filter { wp ->
            seenThumbUrls.add(wp.thumbnailUrl) && seenIds.add(wp.id)
        }

        val picked = ArrayList<Wallpaper>(TARGET_CATALOG_SIZE)
        val pickedIds = HashSet<String>(TARGET_CATALOG_SIZE)

        // 1. Seed priority reference & depth showcase entries first
        uniqueCore.filter { it.id in priorityShowcaseIds }.forEach { wp ->
            if (picked.size < TARGET_CATALOG_SIZE && pickedIds.add(wp.id)) {
                picked += wp
            }
        }

        // 2. Round-robin balanced sampling across every WallpaperCategory & DiscoveryCollection
        val byCategory = WallpaperCategory.entries.associateWith { cat ->
            uniqueCore.filter { it.category == cat && it.id !in pickedIds }
                .sortedByDescending { (if (it.supportsDepthEffect) 100 else 0) + it.popularityScore }
                .toMutableList()
        }

        var progressed = true
        while (picked.size < TARGET_CATALOG_SIZE && progressed) {
            progressed = false
            for (cat in WallpaperCategory.entries) {
                if (picked.size >= TARGET_CATALOG_SIZE) break
                val bucket = byCategory[cat] ?: continue
                if (bucket.isNotEmpty()) {
                    val next = bucket.removeAt(0)
                    if (pickedIds.add(next.id)) {
                        picked += next
                        progressed = true
                    }
                }
            }
        }

        // 3. Ensure every DiscoveryCollection has representation if any collection still lacks an entry
        for (col in DiscoveryCollection.entries) {
            val hasCol = when (col) {
                DiscoveryCollection.DEPTH_STUDIO -> picked.any { it.supportsDepthEffect }
                DiscoveryCollection.AMOLED -> picked.any { it.category == WallpaperCategory.AMOLED || col in it.collections }
                DiscoveryCollection.TRENDING, DiscoveryCollection.NEW_ARRIVALS -> true
                else -> picked.any { col in it.collections || it.category.displayName.equals(col.title, ignoreCase = true) }
            }
            if (!hasCol) {
                val candidate = uniqueCore.firstOrNull { col in it.collections && it.id !in pickedIds }
                if (candidate != null && picked.isNotEmpty()) {
                    val replaceIdx = picked.indexOfLast { it.id !in priorityShowcaseIds }
                    if (replaceIdx >= 0) {
                        pickedIds.remove(picked[replaceIdx].id)
                        picked[replaceIdx] = candidate
                        pickedIds.add(candidate.id)
                    }
                }
            }
        }

        picked.take(TARGET_CATALOG_SIZE)
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
