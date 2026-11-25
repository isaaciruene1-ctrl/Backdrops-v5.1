package com.example.ui.components

import android.Manifest
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.catalog.WallpaperCatalog
import com.example.data.model.StudioWidgetItem
import com.example.data.model.StudioWidgetType
import com.example.data.model.UiStyleMode
import com.example.engine.notifications.BackdropNotificationListenerService
import com.example.ui.theme.AppMotion
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalReducedMotionFlag
import com.example.ui.theme.LocalThemeCustomization
import com.example.ui.theme.LocalUiStyleMode
import com.example.ui.theme.rememberStudioHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

@Immutable
data class CalendarEventEntry(
    val id: Long,
    val title: String,
    val timeLabel: String
)

@Immutable
data class ActiveMediaPlaybackState(
    val hasSession: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "No active media",
    val artist: String = "Tap play in your music app",
    val albumArt: Bitmap? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val controller: MediaController? = null
)

@Immutable
data class DeviceDiagnosticsSummary(
    val ramUsedPercent: Int,
    val ramSummary: String,
    val storageUsedPercent: Int,
    val storageFreeGb: Int,
    val tierLabel: String
)

@Immutable
data class StudioNetworkSummary(
    val isConnected: Boolean,
    val transportLabel: String,
    val statusDetail: String
)

@Immutable
data class StudioSolarSummary(
    val sunriseLabel: String,
    val sunsetLabel: String,
    val phaseLabel: String,
    val progressFraction: Float
)

@Immutable
data class StudioDayProgressSummary(
    val dayPercent: Int,
    val hoursRemaining: Int,
    val weekPercent: Int
)

@Immutable
data class StudioVolumeSummary(
    val mediaPercent: Int,
    val ringerLabel: String
)

@Immutable
data class StudioQuoteEntry(
    val text: String,
    val author: String
)

object StudioWidgetSystemBridge {

    fun hasCalendarPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun loadUpcomingCalendarEvents(context: Context, limit: Int = 3): List<CalendarEventEntry> {
        if (!hasCalendarPermission(context)) return emptyList()
        return runCatching {
            val now = System.currentTimeMillis()
            val end = now + 7L * 24L * 60L * 60L * 1000L
            val results = mutableListOf<CalendarEventEntry>()
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART
            )
            val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
            val args = arrayOf(now.toString(), end.toString())
            val fmt = SimpleDateFormat("EEE h:mm a", Locale.getDefault())
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                args,
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Events._ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                while (cursor.moveToNext() && results.size < limit) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else results.size.toLong()
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx).orEmpty().ifBlank { "Event" } else "Event"
                    val startMs = if (startIdx >= 0) cursor.getLong(startIdx) else now
                    results += CalendarEventEntry(
                        id = id,
                        title = title,
                        timeLabel = fmt.format(Date(startMs))
                    )
                }
            }
            results
        }.getOrDefault(emptyList())
    }

    fun readActiveMediaState(context: Context): ActiveMediaPlaybackState {
        return runCatching {
            val mgr = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                ?: return ActiveMediaPlaybackState()
            val component = ComponentName(context, BackdropNotificationListenerService::class.java)
            val controllers = mgr.getActiveSessions(component)
            val active = controllers.firstOrNull { c ->
                val st = c.playbackState?.state
                st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_PAUSED || st == PlaybackState.STATE_BUFFERING
            } ?: controllers.firstOrNull() ?: return ActiveMediaPlaybackState()
            val meta = active.metadata
            val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty().ifBlank { "Playing Media" }
            val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
                .ifBlank { meta?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty().ifBlank { active.packageName } }
            val isPlaying = active.playbackState?.state == PlaybackState.STATE_PLAYING
            val albumArt = extractBoundedAlbumArt(meta)
            val durationMs = (meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L).coerceAtLeast(0L)
            val positionMs = (active.playbackState?.position ?: 0L).coerceAtLeast(0L)
            ActiveMediaPlaybackState(
                hasSession = true,
                isPlaying = isPlaying,
                title = title,
                artist = artist,
                albumArt = albumArt,
                durationMs = durationMs,
                positionMs = if (durationMs > 0L) positionMs.coerceAtMost(durationMs) else positionMs,
                controller = active
            )
        }.getOrDefault(ActiveMediaPlaybackState())
    }

    internal fun extractBoundedAlbumArt(meta: MediaMetadata?): Bitmap? {
        if (meta == null) return null
        val raw = runCatching {
            meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        }.getOrNull() ?: return null
        if (raw.isRecycled || raw.width <= 0 || raw.height <= 0) return null
        val software = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && raw.config == Bitmap.Config.HARDWARE) {
            runCatching { raw.copy(Bitmap.Config.ARGB_8888, false) }.getOrNull() ?: return null
        } else {
            raw
        }
        val maxEdge = max(software.width, software.height)
        return if (maxEdge > 1024) {
            val scale = 1024f / maxEdge.toFloat()
            val dstW = (software.width * scale).roundToInt().coerceAtLeast(1)
            val dstH = (software.height * scale).roundToInt().coerceAtLeast(1)
            runCatching { Bitmap.createScaledBitmap(software, dstW, dstH, true) }.getOrDefault(software)
        } else {
            software
        }
    }

    val pinnedNotePresets: List<String> = listOf(
        "Deep focus sprint — finish one milestone at a time.",
        "Hydrate, posture check & 10-minute walk at 3 PM.",
        "Zero inbox check before midday creative block.",
        "Capture evening light before sunset."
    )

    val dailyQuotes: List<StudioQuoteEntry> = listOf(
        StudioQuoteEntry("Simplicity is about subtracting the obvious and adding the meaningful.", "John Maeda"),
        StudioQuoteEntry("Good design is as little design as possible.", "Dieter Rams"),
        StudioQuoteEntry("Details are not the details. They make the design.", "Charles Eames"),
        StudioQuoteEntry("Calm technology informs without demanding your full attention.", "Mark Weiser"),
        StudioQuoteEntry("Perfection is achieved not when there is nothing more to add, but nothing left to take away.", "Antoine de Saint-Exupery"),
        StudioQuoteEntry("Light defines space more faithfully than stone.", "Le Corbusier"),
        StudioQuoteEntry("Clarity precedes mastery.", "Robin Sharma"),
        StudioQuoteEntry("Make every screen earn its place in your day.", "Studio Journal")
    )

    fun readNetworkStatus(context: Context): StudioNetworkSummary {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return StudioNetworkSummary(false, "Offline", "No connectivity service")
            val active = cm.activeNetwork ?: return StudioNetworkSummary(false, "Offline", "No active route")
            val caps = cm.getNetworkCapabilities(active)
                ?: return StudioNetworkSummary(false, "Offline", "Awaiting link")
            val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val transport = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Online"
            }
            val mbps = (caps.linkDownstreamBandwidthKbps / 1000).coerceAtLeast(0)
            val detail = when {
                !validated -> "Checking internet"
                mbps > 0 -> "Validated • ${mbps} Mbps"
                else -> "Validated link"
            }
            StudioNetworkSummary(isConnected = true, transportLabel = transport, statusDetail = detail)
        }.getOrDefault(StudioNetworkSummary(false, "Offline", "Link unavailable"))
    }

    fun readSolarSummary(nowMs: Long = System.currentTimeMillis()): StudioSolarSummary {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val seasonalShiftMin = (sin((dayOfYear - 80) * 2.0 * PI / 365.0) * 52.0).roundToInt()
        val sunriseMin = (6 * 60 + 30) - seasonalShiftMin / 2
        val sunsetMin = (18 * 60 + 30) + seasonalShiftMin / 2
        val currentMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val sunriseLabel = String.format(Locale.US, "%02d:%02d", sunriseMin / 60, sunriseMin % 60)
        val sunsetLabel = String.format(Locale.US, "%02d:%02d", sunsetMin / 60, sunsetMin % 60)
        val dayLen = (sunsetMin - sunriseMin).coerceAtLeast(1)
        val fraction = ((currentMin - sunriseMin).toFloat() / dayLen.toFloat()).coerceIn(0f, 1f)
        val phase = when {
            currentMin < sunriseMin -> "Pre-Dawn"
            currentMin < sunriseMin + 55 -> "Golden Hour"
            currentMin < sunsetMin - 55 -> "Daylight"
            currentMin < sunsetMin + 25 -> "Sunset"
            else -> "Night"
        }
        return StudioSolarSummary(
            sunriseLabel = sunriseLabel,
            sunsetLabel = sunsetLabel,
            phaseLabel = phase,
            progressFraction = fraction
        )
    }

    fun readDayProgress(nowMs: Long = System.currentTimeMillis()): StudioDayProgressSummary {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val elapsedMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val dayPct = ((elapsedMin * 100f) / (24f * 60f)).roundToInt().coerceIn(0, 100)
        val hoursLeft = ((24 * 60 - elapsedMin) / 60).coerceIn(0, 24)
        val dayOfWeek = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) // Monday = 0
        val weekPct = (((dayOfWeek * 24f * 60f + elapsedMin) * 100f) / (7f * 24f * 60f))
            .roundToInt().coerceIn(0, 100)
        return StudioDayProgressSummary(dayPercent = dayPct, hoursRemaining = hoursLeft, weekPercent = weekPct)
    }

    fun readAudioSummary(context: Context): StudioVolumeSummary {
        return runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return StudioVolumeSummary(60, "Normal")
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, maxVol)
            val pct = ((curVol * 100f) / maxVol).roundToInt().coerceIn(0, 100)
            val ringer = when (am.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> "Silent"
                AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                else -> "Ring Active"
            }
            StudioVolumeSummary(mediaPercent = pct, ringerLabel = ringer)
        }.getOrDefault(StudioVolumeSummary(60, "Ring Active"))
    }

    fun adjustMediaVolume(context: Context, deltaSteps: Int): StudioVolumeSummary {
        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return@runCatching
            val dir = if (deltaSteps >= 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, 0)
        }
        return readAudioSummary(context)
    }

    fun formatCardinalBearing(deg: Int): String {
        val norm = ((deg % 360) + 360) % 360
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val idx = (((norm + 22.5) / 45.0).toInt()) % dirs.size
        return "${norm}° ${dirs[idx]}"
    }

    fun readDiagnostics(context: Context, tierLabel: String): DeviceDiagnosticsSummary {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        runCatching { am?.getMemoryInfo(memInfo) }
        val totalMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1024L)
        val availMb = (memInfo.availMem / (1024L * 1024L)).coerceAtLeast(128L)
        val usedMb = (totalMb - availMb).coerceAtLeast(0L)
        val ramPct = ((usedMb * 100f) / totalMb).roundToInt().coerceIn(5, 98)

        val stat = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        val totalBytes = stat?.totalBytes ?: (64L * 1024 * 1024 * 1024)
        val availBytes = stat?.availableBytes ?: (24L * 1024 * 1024 * 1024)
        val usedPct = (((totalBytes - availBytes).toFloat() / totalBytes.coerceAtLeast(1L)) * 100f)
            .roundToInt()
            .coerceIn(4, 98)
        val freeGb = (availBytes / (1024L * 1024 * 1024)).toInt().coerceAtLeast(0)
        return DeviceDiagnosticsSummary(
            ramUsedPercent = ramPct,
            ramSummary = "${(usedMb / 1024f).let { String.format(Locale.US, "%.1f", it) }}/${(totalMb / 1024f).roundToInt()}GB",
            storageUsedPercent = usedPct,
            storageFreeGb = freeGb,
            tierLabel = tierLabel
        )
    }
}

