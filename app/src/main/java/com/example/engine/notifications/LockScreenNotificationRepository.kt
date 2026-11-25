package com.example.engine.notifications

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.LruCache
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Central state repository connecting Android's official [NotificationListenerService]
 * to Backdrop's Lock Screen Activity and in-app Lock Screen previews.
 */
object LockScreenNotificationRepository {

    private val _groups = MutableStateFlow<List<LockNotificationGroup>>(emptyList())
    val groups: StateFlow<List<LockNotificationGroup>> = _groups.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _listenerConnected = MutableStateFlow(false)
    val listenerConnected: StateFlow<Boolean> = _listenerConnected.asStateFlow()

    private var boundServiceRef: WeakReference<NotificationListenerService>? = null
    private val appIconCache = object : LruCache<String, Bitmap>(32) {}

    init {
        com.example.engine.crash.ZeroCrashEngine.registerMemoryCleanupHook("LockScreenNotificationRepo") {
            appIconCache.evictAll()
        }
    }

    fun isNotificationAccessEnabled(context: Context): Boolean {
        val appContext = context.applicationContext
        val enabledListeners = runCatching {
            Settings.Secure.getString(appContext.contentResolver, "enabled_notification_listeners")
        }.getOrNull().orEmpty()
        val expectedComponent = ComponentName(appContext, BackdropNotificationListenerService::class.java)
        val flattened = expectedComponent.flattenToString()
        val shortFlattened = expectedComponent.flattenToShortString()
        if (enabledListeners.split(":").any { token ->
                token == flattened || token == shortFlattened || token.endsWith("/${BackdropNotificationListenerService::class.java.name}")
            }
        ) {
            return true
        }
        return runCatching {
            NotificationManagerCompat.getEnabledListenerPackages(appContext).contains(appContext.packageName)
        }.getOrDefault(false)
    }

