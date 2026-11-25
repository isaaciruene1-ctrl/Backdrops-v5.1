package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ApplicationProvider
import com.example.data.catalog.WallpaperCatalog
import com.example.data.media.OfflinePackDownloadManager
import com.example.data.model.LockNotificationStyleConfig
import com.example.data.model.NotificationLayoutMode
import com.example.data.model.StudioWidgetItem
import com.example.data.model.StudioWidgetType
import com.example.data.model.UiStyleMode
import com.example.data.model.UserPreferencesDoc
import com.example.engine.notifications.LockNotificationGroup
import com.example.engine.notifications.LockNotificationItem
import com.example.ui.components.ClockDesignConfig
import com.example.ui.components.ClockWidgetTelemetry
import com.example.ui.components.LockScreenNotificationsSection
import com.example.ui.components.SharedLockScreenRenderer
import com.example.ui.screens.CollectionsScreen
import com.example.ui.screens.LockScreenModeScreen
import com.example.ui.theme.BackdropTheme
import com.example.ui.viewmodel.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class NotificationAndWidgetVerificationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun sampleGroups(): List<LockNotificationGroup> {
        val now = System.currentTimeMillis()
        return listOf(
            LockNotificationGroup(
                packageName = "com.chat.app",
                appName = "Messages",
                appIconBitmap = null,
                notifications = listOf(
                    LockNotificationItem("k1", "com.chat.app", "Messages", "Maya", "Are we still on for the sunset hike?", postTime = now),
                    LockNotificationItem("k1b", "com.chat.app", "Messages", "Alex", "Dropped the trail pin for you", postTime = now - 60_000L)
                ),
                latestTimestamp = now
            ),
            LockNotificationGroup(
                packageName = "com.mail.app",
                appName = "Mail",
                appIconBitmap = null,
                notifications = listOf(
                    LockNotificationItem("k2", "com.mail.app", "Mail", "Flight Confirmation", "Gate B14 boarding starts at 18:10", postTime = now - 120_000L)
                ),
                latestTimestamp = now - 120_000L
            ),
            LockNotificationGroup(
                packageName = "com.cal.app",
                appName = "Calendar",
                appIconBitmap = null,
                notifications = listOf(
                    LockNotificationItem("k3", "com.cal.app", "Calendar", "Design Review", "Room 4B in 20 minutes", postTime = now - 300_000L)
                ),
                latestTimestamp = now - 300_000L
            )
        )
    }

    @Test
    fun test01_iosStackedNotificationsExpandCollapseSwipeClearAndEmptyState() {
        var groups by mutableStateOf(sampleGroups())
        val dismissedKeys = mutableListOf<String>()
        var clearedAll = false

        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.FROSTED_GLASS_OS, reducedMotionEnabled = true) {
                LockScreenNotificationsSection(
                    groups = groups,
                    styleConfig = LockNotificationStyleConfig(layoutMode = NotificationLayoutMode.STACK),
                    expandedKeys = mutableMapOf(),
                    hideSensitive = false,
                    onOpenItem = {},
                    onDismissItem = { key ->
                        dismissedKeys += key
                        groups = groups.mapNotNull { g ->
                            val rem = g.notifications.filterNot { it.key == key }
                            if (rem.isEmpty()) null else g.copy(notifications = rem)
                        }
                    },
                    onClearAll = {
                        clearedAll = true
                        groups = emptyList()
                    }
                )
            }
        }
        composeRule.waitForIdle()

        // Collapsed iOS deck: newest notification front card + peeking stacked cards behind
        composeRule.onNodeWithTag("lockscreen_notif_stacked_deck").assertIsDisplayed()
        composeRule.onNodeWithTag("lockscreen_stack_front_card").assertIsDisplayed()
        composeRule.onNodeWithTag("lockscreen_stack_back_card_1").assertIsDisplayed()
        composeRule.onNodeWithTag("lockscreen_clear_notifications_btn").assertDoesNotExist()

        // Tap stack -> expands into readable collection with Clear All & Show Less
        composeRule.onNodeWithTag("lockscreen_notif_stacked_deck").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lockscreen_notifications_expanded_stack").assertIsDisplayed()
        composeRule.onNodeWithTag("lockscreen_clear_notifications_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("lockscreen_stack_collapse_btn").assertIsDisplayed()

        // Collapse back to stacked deck
        composeRule.onNodeWithTag("lockscreen_stack_collapse_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lockscreen_notif_stacked_deck").assertIsDisplayed()

        // Swipe front card left -> dismisses front item & reveals next
        composeRule.onNodeWithTag("lockscreen_stack_front_card").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        assertTrue(dismissedKeys.isNotEmpty())

        // Expand again and tap Clear All -> empty state disappears cleanly
        composeRule.onNodeWithTag("lockscreen_notif_stacked_deck").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lockscreen_clear_notifications_btn").performClick()
        composeRule.waitForIdle()
        assertTrue(clearedAll)
        composeRule.onNodeWithTag("lockscreen_notif_stacked_deck").assertDoesNotExist()
        composeRule.onNodeWithTag("lockscreen_notifications_expanded_stack").assertDoesNotExist()
        composeRule.onNodeWithTag("lockscreen_notif_count_pill").assertDoesNotExist()
    }

    @Test
    fun test02_notificationListAndCountModesSwitchLiveAndRespectEmptyState() {
        var mode by mutableStateOf(NotificationLayoutMode.STACK)
        var groups by mutableStateOf(sampleGroups())

        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.MATERIAL_3, reducedMotionEnabled = true) {
                LockScreenNotificationsSection(
                    groups = groups,
                    styleConfig = LockNotificationStyleConfig(layoutMode = mode),
                    expandedKeys = mutableMapOf(),
                    hideSensitive = false,
                    onOpenItem = {},
                    onDismissItem = {},
                    onClearAll = { groups = emptyList() }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lockscreen_notif_stacked_deck").assertIsDisplayed()

        composeRule.runOnUiThread { mode = NotificationLayoutMode.LIST }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lockscreen_notifications_list").assertIsDisplayed()

        composeRule.runOnUiThread { mode = NotificationLayoutMode.COUNT }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lockscreen_notif_count_pill").assertIsDisplayed()
        composeRule.onNodeWithTag("lockscreen_notif_count_pill").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lockscreen_notifications_count_expanded").assertIsDisplayed()
    }

    @Test
    fun test03_offlineDownloadedWallpapersReconcileFromDiskAndShowInCollectionsOfflineTab() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val wp1 = WallpaperCatalog.allWallpapers[0]
        val wp2 = WallpaperCatalog.allWallpapers[1]
        val offlineDir = OfflinePackDownloadManager.offlineDir(ctx)
        offlineDir.listFiles()?.forEach { it.delete() }

        // Write valid >128-byte jpg files directly as setup downloader would on disk
        File(offlineDir, "${wp1.id}.jpg").writeBytes(ByteArray(512) { 0x42 })
        File(offlineDir, "${wp2.id}.jpg").writeBytes(ByteArray(512) { 0x43 })

        val resolved = OfflinePackDownloadManager.resolveOfflineWallpapers(emptyList(), ctx)
        assertTrue(resolved.any { it.id == wp1.id })
        assertTrue(resolved.any { it.id == wp2.id })

        OfflinePackDownloadManager.syncPersistedState(
            context = ctx,
            targetCount = 20,
            downloadedIds = emptyList(),
            status = "NOT_DOWNLOADED"
        )
        assertTrue(wp1.id in OfflinePackDownloadManager.downloadedIds.value)
        assertTrue(wp2.id in OfflinePackDownloadManager.downloadedIds.value)

        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.MATERIAL_3, reducedMotionEnabled = true) {
                CollectionsScreen(
                    collectionsState = SyncState.Ready(emptyList()),
                    lockScreenSetsState = SyncState.Ready(emptyList()),
                    offlineWallpapers = resolved,
                    onCreateCollection = { _, _ -> },
                    onRenameCollection = { _, _, _ -> },
                    onDeleteCollection = {},
                    onOpenCollectionDetail = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("collections_section_Offline").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("collections_offline_list").assertIsDisplayed()

        // Clean up test files
        offlineDir.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun test04_lockScreenWidgetCornerHandlesAndEnlargedMediaPlayer() {
        val wp = WallpaperCatalog.allWallpapers.first()
        val dummyBmp = Bitmap.createBitmap(120, 200, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.rgb(40, 80, 120))
        }
        var widgets by mutableStateOf(
            listOf(
                StudioWidgetItem(StudioWidgetType.MUSIC_PLAYER, enabled = true, widthScale = 1.0f, heightScale = 1.0f),
                StudioWidgetItem(StudioWidgetType.WEATHER, enabled = true, widthScale = 1.0f, heightScale = 1.0f)
            )
        )

        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.FROSTED_GLASS_OS, reducedMotionEnabled = true) {
                SharedLockScreenRenderer(
                    wallpaper = wp,
                    customBitmap = null,
                    backdropBitmap = dummyBmp,
                    clockConfig = ClockDesignConfig(),
                    telemetry = ClockWidgetTelemetry("22°C • Clear", 84, true),
                    preferences = UserPreferencesDoc(
                        uiStyle = UiStyleMode.FROSTED_GLASS_OS.name,
                        studioWidgetsJson = StudioWidgetItem.listToJson(widgets)
                    ),
                    useLiveTime = false,
                    onUpdateWidgetItem = { updated ->
                        widgets = widgets.map { if (it.type == updated.type) updated else it }
                    }
                )
            }
        }
        composeRule.waitForIdle()

        // Tapping Music Player opens enlarged full-bleed album art overlay with bottom controls
        composeRule.onNodeWithTag("studio_widget_music_expand_area").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("enlarged_media_player_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("enlarged_media_play_pause_button").assertIsDisplayed()
        composeRule.onNodeWithTag("enlarged_media_collapse_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("enlarged_media_player_overlay").assertDoesNotExist()

        // Tapping Resize Widgets reveals 4 corner drag handles for enabled widgets
        composeRule.onNodeWithTag("lock_resize_widgets_pill").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("widget_resize_handle_MUSIC_PLAYER_tl").assertIsDisplayed()
        composeRule.onNodeWithTag("widget_resize_handle_MUSIC_PLAYER_br").assertIsDisplayed()
    }

    @Test
    fun test05_studioWidgetScaleClampingAndNineWidgetConfiguratorWithoutStackedWidget() {
        val clamped = StudioWidgetItem(StudioWidgetType.WEATHER, enabled = true, widthScale = 4.0f, heightScale = 0.1f).sanitized()
        assertEquals(1.65f, clamped.widthScale, 0.001f)
        assertEquals(0.65f, clamped.heightScale, 0.001f)

        val roundTrip = StudioWidgetItem.listFromJson(StudioWidgetItem.listToJson(listOf(clamped)))
        assertEquals(StudioWidgetType.entries.size, roundTrip.size)
        val restoredWeather = roundTrip.first { it.type == StudioWidgetType.WEATHER }
        assertEquals(1.65f, restoredWeather.widthScale, 0.01f)
        assertEquals(0.65f, restoredWeather.heightScale, 0.01f)
        assertFalse(StudioWidgetType.entries.any { it.name.contains("STACKED") })
    }

    @Test
    fun test06_frostedGlassModeUsesOnlyLiquidSlidersAndCollapsesDropdownsInLockModeAndSettings() {
        composeRule.setContent {
            BackdropTheme(uiStyle = UiStyleMode.FROSTED_GLASS_OS, reducedMotionEnabled = true) {
                LockScreenModeScreen(
                    preferences = UserPreferencesDoc(uiStyle = UiStyleMode.FROSTED_GLASS_OS.name),
                    savedSets = emptyList(),
                    currentClockConfig = ClockDesignConfig(),
                    onBack = {},
                    onOpenCompatibility = {},
                    onSelectOverlaySetId = {},
                    onSetOverlayEnabled = {}
                )
            }
        }
        composeRule.waitForIdle()
        val scrollList = composeRule.onNodeWithTag("lockscreen_mode_scroll_list")
        scrollList.performScrollToNode(hasTestTag("lockscreen_drawer_notif_style_toggle"))
        composeRule.onNodeWithTag("lockscreen_drawer_notif_style_toggle").performClick()
        composeRule.waitForIdle()
        scrollList.performScrollToNode(hasTestTag("lockmode_notif_mode_STACK"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lockmode_notif_mode_STACK").assertIsDisplayed()
        composeRule.onNodeWithTag("lockmode_notif_transparency_slider").assertDoesNotExist()
        scrollList.performScrollToNode(hasTestTag("lockmode_notif_sliders_dropdown_toggle"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lockmode_notif_sliders_dropdown_toggle").performClick()
        composeRule.waitForIdle()
        scrollList.performScrollToNode(hasTestTag("lockmode_notif_transparency_slider"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lockmode_notif_transparency_slider").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag("liquid_glass_slider_track").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithTag("material3_slider").fetchSemanticsNodes().isEmpty())
    }
}