private enum class WidgetResizeCorner(val tag: String, val dxSign: Float, val dySign: Float) {
    TOP_START("tl", -1f, -1f),
    TOP_END("tr", 1f, -1f),
    BOTTOM_START("bl", -1f, 1f),
    BOTTOM_END("br", 1f, 1f)
}

/**
 * Spatial solver governing hybrid Lock Screen widget drag-and-drop:
 * - Dragging within the widget strip band snaps into discrete strip grid slots & reorders `sortOrder`.
 * - Dragging outside places the widget freely on the lock screen canvas (`isFreePositioned = true`).
 * - Dragging a free widget back over the top strip band seamlessly re-docks it into the strip grid.
 */
object LockWidgetDragSpatialSolver {

    fun isInsideStripZone(
        pointerRoot: Offset,
        stripBoundsInRoot: Rect?,
        dockBandBoundsInRoot: Rect? = null,
        verticalBufferPx: Float = 56f
    ): Boolean {
        val zone = stripBoundsInRoot ?: dockBandBoundsInRoot ?: return false
        return pointerRoot.y in (zone.top - verticalBufferPx)..(zone.bottom + verticalBufferPx) &&
            pointerRoot.x in (zone.left - verticalBufferPx)..(zone.right + verticalBufferPx)
    }

    fun computeStripSlotIndex(
        pointerX: Float,
        slotBoundsInRoot: Map<Int, Rect>,
        remainingCount: Int
    ): Int {
        if (remainingCount <= 0 || slotBoundsInRoot.isEmpty()) return 0
        val sortedSlots = slotBoundsInRoot.entries.sortedBy { it.key }
        var insertAt = remainingCount
        for ((idx, rect) in sortedSlots) {
            if (pointerX < rect.center.x) {
                insertAt = idx.coerceIn(0, remainingCount)
                break
            }
        }
        return insertAt.coerceIn(0, remainingCount)
    }