    fun buildNotificationAccessIntent(context: Context): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val cn = ComponentName(context.applicationContext, BackdropNotificationListenerService::class.java)
                putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, cn.flattenToString())
            }
        }
    }

    fun bindService(service: NotificationListenerService?) {
        boundServiceRef = if (service != null) WeakReference(service) else null
        _listenerConnected.value = service != null
    }

    fun updateActiveNotifications(context: Context, sbns: Array<StatusBarNotification>?) {
        if (sbns == null) {
            _groups.value = emptyList()
            _totalCount.value = 0
            return
        }
        val appContext = context.applicationContext
        val ownPkg = appContext.packageName
        val pm = appContext.packageManager

        val parsedItems = sbns.mapNotNull { sbn ->
            if (sbn.packageName == ownPkg) return@mapNotNull null
            val notif = sbn.notification ?: return@mapNotNull null
            if ((notif.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return@mapNotNull null
            val isOngoing = (notif.flags and Notification.FLAG_ONGOING_EVENT) != 0
            val extras = notif.extras
            val convTitle = extras?.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim().orEmpty()
            val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
            val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
            val bodyText = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
            val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()

            val resolvedTitle = convTitle.ifBlank { title }
            val resolvedBody = bigText.ifBlank { bodyText }

            if (resolvedTitle.isBlank() && resolvedBody.isBlank()) return@mapNotNull null
            if (isOngoing && resolvedBody.isBlank() && notif.category == Notification.CATEGORY_SERVICE) {
                return@mapNotNull null
            }

            val appName = resolveAppName(pm, sbn.packageName)
            val appIcon = resolveAppIcon(pm, sbn.packageName)
            val previewBmp = extractBoundedPreviewBitmap(extras)
            val actions = notif.actions
                ?.mapNotNull { act ->
                    val label = act.title?.toString()?.trim().orEmpty()
                    if (label.isBlank()) null else LockNotificationAction(title = label, actionIntent = act.actionIntent)
                }
                ?.take(2)
                .orEmpty()

            val isSensitive = notif.visibility == Notification.VISIBILITY_PRIVATE ||
                notif.visibility == Notification.VISIBILITY_SECRET ||
                notif.category in SENSITIVE_CATEGORIES

            LockNotificationItem(
                key = sbn.key,
                packageName = sbn.packageName,
                appName = appName,
                senderOrTitle = resolvedTitle.ifBlank { appName },
                bodyPreview = resolvedBody.ifBlank { subText.ifBlank { "Tap to view notification" } },
                subText = subText,
                postTime = sbn.postTime,
                isClearable = sbn.isClearable,
                isSensitive = isSensitive,
                contentIntent = notif.contentIntent,
                appIconBitmap = appIcon,
                previewImageBitmap = previewBmp,
                actions = actions
            )
        }

        val grouped = parsedItems
            .groupBy { it.packageName }
            .map { (pkg, items) ->
                val sortedItems = items.sortedByDescending { it.postTime }
                LockNotificationGroup(
                    packageName = pkg,
                    appName = sortedItems.first().appName,
                    appIconBitmap = sortedItems.first().appIconBitmap ?: resolveAppIcon(pm, pkg),
                    notifications = sortedItems,
                    latestTimestamp = sortedItems.first().postTime
                )
            }
            .sortedByDescending { it.latestTimestamp }

        _groups.value = grouped
        _totalCount.value = parsedItems.size
    }

    /**
     * Used cleanly in unit/Robolectric QA tests or fallback refresh without reflection.
     */
    fun setGroupsForTesting(groups: List<LockNotificationGroup>) {
        _groups.value = groups
        _totalCount.value = groups.sumOf { it.notifications.size }
    }

    fun dismissNotification(key: String) {
        runCatching { boundServiceRef?.get()?.cancelNotification(key) }
        val updated = _groups.value.mapNotNull { group ->
            val remaining = group.notifications.filterNot { it.key == key }
            if (remaining.isEmpty()) null else group.copy(
                notifications = remaining,
                latestTimestamp = remaining.maxOf { it.postTime }
            )
        }
        _groups.value = updated
        _totalCount.value = updated.sumOf { it.notifications.size }
    }

    fun dismissGroup(packageName: String) {
        val target = _groups.value.firstOrNull { it.packageName == packageName } ?: return
        target.notifications.filter { it.isClearable }.forEach { item ->
            runCatching { boundServiceRef?.get()?.cancelNotification(item.key) }
        }
        val updated = _groups.value.mapNotNull { group ->
            if (group.packageName != packageName) {
                group
            } else {
                val sticky = group.notifications.filterNot { it.isClearable }
                if (sticky.isEmpty()) null else group.copy(notifications = sticky)
            }
        }
        _groups.value = updated
        _totalCount.value = updated.sumOf { it.notifications.size }
    }

    fun clearAllClearable() {
        val service = boundServiceRef?.get()
        _groups.value.forEach { group ->
            group.notifications.filter { it.isClearable }.forEach { item ->
                runCatching { service?.cancelNotification(item.key) }
            }
        }
        val remaining = _groups.value.mapNotNull { group ->
            val sticky = group.notifications.filterNot { it.isClearable }
            if (sticky.isEmpty()) null else group.copy(notifications = sticky)
        }
        _groups.value = remaining
        _totalCount.value = remaining.sumOf { it.notifications.size }
    }

    private fun extractBoundedPreviewBitmap(extras: Bundle?): Bitmap? {
        if (extras == null) return null
        val raw = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_PICTURE, Bitmap::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable<Bitmap>(Notification.EXTRA_PICTURE)
            }
        }.getOrNull() ?: return null
        if (raw.isRecycled || raw.width <= 0 || raw.height <= 0) return null
        val software = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && raw.config == Bitmap.Config.HARDWARE) {
            runCatching { raw.copy(Bitmap.Config.ARGB_8888, false) }.getOrNull() ?: return null
        } else {
            raw
        }
        val maxEdge = max(software.width, software.height)
        return if (maxEdge > 512) {
            val scale = 512f / maxEdge.toFloat()
            val dstW = (software.width * scale).roundToInt().coerceAtLeast(1)
            val dstH = (software.height * scale).roundToInt().coerceAtLeast(1)
            val scaled = runCatching { Bitmap.createScaledBitmap(software, dstW, dstH, true) }.getOrNull()
            if (scaled != null && scaled !== software && software !== raw && !software.isRecycled) {
                software.recycle()
            }
            scaled ?: software
        } else {
            software
        }
    }

    private fun resolveAppName(pm: android.content.pm.PackageManager, pkg: String): String =
        runCatching {
            val info = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString()
        }.getOrDefault(pkg.substringAfterLast('.').replaceFirstChar { it.uppercaseChar() })

    private fun resolveAppIcon(pm: android.content.pm.PackageManager, pkg: String): Bitmap? {
        appIconCache.get(pkg)?.let { cached ->
            if (!cached.isRecycled && cached.width > 0 && cached.height > 0) {
                return cached
            }
            appIconCache.remove(pkg)
        }
        val bmp = runCatching {
            val drawable = pm.getApplicationIcon(pkg)
            drawableToBitmap(drawable, 64)
        }.getOrNull()
        if (bmp != null && !bmp.isRecycled) {
            appIconCache.put(pkg, bmp)
        }
        return bmp?.takeIf { !it.isRecycled }
    }

    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val src = drawable.bitmap
            return if (src.width <= sizePx && src.height <= sizePx) src
            else Bitmap.createScaledBitmap(src, sizePx, sizePx, true)
        }
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }

    private val SENSITIVE_CATEGORIES = setOf(
        Notification.CATEGORY_MESSAGE,
        Notification.CATEGORY_EMAIL,
        Notification.CATEGORY_CALL,
        Notification.CATEGORY_SOCIAL
    )
}
