package com.zeno.classiclauncher.nlauncher.badges

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Forwards status-bar events to [NotificationRepository] on a background dispatcher.
 * No broadcasts; UI reads [NotificationRepository] StateFlows.
 */
class BadgeNotificationListener : NotificationListenerService() {

    companion object {
        private var instance: BadgeNotificationListener? = null

        fun cancelForPackage(pkg: String) {
            val svc = instance ?: return
            runCatching {
                svc.activeNotifications
                    ?.filter { it.packageName == pkg }
                    ?.forEach { svc.cancelNotification(it.key) }
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        NotificationRepository.replaceAllActive(activeNotifications)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        NotificationRepository.onPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        NotificationRepository.onRemoved(sbn)
    }
}