    fun applyDragDrop(
        allWidgets: List<StudioWidgetItem>,
        draggedType: StudioWidgetType,
        pointerRoot: Offset,
        canvasBoundsInRoot: Rect,
        stripBoundsInRoot: Rect?,
        dockBandBoundsInRoot: Rect? = null,
        slotBoundsInRoot: Map<Int, Rect> = emptyMap()
    ): List<StudioWidgetItem> {
        val dragged = allWidgets.firstOrNull { it.type == draggedType } ?: return allWidgets
        val insideStrip = isInsideStripZone(
            pointerRoot = pointerRoot,
            stripBoundsInRoot = stripBoundsInRoot,
            dockBandBoundsInRoot = dockBandBoundsInRoot
        )
        return if (insideStrip) {
            val existingStripOthers = allWidgets
                .filter { it.enabled && !it.isFreePositioned && it.type != draggedType }
                .sortedBy { it.sortOrder }
            val insertIdx = computeStripSlotIndex(
                pointerX = pointerRoot.x,
                slotBoundsInRoot = slotBoundsInRoot,
                remainingCount = existingStripOthers.size
            )
            val reorderedStrip = existingStripOthers.toMutableList().apply {
                add(insertIdx, dragged.copy(isFreePositioned = false).sanitized())
            }
            val updatedByType = mutableMapOf<StudioWidgetType, StudioWidgetItem>()
            reorderedStrip.forEachIndexed { idx, item ->
                updatedByType[item.type] = item.copy(sortOrder = idx)
            }
            var tailOrder = reorderedStrip.size
            allWidgets.map { item ->
                updatedByType[item.type] ?: item.copy(sortOrder = tailOrder++).sanitized()
            }
        } else {
            val w = canvasBoundsInRoot.width.coerceAtLeast(1f)
            val h = canvasBoundsInRoot.height.coerceAtLeast(1f)
            val fx = ((pointerRoot.x - canvasBoundsInRoot.left) / w).coerceIn(0.12f, 0.88f)
            val fy = ((pointerRoot.y - canvasBoundsInRoot.top) / h).coerceIn(0.18f, 0.82f)
            allWidgets.map { item ->
                if (item.type == draggedType) {
                    item.copy(
                        isFreePositioned = true,
                        freeXFraction = fx,
                        freeYFraction = fy
                    ).sanitized()
                } else {
                    item
                }
            }
        }
    }
}

