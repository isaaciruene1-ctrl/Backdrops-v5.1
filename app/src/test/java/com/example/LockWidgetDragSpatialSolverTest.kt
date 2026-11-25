package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.data.model.StudioWidgetItem
import com.example.data.model.StudioWidgetType
import com.example.ui.components.LockWidgetDragSpatialSolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockWidgetDragSpatialSolverTest {

    private val canvasBounds = Rect(0f, 0f, 1000f, 2000f)
    private val stripBounds = Rect(40f, 120f, 960f, 240f)
    private val slotBounds = mapOf(
        0 to Rect(50f, 125f, 330f, 235f),
        1 to Rect(350f, 125f, 630f, 235f),
        2 to Rect(650f, 125f, 930f, 235f)
    )

    private fun initialWidgets(): List<StudioWidgetItem> = listOf(
        StudioWidgetItem(StudioWidgetType.WEATHER, enabled = true, sortOrder = 0, isFreePositioned = false),
        StudioWidgetItem(StudioWidgetType.BATTERY, enabled = true, sortOrder = 1, isFreePositioned = false),
        StudioWidgetItem(StudioWidgetType.MUSIC_PLAYER, enabled = true, sortOrder = 2, isFreePositioned = false),
        StudioWidgetItem(StudioWidgetType.CALENDAR, enabled = false, sortOrder = 3, isFreePositioned = false)
    )

    @Test
    fun draggingInsideStripSnapsAndReordersContiguousSortOrder() {
        val result = LockWidgetDragSpatialSolver.applyDragDrop(
            allWidgets = initialWidgets(),
            draggedType = StudioWidgetType.WEATHER,
            pointerRoot = Offset(820f, 180f), // past rightmost slot center
            canvasBoundsInRoot = canvasBounds,
            stripBoundsInRoot = stripBounds,
            slotBoundsInRoot = mapOf(
                0 to slotBounds.getValue(1),
                1 to slotBounds.getValue(2)
            )
        ).associateBy { it.type }

        val weather = result.getValue(StudioWidgetType.WEATHER)
        val battery = result.getValue(StudioWidgetType.BATTERY)
        val music = result.getValue(StudioWidgetType.MUSIC_PLAYER)

        assertFalse(weather.isFreePositioned)
        assertEquals(0, battery.sortOrder)
        assertEquals(1, music.sortOrder)
        assertEquals(2, weather.sortOrder)
    }

    @Test
    fun draggingOutsideStripConvertsWidgetToClampedFreePlacement() {
        val result = LockWidgetDragSpatialSolver.applyDragDrop(
            allWidgets = initialWidgets(),
            draggedType = StudioWidgetType.BATTERY,
            pointerRoot = Offset(750f, 1300f), // open lock canvas
            canvasBoundsInRoot = canvasBounds,
            stripBoundsInRoot = stripBounds,
            slotBoundsInRoot = slotBounds
        ).associateBy { it.type }

        val battery = result.getValue(StudioWidgetType.BATTERY)
        assertTrue(battery.isFreePositioned)
        assertEquals(0.75f, battery.freeXFraction, 0.001f)
        assertEquals(0.65f, battery.freeYFraction, 0.001f)

        // Ensure edge clamping when dragged near screen edges
        val clampedResult = LockWidgetDragSpatialSolver.applyDragDrop(
            allWidgets = initialWidgets(),
            draggedType = StudioWidgetType.BATTERY,
            pointerRoot = Offset(10f, 1980f),
            canvasBoundsInRoot = canvasBounds,
            stripBoundsInRoot = stripBounds,
            slotBoundsInRoot = slotBounds
        ).associateBy { it.type }

        val clamped = clampedResult.getValue(StudioWidgetType.BATTERY)
        assertEquals(0.12f, clamped.freeXFraction, 0.001f)
        assertEquals(0.82f, clamped.freeYFraction, 0.001f)
    }

    @Test
    fun draggingFreeWidgetBackOverDockBandRedocksIntoStrip() {
        val freeList = initialWidgets().map {
            if (it.type == StudioWidgetType.MUSIC_PLAYER) {
                it.copy(isFreePositioned = true, freeXFraction = 0.6f, freeYFraction = 0.7f)
            } else {
                it
            }
        }

        val dockBand = Rect(40f, 120f, 960f, 220f)
        val result = LockWidgetDragSpatialSolver.applyDragDrop(
            allWidgets = freeList,
            draggedType = StudioWidgetType.MUSIC_PLAYER,
            pointerRoot = Offset(100f, 170f),
            canvasBoundsInRoot = canvasBounds,
            stripBoundsInRoot = null,
            dockBandBoundsInRoot = dockBand,
            slotBoundsInRoot = mapOf(
                0 to slotBounds.getValue(0),
                1 to slotBounds.getValue(1)
            )
        ).associateBy { it.type }

        val music = result.getValue(StudioWidgetType.MUSIC_PLAYER)
        assertFalse(music.isFreePositioned)
        assertEquals(0, music.sortOrder)
    }
}
