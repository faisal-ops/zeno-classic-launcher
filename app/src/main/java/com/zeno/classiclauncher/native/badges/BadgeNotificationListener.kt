package com.zeno.classiclauncher.nlauncher.badges

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Forwards status-bar events to [NotificationRepository] on a background dispatcher.
 * No broadcasts; UI reads [NotificationRepository] StateFlows.
 */
class BadgeNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationRepository.replaceAllActive(activeNotifications)
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