@Composable
fun StudioWidgetsLockStrip(
    widgets: List<StudioWidgetItem>,
    telemetry: ClockWidgetTelemetry,
    isCharging: Boolean,
    is24HourFormat: Boolean,
    isResizeMode: Boolean = false,
    maxWidgets: Int = 4,
    draggingWidgetType: StudioWidgetType? = null,
    previewInsertIndex: Int? = null,
    onStripBoundsChanged: ((Rect) -> Unit)? = null,
    onSlotBoundsChanged: ((Int, Rect) -> Unit)? = null,
    onStartLongPressDrag: ((StudioWidgetItem, Offset) -> Unit)? = null,
    onDragMove: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDragCancel: (() -> Unit)? = null,
    onUpdateWidgetItem: ((StudioWidgetItem) -> Unit)? = null,
    onExpandMediaPlayer: ((ActiveMediaPlaybackState) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeWidgets = remember(widgets, maxWidgets) {
        widgets.filter { it.enabled && !it.isFreePositioned }
            .sortedBy { it.sortOrder }
            .take(maxWidgets.coerceIn(0, 6))
    }
    if (activeWidgets.isEmpty() && draggingWidgetType == null) return

    val uiStyle = LocalUiStyleMode.current
    val reducedMotion = LocalReducedMotionFlag.current
    val motionSpeed = LocalThemeCustomization.current.m3MotionSpeed
    val latestOnStartDrag by rememberUpdatedState(onStartLongPressDrag)
    val latestOnDragMove by rememberUpdatedState(onDragMove)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)
    val latestOnDragCancel by rememberUpdatedState(onDragCancel)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                onStripBoundsChanged?.invoke(coords.boundsInRoot())
            }
    ) {
        val responsiveMaxCardW = (maxWidth.value - 20f).coerceIn(136f, 360f)
        if (activeWidgets.isEmpty() && draggingWidgetType != null) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .padding(horizontal = 8.dp)
                    .testTag("lockscreen_widgets_strip")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Drop to snap into widget strip",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lockscreen_widgets_strip"),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                contentPadding = PaddingValues(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(
                    count = activeWidgets.size,
                    key = { idx -> activeWidgets[idx].type.name }
                ) { index ->
                    val item = activeWidgets[index]
                    val isBeingDragged = draggingWidgetType == item.type
                    val itemAlpha by animateFloatAsState(
                        targetValue = if (isBeingDragged) 0.28f else 1f,
                        animationSpec = AppMotion.lockWidgetSnapSpring(uiStyle, reducedMotion, motionSpeed),
                        label = "strip_item_alpha_${item.type.name}"
                    )
                    val itemScale by animateFloatAsState(
                        targetValue = if (isBeingDragged) 0.92f else 1f,
                        animationSpec = AppMotion.lockWidgetDragLiftSpring(uiStyle, reducedMotion, motionSpeed),
                        label = "strip_item_scale_${item.type.name}"
                    )
                    var itemCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                    val dragModifier = if (!isResizeMode && onStartLongPressDrag != null) {
                        Modifier.pointerInput(item.type, isResizeMode) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { startOffset ->
                                    itemCoords?.let { coords ->
                                        latestOnStartDrag?.invoke(item, coords.localToRoot(startOffset))
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    itemCoords?.let { coords ->
                                        latestOnDragMove?.invoke(coords.localToRoot(change.position))
                                    }
                                },
                                onDragEnd = { latestOnDragEnd?.invoke() },
                                onDragCancel = { latestOnDragCancel?.invoke() }
                            )
                        }
                    } else {
                        Modifier
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (previewInsertIndex == index && draggingWidgetType != null && !isBeingDragged) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(84.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.88f))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .onGloballyPositioned { coords ->
                                    itemCoords = coords
                                    onSlotBoundsChanged?.invoke(index, coords.boundsInRoot())
                                }
                                .then(dragModifier)
                                .graphicsLayer {
                                    alpha = itemAlpha
                                    scaleX = itemScale
                                    scaleY = itemScale
                                }
                        ) {
                            StudioWidgetCard(
                                item = item,
                                telemetry = telemetry,
                                isCharging = isCharging,
                                is24HourFormat = is24HourFormat,
                                isResizeMode = isResizeMode,
                                onUpdateWidgetItem = onUpdateWidgetItem,
                                onExpandMediaPlayer = onExpandMediaPlayer,
                                onDarkSurface = true,
                                maxCardWidthDp = responsiveMaxCardW
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudioWidgetCard(
    item: StudioWidgetItem,
    telemetry: ClockWidgetTelemetry,
    isCharging: Boolean = false,
    is24HourFormat: Boolean = true,
    isResizeMode: Boolean = false,
    onUpdateWidgetItem: ((StudioWidgetItem) -> Unit)? = null,
    onExpandMediaPlayer: ((ActiveMediaPlaybackState) -> Unit)? = null,
    onDarkSurface: Boolean = false,
    maxCardWidthDp: Float = 360f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val uiStyle = LocalUiStyleMode.current
    val customTokens = LocalThemeCustomization.current
    val haptics = rememberStudioHaptics()
    val isGlass = uiStyle == UiStyleMode.FROSTED_GLASS_OS || onDarkSurface

    val cardBg = if (onDarkSurface) {
        Color(0xFF101522).copy(alpha = if (uiStyle == UiStyleMode.FROSTED_GLASS_OS) 0.56f else 0.72f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val cardFg = if (onDarkSurface) Color.White else MaterialTheme.colorScheme.onSurface
    val cardSub = if (onDarkSurface) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(item.type) {
        if (item.type == StudioWidgetType.DIGITAL_CLOCK ||
            item.type == StudioWidgetType.ANALOG_CLOCK ||
            item.type == StudioWidgetType.WORLD_CLOCK
        ) {
            while (true) {
                nowMs = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    var calendarGranted by remember { mutableStateOf(StudioWidgetSystemBridge.hasCalendarPermission(context)) }
    val calendarPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        calendarGranted = granted
    }

    val safeWScale = item.widthScale.coerceIn(0.65f, 1.65f)
    val safeHScale = item.heightScale.coerceIn(0.65f, 1.65f)
    val baseWidthDp = when (item.type) {
        StudioWidgetType.MUSIC_PLAYER,
        StudioWidgetType.CALENDAR,
        StudioWidgetType.QUICK_INFO,
        StudioWidgetType.WORLD_CLOCK,
        StudioWidgetType.SUNRISE_SUNSET,
        StudioWidgetType.DAY_PROGRESS,
        StudioWidgetType.PINNED_NOTE,
        StudioWidgetType.AUDIO_VOLUME,
        StudioWidgetType.DAILY_QUOTE -> 228f
        else -> 166f
    }
    val baseHeightDp = 114f
    val boundedMaxWidthDp = maxCardWidthDp.coerceAtLeast(112f)
    val resolvedWidthDp = (baseWidthDp * safeWScale).coerceIn(112f, boundedMaxWidthDp).dp
    val resolvedHeightDp = (baseHeightDp * safeHScale).coerceIn(78f, 196f).dp
    val baseWidthPx = with(density) { baseWidthDp.dp.toPx().coerceAtLeast(1f) }
    val baseHeightPx = with(density) { baseHeightDp.dp.toPx().coerceAtLeast(1f) }

    val latestItem by rememberUpdatedState(item)
    val latestOnUpdateWidget by rememberUpdatedState(onUpdateWidgetItem)

    Box(
        modifier = modifier
            .then(
                if (isResizeMode && onUpdateWidgetItem != null) {
                    Modifier.padding(8.dp)
                } else {
                    Modifier
                }
            )
    ) {
        Surface(
            color = cardBg,
            contentColor = cardFg,
            shape = RoundedCornerShape(22.dp),
            border = when {
                isResizeMode -> BorderStroke(1.6.dp, accent.copy(alpha = 0.92f))
                isGlass -> BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
                else -> null
            },
            tonalElevation = if (onDarkSurface) 0.dp else 3.dp,
            modifier = Modifier
                .width(resolvedWidthDp)
                .height(resolvedHeightDp)
                .testTag("studio_widget_${item.type.name.lowercase()}")
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                when (item.type) {
                    StudioWidgetType.DIGITAL_CLOCK -> {
                        val d = Date(nowMs)
                        val hhmm = SimpleDateFormat(if (is24HourFormat) "HH:mm" else "h:mm a", Locale.getDefault()).format(d)
                        val sec = SimpleDateFormat(":ss", Locale.getDefault()).format(d)
                        val date = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(d)
                        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                                Text(date, style = MaterialTheme.typography.labelSmall, color = cardSub, maxLines = 1)
                            }
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = hhmm,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 26.sp,
                                    color = cardFg
                                )
                                Text(
                                    text = sec,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = accent,
                                    modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                                )
                            }
                        }
                    }

                    StudioWidgetType.ANALOG_CLOCK -> {
                        val cal = remember(nowMs) { Calendar.getInstance().apply { timeInMillis = nowMs } }
                        val hr = cal.get(Calendar.HOUR)
                        val min = cal.get(Calendar.MINUTE)
                        val sec = cal.get(Calendar.SECOND)
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Canvas(modifier = Modifier.size((72f * minOf(safeWScale, safeHScale)).coerceIn(48f, 110f).dp)) {
                                val r = size.minDimension * 0.45f
                                val c = Offset(size.width / 2f, size.height / 2f)
                                drawCircle(color = cardSub.copy(alpha = 0.4f), radius = r, center = c, style = Stroke(2.dp.toPx()))
                                val hAngle = ((hr + min / 60f) * 30f - 90f) * PI / 180.0
                                val mAngle = (min * 6f - 90f) * PI / 180.0
                                val sAngle = (sec * 6f - 90f) * PI / 180.0
                                drawLine(cardFg, c, Offset(c.x + (r * 0.52f * cos(hAngle)).toFloat(), c.y + (r * 0.52f * sin(hAngle)).toFloat()), 3.dp.toPx(), StrokeCap.Round)
                                drawLine(cardFg, c, Offset(c.x + (r * 0.78f * cos(mAngle)).toFloat(), c.y + (r * 0.78f * sin(mAngle)).toFloat()), 2.2.dp.toPx(), StrokeCap.Round)
                                drawLine(accent, c, Offset(c.x + (r * 0.84f * cos(sAngle)).toFloat(), c.y + (r * 0.84f * sin(sAngle)).toFloat()), 1.4.dp.toPx(), StrokeCap.Round)
                                drawCircle(color = accent, radius = 3.dp.toPx(), center = c)
                            }
                            Column {
                                Text("Chrono", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = cardFg)
                                Text("${cal.get(Calendar.DAY_OF_MONTH)} ${SimpleDateFormat("MMM", Locale.getDefault()).format(Date(nowMs))}", style = MaterialTheme.typography.labelSmall, color = cardSub)
                            }
                        }
                    }

                    StudioWidgetType.WEATHER -> {
                        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Weather", style = MaterialTheme.typography.labelSmall, color = cardSub)
                                Icon(Icons.Rounded.WbSunny, contentDescription = "Weather condition", tint = Color(0xFFFFCA62), modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = telemetry.weatherText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = cardFg,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (telemetry.isWeatherAvailable) "Open-Meteo Live" else "Sync in Settings",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                    }

                    StudioWidgetType.CALENDAR -> {
                        val events = remember(calendarGranted, nowMs / 60_000L) {
                            if (calendarGranted) StudioWidgetSystemBridge.loadUpcomingCalendarEvents(context, limit = 2)
                            else emptyList()
                        }
                        val cal = Calendar.getInstance()
                        val dayStr = SimpleDateFormat("EEEE • MMM d", Locale.getDefault()).format(cal.time)
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = !calendarGranted && !isResizeMode) {
                                    calendarPermLauncher.launch(Manifest.permission.READ_CALENDAR)
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                                Text(dayStr, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = cardFg, maxLines = 1)
                            }
                            if (!calendarGranted) {
                                Text(
                                    text = "Tap to sync device calendar events",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = accent
                                )
                            } else if (events.isEmpty()) {
                                Text(
                                    text = "No upcoming events in the next 7 days",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cardSub,
                                    maxLines = 2
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    events.forEach { ev ->
                                        Text(
                                            text = "${ev.timeLabel} • ${ev.title}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = cardFg,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    StudioWidgetType.BATTERY -> {
                        val pct = telemetry.batteryPercent.coerceIn(0, 100)
                        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(if (isCharging) "Charging" else "Battery", style = MaterialTheme.typography.labelSmall, color = cardSub)
                                Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = if (isCharging) Color(0xFF5CF28E) else accent, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = cardFg
                            )
                            LinearProgressIndicator(
                                progress = { pct / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                                color = if (isCharging) Color(0xFF5CF28E) else accent,
                                trackColor = cardSub.copy(alpha = 0.25f)
                            )
                        }
                    }

                    StudioWidgetType.MUSIC_PLAYER -> {
                        var mediaState by remember { mutableStateOf(StudioWidgetSystemBridge.readActiveMediaState(context)) }
                        LaunchedEffect(isResizeMode) {
                            while (true) {
                                mediaState = StudioWidgetSystemBridge.readActiveMediaState(context)
                                delay(1000L)
                            }
                        }
                        val validArt = mediaState.albumArt?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = !isResizeMode && onExpandMediaPlayer != null) {
                                    haptics.performTick(16L)
                                    onExpandMediaPlayer?.invoke(mediaState)
                                }
                                .testTag("studio_widget_music_expand_area")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (validArt != null) {
                                    val artImg = remember(validArt) { validArt.asImageBitmap() }
                                    Image(
                                        bitmap = artImg,
                                        contentDescription = "Album art for ${mediaState.title}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                } else {
                                    Surface(
                                        color = accent.copy(alpha = 0.24f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mediaState.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = cardFg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(mediaState.artist, style = MaterialTheme.typography.labelSmall, color = cardSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        haptics.performTick(16L)
                                        mediaState.controller?.transportControls?.skipToPrevious()
                                        mediaState = StudioWidgetSystemBridge.readActiveMediaState(context)
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous track", tint = cardFg, modifier = Modifier.size(20.dp))
                                }
                                Surface(
                                    onClick = {
                                        haptics.performTick(20L)
                                        val tc = mediaState.controller?.transportControls
                                        if (mediaState.isPlaying) tc?.pause() else tc?.play()
                                        mediaState = StudioWidgetSystemBridge.readActiveMediaState(context)
                                    },
                                    shape = CircleShape,
                                    color = accent,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = if (mediaState.isPlaying) "Pause" else "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        haptics.performTick(16L)
                                        mediaState.controller?.transportControls?.skipToNext()
                                        mediaState = StudioWidgetSystemBridge.readActiveMediaState(context)
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next track", tint = cardFg, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    StudioWidgetType.QUICK_INFO -> {
                        val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
                        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                            Text("Quick Telemetry", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "$dateStr • ${telemetry.weatherText}",
                                style = MaterialTheme.typography.labelLarge,
                                color = cardFg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Battery ${telemetry.batteryPercent}% • ${customTokens.liquidGlassTier.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = cardSub,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    StudioWidgetType.PHOTO_FRAME -> {
                        val customUri = item.customPhotoUri
                        val fallbackWp = WallpaperCatalog.allWallpapers.first()
                        val customBmp by produceState<Bitmap?>(initialValue = null, key1 = customUri) {
                            if (customUri.isNotBlank()) {
                                value = withContext(Dispatchers.IO) {
                                    runCatching {
                                        context.contentResolver.openInputStream(Uri.parse(customUri))?.use {
                                            BitmapFactory.decodeStream(it)
                                        }
                                    }.getOrNull()
                                }
                            }
                        }
                        val validPhoto = customBmp?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))) {
                            if (validPhoto != null) {
                                val photoImg = remember(validPhoto) { validPhoto.asImageBitmap() }
                                Image(
                                    bitmap = photoImg,
                                    contentDescription = "Photo Frame",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                WallpaperAsyncImage(
                                    wallpaper = fallbackWp,
                                    useFullRes = false,
                                    contentDescription = "Curated Photo Frame",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    StudioWidgetType.SYSTEM_INFO -> {
                        val diag = remember(customTokens.liquidGlassTier) {
                            StudioWidgetSystemBridge.readDiagnostics(context, customTokens.liquidGlassTier.label)
                        }
                        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Rounded.Memory, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                                Text("System", style = MaterialTheme.typography.labelSmall, color = cardSub)
                            }
                            Text("RAM ${diag.ramUsedPercent}% (${diag.ramSummary})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = cardFg, maxLines = 1)
                            Text("${diag.storageFreeGb}GB free • ${diag.tierLabel}", style = MaterialTheme.typography.labelSmall, color = cardSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    StudioWidgetType.WORLD_CLOCK -> {
                        val cities = remember {
                            listOf(
                                "Tokyo" to "Asia/Tokyo",
                                "New York" to "America/New_York",
                                "London" to "Europe/London",
                                "Paris" to "Europe/Paris",
                                "Seoul" to "Asia/Seoul"
                            )
                        }
                        val initialIdx = item.widgetMeta.toIntOrNull()?.coerceIn(0, cities.lastIndex) ?: 0
                        var cityIdx by rememberSaveable(item.type, initialIdx) { mutableIntStateOf(initialIdx) }
                        val companion = cities[cityIdx % cities.size]
                        val d = Date(nowMs)
                        val pattern = if (is24HourFormat) "HH:mm" else "h:mm a"
                        val localFmt = remember(pattern) { SimpleDateFormat(pattern, Locale.getDefault()) }
                        val remoteFmt = remember(pattern, companion.second) {
                            SimpleDateFormat(pattern, Locale.getDefault()).apply {
                                timeZone = TimeZone.getTimeZone(companion.second)
                            }
                        }
                        val localCityName = remember {
                            TimeZone.getDefault().id.substringAfterLast('/').replace('_', ' ')
                        }
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = !isResizeMode) {
                                    haptics.performTick(14L)
                                    val nextIdx = (cityIdx + 1) % cities.size
                                    cityIdx = nextIdx
                                    onUpdateWidgetItem?.invoke(item.copy(widgetMeta = nextIdx.toString()))
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Rounded.Public, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                                Text("World Clock • Tap to switch", style = MaterialTheme.typography.labelSmall, color = cardSub, maxLines = 1)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(localCityName, style = MaterialTheme.typography.labelSmall, color = cardSub, maxLines = 1)
                                    Text(
                                        text = localFmt.format(d),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = cardFg
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(companion.first, style = MaterialTheme.typography.labelSmall, color = accent, maxLines = 1)
                                    Text(
                                        text = remoteFmt.format(d),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = cardFg
                                    )
                                }
                            }
                        }
                    }

                    StudioWidgetType.FOCUS_TIMER -> {
                        var remainingSec by rememberSaveable { mutableIntStateOf(25 * 60) }
                        var running by rememberSaveable { mutableStateOf(false) }
                        LaunchedEffect(running) {
                            while (running && remainingSec > 0) {
                                delay(1000L)
                                remainingSec = (remainingSec - 1).coerceAtLeast(0)
                                if (remainingSec == 0) running = false
                            }
                        }
                        val mins = remainingSec / 60
                        val secs = remainingSec % 60
                        val formatted = String.format(Locale.US, "%02d:%02d", mins, secs)
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = !isResizeMode) {
                                    haptics.performTick(16L)
                                    if (remainingSec == 0) remainingSec = 25 * 60
                                    running = !running
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.Timer, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                                    Text(if (running) "Focusing" else "Focus", style = MaterialTheme.typography.labelSmall, color = cardSub)
                                }
                                Icon(
                                    imageVector = Icons.Rounded.RestartAlt,
                                    contentDescription = "Reset Focus Timer",
                                    tint = cardSub,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable(enabled = !isResizeMode) {
                                            running = false
                                            remainingSec = 25 * 60
                                        }
                                )
                            }
                            Text(
                                text = formatted,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = cardFg
                            )
                            LinearProgressIndicator(
                                progress = { (remainingSec / (25f * 60f)).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                                color = accent,
                                trackColor = cardSub.copy(alpha = 0.25f)
                            )
                        }
                    }

                    StudioWidgetType.HABIT_STREAK -> {
                        val initialStreak = item.widgetMeta.toIntOrNull()?.coerceAtLeast(1) ?: 7
                        var streakDays by rememberSaveable(item.type, initialStreak) { mutableIntStateOf(initialStreak) }
                        var checkedToday by rememberSaveable(item.type) { mutableStateOf(false) }
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = !isResizeMode) {
                                    haptics.performConfirm()
                                    if (!checkedToday) {
                                        checkedToday = true
                                        streakDays += 1
                                        onUpdateWidgetItem?.invoke(item.copy(widgetMeta = streakDays.toString()))
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF9F43), modifier = Modifier.size(16.dp))
                                    Text("Habit Streak", style = MaterialTheme.typography.labelSmall, color = cardSub)
                                }
                                if (checkedToday) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Checked in", tint = Color(0xFF5CF28E), modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = "$streakDays Days",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = cardFg
                            )
                            Text(
                                text = if (checkedToday) "Today complete" else "Tap to check in today",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (checkedToday) Color(0xFF5CF28E) else accent,
                                maxLines = 1
                            )
                        }
                    }

                    StudioWidgetType.STOPWATCH -> {
                        var running by rememberSaveable(item.type) { mutableStateOf(false) }
                        var elapsedMs by rememberSaveable(item.type) { mutableLongStateOf(0L) }
                        LaunchedEffect(running) {
                            var lastTick = System.currentTimeMillis()
                            while (running) {
                                delay(100L)
                                val now = System.currentTimeMillis()
                                elapsedMs += (now - lastTick).coerceIn(0L, 1000L)
                                lastTick = now
                            }
                        }
                        val totalTenths = (elapsedMs / 100L).toInt()
                        val mins = (totalTenths / 600) % 100
                        val secs = (totalTenths / 10) % 60
                        val tenths = totalTenths % 10
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = !isResizeMode) {
                                    haptics.performTick(16L)
                                    running = !running
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.AvTimer, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                                    Text(if (running) "Running" else "Stopwatch", style = MaterialTheme.typography.labelSmall, color = cardSub)
                                }
                                Icon(
                                    imageVector = Icons.Rounded.RestartAlt,
                                    contentDescription = "Reset Stopwatch",
                                    tint = cardSub,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable(enabled = !isResizeMode) {
                                            running = false
                                            elapsedMs = 0L
                                        }
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%02d:%02d.%d", mins, secs, tenths),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 23.sp,
                                color = cardFg
                            )
                            Text(
                                text = if (running) "Tap to pause" else "Tap to start",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                    }

                    StudioWidgetType.COUNTDOWN -> {
                        val initialTotal = item.widgetMeta.toIntOrNull()?.coerceIn(60, 3600) ?: 300
                        var presetSec by rememberSaveable(item.type, initialTotal) { mutableIntStateOf(initialTotal) }
                        var remainingSec by rememberSaveable(item.type) { mutableIntStateOf(initialTotal) }
                        var running by rememberSaveable(item.type) { mutableStateOf(false) }
                        LaunchedEffect(running) {
                            while (running && remainingSec > 0) {
                                delay(1000L)
                                remainingSec = (remainingSec - 1).coerceAtLeast(0)
                                if (remainingSec == 0) running = false
                            }
                        }
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = !isResizeMode) {
                                    haptics.performTick(16L)
                                    if (remainingSec == 0) remainingSec = presetSec
                                    running = !running
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.HourglassBottom, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                                    Text(if (running) "Counting" else "Countdown", style = MaterialTheme.typography.labelSmall, color = cardSub)
                                }
                                Text(
                                    text = "+1m",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accent,
                                    modifier = Modifier.clickable(enabled = !isResizeMode) {
                                        val next = ((presetSec + 60).takeIf { it <= 3600 } ?: 60)
                                        presetSec = next
                                        remainingSec = next
                                        running = false
                                        onUpdateWidgetItem?.invoke(item.copy(widgetMeta = next.toString()))
                                    }
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", remainingSec / 60, remainingSec % 60),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 23.sp,
                                color = cardFg
                            )
                            LinearProgressIndicator(
                                progress = { (remainingSec.toFloat() / presetSec.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                                color = accent,
                                trackColor = cardSub.copy(alpha = 0.25f)
                            )
                        }
                    }

                    StudioWidgetType.COMPASS -> {
                        var azimuthDeg by remember { mutableFloatStateOf(0f) }
                        var sensorLive by remember { mutableStateOf(false) }
                        DisposableEffect(context) {
                            val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                            val rotSensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                            val listener = object : SensorEventListener {
                                private val rotMatrix = FloatArray(9)
                                private val orientation = FloatArray(3)
                                override fun onSensorChanged(event: SensorEvent) {
                                    SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                                    SensorManager.getOrientation(rotMatrix, orientation)
                                    val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                                    azimuthDeg = ((deg % 360f) + 360f) % 360f
                                    sensorLive = true
                                }
                                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                            }
                            if (sm != null && rotSensor != null) {
                                sm.registerListener(listener, rotSensor, SensorManager.SENSOR_DELAY_UI)
                            }
                            onDispose {
                                sm?.unregisterListener(listener)
                            }
                        }
                        val roundedDeg = azimuthDeg.roundToInt() % 360
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Canvas(modifier = Modifier.size((66f * minOf(safeWScale, safeHScale)).coerceIn(44f, 98f).dp)) {
                                val r = size.minDimension * 0.44f
                                val c = Offset(size.width / 2f, size.height / 2f)
                                drawCircle(cardSub.copy(alpha = 0.35f), radius = r, center = c, style = Stroke(2.dp.toPx()))
                                val rad = ((azimuthDeg - 90f) * PI / 180.0)
                                drawLine(
                                    color = Color(0xFFFF6B6B),
                                    start = c,
                                    end = Offset(c.x + (r * 0.78f * cos(rad)).toFloat(), c.y + (r * 0.78f * sin(rad)).toFloat()),
                                    strokeWidth = 2.8.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = cardSub.copy(alpha = 0.7f),
                                    start = c,
                                    end = Offset(c.x - (r * 0.45f * cos(rad)).toFloat(), c.y - (r * 0.45f * sin(rad)).toFloat()),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawCircle(accent, radius = 3.dp.toPx(), center = c)
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.Explore, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                                    Text("Compass", style = MaterialTheme.typography.labelSmall, color = cardSub)
                                }
                                Text(
                                    text = StudioWidgetSystemBridge.formatCardinalBearing(roundedDeg),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = cardFg
                                )
                                Text(
                                    text = if (sensorLive) "Sensor Active" else "Magnetic North",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent
                                )
                            }
                        }
                    }

                    StudioWidgetType.SUNRISE_SUNSET -> {
                        val solar = remember(nowMs / 60_000L) { StudioWidgetSystemBridge.readSolarSummary() }
                        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(Icons.Rounded.WbTwilight, contentDescription = null, tint = Color(0xFFFFCA62), modifier = Modifier.size(15.dp))
                                    Text("Solar Arc", style = MaterialTheme.typography.labelSmall, color = cardSub)
                                }
                                Text(solar.phaseLabel, style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFCA62), fontWeight = FontWeight.SemiBold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("↑ ${solar.sunriseLabel}", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = cardFg)
                                Text("↓ ${solar.sunsetLabel}", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = cardFg)
                            }
                            LinearProgressIndicator(
                                progress = { solar.progressFraction },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                                color = Color(0xFFFFCA62),
                                trackColor = cardSub.copy(alpha = 0.25f)
                            )
                        }
                    }

                    StudioWidgetType.DAY_PROGRESS -> {
                        val prog = remember(nowMs / 60_000L) { StudioWidgetSystemBridge.readDayProgress() }
                        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(Icons.Rounded.DonutLarge, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                                    Text("Day Progress", style = MaterialTheme.typography.labelSmall, color = cardSub)
                                }
                                Text("Week ${prog.weekPercent}%", style = MaterialTheme.typography.labelSmall, color = accent)
                            }
                            Text(
                                text = "${prog.dayPercent}% • ${prog.hoursRemaining}h remaining",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = cardFg
                            )
                            LinearProgressIndicator(
                                progress = { prog.dayPercent / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                                color = accent,
                                trackColor = cardSub.copy(alpha = 0.25f)
                            )
                        }
                    }

                    StudioWidgetType.PINNED_NOTE -> {
                        val initialNote = item.widgetMeta.ifBlank { StudioWidgetSystemBridge.pinnedNotePresets.first() }
                        var noteText by remember(item.widgetMeta) { mutableStateOf(initialNote) }
                        var showNoteEditor by remember { mutableStateOf(false) }
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = !isResizeMode) {
                                    haptics.performTick(16L)
                                    if (onUpdateWidgetItem != null) {
                                        showNoteEditor = true
                                    } else {
                                        val presets = StudioWidgetSystemBridge.pinnedNotePresets
                                        val next = presets[(presets.indexOf(noteText) + 1) % presets.size]
                                        noteText = next
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(Icons.Rounded.StickyNote2, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                                    Text("Pinned Note", style = MaterialTheme.typography.labelSmall, color = cardSub)
                                }
                                Text("Tap to edit", style = MaterialTheme.typography.labelSmall, color = accent)
                            }
                            Text(
                                text = noteText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = cardFg,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (showNoteEditor) {
                            var draft by remember { mutableStateOf(noteText) }
                            AlertDialog(
                                onDismissRequest = { showNoteEditor = false },
                                title = { Text("Pinned Note") },
                                text = {
                                    OutlinedTextField(
                                        value = draft,
                                        onValueChange = { draft = it.take(140) },
                                        label = { Text("Lock & Home memo") },
                                        maxLines = 3,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        val clean = draft.trim().ifBlank { StudioWidgetSystemBridge.pinnedNotePresets.first() }
                                        noteText = clean
                                        onUpdateWidgetItem?.invoke(item.copy(widgetMeta = clean))
                                        showNoteEditor = false
                                    }) { Text("Save") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showNoteEditor = false }) { Text("Cancel") }
                                }
                            )
                        }
                    }

                    StudioWidgetType.NETWORK_STATUS -> {
                        var net by remember { mutableStateOf(StudioWidgetSystemBridge.readNetworkStatus(context)) }
                        LaunchedEffect(Unit) {
                            while (true) {
                                net = StudioWidgetSystemBridge.readNetworkStatus(context)
                                delay(4000L)
                            }
                        }
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = !isResizeMode) {
                                    net = StudioWidgetSystemBridge.readNetworkStatus(context)
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Network", style = MaterialTheme.typography.labelSmall, color = cardSub)
                                Icon(
                                    imageVector = if (net.isConnected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                                    contentDescription = net.transportLabel,
                                    tint = if (net.isConnected) Color(0xFF5CF28E) else Color(0xFFFF7676),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = net.transportLabel,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = cardFg
                            )
                            Text(
                                text = net.statusDetail,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (net.isConnected) Color(0xFF5CF28E) else cardSub,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    StudioWidgetType.STEP_COUNTER -> {
                        val savedSteps = item.widgetMeta.toIntOrNull()?.coerceAtLeast(0) ?: 1420
                        var steps by rememberSaveable(item.type, savedSteps) { mutableIntStateOf(savedSteps) }
                        DisposableEffect(context) {
                            val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                            val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                            var lastStepAt = 0L
                            val listener = object : SensorEventListener {
                                override fun onSensorChanged(event: SensorEvent) {
                                    val x = event.values[0]
                                    val y = event.values[1]
                                    val z = event.values[2]
                                    val mag = kotlin.math.sqrt((x * x + y * y + z * z).toDouble())
                                    val now = System.currentTimeMillis()
                                    if (mag > 12.6 && now - lastStepAt > 420L) {
                                        lastStepAt = now
                                        steps += 1
                                    }
                                }
                                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                            }
                            if (sm != null && accel != null) {
                                sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_NORMAL)
                            }
                            onDispose { sm?.unregisterListener(listener) }
                        }
                        val goal = 8000
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = !isResizeMode) {
                                    haptics.performTick(16L)
                                    steps += 250
                                    onUpdateWidgetItem?.invoke(item.copy(widgetMeta = steps.toString()))
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.DirectionsRun, contentDescription = null, tint = Color(0xFF5CF28E), modifier = Modifier.size(15.dp))
                                    Text("Steps", style = MaterialTheme.typography.labelSmall, color = cardSub)
                                }
                                Text("+250", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = String.format(Locale.US, "%,d", steps),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 23.sp,
                                color = cardFg
                            )
                            LinearProgressIndicator(
                                progress = { (steps / goal.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                                color = Color(0xFF5CF28E),
                                trackColor = cardSub.copy(alpha = 0.25f)
                            )
                        }
                    }

                    StudioWidgetType.AUDIO_VOLUME -> {
                        var audio by remember { mutableStateOf(StudioWidgetSystemBridge.readAudioSummary(context)) }
                        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                                    Text("Media • ${audio.ringerLabel}", style = MaterialTheme.typography.labelSmall, color = cardSub, maxLines = 1)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = Icons.Rounded.Remove,
                                        contentDescription = "Volume down",
                                        tint = cardFg,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable(enabled = !isResizeMode) {
                                                haptics.performTick(14L)
                                                audio = StudioWidgetSystemBridge.adjustMediaVolume(context, -1)
                                            }
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "Volume up",
                                        tint = accent,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable(enabled = !isResizeMode) {
                                                haptics.performTick(14L)
                                                audio = StudioWidgetSystemBridge.adjustMediaVolume(context, 1)
                                            }
                                    )
                                }
                            }
                            Text(
                                text = "${audio.mediaPercent}% Output",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = cardFg
                            )
                            LinearProgressIndicator(
                                progress = { (audio.mediaPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                                color = accent,
                                trackColor = cardSub.copy(alpha = 0.25f)
                            )
                        }
                    }

                    StudioWidgetType.DAILY_QUOTE -> {
                        val dayIndex = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) }
                        val initialIdx = item.widgetMeta.toIntOrNull() ?: dayIndex
                        var quoteIdx by rememberSaveable(item.type, initialIdx) { mutableIntStateOf(initialIdx) }
                        val quotes = StudioWidgetSystemBridge.dailyQuotes
                        val entry = quotes[((quoteIdx % quotes.size) + quotes.size) % quotes.size]
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = !isResizeMode) {
                                    haptics.performTick(14L)
                                    val next = (quoteIdx + 1) % quotes.size
                                    quoteIdx = next
                                    onUpdateWidgetItem?.invoke(item.copy(widgetMeta = next.toString()))
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.FormatQuote, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                                    Text("Daily Quote", style = MaterialTheme.typography.labelSmall, color = cardSub)
                                }
                                Text("Tap to cycle", style = MaterialTheme.typography.labelSmall, color = accent)
                            }
                            Text(
                                text = "\"${entry.text}\"",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = cardFg,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "— ${entry.author}",
                                style = MaterialTheme.typography.labelSmall,
                                color = cardSub,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        if (isResizeMode && onUpdateWidgetItem != null) {
            // Subtle center size & reset badge when widget dimensions differ from 1.0x
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${(safeWScale * 100).roundToInt()}%×${(safeHScale * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontSize = 10.sp
                )
                if (kotlin.math.abs(safeWScale - 1f) > 0.03f || kotlin.math.abs(safeHScale - 1f) > 0.03f) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = "Reset ${item.type.title} size",
                        tint = Color.White,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable {
                                haptics.performSnapDetent()
                                onUpdateWidgetItem(item.copy(widthScale = 1f, heightScale = 1f))
                            }
                            .testTag("widget_resize_reset_${item.type.name.lowercase()}")
                    )
                }
            }

            // 4 direct corner drag handles (TL / TR / BL / BR) with 48dp accessible hit targets
            WidgetResizeCorner.entries.forEach { corner ->
                val alignment = when (corner) {
                    WidgetResizeCorner.TOP_START -> Alignment.TopStart
                    WidgetResizeCorner.TOP_END -> Alignment.TopEnd
                    WidgetResizeCorner.BOTTOM_START -> Alignment.BottomStart
                    WidgetResizeCorner.BOTTOM_END -> Alignment.BottomEnd
                }
                WidgetCornerGripHandle(
                    corner = corner,
                    uiStyle = uiStyle,
                    useClassic = customTokens.useClassicClockHandles,
                    modifier = Modifier
                        .align(alignment)
                        .size(48.dp)
                        .pointerInput(item.type, isResizeMode) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val current = latestItem
                                val nextW = (current.widthScale + (dragAmount.x * corner.dxSign) / baseWidthPx)
                                    .coerceIn(0.65f, 1.65f)
                                val nextH = (current.heightScale + (dragAmount.y * corner.dySign) / baseHeightPx)
                                    .coerceIn(0.65f, 1.65f)
                                latestOnUpdateWidget?.invoke(
                                    current.copy(widthScale = nextW, heightScale = nextH).sanitized()
                                )
                            }
                        }
                        .testTag("widget_resize_handle_${item.type.name.lowercase()}_${corner.tag}")
                )
            }
        }
    }
}

