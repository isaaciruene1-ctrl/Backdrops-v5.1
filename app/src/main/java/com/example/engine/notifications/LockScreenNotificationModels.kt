package com.example.engine.notifications

import android.app.PendingIntent
import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class LockNotificationAction(
    val title: String,
    val actionIntent: PendingIntent? = null
)

@Immutable
data class LockNotificationItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val senderOrTitle: String,
    val bodyPreview: String,
    val subText: String = "",
    val postTime: Long = 0L,
    val isClearable: Boolean = true,
    val isSensitive: Boolean = false,
    val contentIntent: PendingIntent? = null,
    val appIconBitmap: Bitmap? = null,
    val previewImageBitmap: Bitmap? = null,
    val actions: List<LockNotificationAction> = emptyList()
)

@Immutable
data class LockNotificationGroup(
    val packageName: String,
    val appName: String,
    val appIconBitmap: Bitmap? = null,
    val notifications: List<LockNotificationItem>,
    val latestTimestamp: Long
) {
    val count: Int get() = notifications.size
    val leadItem: LockNotificationItem? get() = notifications.firstOrNull()
}
