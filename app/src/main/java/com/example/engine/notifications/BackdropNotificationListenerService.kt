package com.example.engine.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.Executors

/**
 * Official Android NotificationListenerService integration for Backdrop Lock Screen.
 * Keeps active notification state synchronized cleanly on a background executor without
 * retaining sensitive message content on disk.
 */
class BackdropNotificationListenerService : NotificationListenerService() {

    private val worker = Executors.newSingleThreadExecutor()

    override fun onListenerConnected() {
        super.onListenerConnected()
        LockScreenNotificationRepository.bindService(this)
        syncSnapshot()
    }

    override fun onListenerDisconnected() {
        LockScreenNotificationRepository.bindService(null)
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        syncSnapshot()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        syncSnapshot()
    }

    override fun onDestroy() {
        LockScreenNotificationRepository.bindService(null)
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun syncSnapshot() {
        worker.execute {
            val current = runCatching { activeNotifications }.getOrNull()
            LockScreenNotificationRepository.updateActiveNotifications(applicationContext, current)
        }
    }
}