@Composable
private fun WidgetCornerGripHandle(
    corner: WidgetResizeCorner,
    uiStyle: UiStyleMode,
    useClassic: Boolean,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            useClassic -> {
                Canvas(modifier = Modifier.size(20.dp)) {
                    drawCircle(color = surface, radius = size.minDimension * 0.42f)
                    drawCircle(color = primary, radius = size.minDimension * 0.42f, style = Stroke(2.dp.toPx()))
                }
            }
            uiStyle == UiStyleMode.FROSTED_GLASS_OS -> {
                Canvas(modifier = Modifier.size(24.dp)) {
                    val startAngle = when (corner) {
                        WidgetResizeCorner.TOP_START -> 180f
                        WidgetResizeCorner.TOP_END -> 270f
                        WidgetResizeCorner.BOTTOM_END -> 0f
                        WidgetResizeCorner.BOTTOM_START -> 90f
                    }
                    drawArc(
                        brush = Brush.linearGradient(listOf(Color.White, primary.copy(alpha = 0.72f))),
                        startAngle = startAngle,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            else -> {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val m = 3.dp.toPx()
                    val arm = 8.dp.toPx()
                    val p = Path().apply {
                        when (corner) {
                            WidgetResizeCorner.TOP_START -> {
                                moveTo(m, m + arm); lineTo(m, m); lineTo(m + arm, m)
                            }
                            WidgetResizeCorner.TOP_END -> {
                                moveTo(size.width - m - arm, m); lineTo(size.width - m, m); lineTo(size.width - m, m + arm)
                            }
                            WidgetResizeCorner.BOTTOM_START -> {
                                moveTo(m, size.height - m - arm); lineTo(m, size.height - m); lineTo(m + arm, size.height - m)
                            }
                            WidgetResizeCorner.BOTTOM_END -> {
                                moveTo(size.width - m - arm, size.height - m); lineTo(size.width - m, size.height - m); lineTo(size.width - m, size.height - m - arm)
                            }
                        }
                    }
                    drawPath(p, surface, style = Stroke(4.5.dp.toPx(), cap = StrokeCap.Square, join = StrokeJoin.Miter))
                    drawPath(p, primary, style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Square, join = StrokeJoin.Miter))
                }
            }
        }
    }
}

