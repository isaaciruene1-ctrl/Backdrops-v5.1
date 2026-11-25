package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import androidx.core.content.edit
import com.example.MainActivity
import com.example.R
import com.example.data.catalog.WallpaperCatalog
import com.example.data.media.BackdropImageLoader
import com.example.data.media.WallpaperMediaService
import com.example.data.media.WallpaperTarget
import com.example.data.model.UiStyleMode
import com.example.data.model.Wallpaper
import com.example.data.repository.GuestLocalUserStore
import com.example.data.weather.OpenMeteoWeatherRepository
import com.example.data.weather.WeatherTelemetryState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

enum class BackdropWidgetVariant(
    @LayoutRes val layoutRes: Int,
    val maxPreviewWidth: Int,
    val maxPreviewHeight: Int
) {
    SWITCHER_2X2(
        layoutRes = R.layout.widget_backdrop_switcher,
        maxPreviewWidth = 360,
        maxPreviewHeight = 360
    ),
    STRIP_4X1(
        layoutRes = R.layout.widget_backdrop_strip,
        maxPreviewWidth = 180,
        maxPreviewHeight = 180
    ),
    SHOWCASE_4X2(
        layoutRes = R.layout.widget_backdrop_showcase,
        maxPreviewWidth = 540,
        maxPreviewHeight = 360
    )
}

object BackdropWidgetContract {
    const val ACTION_APPLY_NEXT = "com.example.widget.ACTION_APPLY_NEXT"
    const val ACTION_NEXT_WALLPAPER = "com.example.widget.ACTION_NEXT_WALLPAPER"
    const val ACTION_APPLY_CURRENT = "com.example.widget.ACTION_APPLY_CURRENT"
    const val ACTION_TOGGLE_FAVORITE = "com.example.widget.ACTION_TOGGLE_FAVORITE"
    const val EXTRA_OPEN_STUDIO = "com.example.EXTRA_OPEN_STUDIO"
    const val EXTRA_WALLPAPER_ID = "com.example.EXTRA_WALLPAPER_ID"

    private const val PREFS_WIDGET = "backdrop_widget_state"
    private const val KEY_ACTIVE_WP_ID = "active_widget_wallpaper_id"

    fun getCurrentWallpaperId(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_ACTIVE_WP_ID, null)
        if (saved != null && WallpaperCatalog.getById(saved) != null) {
            return saved
        }
        val activePref = runCatching {
            GuestLocalUserStore.getInstance(context.applicationContext)
                .getCurrentPreferencesSync()
                .activeHomeWallpaperId
        }.getOrNull()
        return activePref?.takeIf { WallpaperCatalog.getById(it) != null }
            ?: WallpaperCatalog.allWallpapers.firstOrNull()?.id.orEmpty()
    }

    fun setCurrentWallpaperId(context: Context, wallpaperId: String) {
        context.applicationContext.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
            .edit(commit = true) { putString(KEY_ACTIVE_WP_ID, wallpaperId) }
    }

    fun advanceNextWallpaperId(context: Context): String {
        val all = WallpaperCatalog.allWallpapers
        if (all.isEmpty()) return ""
        val currentId = getCurrentWallpaperId(context)
        val idx = all.indexOfFirst { it.id == currentId }
        val next = all[(idx + 1) % all.size]
        setCurrentWallpaperId(context, next.id)
        return next.id
    }

    fun refreshAllWidgets(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        (listOf(
            BackdropSwitcherWidgetProvider::class.java,
            BackdropStripWidgetProvider::class.java,
            BackdropShowcaseWidgetProvider::class.java
        ) + BackdropExpandedWidgetContract.ALL_EXPANDED_PROVIDERS).forEach { cls ->
            val component = ComponentName(appContext, cls)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val updateIntent = Intent(appContext, cls).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                appContext.sendBroadcast(updateIntent)
            }
        }
    }
}

