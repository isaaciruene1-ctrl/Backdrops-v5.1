package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.catalog.WallpaperCatalog
import com.example.data.media.OfflinePackDownloadManager
import com.example.data.model.DockLayoutMode
import com.example.data.model.UiStyleMode
import com.example.data.repository.OnDeviceWallpaperRepository
import com.example.engine.crash.ZeroCrashErrorBoundary
import com.example.engine.performance.ZeroLagPerformanceEngine
import com.example.ui.components.BackdropLogoMark
import com.example.ui.components.DockDestinationSpec
import com.example.ui.components.FrostedFloatingNavDock
import com.example.ui.components.FrostedFloatingNavRail
import com.example.ui.components.GlyphBottomNavDock
import com.example.ui.components.GlyphNavRail
import com.example.ui.components.PixelMaterialBottomBar
import com.example.ui.components.StudioDestination
import com.example.ui.components.TactileAmbientBackdrop
import com.example.ui.screens.CollectionDetailScreen
import com.example.ui.screens.CollectionsScreen
import com.example.ui.screens.DepthStudioScreen
import com.example.ui.screens.ExploreSearchScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LockScreenCompatibilityScreen
import com.example.ui.screens.LockScreenModeScreen
import com.example.ui.screens.OnboardingFlowScreen
import com.example.ui.screens.PreviewStudioScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.SettingsPanelScreen
import com.example.ui.screens.WallpaperDetailScreen
import com.example.ui.theme.AppMotion
import com.example.ui.theme.BackdropTheme
import com.example.ui.theme.ProvideLocalizedAppContext
import com.example.ui.theme.ThemeCustomizationTokens
import com.example.ui.viewmodel.BackdropViewModel
import com.example.ui.viewmodel.DepthStudioViewModel
import com.example.ui.viewmodel.SyncState
import kotlinx.coroutines.launch

object BackdropRoutes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val FAVORITES = "favorites"
    const val COLLECTIONS = "collections"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val LOCK_SCREEN_MODE = "lock_screen_mode"
    const val LOCK_SCREEN_COMPAT = "lock_screen_compat"
    const val ONBOARDING = "onboarding"
    const val TUTORIAL = "tutorial"
    const val DETAIL = "detail/{wallpaperId}"
    const val PREVIEW = "preview/{wallpaperId}"
    const val DEPTH = "depth/{wallpaperId}?setId={setId}"
    const val COLLECTION_DETAIL = "collection/{collectionId}"

    fun detail(id: String) = "detail/$id"
    fun preview(id: String) = "preview/$id"
    fun depth(id: String, setId: String? = null) =
        if (!setId.isNullOrBlank()) "depth/$id?setId=$setId" else "depth/$id"
    fun collectionDetail(id: String) = "collection/$id"
}

