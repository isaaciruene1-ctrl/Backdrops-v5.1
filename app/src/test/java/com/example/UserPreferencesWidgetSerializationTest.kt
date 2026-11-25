package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SecondaryWidgetPagePosition
import com.example.data.model.StudioWidgetItem
import com.example.data.model.StudioWidgetType
import com.example.data.model.UserPreferencesDoc
import com.example.data.repository.GuestLocalUserStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserPreferencesWidgetSerializationTest {

    private lateinit var context: Context
    private lateinit var store: GuestLocalUserStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        store = GuestLocalUserStore.getInstance(context)
    }

    @After
    fun tearDown() = runBlocking {
        store.updatePreferences(GuestLocalUserStore.GUEST_USER_ID) { UserPreferencesDoc() }
        Unit
    }

    @Test
    fun widgetCountsClampWithinValidRangesAndPageSidePersists() = runBlocking {
        store.updatePreferences(GuestLocalUserStore.GUEST_USER_ID) { prefs ->
            prefs.copy(
                lockScreenMaxWidgets = 99,
                widgetPageMaxWidgets = 0,
                secondaryWidgetPagePosition = SecondaryWidgetPagePosition.RIGHT.name,
                secondaryWidgetPageLeft = false
            )
        }

        val saved = store.getCurrentPreferencesSync()
        assertEquals(6, saved.safeLockScreenMaxWidgets)
        assertEquals(1, saved.safeWidgetPageMaxWidgets)
        assertEquals(SecondaryWidgetPagePosition.RIGHT, saved.parsedSecondaryWidgetPagePosition)

        val sharedPrefs = context.getSharedPreferences("backdrop_guest_store", Context.MODE_PRIVATE)
        assertEquals(6, sharedPrefs.getInt("lock_screen_max_widgets", -1))
        assertEquals(1, sharedPrefs.getInt("widget_page_max_widgets", -1))
        assertEquals("RIGHT", sharedPrefs.getString("secondary_widget_page_position", ""))
        assertFalse(sharedPrefs.getBoolean("secondary_widget_page_left", true))
    }

    @Test
    fun legacySecondaryWidgetPageLeftFalseMigratesToOff() {
        val legacyOff = UserPreferencesDoc(
            secondaryWidgetPageLeft = false,
            secondaryWidgetPagePosition = SecondaryWidgetPagePosition.LEFT.name
        )
        assertEquals(SecondaryWidgetPagePosition.OFF, legacyOff.parsedSecondaryWidgetPagePosition)

        val legacyLeft = UserPreferencesDoc(
            secondaryWidgetPageLeft = true,
            secondaryWidgetPagePosition = "INVALID_VALUE"
        )
        assertEquals(SecondaryWidgetPagePosition.LEFT, legacyLeft.parsedSecondaryWidgetPagePosition)

        val explicitRight = UserPreferencesDoc(
            secondaryWidgetPageLeft = false,
            secondaryWidgetPagePosition = SecondaryWidgetPagePosition.RIGHT.name
        )
        assertEquals(SecondaryWidgetPagePosition.RIGHT, explicitRight.parsedSecondaryWidgetPagePosition)
    }

    @Test
    fun studioWidgetItemRoundTripsSortOrderAndFreePositioning() {
        val base = StudioWidgetItem.listFromJson("")
        val customized = base.map { item ->
            when (item.type) {
                StudioWidgetType.WEATHER -> item.copy(
                    enabled = true,
                    sortOrder = 2,
                    isFreePositioned = true,
                    freeXFraction = 0.78f,
                    freeYFraction = 0.64f
                )
                StudioWidgetType.BATTERY -> item.copy(
                    enabled = true,
                    sortOrder = 0,
                    isFreePositioned = false
                )
                StudioWidgetType.MUSIC_PLAYER -> item.copy(
                    enabled = true,
                    sortOrder = 1,
                    isFreePositioned = false
                )
                else -> item
            }
        }

        val json = StudioWidgetItem.listToJson(customized)
        val restored = StudioWidgetItem.listFromJson(json).associateBy { it.type }

        val weather = restored.getValue(StudioWidgetType.WEATHER)
        assertTrue(weather.enabled)
        assertTrue(weather.isFreePositioned)
        assertEquals(2, weather.sortOrder)
        assertEquals(0.78f, weather.freeXFraction, 0.001f)
        assertEquals(0.64f, weather.freeYFraction, 0.001f)

        val battery = restored.getValue(StudioWidgetType.BATTERY)
        assertFalse(battery.isFreePositioned)
        assertEquals(0, battery.sortOrder)

        val music = restored.getValue(StudioWidgetType.MUSIC_PLAYER)
        assertTrue(music.enabled)
        assertFalse(music.isFreePositioned)
        assertEquals(1, music.sortOrder)
    }
}