abstract class BaseBackdropWidgetProvider(
    private val variant: BackdropWidgetVariant
) : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateSingleWidget(context, appWidgetManager, appWidgetId, variant)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appContext = context.applicationContext
        when (intent.action) {
            BackdropWidgetContract.ACTION_NEXT_WALLPAPER -> {
                runActionAsync {
                    BackdropWidgetContract.advanceNextWallpaperId(appContext)
                    BackdropWidgetContract.refreshAllWidgets(appContext)
                }
            }

            BackdropWidgetContract.ACTION_APPLY_CURRENT -> {
                runActionAsync {
                    val currentId = BackdropWidgetContract.getCurrentWallpaperId(appContext)
                    val wp = WallpaperCatalog.getById(currentId)
                    if (wp != null) {
                        val applied = WallpaperMediaService(appContext).applyWallpaper(wp, WallpaperTarget.BOTH)
                        if (applied.isSuccess) {
                            GuestLocalUserStore.getInstance(appContext).updatePreferences(
                                GuestLocalUserStore.GUEST_USER_ID
                            ) {
                                it.copy(
                                    activeHomeWallpaperId = wp.id,
                                    activeLockWallpaperId = wp.id
                                )
                            }
                        }
                    }
                    BackdropWidgetContract.refreshAllWidgets(appContext)
                }
            }

            BackdropWidgetContract.ACTION_APPLY_NEXT -> {
                runActionAsync {
                    val nextId = BackdropWidgetContract.advanceNextWallpaperId(appContext)
                    val wp = WallpaperCatalog.getById(nextId)
                    if (wp != null) {
                        val applied = WallpaperMediaService(appContext).applyWallpaper(wp, WallpaperTarget.BOTH)
                        if (applied.isSuccess) {
                            GuestLocalUserStore.getInstance(appContext).updatePreferences(
                                GuestLocalUserStore.GUEST_USER_ID
                            ) {
                                it.copy(
                                    activeHomeWallpaperId = wp.id,
                                    activeLockWallpaperId = wp.id
                                )
                            }
                        }
                    }
                    BackdropWidgetContract.refreshAllWidgets(appContext)
                }
            }

            BackdropWidgetContract.ACTION_TOGGLE_FAVORITE -> {
                runActionAsync {
                    val currentId = BackdropWidgetContract.getCurrentWallpaperId(appContext)
                    if (currentId.isNotBlank()) {
                        val store = GuestLocalUserStore.getInstance(appContext)
                        val alreadyFav = store.getFavoritesSync().any { it.wallpaperId == currentId }
                        if (alreadyFav) {
                            store.removeFavorite(GuestLocalUserStore.GUEST_USER_ID, currentId)
                        } else {
                            store.addFavorite(GuestLocalUserStore.GUEST_USER_ID, currentId)
                        }
                    }
                    BackdropWidgetContract.refreshAllWidgets(appContext)
                }
            }
        }
    }

    private fun runActionAsync(block: suspend () -> Unit) {
        val pending = runCatching { goAsync() }.getOrNull()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                block()
            } finally {
                pending?.finish()
            }
        }
    }

    companion object {
        fun buildRemoteViews(
            context: Context,
            appWidgetId: Int,
            variant: BackdropWidgetVariant
        ): RemoteViews {
            val appContext = context.applicationContext
            val views = RemoteViews(appContext.packageName, variant.layoutRes)
            val store = GuestLocalUserStore.getInstance(appContext)
            val prefs = store.getCurrentPreferencesSync()
            val wpId = BackdropWidgetContract.getCurrentWallpaperId(appContext)
            val wp = WallpaperCatalog.getById(wpId) ?: WallpaperCatalog.allWallpapers.firstOrNull()
            val isFavorite = wp != null && store.getFavoritesSync().any { it.wallpaperId == wp.id }

            val containerBg = if (prefs.uiStyle == UiStyleMode.FROSTED_GLASS_OS.name) {
                R.drawable.widget_glass_surface_bg
            } else {
                R.drawable.widget_m3_surface_bg
            }
            views.setInt(R.id.widget_container, "setBackgroundResource", containerBg)

            if (wp != null) {
                views.setTextViewText(R.id.widget_title, wp.title)
                val artworkBmp = resolveWidgetArtworkBitmap(
                    context = appContext,
                    wallpaper = wp,
                    maxWidth = variant.maxPreviewWidth,
                    maxHeight = variant.maxPreviewHeight
                )
                views.setImageViewBitmap(R.id.widget_artwork, artworkBmp)
            }

            views.setTextViewText(
                R.id.widget_apply_label,
                appContext.getString(R.string.widget_apply)
            )
            views.setImageViewResource(
                R.id.widget_fav_icon,
                if (isFavorite) R.drawable.ic_widget_favorite else R.drawable.ic_widget_favorite_outline
            )

            when (variant) {
                BackdropWidgetVariant.SWITCHER_2X2 -> {
                    if (wp != null) {
                        views.setTextViewText(R.id.widget_badge, wp.category.displayName.uppercase())
                    }
                }

                BackdropWidgetVariant.STRIP_4X1 -> {
                    if (wp != null) {
                        views.setTextViewText(
                            R.id.widget_subtitle,
                            "${wp.category.displayName.uppercase()} • ${wp.resolutionText}"
                        )
                    }
                }

                BackdropWidgetVariant.SHOWCASE_4X2 -> {
                    if (wp != null) {
                        views.setTextViewText(
                            R.id.widget_subtitle,
                            "${wp.category.displayName.uppercase()} • ${wp.resolutionText}"
                        )
                    }
                    views.setTextViewText(
                        R.id.widget_fav_label,
                        appContext.getString(
                            if (isFavorite) R.string.widget_favorited else R.string.widget_favorite
                        )
                    )
                    val weatherLabel = when (val w = OpenMeteoWeatherRepository.state.value) {
                        is WeatherTelemetryState.Ready -> w.compactReadout
                        else -> wp?.category?.displayName?.uppercase() ?: "BACKDROPS"
                    }
                    views.setTextViewText(R.id.widget_weather, weatherLabel)
                }
            }

            val providerClass = when (variant) {
                BackdropWidgetVariant.SWITCHER_2X2 -> BackdropSwitcherWidgetProvider::class.java
                BackdropWidgetVariant.STRIP_4X1 -> BackdropStripWidgetProvider::class.java
                BackdropWidgetVariant.SHOWCASE_4X2 -> BackdropShowcaseWidgetProvider::class.java
            }
            val codeBase = (appWidgetId.coerceAtLeast(1) * 100) + (variant.ordinal * 10)

            val nextPending = PendingIntent.getBroadcast(
                appContext,
                codeBase + 1,
                Intent(appContext, providerClass).apply {
                    action = BackdropWidgetContract.ACTION_NEXT_WALLPAPER
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextPending)

            val applyPending = PendingIntent.getBroadcast(
                appContext,
                codeBase + 2,
                Intent(appContext, providerClass).apply {
                    action = BackdropWidgetContract.ACTION_APPLY_CURRENT
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_apply, applyPending)

            val favPending = PendingIntent.getBroadcast(
                appContext,
                codeBase + 3,
                Intent(appContext, providerClass).apply {
                    action = BackdropWidgetContract.ACTION_TOGGLE_FAVORITE
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_favorite, favPending)

            val studioIntent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(BackdropWidgetContract.EXTRA_OPEN_STUDIO, true)
                putExtra(BackdropWidgetContract.EXTRA_WALLPAPER_ID, wp?.id.orEmpty())
            }
            val studioPending = PendingIntent.getActivity(
                appContext,
                codeBase + 4,
                studioIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_studio, studioPending)

            return views
        }

        fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            variant: BackdropWidgetVariant
        ) {
            val views = buildRemoteViews(context, appWidgetId, variant)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        @OptIn(coil.annotation.ExperimentalCoilApi::class)
        internal fun resolveWidgetArtworkBitmap(
            context: Context,
            wallpaper: Wallpaper,
            maxWidth: Int,
            maxHeight: Int
        ): Bitmap {
            if (wallpaper.isBundledAsset && wallpaper.fallbackResId != 0) {
                val decoded = decodeSampledResource(context, wallpaper.fallbackResId, maxWidth, maxHeight)
                if (decoded != null) return scaleWithinBounds(decoded, maxWidth, maxHeight)
            }
            val cached = runCatching {
                val diskCache = BackdropImageLoader.get(context).diskCache
                listOf(
                    "${wallpaper.id}_thumb",
                    "${wallpaper.id}_full",
                    wallpaper.thumbnailUrl,
                    wallpaper.fullResUrl
                ).firstNotNullOfOrNull { key ->
                    if (key.isBlank()) return@firstNotNullOfOrNull null
                    diskCache?.openSnapshot(key)?.use { snap ->
                        decodeSampledFile(snap.data.toFile().absolutePath, maxWidth, maxHeight)
                    }
                }
            }.getOrNull()
            if (cached != null) return scaleWithinBounds(cached, maxWidth, maxHeight)
            return renderPaletteGradientBitmap(
                colors = wallpaper.dominantColors,
                width = maxWidth.coerceAtLeast(64),
                height = maxHeight.coerceAtLeast(64)
            )
        }

        private fun renderPaletteGradientBitmap(colors: List<Long>, width: Int, height: Int): Bitmap {
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            val canvas = android.graphics.Canvas(bmp)
            val top = colors.getOrNull(0)?.toInt() ?: 0xFF1A2233.toInt()
            val mid = colors.getOrNull(1)?.toInt() ?: 0xFF2D3E5E.toInt()
            val bot = colors.getOrNull(2)?.toInt() ?: 0xFF10131A.toInt()
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(top, mid, bot), null, android.graphics.Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            return bmp
        }

        private fun decodeSampledResource(
            context: Context,
            resId: Int,
            reqW: Int,
            reqH: Int
        ): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, resId, bounds)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqW, reqH)
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            return BitmapFactory.decodeResource(context.resources, resId, opts)
        }

        private fun decodeSampledFile(path: String, reqW: Int, reqH: Int): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val opts = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqW, reqH)
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            return BitmapFactory.decodeFile(path, opts)
        }

        private fun calculateInSampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
            var inSampleSize = 1
            if (srcH > reqH || srcW > reqW) {
                val halfH = srcH / 2
                val halfW = srcW / 2
                while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize.coerceAtLeast(1)
        }

        private fun scaleWithinBounds(bitmap: Bitmap, maxW: Int, maxH: Int): Bitmap {
            if (bitmap.width <= maxW && bitmap.height <= maxH) return bitmap
            val scale = minOf(maxW.toFloat() / bitmap.width, maxH.toFloat() / bitmap.height)
            val outW = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val outH = (bitmap.height * scale).toInt().coerceAtLeast(1)
            return Bitmap.createScaledBitmap(bitmap, outW, outH, true)
        }
    }
}

class BackdropSwitcherWidgetProvider : BaseBackdropWidgetProvider(BackdropWidgetVariant.SWITCHER_2X2)

class BackdropStripWidgetProvider : BaseBackdropWidgetProvider(BackdropWidgetVariant.STRIP_4X1)

class BackdropShowcaseWidgetProvider : BaseBackdropWidgetProvider(BackdropWidgetVariant.SHOWCASE_4X2)
