package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.BatteryManager
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.edit
import com.example.MainActivity
import com.example.R
import com.example.data.catalog.WallpaperCatalog
import com.example.data.media.WallpaperMediaService
import com.example.data.media.WallpaperTarget
import com.example.data.model.DiscoveryCollection
import com.example.data.model.StudioWidgetType
import com.example.data.model.UiStyleMode
import com.example.data.model.UserPreferencesDoc
import com.example.data.repository.GuestLocalUserStore
import com.example.data.weather.OpenMeteoWeatherRepository
import com.example.data.weather.WeatherTelemetryState
import com.example.engine.performance.ZeroLagPerformanceEngine
import com.example.ui.components.StudioWidgetSystemBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object BackdropExpandedWidgetContract {
    const val ACTION_WEATHER_REFRESH = "com.example.widget.ACTION_WEATHER_REFRESH"
    const val ACTION_MUSIC_PREV = "com.example.widget.ACTION_MUSIC_PREV"
    const val ACTION_MUSIC_PLAY_PAUSE = "com.example.widget.ACTION_MUSIC_PLAY_PAUSE"
    const val ACTION_MUSIC_NEXT = "com.example.widget.ACTION_MUSIC_NEXT"
    const val ACTION_WORLD_CLOCK_CYCLE = "com.example.widget.ACTION_WORLD_CLOCK_CYCLE"
    const val ACTION_FOCUS_TIMER_TOGGLE = "com.example.widget.ACTION_FOCUS_TIMER_TOGGLE"
    const val ACTION_HABIT_CHECK_IN = "com.example.widget.ACTION_HABIT_CHECK_IN"
    const val ACTION_STOPWATCH_TOGGLE = "com.example.widget.ACTION_STOPWATCH_TOGGLE"
    const val ACTION_COUNTDOWN_TOGGLE = "com.example.widget.ACTION_COUNTDOWN_TOGGLE"
    const val ACTION_COUNTDOWN_ADD_MIN = "com.example.widget.ACTION_COUNTDOWN_ADD_MIN"
    const val ACTION_PINNED_NOTE_CYCLE = "com.example.widget.ACTION_PINNED_NOTE_CYCLE"
    const val ACTION_NETWORK_REFRESH = "com.example.widget.ACTION_NETWORK_REFRESH"
    const val ACTION_STEP_INCREMENT = "com.example.widget.ACTION_STEP_INCREMENT"
    const val ACTION_AUDIO_VOL_DOWN = "com.example.widget.ACTION_AUDIO_VOL_DOWN"
    const val ACTION_AUDIO_VOL_UP = "com.example.widget.ACTION_AUDIO_VOL_UP"
    const val ACTION_QUOTE_NEXT = "com.example.widget.ACTION_QUOTE_NEXT"

    val ALL_EXPANDED_PROVIDERS: List<Class<out AppWidgetProvider>> = listOf(
        BackdropDigitalClockWidgetProvider::class.java,
        BackdropAnalogClockWidgetProvider::class.java,
        BackdropWeatherWidgetProvider::class.java,
        BackdropCalendarWidgetProvider::class.java,
        BackdropBatteryWidgetProvider::class.java,
        BackdropMusicWidgetProvider::class.java,
        BackdropQuickInfoWidgetProvider::class.java,
        BackdropPhotoFrameWidgetProvider::class.java,
        BackdropSystemInfoWidgetProvider::class.java,
        BackdropWorldClockWidgetProvider::class.java,
        BackdropFocusTimerWidgetProvider::class.java,
        BackdropHabitStreakWidgetProvider::class.java,
        BackdropStopwatchWidgetProvider::class.java,
        BackdropCountdownWidgetProvider::class.java,
        BackdropCompassWidgetProvider::class.java,
        BackdropSunriseSunsetWidgetProvider::class.java,
        BackdropDayProgressWidgetProvider::class.java,
        BackdropPinnedNoteWidgetProvider::class.java,
        BackdropNetworkStatusWidgetProvider::class.java,
        BackdropStepCounterWidgetProvider::class.java,
        BackdropAudioVolumeWidgetProvider::class.java,
        BackdropDailyQuoteWidgetProvider::class.java
    )
}

abstract class BaseExpandedHomeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val appContext = context.applicationContext
        appWidgetIds.forEach { widgetId ->
            val views = buildViews(appContext, widgetId)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    protected abstract fun buildViews(context: Context, appWidgetId: Int): RemoteViews

    protected fun applySurfaceTheme(views: RemoteViews, prefs: UserPreferencesDoc) {
        val bgRes = when (prefs.uiStyle) {
            UiStyleMode.FROSTED_GLASS_OS.name -> R.drawable.widget_glass_surface_bg
            UiStyleMode.GLYPH_UI.name -> R.drawable.widget_glyph_surface_bg
            else -> R.drawable.widget_m3_surface_bg
        }
        views.setInt(R.id.widget_container, "setBackgroundResource", bgRes)
    }

    protected fun launchAppPendingIntent(
        context: Context,
        requestCode: Int,
        wallpaperId: String = ""
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (wallpaperId.isNotBlank()) {
                putExtra(BackdropWidgetContract.EXTRA_OPEN_STUDIO, true)
                putExtra(BackdropWidgetContract.EXTRA_WALLPAPER_ID, wallpaperId)
            }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    protected fun broadcastPendingIntent(
        context: Context,
        requestCode: Int,
        providerClass: Class<out AppWidgetProvider>,
        actionName: String
    ): PendingIntent {
        val intent = Intent(context, providerClass).apply {
            action = actionName
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    protected fun runAsyncAction(block: suspend () -> Unit) {
        val pending = runCatching { goAsync() }.getOrNull()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                block()
            } finally {
                pending?.finish()
            }
        }
    }
}

class BackdropDigitalClockWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_digital_clock)
        applySurfaceTheme(views, prefs)
        views.setViewVisibility(R.id.widget_digital_clock_badge, View.GONE)
        views.setOnClickPendingIntent(
            R.id.widget_container,
            launchAppPendingIntent(context, 4100 + appWidgetId, prefs.activeLockWallpaperId)
        )
        return views
    }
}

class BackdropAnalogClockWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_analog_clock)
        applySurfaceTheme(views, prefs)
        val dialAccent = if (prefs.uiStyle == UiStyleMode.GLYPH_UI.name) 0xFFD92B2BL else prefs.customAccentHex
        views.setImageViewBitmap(R.id.widget_analog_dial, renderAnalogDialBitmap(dialAccent))
        val dateText = SimpleDateFormat("EEE d", Locale.getDefault()).format(Date()).uppercase()
        views.setTextViewText(R.id.widget_analog_date, dateText)
        views.setOnClickPendingIntent(
            R.id.widget_container,
            launchAppPendingIntent(context, 4200 + appWidgetId, prefs.activeLockWallpaperId)
        )
        return views
    }

    private fun renderAnalogDialBitmap(accentHex: Long): Bitmap {
        val size = 220
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = size / 2f
        val cy = size / 2f
        val r = size * 0.43f

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawCircle(cx, cy, r, ringPaint)

        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 255, 255, 255)
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
        }
        for (i in 0 until 12) {
            val angle = (i * 30.0 - 90.0) * PI / 180.0
            val inner = if (i % 3 == 0) r * 0.78f else r * 0.86f
            canvas.drawLine(
                cx + (cos(angle) * inner).toFloat(),
                cy + (sin(angle) * inner).toFloat(),
                cx + (cos(angle) * (r * 0.94f)).toFloat(),
                cy + (sin(angle) * (r * 0.94f)).toFloat(),
                tickPaint
            )
        }

        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR)
        val minute = cal.get(Calendar.MINUTE)
        val hourAngle = ((hour + minute / 60f) * 30f - 90f) * PI / 180.0
        val minAngle = (minute * 6f - 90f) * PI / 180.0

        val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = 7f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(
            cx, cy,
            cx + (cos(hourAngle) * r * 0.52f).toFloat(),
            cy + (sin(hourAngle) * r * 0.52f).toFloat(),
            hourPaint
        )

        val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(
                ((accentHex shr 16) and 0xFF).toInt(),
                ((accentHex shr 8) and 0xFF).toInt(),
                (accentHex and 0xFF).toInt()
            )
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(
            cx, cy,
            cx + (cos(minAngle) * r * 0.76f).toFloat(),
            cy + (sin(minAngle) * r * 0.76f).toFloat(),
            minPaint
        )

        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawCircle(cx, cy, 7f, centerPaint)
        return bmp
    }
}

class BackdropWeatherWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == BackdropExpandedWidgetContract.ACTION_WEATHER_REFRESH) {
            val appContext = context.applicationContext
            runAsyncAction {
                val prefs = GuestLocalUserStore.getInstance(appContext).getCurrentPreferencesSync()
                if (prefs.weatherUseLocation) {
                    OpenMeteoWeatherRepository.refreshByDeviceCoarseLocation(appContext)
                } else if (prefs.weatherCity.isNotBlank()) {
                    OpenMeteoWeatherRepository.refreshByCity(prefs.weatherCity)
                }
                BackdropWidgetContract.refreshAllWidgets(appContext)
            }
        }
    }

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_weather)
        applySurfaceTheme(views, prefs)

        when (val w = OpenMeteoWeatherRepository.state.value) {
            is WeatherTelemetryState.Ready -> {
                views.setTextViewText(R.id.widget_weather_city, w.cityLabel.ifBlank { "LOCAL" }.uppercase())
                views.setTextViewText(R.id.widget_weather_temp, "${w.temperatureC}°")
                views.setTextViewText(R.id.widget_weather_condition, "${w.conditionLabel} • Tap to refresh")
            }
            is WeatherTelemetryState.Loading -> {
                views.setTextViewText(R.id.widget_weather_city, w.sourceLabel.uppercase())
                views.setTextViewText(R.id.widget_weather_temp, "--°")
                views.setTextViewText(R.id.widget_weather_condition, "Updating forecast...")
            }
            else -> {
                val city = prefs.weatherCity.ifBlank { "SET CITY IN SETTINGS" }.uppercase()
                views.setTextViewText(R.id.widget_weather_city, city)
                views.setTextViewText(R.id.widget_weather_temp, "--°")
                views.setTextViewText(R.id.widget_weather_condition, "Tap to refresh")
            }
        }

        views.setOnClickPendingIntent(
            R.id.widget_container,
            broadcastPendingIntent(
                context = context,
                requestCode = 4300 + appWidgetId,
                providerClass = BackdropWeatherWidgetProvider::class.java,
                actionName = BackdropExpandedWidgetContract.ACTION_WEATHER_REFRESH
            )
        )
        return views
    }
}

class BackdropCalendarWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_calendar)
        applySurfaceTheme(views, prefs)

        val now = Date()
        views.setTextViewText(
            R.id.widget_calendar_month,
            SimpleDateFormat("MMMM d", Locale.getDefault()).format(now).uppercase()
        )
        views.setTextViewText(
            R.id.widget_calendar_day,
            SimpleDateFormat("EEE", Locale.getDefault()).format(now).uppercase()
        )

        if (!StudioWidgetSystemBridge.hasCalendarPermission(context)) {
            views.setTextViewText(R.id.widget_calendar_event_1, "Calendar access paused")
            views.setTextViewText(R.id.widget_calendar_event_2, "Open Backdrops Lock Screen Mode to grant access")
        } else {
            val events = StudioWidgetSystemBridge.loadUpcomingCalendarEvents(context, limit = 2)
            if (events.isEmpty()) {
                views.setTextViewText(R.id.widget_calendar_event_1, "Clear schedule ahead")
                views.setTextViewText(R.id.widget_calendar_event_2, "No upcoming events in the next 7 days")
            } else {
                val first = events[0]
                views.setTextViewText(R.id.widget_calendar_event_1, "${first.title} • ${first.timeLabel}")
                val second = events.getOrNull(1)
                views.setTextViewText(
                    R.id.widget_calendar_event_2,
                    second?.let { "${it.title} • ${it.timeLabel}" } ?: "Next 7 days synced"
                )
            }
        }

        views.setOnClickPendingIntent(
            R.id.widget_container,
            launchAppPendingIntent(context, 4400 + appWidgetId)
        )
        return views
    }
}

class BackdropBatteryWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_battery)
        applySurfaceTheme(views, prefs)

        val pct = OpenMeteoWeatherRepository.readBatteryPercent(context)
        val batteryIntent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val mv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 3900) ?: 3900
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280

        views.setTextViewText(R.id.widget_battery_percent, "$pct%")
        views.setTextViewText(
            R.id.widget_battery_status,
            if (isCharging) "Charging • ${tempTenths / 10f}°C" else "Active • ${tempTenths / 10f}°C"
        )
        views.setTextViewText(
            R.id.widget_battery_chip,
            String.format(Locale.US, "%.1fV", mv / 1000f)
        )
        views.setOnClickPendingIntent(
            R.id.widget_container,
            launchAppPendingIntent(context, 4500 + appWidgetId)
        )
        return views
    }
}

class BackdropMusicWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action == BackdropExpandedWidgetContract.ACTION_MUSIC_PREV ||
            action == BackdropExpandedWidgetContract.ACTION_MUSIC_PLAY_PAUSE ||
            action == BackdropExpandedWidgetContract.ACTION_MUSIC_NEXT
        ) {
            val appContext = context.applicationContext
            runAsyncAction {
                val media = StudioWidgetSystemBridge.readActiveMediaState(appContext)
                val controls = media.controller?.transportControls
                when (action) {
                    BackdropExpandedWidgetContract.ACTION_MUSIC_PREV -> controls?.skipToPrevious()
                    BackdropExpandedWidgetContract.ACTION_MUSIC_NEXT -> controls?.skipToNext()
                    BackdropExpandedWidgetContract.ACTION_MUSIC_PLAY_PAUSE -> {
                        if (media.isPlaying) controls?.pause() else controls?.play()
                    }
                }
                BackdropWidgetContract.refreshAllWidgets(appContext)
            }
        }
    }

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_music)
        applySurfaceTheme(views, prefs)

        val media = StudioWidgetSystemBridge.readActiveMediaState(context)
        views.setTextViewText(R.id.widget_music_title, media.title)
        views.setTextViewText(R.id.widget_music_artist, media.artist)
        views.setImageViewResource(
            R.id.widget_music_play_pause,
            if (media.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )

        views.setOnClickPendingIntent(
            R.id.widget_music_header,
            launchAppPendingIntent(context, 4600 + appWidgetId)
        )
        views.setOnClickPendingIntent(
            R.id.widget_music_prev,
            broadcastPendingIntent(
                context,
                4610 + appWidgetId,
                BackdropMusicWidgetProvider::class.java,
                BackdropExpandedWidgetContract.ACTION_MUSIC_PREV
            )
        )
        views.setOnClickPendingIntent(
            R.id.widget_music_play_pause,
            broadcastPendingIntent(
                context,
                4620 + appWidgetId,
                BackdropMusicWidgetProvider::class.java,
                BackdropExpandedWidgetContract.ACTION_MUSIC_PLAY_PAUSE
            )
        )
        views.setOnClickPendingIntent(
            R.id.widget_music_next,
            broadcastPendingIntent(
                context,
                4630 + appWidgetId,
                BackdropMusicWidgetProvider::class.java,
                BackdropExpandedWidgetContract.ACTION_MUSIC_NEXT
            )
        )
        return views
    }
}

class BackdropQuickInfoWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_quick_info)
        applySurfaceTheme(views, prefs)

        val dateLabel = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
        val battery = OpenMeteoWeatherRepository.readBatteryPercent(context)
        val weatherPart = when (val w = OpenMeteoWeatherRepository.state.value) {
            is WeatherTelemetryState.Ready -> "${w.temperatureC}° ${w.conditionLabel}"
            else -> prefs.weatherCity.ifBlank { "Backdrops" }
        }
        val nextAgenda = StudioWidgetSystemBridge.loadUpcomingCalendarEvents(context, limit = 1)
            .firstOrNull()?.title

        views.setTextViewText(R.id.widget_quick_date, dateLabel)
        views.setTextViewText(
            R.id.widget_quick_summary,
            listOfNotNull(weatherPart, "$battery% Battery", nextAgenda).joinToString(" • ")
        )
        views.setViewVisibility(R.id.widget_quick_badge, View.GONE)
        views.setOnClickPendingIntent(
            R.id.widget_container,
            launchAppPendingIntent(context, 4700 + appWidgetId)
        )
        return views
    }
}

class BackdropPhotoFrameWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_photo_frame)
        applySurfaceTheme(views, prefs)

        val frameUri = prefs.parsedStudioWidgets
            .firstOrNull { it.type == StudioWidgetType.PHOTO_FRAME }
            ?.customPhotoUri
            .orEmpty()

        val wp = WallpaperCatalog.getById(prefs.activeHomeWallpaperId)
            ?: WallpaperCatalog.getById(prefs.activeLockWallpaperId)
            ?: WallpaperCatalog.allWallpapers.firstOrNull()

        val customBmp = if (frameUri.isNotBlank()) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(frameUri))?.use { stream ->
                    android.graphics.BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
        } else null

        if (customBmp != null) {
            views.setImageViewBitmap(R.id.widget_photo_frame_img, customBmp)
            views.setTextViewText(R.id.widget_photo_frame_caption, "Studio Photo Frame")
        } else if (wp != null) {
            val bmp = BaseBackdropWidgetProvider.resolveWidgetArtworkBitmap(context, wp, 360, 360)
            views.setImageViewBitmap(R.id.widget_photo_frame_img, bmp)
            views.setTextViewText(R.id.widget_photo_frame_caption, wp.title)
        }

        views.setOnClickPendingIntent(
            R.id.widget_container,
            launchAppPendingIntent(context, 4800 + appWidgetId, wp?.id.orEmpty())
        )
        return views
    }
}

class BackdropSystemInfoWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_system_info)
        applySurfaceTheme(views, prefs)

        val tier = ZeroLagPerformanceEngine.resolveEffectiveLiquidGlassTier(context, prefs.parsedLiquidGlassTier)
        val diag = StudioWidgetSystemBridge.readDiagnostics(context, tier.label)
        views.setTextViewText(R.id.widget_sys_tier, diag.tierLabel.uppercase())
        views.setTextViewText(R.id.widget_sys_ram, "RAM ${diag.ramUsedPercent}% (${diag.ramSummary})")
        views.setTextViewText(R.id.widget_sys_storage, "Storage ${diag.storageFreeGb} GB free")
        views.setOnClickPendingIntent(
            R.id.widget_container,
            launchAppPendingIntent(context, 4900 + appWidgetId)
        )
        return views
    }
}

class BackdropWorldClockWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == BackdropExpandedWidgetContract.ACTION_WORLD_CLOCK_CYCLE) {
            val sp = context.applicationContext.getSharedPreferences(PREFS_EXTRA_WIDGETS, Context.MODE_PRIVATE)
            val next = (sp.getInt(KEY_WORLD_CITY_IDX, 0) + 1) % CITIES.size
            sp.edit(commit = true) { putInt(KEY_WORLD_CITY_IDX, next) }
            BackdropWidgetContract.refreshAllWidgets(context.applicationContext)
        }
    }

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_world_clock)
        applySurfaceTheme(views, prefs)

        val sp = context.applicationContext.getSharedPreferences(PREFS_EXTRA_WIDGETS, Context.MODE_PRIVATE)
        val cityIdx = sp.getInt(KEY_WORLD_CITY_IDX, 0).coerceIn(0, CITIES.lastIndex)
        val companion = CITIES[cityIdx]
        val now = Date()
        val localFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val remoteFmt = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(companion.second)
        }
        val localName = TimeZone.getDefault().id.substringAfterLast('/').replace('_', ' ').uppercase()

        views.setTextViewText(R.id.widget_world_local_city, localName.ifBlank { "LOCAL" })
        views.setTextViewText(R.id.widget_world_local_time, localFmt.format(now))
        views.setTextViewText(R.id.widget_world_remote_city, companion.first.uppercase())
        views.setTextViewText(R.id.widget_world_remote_time, remoteFmt.format(now))
        views.setOnClickPendingIntent(
            R.id.widget_container,
            broadcastPendingIntent(
                context,
                5000 + appWidgetId,
                BackdropWorldClockWidgetProvider::class.java,
                BackdropExpandedWidgetContract.ACTION_WORLD_CLOCK_CYCLE
            )
        )
        return views
    }

    companion object {
        private const val PREFS_EXTRA_WIDGETS = "backdrop_expanded_widget_prefs"
        private const val KEY_WORLD_CITY_IDX = "world_city_idx"
        private val CITIES = listOf(
            "Tokyo" to "Asia/Tokyo",
            "New York" to "America/New_York",
            "London" to "Europe/London",
            "Paris" to "Europe/Paris",
            "Seoul" to "Asia/Seoul"
        )
    }
}

class BackdropFocusTimerWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == BackdropExpandedWidgetContract.ACTION_FOCUS_TIMER_TOGGLE) {
            val sp = context.applicationContext.getSharedPreferences(PREFS_EXTRA_WIDGETS, Context.MODE_PRIVATE)
            val active = !sp.getBoolean(KEY_FOCUS_RUNNING, false)
            sp.edit(commit = true) {
                putBoolean(KEY_FOCUS_RUNNING, active)
                if (active) putLong(KEY_FOCUS_END_MS, System.currentTimeMillis() + 25L * 60L * 1000L)
            }
            BackdropWidgetContract.refreshAllWidgets(context.applicationContext)
        }
    }

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_focus_timer)
        applySurfaceTheme(views, prefs)

        val sp = context.applicationContext.getSharedPreferences(PREFS_EXTRA_WIDGETS, Context.MODE_PRIVATE)
        val running = sp.getBoolean(KEY_FOCUS_RUNNING, false)
        val endMs = sp.getLong(KEY_FOCUS_END_MS, 0L)
        val remainingSec = if (running && endMs > System.currentTimeMillis()) {
            ((endMs - System.currentTimeMillis()) / 1000L).toInt()
        } else 25 * 60

        views.setTextViewText(R.id.widget_focus_label, if (running) "FOCUSING" else "FOCUS TIMER")
        views.setTextViewText(R.id.widget_focus_time, String.format(Locale.US, "%02d:%02d", remainingSec / 60, remainingSec % 60))
        views.setTextViewText(R.id.widget_focus_action, if (running) "PAUSE" else "START")
        views.setOnClickPendingIntent(
            R.id.widget_container,
            broadcastPendingIntent(
                context,
                5100 + appWidgetId,
                BackdropFocusTimerWidgetProvider::class.java,
                BackdropExpandedWidgetContract.ACTION_FOCUS_TIMER_TOGGLE
            )
        )
        return views
    }

    companion object {
        private const val PREFS_EXTRA_WIDGETS = "backdrop_expanded_widget_prefs"
        private const val KEY_FOCUS_RUNNING = "focus_running"
        private const val KEY_FOCUS_END_MS = "focus_end_ms"
    }
}

class BackdropHabitStreakWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == BackdropExpandedWidgetContract.ACTION_HABIT_CHECK_IN) {
            val sp = context.applicationContext.getSharedPreferences(PREFS_EXTRA_WIDGETS, Context.MODE_PRIVATE)
            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val lastDay = sp.getString(KEY_HABIT_LAST_DAY, "")
            if (lastDay != todayKey) {
                val nextStreak = sp.getInt(KEY_HABIT_STREAK, 7) + 1
                sp.edit(commit = true) {
                    putInt(KEY_HABIT_STREAK, nextStreak)
                    putString(KEY_HABIT_LAST_DAY, todayKey)
                }
            }
            BackdropWidgetContract.refreshAllWidgets(context.applicationContext)
        }
    }

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_habit_streak)
        applySurfaceTheme(views, prefs)

        val sp = context.applicationContext.getSharedPreferences(PREFS_EXTRA_WIDGETS, Context.MODE_PRIVATE)
        val streak = sp.getInt(KEY_HABIT_STREAK, 7)
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val doneToday = sp.getString(KEY_HABIT_LAST_DAY, "") == todayKey

        views.setTextViewText(R.id.widget_habit_count, "$streak Days")
        views.setTextViewText(
            R.id.widget_habit_status,
            if (doneToday) "Checked in today" else "Tap to check in today"
        )
        views.setOnClickPendingIntent(
            R.id.widget_container,
            broadcastPendingIntent(
                context,
                5200 + appWidgetId,
                BackdropHabitStreakWidgetProvider::class.java,
                BackdropExpandedWidgetContract.ACTION_HABIT_CHECK_IN
            )
        )
        return views
    }

    companion object {
        private const val PREFS_EXTRA_WIDGETS = "backdrop_expanded_widget_prefs"
        private const val KEY_HABIT_STREAK = "habit_streak"
        private const val KEY_HABIT_LAST_DAY = "habit_last_day"
    }
}

class BackdropStopwatchWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == BackdropExpandedWidgetContract.ACTION_STOPWATCH_TOGGLE) {
            val appContext = context.applicationContext
            val sp = appContext.getSharedPreferences(PREFS_EXTRA_WIDGETS, Context.MODE_PRIVATE)
            val running = sp.getBoolean(KEY_SW_RUNNING, false)
            val now = System.currentTimeMillis()
            if (running) {
                val start = sp.getLong(KEY_SW_START_MS, now)
                val acc = sp.getLong(KEY_SW_ACCUM_MS, 0L) + (now - start).coerceAtLeast(0L)
                sp.edit(commit = true) {
                    putBoolean(KEY_SW_RUNNING, false)
                    putLong(KEY_SW_ACCUM_MS, acc)
                }
            } else {
                sp.edit(commit = true) {
                    putBoolean(KEY_SW_RUNNING, true)
                    putLong(KEY_SW_START_MS, now)
                }
            }
            BackdropWidgetContract.refreshAllWidgets(appContext)
        }
    }

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_stopwatch)
        applySurfaceTheme(views, prefs)

        val sp = context.applicationContext.getSharedPreferences(PREFS_EXTRA_WIDGETS, Context.MODE_PRIVATE)
        val running = sp.getBoolean(KEY_SW_RUNNING, false)
        val accum = sp.getLong(KEY_SW_ACCUM_MS, 0L)
        val start = sp.getLong(KEY_SW_START_MS, System.currentTimeMillis())
        val totalMs = accum + if (running) (System.currentTimeMillis() - start).coerceAtLeast(0L) else 0L
        val sec = (totalMs / 1000L).toInt()

        views.setTextViewText(R.id.widget_stopwatch_label, if (running) "RUNNING" else "STOPWATCH")
        views.setTextViewText(R.id.widget_stopwatch_time, String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60))
        views.setTextViewText(R.id.widget_stopwatch_action, if (running) "PAUSE" else "START")
        views.setOnClickPendingIntent(
            R.id.widget_container,
            broadcastPendingIntent(
                context,
                5300 + appWidgetId,
                BackdropStopwatchWidgetProvider::class.java,
                BackdropExpandedWidgetContract.ACTION_STOPWATCH_TOGGLE
            )
        )
        return views
    }

    companion object {
        private const val PREFS_EXTRA_WIDGETS = "backdrop_expanded_widget_prefs"
        private const val KEY_SW_RUNNING = "sw_running"
        private const val KEY_SW_START_MS = "sw_start_ms"
        private const val KEY_SW_ACCUM_MS = "sw_accum_ms"
    }
}

class BackdropCountdownWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appContext = context.applicationContext
        val sp = appContext.getSharedPreferences(PREFS_EXTRA_WIDGETS, Context.MODE_PRIVATE)
        when (intent.action) {
            BackdropExpandedWidgetContract.ACTION_COUNTDOWN_ADD_MIN -> {
                val cur = sp.getInt(KEY_CD_PRESET_SEC, 5 * 60)
                val next = if (cur + 60 > 3600) 60 else cur + 60
                sp.edit(commit = true) {
                    putInt(KEY_CD_PRESET_SEC, next)
                    putBoolean(KEY_CD_RUNNING, false)
                }
                BackdropWidgetContract.refreshAllWidgets(appContext)
            }
            BackdropExpandedWidgetContract.ACTION_COUNTDOWN_TOGGLE -> {
                val nextRunning = !sp.getBoolean(KEY_CD_RUNNING, false)
                val preset = sp.getInt(KEY_CD_PRESET_SEC, 5 * 60)
                sp.edit(commit = true) {
                    putBoolean(KEY_CD_RUNNING, nextRunning)
                    if (nextRunning) putLong(KEY_CD_END_MS, System.currentTimeMillis() + preset * 1000L)
                }
                BackdropWidgetContract.refreshAllWidgets(appContext)
            }
        }
    }

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_countdown)
        applySurfaceTheme(views, prefs)

        val sp = context.applicationContext.getSharedPreferences(PREFS_EXTRA_WIDGETS, Context.MODE_PRIVATE)
        val running = sp.getBoolean(KEY_CD_RUNNING, false)
        val preset = sp.getInt(KEY_CD_PRESET_SEC, 5 * 60)
        val endMs = sp.getLong(KEY_CD_END_MS, 0L)
        val remSec = if (running && endMs > System.currentTimeMillis()) {
            ((endMs - System.currentTimeMillis()) / 1000L).toInt()
        } else preset

        views.setTextViewText(R.id.widget_countdown_label, if (running) "COUNTING" else "COUNTDOWN")
        views.setTextViewText(R.id.widget_countdown_time, String.format(Locale.US, "%02d:%02d", remSec / 60, remSec % 60))
        views.setTextViewText(R.id.widget_countdown_action, if (running) "STOP" else "GO")
        views.setOnClickPendingIntent(
            R.id.widget_countdown_add,
            broadcastPendingIntent(
                context,
                5400 + appWidgetId,
                BackdropCountdownWidgetProvider::class.java,
                BackdropExpandedWidgetContract.ACTION_COUNTDOWN_ADD_MIN
            )
        )
        views.setOnClickPendingIntent(
            R.id.widget_container,
            broadcastPendingIntent(
                context,
                5410 + appWidgetId,
                BackdropCountdownWidgetProvider::class.java,
                BackdropExpandedWidgetContract.ACTION_COUNTDOWN_TOGGLE
            )
        )
        return views
    }

    companion object {
        private const val PREFS_EXTRA_WIDGETS = "backdrop_expanded_widget_prefs"
        private const val KEY_CD_RUNNING = "cd_running"
        private const val KEY_CD_PRESET_SEC = "cd_preset_sec"
        private const val KEY_CD_END_MS = "cd_end_ms"
    }
}

class BackdropCompassWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_compass)
        applySurfaceTheme(views, prefs)
        val dialAccent = if (prefs.uiStyle == UiStyleMode.GLYPH_UI.name) 0xFFD92B2BL else prefs.customAccentHex
        views.setImageViewBitmap(R.id.widget_compass_dial, renderCompassBitmap(dialAccent))
        views.setTextViewText(R.id.widget_compass_bearing, "0° N • Studio Compass")
        views.setOnClickPendingIntent(
            R.id.widget_container,
            launchAppPendingIntent(context, 5500 + appWidgetId)
        )
        return views
    }

    private fun renderCompassBitmap(accentHex: Long): Bitmap {
        val size = 180
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = size / 2f
        val cy = size / 2f
        val r = size * 0.42f
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawCircle(cx, cy, r, ringPaint)
        val northPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 107, 107)
            strokeWidth = 7f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(cx, cy, cx, cy - r * 0.78f, northPaint)
        val southPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 255, 255, 255)
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(cx, cy, cx, cy + r * 0.56f, southPaint)
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(
                ((accentHex shr 16) and 0xFF).toInt(),
                ((accentHex shr 8) and 0xFF).toInt(),
                (accentHex and 0xFF).toInt()
            )
        }
        canvas.drawCircle(cx, cy, 7f, dotPaint)
        return bmp
    }
}

class BackdropSunriseSunsetWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_sunrise_sunset)
        applySurfaceTheme(views, prefs)
        val solar = StudioWidgetSystemBridge.readSolarSummary()
        views.setTextViewText(R.id.widget_solar_times, "↑ ${solar.sunriseLabel}   •   ↓ ${solar.sunsetLabel}")
        views.setTextViewText(R.id.widget_solar_phase, solar.phaseLabel.uppercase())
        views.setOnClickPendingIntent(
            R.id.widget_container,
            launchAppPendingIntent(context, 5600 + appWidgetId)
        )
        return views
    }
}

class BackdropDayProgressWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_day_progress)
        applySurfaceTheme(views, prefs)
        val prog = StudioWidgetSystemBridge.readDayProgress()
        views.setTextViewText(R.id.widget_day_progress_label, "DAY PROGRESS • WEEK ${prog.weekPercent}%")
        views.setTextViewText(R.id.widget_day_progress_pct, "${prog.dayPercent}% • ${prog.hoursRemaining}h left")
        views.setProgressBar(R.id.widget_day_progress_bar, 100, prog.dayPercent, false)
        views.setOnClickPendingIntent(
            R.id.widget_container,
            launchAppPendingIntent(context, 5700 + appWidgetId)
        )
        return views
    }
}

class BackdropPinnedNoteWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == BackdropExpandedWidgetContract.ACTION_PINNED_NOTE_CYCLE) {
            val appContext = context.applicationContext
            val sp = appContext.getSharedPreferences(PREFS_EXTRA_WIDGETS, Context.MODE_PRIVATE)
            val nextIdx = (sp.getInt(KEY_NOTE_IDX, 0) + 1) % StudioWidgetSystemBridge.pinnedNotePresets.size
            sp.edit(commit = true) { putInt(KEY_NOTE_IDX, nextIdx) }
            BackdropWidgetContract.refreshAllWidgets(appContext)
        }
    }

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_pinned_note)
        applySurfaceTheme(views, prefs)
        val customNote = prefs.parsedStudioWidgets
            .firstOrNull { it.type == StudioWidgetType.PINNED_NOTE }
            ?.widgetMeta.orEmpty()
        val sp = context.applicationContext.getSharedPreferences(PREFS_EXTRA_WIDGETS, Context.MODE_PRIVATE)
        val presets = StudioWidgetSystemBridge.pinnedNotePresets
        val display = customNote.ifBlank { presets[sp.getInt(KEY_NOTE_IDX, 0).coerceIn(0, presets.lastIndex)] }
        views.setTextViewText(R.id.widget_pinned_note_body, display)
        views.setOnClickPendingIntent(
            R.id.widget_pinned_note_cycle,
            broadcastPendingIntent(
                context,
                5800 + appWidgetId,
                BackdropPinnedNoteWidgetProvider::class.java,
                BackdropExpandedWidgetContract.ACTION_PINNED_NOTE_CYCLE
            )
        )
        views.setOnClickPendingIntent(
            R.id.widget_container,
            launchAppPendingIntent(context, 5810 + appWidgetId)
        )
        return views
    }

    companion object {
        private const val PREFS_EXTRA_WIDGETS = "backdrop_expanded_widget_prefs"
        private const val KEY_NOTE_IDX = "pinned_note_idx"
    }
}

class BackdropNetworkStatusWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == BackdropExpandedWidgetContract.ACTION_NETWORK_REFRESH) {
            BackdropWidgetContract.refreshAllWidgets(context.applicationContext)
        }
    }

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_network_status)
        applySurfaceTheme(views, prefs)
        val net = StudioWidgetSystemBridge.readNetworkStatus(context)
        views.setTextViewText(R.id.widget_network_type, net.transportLabel.uppercase())
        views.setTextViewText(R.id.widget_network_detail, net.statusDetail)
        views.setOnClickPendingIntent(
            R.id.widget_container,
            broadcastPendingIntent(
                context,
                5900 + appWidgetId,
                BackdropNetworkStatusWidgetProvider::class.java,
                BackdropExpandedWidgetContract.ACTION_NETWORK_REFRESH
            )
        )
        return views
    }
}

class BackdropStepCounterWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == BackdropExpandedWidgetContract.ACTION_STEP_INCREMENT) {
            val appContext = context.applicationContext
            val sp = appContext.getSharedPreferences(PREFS_EXTRA_WIDGETS, Context.MODE_PRIVATE)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val prevDay = sp.getString(KEY_STEP_DAY, "")
            val cur = if (prevDay == today) sp.getInt(KEY_STEP_COUNT, 1420) else 0
            sp.edit(commit = true) {
                putString(KEY_STEP_DAY, today)
                putInt(KEY_STEP_COUNT, cur + 250)
            }
            BackdropWidgetContract.refreshAllWidgets(appContext)
        }
    }

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_step_counter)
        applySurfaceTheme(views, prefs)
        val syncedSteps = prefs.parsedStudioWidgets
            .firstOrNull { it.type == StudioWidgetType.STEP_COUNTER }
            ?.widgetMeta?.toIntOrNull()
        val sp = context.applicationContext.getSharedPreferences(PREFS_EXTRA_WIDGETS, Context.MODE_PRIVATE)
        val steps = syncedSteps ?: sp.getInt(KEY_STEP_COUNT, 1420)
        views.setTextViewText(R.id.widget_steps_count, String.format(Locale.US, "%,d", steps))
        views.setTextViewText(R.id.widget_steps_goal, "Goal: 8,000 today")
        views.setOnClickPendingIntent(
            R.id.widget_container,
            broadcastPendingIntent(
                context,
                6000 + appWidgetId,
                BackdropStepCounterWidgetProvider::class.java,
                BackdropExpandedWidgetContract.ACTION_STEP_INCREMENT
            )
        )
        return views
    }

    companion object {
        private const val PREFS_EXTRA_WIDGETS = "backdrop_expanded_widget_prefs"
        private const val KEY_STEP_DAY = "step_day"
        private const val KEY_STEP_COUNT = "step_count"
    }
}

class BackdropAudioVolumeWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appContext = context.applicationContext
        when (intent.action) {
            BackdropExpandedWidgetContract.ACTION_AUDIO_VOL_DOWN -> {
                StudioWidgetSystemBridge.adjustMediaVolume(appContext, -1)
                BackdropWidgetContract.refreshAllWidgets(appContext)
            }
            BackdropExpandedWidgetContract.ACTION_AUDIO_VOL_UP -> {
                StudioWidgetSystemBridge.adjustMediaVolume(appContext, 1)
                BackdropWidgetContract.refreshAllWidgets(appContext)
            }
        }
    }

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_audio_volume)
        applySurfaceTheme(views, prefs)
        val audio = StudioWidgetSystemBridge.readAudioSummary(context)
        views.setTextViewText(R.id.widget_audio_level, "${audio.mediaPercent}% • ${audio.ringerLabel}")
        views.setOnClickPendingIntent(
            R.id.widget_audio_down,
            broadcastPendingIntent(
                context,
                6100 + appWidgetId,
                BackdropAudioVolumeWidgetProvider::class.java,
                BackdropExpandedWidgetContract.ACTION_AUDIO_VOL_DOWN
            )
        )
        views.setOnClickPendingIntent(
            R.id.widget_audio_up,
            broadcastPendingIntent(
                context,
                6110 + appWidgetId,
                BackdropAudioVolumeWidgetProvider::class.java,
                BackdropExpandedWidgetContract.ACTION_AUDIO_VOL_UP
            )
        )
        views.setOnClickPendingIntent(
            R.id.widget_container,
            launchAppPendingIntent(context, 6120 + appWidgetId)
        )
        return views
    }
}

class BackdropDailyQuoteWidgetProvider : BaseExpandedHomeWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == BackdropExpandedWidgetContract.ACTION_QUOTE_NEXT) {
            val appContext = context.applicationContext
            val sp = appContext.getSharedPreferences(PREFS_EXTRA_WIDGETS, Context.MODE_PRIVATE)
            val next = (sp.getInt(KEY_QUOTE_OFFSET, 0) + 1) % StudioWidgetSystemBridge.dailyQuotes.size
            sp.edit(commit = true) { putInt(KEY_QUOTE_OFFSET, next) }
            BackdropWidgetContract.refreshAllWidgets(appContext)
        }
    }

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val prefs = GuestLocalUserStore.getInstance(context).getCurrentPreferencesSync()
        val views = RemoteViews(context.packageName, R.layout.widget_expanded_daily_quote)
        applySurfaceTheme(views, prefs)
        val sp = context.applicationContext.getSharedPreferences(PREFS_EXTRA_WIDGETS, Context.MODE_PRIVATE)
        val offset = sp.getInt(KEY_QUOTE_OFFSET, 0)
        val dayIdx = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val quotes = StudioWidgetSystemBridge.dailyQuotes
        val quote = quotes[(dayIdx + offset) % quotes.size]
        views.setTextViewText(R.id.widget_quote_text, "\"${quote.text}\"")
        views.setTextViewText(R.id.widget_quote_author, "— ${quote.author}")
        views.setOnClickPendingIntent(
            R.id.widget_container,
            broadcastPendingIntent(
                context,
                6200 + appWidgetId,
                BackdropDailyQuoteWidgetProvider::class.java,
                BackdropExpandedWidgetContract.ACTION_QUOTE_NEXT
            )
        )
        return views
    }

    companion object {
        private const val PREFS_EXTRA_WIDGETS = "backdrop_expanded_widget_prefs"
        private const val KEY_QUOTE_OFFSET = "daily_quote_offset"
    }
}