/**
 * Interactive configurator for the 22-Widget Suite inside Lock Screen Mode / Studio.
 */
@Composable
fun StudioWidgetsConfiguratorCard(
    widgets: List<StudioWidgetItem>,
    onUpdateWidgets: (List<StudioWidgetItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberStudioHaptics()
    val calendarPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* UI automatically refreshes when granted */ }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val next = widgets.map {
                if (it.type == StudioWidgetType.PHOTO_FRAME) it.copy(customPhotoUri = uri.toString(), enabled = true)
                else it
            }
            onUpdateWidgets(next)
        }
    }

    TactileSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("studio_widgets_configurator_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Style, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Studio & Lock Screen Widgets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Enable widgets for Lock Screen & Widget Page, then tap, hold & drag to reorder or place anywhere",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            widgets.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("widget_toggle_row_${item.type.name}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text("${item.type.title} (${item.type.gridSpanLabel})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(item.type.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (item.type == StudioWidgetType.PHOTO_FRAME && item.enabled) {
                        FilledTonalButton(
                            onClick = {
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Photo", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Switch(
                        checked = item.enabled,
                        onCheckedChange = { enable ->
                            haptics.performTick(18L)
                            if (enable && item.type == StudioWidgetType.CALENDAR && !StudioWidgetSystemBridge.hasCalendarPermission(context)) {
                                calendarPermLauncher.launch(Manifest.permission.READ_CALENDAR)
                            }
                            onUpdateWidgets(widgets.map {
                                if (it.type == item.type) it.copy(enabled = enable) else it
                            })
                        },
                        modifier = Modifier.testTag("widget_switch_${item.type.name}")
                    )
                }
            }
        }
    }
}
