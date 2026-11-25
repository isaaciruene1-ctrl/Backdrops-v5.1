package com.example

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Color as AndroidColor
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.catalog.WallpaperCatalog
import com.example.data.media.WallpaperMediaService
import com.example.data.model.DiscoveryCollection
import com.example.data.model.SearchFilters
import com.example.data.model.ThemeMode
import com.example.data.model.UiStyleMode
import com.example.data.model.UserPreferencesDoc
import com.example.data.model.WallpaperCategory
import com.example.data.repository.GuestLocalUserStore
import com.example.engine.notifications.LockNotificationGroup
import com.example.engine.notifications.LockNotificationItem
import com.example.engine.notifications.LockScreenNotificationRepository
import com.example.engine.overlay.LockScreenOverlaySurface
import com.example.engine.overlay.resolveActiveLockArtwork
import com.example.engine.performance.ZeroLagPerformanceEngine
import com.example.ui.components.ClockDesignConfig
import com.example.ui.components.ClockWidgetTelemetry
import com.example.ui.screens.ExploreSearchScreen
import com.example.ui.theme.BackdropTheme
import com.example.ui.theme.buildSeededColorScheme
import com.example.ui.theme.ensureDarkGlassContrastScheme
import com.example.ui.viewmodel.BackdropViewModel
import com.example.widget.BackdropShowcaseWidgetProvider
import com.example.widget.BackdropStripWidgetProvider
import com.example.widget.BackdropSwitcherWidgetProvider
import com.example.widget.BackdropWidgetContract
import com.example.widget.BackdropWidgetVariant
import com.example.widget.BaseBackdropWidgetProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class BackdropReconstructionQaTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun qa01_catalogHasUniqueThumbnailsAndUniqueRealFallbacksPerEntry() {
        val all = WallpaperCatalog.allWallpapers
        assertTrue(all.size >= 2500)
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertEquals(all.size, all.map { it.thumbnailUrl }.toSet().size)
        assertTrue(all.none { it.fullResUrl.startsWith("procedural://") || it.thumbnailUrl.startsWith("procedural://") })

        val featured = WallpaperCatalog.getForCollection(DiscoveryCollection.FEATURED)
        assertTrue(featured.size >= 20)
        assertEquals(featured.size, featured.map { it.thumbnailUrl }.toSet().size)
    }

    @Test
    fun qa02_onboardingAndActiveWallpapersPersistAcrossStoreAndViewModelRestarts() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val store = GuestLocalUserStore(ctx)
        val targetWp = WallpaperCatalog.allWallpapers[5]
        val secondWp = WallpaperCatalog.allWallpapers[9]

        val vm1 = BackdropViewModel(
            syncRepository = store,
            mediaService = WallpaperMediaService(ctx)
        )
        vm1.setOnboardingCompleted(true)
        store.updatePreferences(GuestLocalUserStore.GUEST_USER_ID) {
            it.copy(
                activeHomeWallpaperId = targetWp.id,
                activeLockWallpaperId = secondWp.id,
                lockNotificationsEnabled = true,
                hideSensitiveNotifications = true
            )
        }
        delay(50)

        // Fresh store & ViewModel must immediately start with onboardingCompleted = true (zero onboarding re-show)
        val reloadedStore = GuestLocalUserStore(ctx)
        val reloadedPrefs = reloadedStore.getCurrentPreferencesSync()
        assertTrue(reloadedPrefs.onboardingCompleted)
        assertEquals(targetWp.id, reloadedPrefs.activeHomeWallpaperId)
        assertEquals(secondWp.id, reloadedPrefs.activeLockWallpaperId)
        assertTrue(reloadedPrefs.hideSensitiveNotifications)

        val vm2 = BackdropViewModel(
            syncRepository = reloadedStore,
            mediaService = WallpaperMediaService(ctx)
        )
        assertTrue(vm2.preferences.value.onboardingCompleted)
        assertEquals(secondWp.id, vm2.preferences.value.activeLockWallpaperId)
    }

    @Test
    fun qa03_darkAndGlassThemePreservesCustomAccentHueAndSurfaceDepth() {
        val emeraldHex = 0xFF2F856BL
        val seededDark = buildSeededColorScheme(
            seedHex = emeraldHex,
            isDark = true,
            surfaceDepth = 1.0f
        )
        val safeguarded = ensureDarkGlassContrastScheme(
            scheme = seededDark,
            amoledDark = false,
            isGlass = true,
            dynamicThemingEnabled = false,
            surfaceDepth = 1.3f
        )
        // Must NOT overwrite green accent with default DarkPastelLilac lavender
        assertTrue(
            "Expected green channel to dominate red/blue when dynamicThemingEnabled=false",
            safeguarded.primary.green > safeguarded.primary.red &&
                safeguarded.primary.green >= safeguarded.primary.blue * 0.9f
        )

        val shallow = ensureDarkGlassContrastScheme(
            scheme = seededDark,
            amoledDark = true,
            isGlass = false,
            dynamicThemingEnabled = false,
            surfaceDepth = 0.6f
        )
        val deep = ensureDarkGlassContrastScheme(
            scheme = seededDark,
            amoledDark = true,
            isGlass = false,
            dynamicThemingEnabled = false,
            surfaceDepth = 1.4f
        )
        assertNotEquals(shallow.surfaceContainerHighest, deep.surfaceContainerHighest)
    }

    @Test
    fun qa04_explorePreservesFiltersAcrossScrollToTopRetap() {
        val activeFilters = SearchFilters(
            category = WallpaperCategory.AMOLED,
            amoledOnly = true
        )
        val results = WallpaperCatalog.search(activeFilters)
        assertTrue(results.isNotEmpty())

        var scrollTick by mutableIntStateOf(0)
        val listState = androidx.compose.foundation.lazy.LazyListState()
        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.MATERIAL_3, reducedMotionEnabled = true) {
                ExploreSearchScreen(
                    filters = activeFilters,
                    results = results,
                    recentSearches = emptyList(),
                    favoriteIds = emptySet(),
                    columns = 2,
                    listState = listState,
                    onQueryChange = {},
                    onCommitSearch = {},
                    onClearHistory = {},
                    onSelectResolution = {},
                    onToggleAmoled = {},
                    onTogglePortrait = {},
                    onToggleDepthStudio = {},
                    onSelectColor = {},
                    onSelectCategory = {},
                    onSelectCollection = {},
                    onSelectSort = {},
                    onResetFilters = {},
                    onOpenWallpaper = {},
                    onToggleFavorite = {},
                    modifier = Modifier.testTag("explore_lazy_list"),
                    scrollToTopTick = scrollTick
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("explore_lazy_list").performScrollToIndex(6)
        composeRule.waitForIdle()
        assertTrue(listState.firstVisibleItemIndex >= 5)

        // Simulate re-tapping Explore tab -> scroll-to-top brings index back to 0 while keeping filters active
        composeRule.runOnUiThread { scrollTick += 1 }
        composeRule.waitForIdle()
        assertEquals(0, listState.firstVisibleItemIndex)
        composeRule.onNodeWithTag("explore_lazy_list")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("reset_filters_button"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reset_filters_button").assertIsDisplayed()
        composeRule.onNodeWithText("${results.size} wallpapers", substring = true).assertIsDisplayed()
    }

    @Test
    fun qa05_notificationRepositoryGroupingPrivacyAndDismissal() {
        LockScreenNotificationRepository.clearAllClearable()
        val sensitiveMsg = LockNotificationItem(
            key = "k_msg_1",
            packageName = "com.example.chat",
            appName = "Messages",
            senderOrTitle = "Alex",
            bodyPreview = "Meet at 5pm",
            postTime = 2000L,
            isClearable = true,
            isSensitive = true
        )
        val stickySystem = LockNotificationItem(
            key = "k_sys_1",
            packageName = "com.example.player",
            appName = "Audio",
            senderOrTitle = "Now Playing",
            bodyPreview = "Midnight Synth",
            postTime = 1500L,
            isClearable = false,
            isSensitive = false
        )
        LockScreenNotificationRepository.setGroupsForTesting(
            listOf(
                LockNotificationGroup(
                    packageName = "com.example.chat",
                    appName = "Messages",
                    notifications = listOf(sensitiveMsg),
                    latestTimestamp = 2000L
                ),
                LockNotificationGroup(
                    packageName = "com.example.player",
                    appName = "Audio",
                    notifications = listOf(stickySystem),
                    latestTimestamp = 1500L
                )
            )
        )
        assertEquals(2, LockScreenNotificationRepository.totalCount.value)

        // Render lock surface with sensitive content hidden -> collapsed by default, then tap expands masked text
        var hideSensitive by mutableStateOf(true)
        val groupsState = LockScreenNotificationRepository.groups.value
        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.FROSTED_GLASS_OS, reducedMotionEnabled = true) {
                LockScreenOverlaySurface(
                    selectedSet = null,
                    clockConfig = ClockDesignConfig(),
                    telemetry = ClockWidgetTelemetry(),
                    onDismissToUnlock = {},
                    preferences = UserPreferencesDoc(activeLockWallpaperId = WallpaperCatalog.allWallpapers[3].id),
                    notificationGroups = groupsState,
                    hideSensitiveNotifications = hideSensitive,
                    isPreviewMode = true
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lockscreen_notif_group_com.example.chat").assertIsDisplayed()
        composeRule.onNodeWithText("Alex").assertIsDisplayed()
        composeRule.onNodeWithTag("lockscreen_notif_group_com.example.chat").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Notification content hidden", substring = true).assertIsDisplayed()

        // Reveal when sensitive hiding is off
        composeRule.runOnUiThread { hideSensitive = false }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Meet at 5pm").assertIsDisplayed()

        // Clear all clearable leaves sticky non-clearable item intact
        LockScreenNotificationRepository.clearAllClearable()
        assertEquals(1, LockScreenNotificationRepository.totalCount.value)
        assertEquals("k_sys_1", LockScreenNotificationRepository.groups.value.first().notifications.first().key)
    }

    @Test
    fun qa06_lockScreenResolvesActiveWallpaperOverFallback() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val targetWp = WallpaperCatalog.allWallpapers[12]
        val resolved = resolveActiveLockArtwork(
            context = ctx,
            preferences = UserPreferencesDoc(activeLockWallpaperId = targetWp.id),
            selectedSet = null
        )
        assertEquals(targetWp.id, resolved.wallpaper.id)
    }

    @Test
    fun qa07_originalThreeWidgetsRenderRemoteViewsAndHandleActions() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val firstWp = WallpaperCatalog.allWallpapers[0]
        val secondWp = WallpaperCatalog.allWallpapers[1]
        BackdropWidgetContract.setCurrentWallpaperId(ctx, firstWp.id)

        val variants = listOf(
            BackdropWidgetVariant.SWITCHER_2X2,
            BackdropWidgetVariant.STRIP_4X1,
            BackdropWidgetVariant.SHOWCASE_4X2
        )
        variants.forEachIndexed { idx, variant ->
            val rv = BaseBackdropWidgetProvider.buildRemoteViews(ctx, appWidgetId = 10 + idx, variant = variant)
            assertNotNull(rv)
            val inflated = rv.apply(ctx, FrameLayout(ctx))
            assertNotNull("RemoteViews inflation failed for $variant", inflated)
        }

        // Advance next cycles cleanly
        val advancedId = BackdropWidgetContract.advanceNextWallpaperId(ctx)
        assertEquals(secondWp.id, advancedId)
        assertEquals(secondWp.id, BackdropWidgetContract.getCurrentWallpaperId(ctx))

        // Instantiate all 3 providers cleanly
        assertNotNull(BackdropSwitcherWidgetProvider())
        assertNotNull(BackdropStripWidgetProvider())
        assertNotNull(BackdropShowcaseWidgetProvider())
    }

    @Test
    fun qa08_zeroLagPerformanceEngineTieringAndTrimMemoryWorkCleanly() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ZeroLagPerformanceEngine.init(ctx)
        val tier = ZeroLagPerformanceEngine.classifyTier(ctx)
        assertTrue(ZeroLagPerformanceEngine.memoryCachePercent(tier) in 0.10..0.30)
        assertTrue(ZeroLagPerformanceEngine.diskCacheLimitBytes(tier) >= 80L * 1024L * 1024L)
        ZeroLagPerformanceEngine.handleTrimMemory(ctx, ComponentCallbacks2.TRIM_MEMORY_MODERATE)
        ZeroLagPerformanceEngine.handleTrimMemory(ctx, ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }
}