private enum class TopDestination(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(BackdropRoutes.HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    EXPLORE(BackdropRoutes.EXPLORE, R.string.nav_explore, Icons.Filled.Explore, Icons.Outlined.Explore),
    FAVORITES(BackdropRoutes.FAVORITES, R.string.nav_favorites, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    COLLECTIONS(BackdropRoutes.COLLECTIONS, R.string.nav_collections, Icons.Filled.CollectionsBookmark, Icons.Outlined.CollectionsBookmark),
    PROFILE(BackdropRoutes.PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun BackdropRootApp(
    enableOnboardingGate: Boolean = false,
    initialStudioWallpaperId: String? = null,
    onStudioDeepLinkConsumed: () -> Unit = {}
) {
    ZeroCrashErrorBoundary {
        BackdropMainShell(
            enableOnboardingGate = enableOnboardingGate,
            initialStudioWallpaperId = initialStudioWallpaperId,
            onStudioDeepLinkConsumed = onStudioDeepLinkConsumed
        )
    }
}

@Composable
private fun BackdropMainShell(
    enableOnboardingGate: Boolean,
    initialStudioWallpaperId: String?,
    onStudioDeepLinkConsumed: () -> Unit
) {
    val context = LocalContext.current

    val backdropViewModel: BackdropViewModel = viewModel(
        key = "backdrop_local_vm",
        factory = BackdropViewModel.provideFactory(context)
    )
    val depthStudioViewModel: DepthStudioViewModel = viewModel(
        key = "depth_studio_local_vm",
        factory = DepthStudioViewModel.provideFactory(context)
    )

    val preferences by backdropViewModel.preferences.collectAsStateWithLifecycle()
    val favoriteIds by backdropViewModel.favoriteIds.collectAsStateWithLifecycle()
    val favoriteWallpapers by backdropViewModel.favoriteWallpapers.collectAsStateWithLifecycle()
    val collectionsState by backdropViewModel.collectionsState.collectAsStateWithLifecycle()
    val lockScreenSetsState by backdropViewModel.lockScreenSetsState.collectAsStateWithLifecycle()
    val recentlyViewed by backdropViewModel.recentlyViewedWallpapers.collectAsStateWithLifecycle()
    val searchFilters by backdropViewModel.searchFilters.collectAsStateWithLifecycle()
    val searchResults by backdropViewModel.searchResults.collectAsStateWithLifecycle()
    val downloadProgress by backdropViewModel.downloadProgress.collectAsStateWithLifecycle()
    val isApplying by backdropViewModel.isApplyingWallpaper.collectAsStateWithLifecycle()
    val isRefreshing by backdropViewModel.isRefreshing.collectAsStateWithLifecycle()
    val cacheBytes by backdropViewModel.cacheBytes.collectAsStateWithLifecycle()
    val favoriteCount by backdropViewModel.favoriteCount.collectAsStateWithLifecycle()
    val collectionCount by backdropViewModel.collectionCount.collectAsStateWithLifecycle()
    val discoverySeed by backdropViewModel.discoverySeed.collectAsStateWithLifecycle()

    val depthStudioState by depthStudioViewModel.state.collectAsStateWithLifecycle()
    val clockConfig by depthStudioViewModel.clockConfig.collectAsStateWithLifecycle()
    val canvasFraming by depthStudioViewModel.canvasFraming.collectAsStateWithLifecycle()
    val maskStrokes by depthStudioViewModel.maskStrokes.collectAsStateWithLifecycle()
    val maskFeather by depthStudioViewModel.maskFeather.collectAsStateWithLifecycle()
    val refinedCutoutBitmap by depthStudioViewModel.refinedCutoutBitmap.collectAsStateWithLifecycle()
    val widgetTelemetry by depthStudioViewModel.widgetTelemetry.collectAsStateWithLifecycle()
    val canUndoClock by depthStudioViewModel.canUndo.collectAsStateWithLifecycle()
    val canRedoClock by depthStudioViewModel.canRedo.collectAsStateWithLifecycle()
    val offlinePackProgress by OfflinePackDownloadManager.progress.collectAsStateWithLifecycle()
    val offlineDownloadedIds by OfflinePackDownloadManager.downloadedIds.collectAsStateWithLifecycle()
    val offlineWallpapers = remember(offlineDownloadedIds, preferences.offlineDownloadedIds, context) {
        OfflinePackDownloadManager.resolveOfflineWallpapers(
            downloadedIds = offlineDownloadedIds + preferences.offlineDownloadedIds,
            context = context
        )
    }
    val onDeviceWallpapers by OnDeviceWallpaperRepository.items.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        ZeroLagPerformanceEngine.init(context.applicationContext)
        OfflinePackDownloadManager.syncPersistedState(
            context = context,
            targetCount = preferences.offlinePackTargetCount,
            downloadedIds = preferences.offlineDownloadedIds,
            status = preferences.offlinePackStatus
        )
    }
    LaunchedEffect(
        preferences.onDeviceCollectionEnabled,
        preferences.onDeviceFolderUris,
        preferences.onDevicePhotoUris
    ) {
        OnDeviceWallpaperRepository.scanOnDeviceWallpapers(context, preferences)
    }
    LaunchedEffect(backdropViewModel) {
        backdropViewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Refresh weather telemetry smoothly whenever preferences or city/location changes
    LaunchedEffect(preferences.weatherCity, preferences.weatherUseLocation) {
        depthStudioViewModel.refreshTelemetry(
            context = context,
            weatherCity = preferences.weatherCity,
            useLocation = preferences.weatherUseLocation
        )
    }

    // Pre-warm Lock Screen Engine state + decoded bitmap & depth cutout so waking Lock Mode opens instantaneously
    LaunchedEffect(
        preferences.activeOverlaySetId,
        preferences.activeLockWallpaperId,
        preferences.activeHomeWallpaperId,
        preferences.activeLockCustomBitmapPath,
        preferences.manualDepthEffectEnabled,
        lockScreenSetsState
    ) {
        val activeSet = (lockScreenSetsState as? SyncState.Ready)?.data
            ?.firstOrNull { it.id == preferences.activeOverlaySetId }
        ZeroLagPerformanceEngine.InstantLockScreenEngine.warmLockAssets(
            context = context,
            preferences = preferences,
            activeSet = activeSet
        )
    }

    val customization = remember(preferences) {
        ThemeCustomizationTokens(
            dynamicThemingEnabled = preferences.dynamicThemingEnabled,
            customAccentHex = preferences.customAccentHex,
            m3SurfaceDepth = preferences.m3SurfaceDepth,
            m3CornerRadiusScale = preferences.m3CornerRadiusScale,
            m3ElevationScale = preferences.m3ElevationScale,
            m3DensityScale = preferences.m3DensityScale,
            m3NavStyle = preferences.parsedMaterialNavStyle,
            m3TypographyScale = preferences.m3TypographyScale,
            m3MotionSpeed = preferences.m3MotionSpeed,
            glassBlurLevel = preferences.glassBlurLevel,
            glassRefractionLevel = preferences.glassRefractionLevel,
            glassTransparencyLevel = preferences.glassTransparencyLevel,
            glassLightResponse = preferences.glassLightResponse,
            glassSpecularHighlights = preferences.glassSpecularHighlights,
            glassBorderSoftness = preferences.glassBorderSoftness,
            glassAdaptiveContrast = preferences.glassAdaptiveContrast,
            dockLayoutMode = preferences.parsedDockLayoutMode,
            useClassicClockHandles = preferences.useClassicClockHandles,
            language = preferences.parsedLanguage,
            studioFont = preferences.parsedStudioFont,
            glowMaterial = preferences.parsedGlowMaterial,
            liquidGlassTier = preferences.parsedLiquidGlassTier
        )
    }

    ProvideLocalizedAppContext(languageCode = preferences.languageCode) {
        BackdropTheme(
            themeMode = preferences.parsedThemeMode,
            amoledDark = preferences.amoledDark,
            uiStyle = preferences.parsedUiStyle,
            useDynamicColor = preferences.dynamicThemingEnabled,
            hapticFeedbackEnabled = preferences.hapticFeedbackEnabled,
            reducedMotionEnabled = preferences.reducedMotionEnabled,
            glassDepthIntensity = preferences.glassDepthIntensity,
            customization = customization
        ) {
            val isGlassUi = preferences.parsedUiStyle == UiStyleMode.FROSTED_GLASS_OS
            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route ?: BackdropRoutes.HOME

            LaunchedEffect(currentRoute) {
                if (currentRoute == BackdropRoutes.HOME) {
                    kotlinx.coroutines.delay(450L)
                }
                ZeroLagPerformanceEngine.TabPreloadingEngine.preloadLikelyDestinations(
                    context = context,
                    currentRoute = currentRoute,
                    favoriteWallpapers = (favoriteWallpapers as? SyncState.Ready)?.data.orEmpty(),
                    exploreResults = searchResults
                )
            }

            // First-launch adaptive onboarding navigation when enabled in MainActivity
            var onboardingChecked by remember { mutableStateOf(false) }
            LaunchedEffect(enableOnboardingGate, preferences.onboardingCompleted) {
                if (enableOnboardingGate && !onboardingChecked) {
                    onboardingChecked = true
                    if (!preferences.onboardingCompleted) {
                        navController.navigate(BackdropRoutes.ONBOARDING) {
                            launchSingleTop = true
                        }
                    }
                }
            }

            // Deep link from Home Screen 2x2 / 4x1 / 4x2 widget Studio button
            LaunchedEffect(initialStudioWallpaperId) {
                if (!initialStudioWallpaperId.isNullOrBlank()) {
                    navController.navigate(BackdropRoutes.depth(initialStudioWallpaperId)) {
                        launchSingleTop = true
                    }
                    onStudioDeepLinkConsumed()
                }
            }

            val homeListState = rememberLazyListState()
            var homeScrollToTopTick by remember { mutableIntStateOf(0) }
            val exploreListState = rememberLazyListState()
            var exploreScrollToTopTick by remember { mutableIntStateOf(0) }

            val isTopLevelRoute = TopDestination.entries.any { it.route == currentRoute }
            val dockDestinations = TopDestination.entries.map {
                DockDestinationSpec(
                    route = it.route,
                    label = stringResource(id = it.labelResId),
                    selectedIcon = it.selectedIcon,
                    unselectedIcon = it.unselectedIcon
                )
            }

            fun navigateTopLevel(route: String) {
                if (route == BackdropRoutes.HOME && currentRoute == BackdropRoutes.HOME) {
                    homeScrollToTopTick += 1
                    return
                }
                if (route == BackdropRoutes.EXPLORE && currentRoute == BackdropRoutes.EXPLORE) {
                    exploreScrollToTopTick += 1
                    return
                }
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            // Pressing Android system Back on non-Home top-level tabs returns cleanly to Home
            BackHandler(enabled = isTopLevelRoute && currentRoute != BackdropRoutes.HOME) {
                navigateTopLevel(BackdropRoutes.HOME)
            }

            TactileAmbientBackdrop {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isLandscapeOrWide = maxWidth >= 600.dp || maxWidth > maxHeight
                    val useRail = when (preferences.parsedDockLayoutMode) {
                        DockLayoutMode.SIDE -> true
                        DockLayoutMode.BOTTOM -> false
                        DockLayoutMode.AUTOMATIC -> isLandscapeOrWide
                    }
                    val gridColumns = when {
                        maxWidth >= 1000.dp -> 5
                        maxWidth >= 800.dp -> 4
                        maxWidth >= 560.dp -> 3
                        else -> 2
                    }

                    val currentUiStyle = preferences.parsedUiStyle
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (useRail && isTopLevelRoute) {
                            when (currentUiStyle) {
                                UiStyleMode.FROSTED_GLASS_OS -> {
                                    FrostedFloatingNavRail(
                                        destinations = dockDestinations,
                                        currentRoute = currentRoute,
                                        onSelectRoute = ::navigateTopLevel
                                    )
                                }
                                UiStyleMode.GLYPH_UI -> {
                                    GlyphNavRail(
                                        destinations = dockDestinations,
                                        currentRoute = currentRoute,
                                        onSelectRoute = ::navigateTopLevel
                                    )
                                }
                                UiStyleMode.MATERIAL_3 -> {
                                    NavigationRail(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.navigationBars))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .verticalScroll(rememberScrollState()),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            BackdropLogoMark(size = 30.dp)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            dockDestinations.forEach { dest ->
                                                val selected = currentRoute == dest.route
                                                NavigationRailItem(
                                                    selected = selected,
                                                    onClick = { navigateTopLevel(dest.route) },
                                                    icon = {
                                                        Icon(
                                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                            contentDescription = dest.label
                                                        )
                                                    },
                                                    label = { Text(dest.label) },
                                                    modifier = Modifier.testTag("nav_${dest.route}")
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Scaffold(
                            modifier = Modifier.weight(1f),
                            containerColor = if (isGlassUi) Color.Transparent else MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                            contentWindowInsets = if (
                                isTopLevelRoute ||
                                currentRoute == BackdropRoutes.SETTINGS ||
                                currentRoute == BackdropRoutes.LOCK_SCREEN_MODE ||
                                currentRoute == BackdropRoutes.LOCK_SCREEN_COMPAT
                            ) {
                                WindowInsets.statusBars.union(WindowInsets.displayCutout)
                            } else {
                                WindowInsets(0, 0, 0, 0)
                            },
                            bottomBar = {
                                if (!useRail && isTopLevelRoute) {
                                    when (currentUiStyle) {
                                        UiStyleMode.FROSTED_GLASS_OS -> {
                                            FrostedFloatingNavDock(
                                                destinations = dockDestinations,
                                                currentRoute = currentRoute,
                                                onSelectRoute = ::navigateTopLevel
                                            )
                                        }
                                        UiStyleMode.GLYPH_UI -> {
                                            GlyphBottomNavDock(
                                                destinations = dockDestinations,
                                                currentRoute = currentRoute,
                                                onSelectRoute = ::navigateTopLevel
                                            )
                                        }
                                        UiStyleMode.MATERIAL_3 -> {
                                            PixelMaterialBottomBar(
                                                destinations = dockDestinations,
                                                currentRoute = currentRoute,
                                                onSelectRoute = ::navigateTopLevel
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = BackdropRoutes.HOME,
                                enterTransition = {
                                    AppMotion.screenEnter(preferences.parsedUiStyle, preferences.reducedMotionEnabled)
                                },
                                exitTransition = {
                                    AppMotion.screenExit(preferences.parsedUiStyle, preferences.reducedMotionEnabled)
                                },
                                popEnterTransition = {
                                    AppMotion.screenEnter(preferences.parsedUiStyle, preferences.reducedMotionEnabled)
                                },
                                popExitTransition = {
                                    AppMotion.screenExit(preferences.parsedUiStyle, preferences.reducedMotionEnabled)
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                composable(BackdropRoutes.HOME) {
                                    val savedSets = (lockScreenSetsState as? SyncState.Ready)?.data.orEmpty()
                                    val activeSet = savedSets.firstOrNull { it.id == preferences.activeOverlaySetId } ?: savedSets.firstOrNull()
                                    val activeHomeWp = WallpaperCatalog.getById(preferences.activeHomeWallpaperId)
                                        ?: onDeviceWallpapers.firstOrNull { it.id == preferences.activeHomeWallpaperId }
                                    val activeLockWp = WallpaperCatalog.getById(preferences.activeLockWallpaperId)
                                        ?: onDeviceWallpapers.firstOrNull { it.id == preferences.activeLockWallpaperId }
                                        ?: activeHomeWp
                                    HomeScreen(
                                        favoriteIds = favoriteIds,
                                        isRefreshing = isRefreshing,
                                        showCategoryChips = preferences.homeCategoryChipsEnabled,
                                        discoverySeed = discoverySeed,
                                        onRefresh = { backdropViewModel.refreshDiscovery(context) },
                                        onShuffleDiscovery = backdropViewModel::shuffleDiscovery,
                                        onSearchShortcut = { navigateTopLevel(BackdropRoutes.EXPLORE) },
                                        onSettingsShortcut = { navController.navigate(BackdropRoutes.SETTINGS) },
                                        onProfileShortcut = { navigateTopLevel(BackdropRoutes.PROFILE) },
                                        onOpenWallpaper = { wp -> navController.navigate(BackdropRoutes.detail(wp.id)) },
                                        onToggleFavorite = backdropViewModel::toggleFavorite,
                                        onSelectCategory = { category ->
                                            backdropViewModel.selectCategoryFromHome(category)
                                            navigateTopLevel(BackdropRoutes.EXPLORE)
                                        },
                                        onOpenCollection = { col ->
                                            backdropViewModel.setCollectionFilter(col)
                                            navigateTopLevel(BackdropRoutes.EXPLORE)
                                        },
                                        onOpenDepthStudio = { wp -> navController.navigate(BackdropRoutes.depth(wp.id)) },
                                        onDeviceWallpapers = onDeviceWallpapers,
                                        onOpenOnDeviceCollection = { navigateTopLevel(BackdropRoutes.EXPLORE) },
                                        offlineWallpapers = offlineWallpapers,
                                        onOpenOfflineCollection = { navigateTopLevel(BackdropRoutes.COLLECTIONS) },
                                        activeHomeWallpaper = activeHomeWp,
                                        activeLockWallpaper = activeLockWp,
                                        activeLockSetName = activeSet?.name.orEmpty(),
                                        savedLockSets = savedSets,
                                        onDeviceFolderCount = preferences.onDeviceFolderUris.size,
                                        onOpenLockScreenMode = { navController.navigate(BackdropRoutes.LOCK_SCREEN_MODE) },
                                        onOpenPreviewStudio = { wp -> navController.navigate(BackdropRoutes.preview(wp.id)) },
                                        onAddOnDevicePhotos = backdropViewModel::addOnDevicePhotoUris,
                                        onAddOnDeviceFolder = backdropViewModel::addOnDeviceFolderUri,
                                        onOpenLockScreenSet = { setDoc ->
                                            val wpId = setDoc.wallpaperId.ifBlank {
                                                WallpaperCatalog.allWallpapers.first().id
                                            }
                                            navController.navigate(BackdropRoutes.depth(wpId, setId = setDoc.id))
                                        },
                                        listState = homeListState,
                                        scrollToTopTick = homeScrollToTopTick
                                    )
                                }

                                composable(BackdropRoutes.EXPLORE) {
                                    ExploreSearchScreen(
                                        filters = searchFilters,
                                        results = searchResults,
                                        recentSearches = preferences.recentSearches,
                                        favoriteIds = favoriteIds,
                                        columns = gridColumns,
                                        onQueryChange = backdropViewModel::updateSearchQuery,
                                        onCommitSearch = backdropViewModel::commitSearchToHistory,
                                        onClearHistory = backdropViewModel::clearSearchHistory,
                                        onSelectResolution = backdropViewModel::setResolutionFilter,
                                        onToggleAmoled = backdropViewModel::toggleAmoledFilter,
                                        onTogglePortrait = backdropViewModel::togglePortraitFilter,
                                        onToggleDepthStudio = backdropViewModel::toggleDepthStudioFilter,
                                        onSelectColor = backdropViewModel::setColorFilter,
                                        onSelectCategory = backdropViewModel::setCategoryFilter,
                                        onSelectCollection = backdropViewModel::setCollectionFilter,
                                        onSelectSort = backdropViewModel::setSearchSort,
                                        onResetFilters = backdropViewModel::resetFilters,
                                        onOpenWallpaper = { wp -> navController.navigate(BackdropRoutes.detail(wp.id)) },
                                        onToggleFavorite = backdropViewModel::toggleFavorite,
                                        listState = exploreListState,
                                        scrollToTopTick = exploreScrollToTopTick,
                                        onDeviceWallpapers = onDeviceWallpapers,
                                        onOpenSettingsForOnDevice = { navController.navigate(BackdropRoutes.SETTINGS) },
                                        offlineWallpapers = offlineWallpapers,
                                        curatedShelfEnabled = preferences.exploreCuratedShelfEnabled,
                                        onToggleCuratedShelf = backdropViewModel::setExploreCuratedShelfEnabled,
                                        deviceFolderUris = preferences.onDeviceFolderUris,
                                        onAddDevicePhotos = backdropViewModel::addOnDevicePhotoUris,
                                        onAddDeviceFolder = backdropViewModel::addOnDeviceFolderUri
                                    )
                                }

                                composable(BackdropRoutes.FAVORITES) {
                                    FavoritesScreen(
                                        favoritesState = favoriteWallpapers,
                                        recentlyViewed = recentlyViewed,
                                        favoriteIds = favoriteIds,
                                        columns = gridColumns,
                                        onOpenWallpaper = { wp -> navController.navigate(BackdropRoutes.detail(wp.id)) },
                                        onToggleFavorite = backdropViewModel::toggleFavorite,
                                        onExploreClick = { navigateTopLevel(BackdropRoutes.EXPLORE) }
                                    )
                                }

                                composable(BackdropRoutes.COLLECTIONS) {
                                    CollectionsScreen(
                                        collectionsState = collectionsState,
                                        lockScreenSetsState = lockScreenSetsState,
                                        columns = if (gridColumns >= 3) 2 else 1,
                                        onCreateCollection = { name, desc ->
                                            backdropViewModel.createCollection(name, desc, null)
                                        },
                                        onRenameCollection = backdropViewModel::renameCollection,
                                        onDeleteCollection = backdropViewModel::deleteCollection,
                                        onOpenCollectionDetail = { col ->
                                            navController.navigate(BackdropRoutes.collectionDetail(col.id))
                                        },
                                        onOpenLockScreenSet = { setDoc ->
                                            val wpId = setDoc.wallpaperId.ifBlank {
                                                WallpaperCatalog.allWallpapers.first().id
                                            }
                                            navController.navigate(BackdropRoutes.depth(wpId, setId = setDoc.id))
                                        },
                                        onToggleLockScreenSetFavorite = backdropViewModel::toggleLockScreenSetFavorite,
                                        onDeleteLockScreenSet = backdropViewModel::deleteLockScreenSet,
                                        onDeleteLockScreenSets = backdropViewModel::deleteLockScreenSets,
                                        offlineWallpapers = offlineWallpapers,
                                        favoriteIds = favoriteIds,
                                        onOpenWallpaper = { wp -> navController.navigate(BackdropRoutes.detail(wp.id)) },
                                        onToggleFavorite = backdropViewModel::toggleFavorite,
                                        onOpenOfflineSettings = { navController.navigate(BackdropRoutes.SETTINGS) },
                                        offlinePackProgress = offlinePackProgress,
                                        offlinePackTargetCount = preferences.offlinePackTargetCount,
                                        onStartOfflinePack = { option ->
                                            OfflinePackDownloadManager.startOrResumeDownload(
                                                context = context,
                                                option = option,
                                                existingDownloadedIds = preferences.offlineDownloadedIds,
                                                onStatePersisted = backdropViewModel::setOfflinePackState
                                            )
                                        },
                                        onPauseOfflinePack = { OfflinePackDownloadManager.pauseDownload() },
                                        onCancelOfflinePack = { OfflinePackDownloadManager.cancelDownload() },
                                        onClearOfflinePack = {
                                            OfflinePackDownloadManager.clearOfflinePack(
                                                context = context,
                                                onStatePersisted = backdropViewModel::setOfflinePackState
                                            )
                                        }
                                    )
                                }

                                composable(BackdropRoutes.PROFILE) {
                                    ProfileSettingsScreen(
                                        favoriteCount = favoriteCount,
                                        collectionCount = collectionCount,
                                        preferences = preferences,
                                        cacheBytes = cacheBytes,
                                        onRefreshCacheSize = { backdropViewModel.refreshCacheSize(context) },
                                        onClearCache = { backdropViewModel.clearImageCache(context) },
                                        onOpenSettingsPanel = { navController.navigate(BackdropRoutes.SETTINGS) },
                                        onOpenDepthStudio = {
                                            val defaultWp = WallpaperCatalog.allWallpapers.firstOrNull { it.supportsDepthEffect }
                                                ?: WallpaperCatalog.allWallpapers.first()
                                            navController.navigate(BackdropRoutes.depth(defaultWp.id))
                                        },
                                        onOpenLockScreenMode = { navController.navigate(BackdropRoutes.LOCK_SCREEN_MODE) },
                                        onOpenInteractiveTutorial = { navController.navigate(BackdropRoutes.TUTORIAL) }
                                    )
                                }

                                composable(BackdropRoutes.SETTINGS) {
                                    SettingsPanelScreen(
                                        preferences = preferences,
                                        cacheBytes = cacheBytes,
                                        onBack = { navController.popBackStack() },
                                        onUiStyleChange = backdropViewModel::setUiStyle,
                                        onThemeModeChange = backdropViewModel::setThemeMode,
                                        onAmoledDarkChange = backdropViewModel::setAmoledDark,
                                        onHomeCategoryChipsChange = backdropViewModel::setHomeCategoryChipsEnabled,
                                        onHapticFeedbackChange = backdropViewModel::setHapticFeedbackEnabled,
                                        onReducedMotionChange = backdropViewModel::setReducedMotionEnabled,
                                        onGlassDepthIntensityChange = backdropViewModel::setGlassDepthIntensity,
                                        onImageQualityChange = backdropViewModel::setImageQuality,
                                        onWifiOnlyChange = backdropViewModel::setWifiOnlyDownloads,
                                        onRefreshCacheSize = { backdropViewModel.refreshCacheSize(context) },
                                        onClearCache = { backdropViewModel.clearImageCache(context) },
                                        onDynamicThemingChange = backdropViewModel::setDynamicThemingEnabled,
                                        onCustomAccentChange = backdropViewModel::setCustomAccentHex,
                                        onM3SurfaceDepthChange = backdropViewModel::setM3SurfaceDepth,
                                        onM3CornerRadiusChange = backdropViewModel::setM3CornerRadiusScale,
                                        onM3ElevationChange = backdropViewModel::setM3ElevationScale,
                                        onM3DensityChange = backdropViewModel::setM3DensityScale,
                                        onM3NavStyleChange = backdropViewModel::setM3NavStyle,
                                        onM3TypographyScaleChange = backdropViewModel::setM3TypographyScale,
                                        onM3MotionSpeedChange = backdropViewModel::setM3MotionSpeed,
                                        onGlassBlurChange = backdropViewModel::setGlassBlurLevel,
                                        onGlassRefractionChange = backdropViewModel::setGlassRefractionLevel,
                                        onGlassTransparencyChange = backdropViewModel::setGlassTransparencyLevel,
                                        onGlassLightResponseChange = backdropViewModel::setGlassLightResponse,
                                        onGlassSpecularHighlightsChange = backdropViewModel::setGlassSpecularHighlights,
                                        onGlassBorderSoftnessChange = backdropViewModel::setGlassBorderSoftness,
                                        onGlassAdaptiveContrastChange = backdropViewModel::setGlassAdaptiveContrast,
                                        onDockLayoutChange = backdropViewModel::setDockLayoutMode,
                                        onClassicClockHandlesChange = backdropViewModel::setUseClassicClockHandles,
                                        onLanguageChange = backdropViewModel::setLanguageCode,
                                        onWeatherCityChange = backdropViewModel::setWeatherCity,
                                        onWeatherUseLocationChange = backdropViewModel::setWeatherUseLocation,
                                        onOpenLockScreenMode = { navController.navigate(BackdropRoutes.LOCK_SCREEN_MODE) },
                                        onOpenCompatibilityManager = { navController.navigate(BackdropRoutes.LOCK_SCREEN_COMPAT) },
                                        onReplayOnboarding = { navController.navigate(BackdropRoutes.ONBOARDING) },
                                        onResetMaterial3Defaults = backdropViewModel::resetMaterial3Customizations,
                                        onResetGlassDefaults = backdropViewModel::resetFrostedGlassCustomizations,
                                        onRestoreAllDefaults = backdropViewModel::restoreAllPreferenceDefaults,
                                        onManualDepthEffectChange = backdropViewModel::setManualDepthEffectEnabled,
                                        onStudioFontChange = backdropViewModel::setSelectedStudioFont,
                                        onLiquidGlassTierChange = backdropViewModel::setLiquidGlassTier,
                                        onGlowMaterialChange = backdropViewModel::setGlowMaterialConfig,
                                        onRememberEditingChange = backdropViewModel::setRememberPreviousEditingSettings,
                                        onScaleTextDefaultChange = backdropViewModel::setScaleTextWithClockDefault,
                                        onDeviceCount = onDeviceWallpapers.size,
                                        onToggleOnDeviceEnabled = backdropViewModel::setOnDeviceCollectionEnabled,
                                        onAddOnDevicePhotos = backdropViewModel::addOnDevicePhotoUris,
                                        onAddOnDeviceFolder = backdropViewModel::addOnDeviceFolderUri,
                                        onRemoveOnDeviceFolder = backdropViewModel::removeOnDeviceFolderUri,
                                        onRefreshOnDevice = {
                                            coroutineScope.launch {
                                                OnDeviceWallpaperRepository.scanOnDeviceWallpapers(context, preferences)
                                            }
                                        },
                                        offlinePackProgress = offlinePackProgress,
                                        onStartOfflinePack = { option ->
                                            OfflinePackDownloadManager.startOrResumeDownload(
                                                context = context,
                                                option = option,
                                                existingDownloadedIds = preferences.offlineDownloadedIds,
                                                onStatePersisted = backdropViewModel::setOfflinePackState
                                            )
                                        },
                                        onPauseOfflinePack = { OfflinePackDownloadManager.pauseDownload() },
                                        onCancelOfflinePack = { OfflinePackDownloadManager.cancelDownload() },
                                        onClearOfflinePack = {
                                            OfflinePackDownloadManager.clearOfflinePack(
                                                context = context,
                                                onStatePersisted = backdropViewModel::setOfflinePackState
                                            )
                                        },
                                        onExploreCuratedShelfChange = backdropViewModel::setExploreCuratedShelfEnabled,
                                        onGlancePillEnabledChange = backdropViewModel::setGlancePillEnabled,
                                        onMediaPlayerSizeTierChange = backdropViewModel::setMediaPlayerSizeTier,
                                        onWallpaperMotionModeChange = backdropViewModel::setWallpaperMotionMode
                                    )
                                }

                                composable(BackdropRoutes.LOCK_SCREEN_MODE) {
                                    val savedSets = (lockScreenSetsState as? SyncState.Ready)?.data.orEmpty()
                                    val activeSet = savedSets.firstOrNull { it.id == preferences.activeOverlaySetId } ?: savedSets.firstOrNull()
                                    val targetWpId = activeSet?.wallpaperId?.takeIf { it.isNotBlank() }
                                        ?: preferences.activeLockWallpaperId.takeIf { it.isNotBlank() }
                                        ?: WallpaperCatalog.allWallpapers.firstOrNull { it.supportsDepthEffect }?.id
                                        ?: WallpaperCatalog.allWallpapers.first().id
                                    LockScreenModeScreen(
                                        preferences = preferences,
                                        savedSets = savedSets,
                                        currentClockConfig = clockConfig,
                                        onBack = { navController.popBackStack() },
                                        onOpenCompatibility = { navController.navigate(BackdropRoutes.LOCK_SCREEN_COMPAT) },
                                        onSelectOverlaySetId = backdropViewModel::setActiveOverlaySetId,
                                        onSetOverlayEnabled = backdropViewModel::setLockScreenOverlayEnabled,
                                        onSetLockNotificationsEnabled = backdropViewModel::setLockNotificationsEnabled,
                                        onSetHideSensitiveNotifications = backdropViewModel::setHideSensitiveNotifications,
                                        onSetLockCredential = backdropViewModel::setLockCredential,
                                        onClearLockCredential = backdropViewModel::clearLockCredential,
                                        onSetNotificationStyleConfig = backdropViewModel::setNotificationStyleConfig,
                                        onSetChargingAnimationConfig = backdropViewModel::setChargingAnimationConfig,
                                        onSetUnlockButtonThemeConfig = backdropViewModel::setUnlockButtonThemeConfig,
                                        onSetStudioWidgets = backdropViewModel::setStudioWidgets,
                                        onSetDragToExpandNotifications = backdropViewModel::setDragToExpandNotifications,
                                        onSetShowUnlockButtonOnLockScreen = backdropViewModel::setShowUnlockButtonOnLockScreen,
                                        onSetSecondaryWidgetPageLeft = backdropViewModel::setSecondaryWidgetPageLeft,
                                        onSetSecondaryWidgetPagePosition = backdropViewModel::setSecondaryWidgetPagePosition,
                                        onSetLockScreenMaxWidgets = backdropViewModel::setLockScreenMaxWidgets,
                                        onSetWidgetPageMaxWidgets = backdropViewModel::setWidgetPageMaxWidgets,
                                        onSetSamsungMediaPillEnabled = backdropViewModel::setSamsungMediaPillEnabled,
                                        onSetPixelChargingWaveEnabled = backdropViewModel::setPixelChargingWaveEnabled,
                                        onSetGlancePillEnabled = backdropViewModel::setGlancePillEnabled,
                                        onSetGlancePillCoexistWithWidgets = backdropViewModel::setGlancePillCoexistWithWidgets,
                                        onSetMediaPlayerSizeTier = backdropViewModel::setMediaPlayerSizeTier,
                                        onSetWidgetPageWidgets = backdropViewModel::setWidgetPageWidgets,
                                        onSetInlineTopWidget = backdropViewModel::setInlineTopWidget,
                                        onSetLeftShortcut = backdropViewModel::setLeftShortcut,
                                        onSetRightShortcut = backdropViewModel::setRightShortcut,
                                        onSetWallpaperMotionMode = backdropViewModel::setWallpaperMotionMode,
                                        onCommitClockConfig = depthStudioViewModel::commitClockConfig,
                                        onResetLockSetup = backdropViewModel::resetLockSetup,
                                        onNavigateStudioDestination = { dest ->
                                            when (dest) {
                                                StudioDestination.DEPTH -> navController.navigate(BackdropRoutes.depth(targetWpId)) {
                                                    popUpTo(BackdropRoutes.LOCK_SCREEN_MODE) { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                                StudioDestination.PREVIEW -> navController.navigate(BackdropRoutes.preview(targetWpId)) {
                                                    popUpTo(BackdropRoutes.LOCK_SCREEN_MODE) { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                                StudioDestination.LOCK -> Unit
                                            }
                                        }
                                    )
                                }

                                composable(BackdropRoutes.LOCK_SCREEN_COMPAT) {
                                    LockScreenCompatibilityScreen(
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable(BackdropRoutes.ONBOARDING) {
                                    OnboardingFlowScreen(
                                        preferences = preferences,
                                        onLanguageSelected = backdropViewModel::setLanguageCode,
                                        onUiStyleSelected = backdropViewModel::setUiStyle,
                                        onThemeModeSelected = backdropViewModel::setThemeMode,
                                        onAmoledDarkSelected = backdropViewModel::setAmoledDark,
                                        onDynamicThemingSelected = backdropViewModel::setDynamicThemingEnabled,
                                        onClassicHandlesSelected = backdropViewModel::setUseClassicClockHandles,
                                        onCompleteOnboarding = {
                                            backdropViewModel.setOnboardingCompleted(true)
                                            if (!navController.popBackStack(BackdropRoutes.HOME, inclusive = false)) {
                                                navController.navigate(BackdropRoutes.HOME) {
                                                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                            }
                                        },
                                        offlinePackProgress = offlinePackProgress,
                                        onStartOfflinePack = { option ->
                                            OfflinePackDownloadManager.startOrResumeDownload(
                                                context = context,
                                                option = option,
                                                existingDownloadedIds = preferences.offlineDownloadedIds,
                                                onStatePersisted = backdropViewModel::setOfflinePackState
                                            )
                                        },
                                        onPauseOfflinePack = { OfflinePackDownloadManager.pauseDownload() },
                                        onCancelOfflinePack = { OfflinePackDownloadManager.cancelDownload() },
                                        onClearOfflinePack = {
                                            OfflinePackDownloadManager.clearOfflinePack(
                                                context = context,
                                                onStatePersisted = backdropViewModel::setOfflinePackState
                                            )
                                        }
                                    )
                                }

                                composable(BackdropRoutes.TUTORIAL) {
                                    OnboardingFlowScreen(
                                        preferences = preferences,
                                        onLanguageSelected = backdropViewModel::setLanguageCode,
                                        onUiStyleSelected = backdropViewModel::setUiStyle,
                                        onThemeModeSelected = backdropViewModel::setThemeMode,
                                        onAmoledDarkSelected = backdropViewModel::setAmoledDark,
                                        onDynamicThemingSelected = backdropViewModel::setDynamicThemingEnabled,
                                        onClassicHandlesSelected = backdropViewModel::setUseClassicClockHandles,
                                        onCompleteOnboarding = { navController.popBackStack() },
                                        initialStep = 3,
                                        offlinePackProgress = offlinePackProgress,
                                        onStartOfflinePack = { option ->
                                            OfflinePackDownloadManager.startOrResumeDownload(
                                                context = context,
                                                option = option,
                                                existingDownloadedIds = preferences.offlineDownloadedIds,
                                                onStatePersisted = backdropViewModel::setOfflinePackState
                                            )
                                        },
                                        onPauseOfflinePack = { OfflinePackDownloadManager.pauseDownload() },
                                        onCancelOfflinePack = { OfflinePackDownloadManager.cancelDownload() },
                                        onClearOfflinePack = {
                                            OfflinePackDownloadManager.clearOfflinePack(
                                                context = context,
                                                onStatePersisted = backdropViewModel::setOfflinePackState
                                            )
                                        }
                                    )
                                }

                                composable(
                                    route = BackdropRoutes.DETAIL,
                                    arguments = listOf(navArgument("wallpaperId") { type = NavType.StringType })
                                ) { entry ->
                                    val wpId = entry.arguments?.getString("wallpaperId").orEmpty()
                                    val wp = WallpaperCatalog.getById(wpId)
                                        ?: onDeviceWallpapers.firstOrNull { it.id == wpId }
                                        ?: WallpaperCatalog.allWallpapers.first()
                                    WallpaperDetailScreen(
                                        wallpaper = wp,
                                        favoriteIds = favoriteIds,
                                        collectionsState = collectionsState,
                                        downloadProgress = downloadProgress,
                                        isApplying = isApplying,
                                        onBack = { navController.popBackStack() },
                                        onRecordViewed = backdropViewModel::recordViewed,
                                        onToggleFavorite = backdropViewModel::toggleFavorite,
                                        onDownload = backdropViewModel::downloadWallpaper,
                                        onClearDownloadStatus = backdropViewModel::clearDownloadStatus,
                                        onApplyWallpaper = backdropViewModel::applyWallpaper,
                                        onShareWallpaper = backdropViewModel::shareWallpaper,
                                        onAddToCollection = backdropViewModel::addWallpaperToCollection,
                                        onCreateCollectionWithWallpaper = { name, desc, targetWp ->
                                            backdropViewModel.createCollection(name, desc, targetWp)
                                        },
                                        onOpenPreviewStudio = { targetWp ->
                                            navController.navigate(BackdropRoutes.preview(targetWp.id))
                                        },
                                        onOpenDepthStudio = { targetWp ->
                                            navController.navigate(BackdropRoutes.depth(targetWp.id))
                                        },
                                        syncHomeWithLockMode = preferences.syncHomeWithLockMode,
                                        onSyncHomeWithLockModeChange = backdropViewModel::setSyncHomeWithLockMode,
                                        onSaveToLockScreenMode = backdropViewModel::saveWallpaperToLockScreenMode
                                    )
                                }

                                composable(
                                    route = BackdropRoutes.PREVIEW,
                                    arguments = listOf(navArgument("wallpaperId") { type = NavType.StringType })
                                ) { entry ->
                                    val wpId = entry.arguments?.getString("wallpaperId").orEmpty()
                                    val wp = WallpaperCatalog.getById(wpId)
                                        ?: onDeviceWallpapers.firstOrNull { it.id == wpId }
                                        ?: WallpaperCatalog.allWallpapers.first()
                                    PreviewStudioScreen(
                                        wallpaper = wp,
                                        isApplying = isApplying,
                                        onBack = { navController.popBackStack() },
                                        onApplyWallpaper = backdropViewModel::applyWallpaper,
                                        onApplyEditedWallpaper = backdropViewModel::applyEditedWallpaper,
                                        preferences = preferences,
                                        initialClockConfig = clockConfig,
                                        onSyncHomeWithLockModeChange = backdropViewModel::setSyncHomeWithLockMode,
                                        onSaveToLockScreenMode = backdropViewModel::saveWallpaperToLockScreenMode,
                                        onNavigateStudioDestination = { dest ->
                                            when (dest) {
                                                StudioDestination.DEPTH -> navController.navigate(BackdropRoutes.depth(wp.id)) {
                                                    popUpTo(BackdropRoutes.PREVIEW) { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                                StudioDestination.PREVIEW -> Unit
                                                StudioDestination.LOCK -> navController.navigate(BackdropRoutes.LOCK_SCREEN_MODE) {
                                                    popUpTo(BackdropRoutes.PREVIEW) { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                            }
                                        }
                                    )
                                }

                                composable(
                                    route = BackdropRoutes.DEPTH,
                                    arguments = listOf(
                                        navArgument("wallpaperId") { type = NavType.StringType },
                                        navArgument("setId") {
                                            type = NavType.StringType
                                            nullable = true
                                            defaultValue = null
                                        }
                                    )
                                ) { entry ->
                                    val wpId = entry.arguments?.getString("wallpaperId").orEmpty()
                                    val setId = entry.arguments?.getString("setId")
                                    val activeSet = (lockScreenSetsState as? SyncState.Ready)?.data
                                        ?.firstOrNull { it.id == setId }
                                    val resolvedId = activeSet?.wallpaperId ?: wpId
                                    val wp = WallpaperCatalog.getById(resolvedId)
                                        ?: onDeviceWallpapers.firstOrNull { it.id == resolvedId }
                                        ?: WallpaperCatalog.allWallpapers.first()
                                    DepthStudioScreen(
                                        initialWallpaper = wp,
                                        depthState = depthStudioState,
                                        clockConfig = clockConfig,
                                        canUndo = canUndoClock,
                                        canRedo = canRedoClock,
                                        isApplying = isApplying,
                                        initialSet = activeSet,
                                        onLoadWallpaper = depthStudioViewModel::loadWallpaperForDepthStudio,
                                        onPickCustomPhoto = { uri, forceRescan ->
                                            depthStudioViewModel.loadCustomPhotoForDepthStudio(context, uri, forceRescan)
                                        },
                                        onBeginClockGesture = depthStudioViewModel::beginClockGesture,
                                        onPreviewClockConfig = depthStudioViewModel::previewClockConfig,
                                        onEndClockGesture = depthStudioViewModel::endClockGesture,
                                        onCommitClockConfig = depthStudioViewModel::commitClockConfig,
                                        onUndoClock = depthStudioViewModel::undoEdit,
                                        onRedoClock = depthStudioViewModel::redoEdit,
                                        onResetProportions = depthStudioViewModel::resetClockProportions,
                                        onCenterClock = depthStudioViewModel::centerClockOnScreen,
                                        onAutoSnapClock = depthStudioViewModel::autoSnapClockToDepth,
                                        onRestoreSetClock = depthStudioViewModel::restoreClockConfig,
                                        onSaveLockScreenSet = backdropViewModel::saveLockScreenSet,
                                        onBack = { navController.popBackStack() },
                                        onApplyWallpaper = backdropViewModel::applyWallpaper,
                                        onApplyCustomBitmapWallpaper = backdropViewModel::applyBitmapWallpaper,
                                        canvasFraming = canvasFraming,
                                        onBeginStudioGesture = depthStudioViewModel::beginStudioGesture,
                                        onEndStudioGesture = depthStudioViewModel::endStudioGesture,
                                        onPreviewCanvasFraming = depthStudioViewModel::previewCanvasFraming,
                                        onCommitCanvasFraming = depthStudioViewModel::commitCanvasFraming,
                                        onResetCanvasFraming = depthStudioViewModel::resetCanvasFraming,
                                        maskStrokes = maskStrokes,
                                        maskFeather = maskFeather,
                                        refinedCutoutBitmap = refinedCutoutBitmap,
                                        onAddMaskStroke = depthStudioViewModel::addMaskStroke,
                                        onClearMaskStrokes = depthStudioViewModel::clearMaskStrokes,
                                        onSetMaskFeather = depthStudioViewModel::setMaskFeather,
                                        widgetTelemetry = widgetTelemetry,
                                        weatherCity = preferences.weatherCity,
                                        onWeatherCityChange = backdropViewModel::setWeatherCity,
                                        onRefreshWeather = { cityArg, locArg ->
                                            depthStudioViewModel.refreshTelemetry(context, cityArg, locArg)
                                        },
                                        onRestoreSet = depthStudioViewModel::restoreFromLockScreenSet,
                                        onAutoSnapWithViewport = { vw, vh ->
                                            depthStudioViewModel.autoSnapClockToDepth(vw, vh)
                                        },
                                        manualDepthEffectEnabled = preferences.manualDepthEffectEnabled,
                                        syncHomeWithLockMode = preferences.syncHomeWithLockMode,
                                        onSyncHomeWithLockModeChange = backdropViewModel::setSyncHomeWithLockMode,
                                        onSaveWallpaperToLockScreenMode = backdropViewModel::saveWallpaperToLockScreenMode,
                                        onNavigateStudioDestination = { dest ->
                                            when (dest) {
                                                StudioDestination.DEPTH -> Unit
                                                StudioDestination.PREVIEW -> navController.navigate(BackdropRoutes.preview(wp.id)) {
                                                    popUpTo(BackdropRoutes.DEPTH) { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                                StudioDestination.LOCK -> navController.navigate(BackdropRoutes.LOCK_SCREEN_MODE) {
                                                    popUpTo(BackdropRoutes.DEPTH) { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                            }
                                        }
                                    )
                                }

                                composable(
                                    route = BackdropRoutes.COLLECTION_DETAIL,
                                    arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
                                ) { entry ->
                                    val colId = entry.arguments?.getString("collectionId").orEmpty()
                                    val cols = (collectionsState as? SyncState.Ready)?.data.orEmpty()
                                    val collection = cols.firstOrNull { it.id == colId }
                                    if (collection != null) {
                                        CollectionDetailScreen(
                                            collection = collection,
                                            favoriteIds = favoriteIds,
                                            columns = gridColumns,
                                            onBack = { navController.popBackStack() },
                                            onOpenWallpaper = { wp -> navController.navigate(BackdropRoutes.detail(wp.id)) },
                                            onToggleFavorite = backdropViewModel::toggleFavorite,
                                            onRemoveFromCollection = { wpId ->
                                                backdropViewModel.removeWallpaperFromCollection(collection, wpId)
                                            }
                                        )
                                    } else {
                                        LaunchedEffect(colId) {
                                            navController.popBackStack()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
