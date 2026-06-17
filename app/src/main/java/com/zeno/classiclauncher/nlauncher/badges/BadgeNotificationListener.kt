package com.zeno.classiclauncher.nlauncher.badges

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Forwards status-bar events to [NotificationRepository] on a background dispatcher.
 * No broadcasts; UI reads [NotificationRepository] StateFlows.
 */
class BadgeNotificationListener : NotificationListenerService() {

    companion object {
        private var instance: BadgeNotificationListener? = null

        /**
         * Fully enables or disables the NotificationListenerService component.
         * When disabled the system unbinds the service entirely — zero IPC overhead.
         * The notification-access grant is preserved and reconnects on re-enable.
         * Call whenever notificationBadgesEnabled changes (wired from MainActivity).
         */
        fun setComponentEnabled(context: Context, enabled: Boolean) {
            runCatching {
                val cn = ComponentName(context, BadgeNotificationListener::class.java)
                val state = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                context.packageManager.setComponentEnabledSetting(cn, state, PackageManager.DONT_KILL_APP)
            }
        }

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
