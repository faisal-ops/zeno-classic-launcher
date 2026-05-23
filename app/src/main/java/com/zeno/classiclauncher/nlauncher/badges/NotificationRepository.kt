package com.zeno.classiclauncher.nlauncher.badges

import android.app.Notification
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central store for filtered notification presence used by dock badges.
 *
 * **Filtering (SMS / WhatsApp):** Indexed if not a group summary, not silent-style
 * ([Notification.PRIORITY_LOW] or below), and [Notification.category] is null or one of
 * MESSAGE, EMAIL, or CALL.
 *
 * **Filtering (mail apps):** Any package in [MAIL_PACKAGES] or matching the internal name heuristic.
 * We do **not** filter by notification category (Primary vs Social vs Promotions, etc.) or by
 * legacy priority — any active status-bar notification from that app counts toward the mail badge,
 * including bundled **group summaries**.
 *
 * **State:** Three independent [StateFlow]s expose only presence (no counts). Each update touches
 * at most one bucket with O(1) set add/remove; full resync runs only on listener connect.
 *
 * **Threading:** The listener posts work onto [Dispatchers.Default]; UI collects flows on the main thread.
 */
object NotificationRepository {

    val MAIL_PACKAGES = setOf(
        "com.google.android.gm",
        "com.microsoft.office.outlook",
        "com.yahoo.mobile.client.android.mail",
    )

    val SMS_PACKAGES = setOf(
        "com.google.android.apps.messaging",
        "com.android.messaging",
    )

    const val WHATSAPP_PACKAGE = "com.whatsapp"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val lock = Any()

    /** Spec: map of active, filtered notifications by system key. */
    private val activeByKey = HashMap<String, StatusBarNotification>()
    /** All active notifications used for icon-dot badges (broader scope than dock badge buckets). */
    private val iconBadgePackagesByKey = HashMap<String, String>()

    private val mailKeys = HashSet<String>()
    private val smsKeys = HashSet<String>()
    private val waKeys = HashSet<String>()

    private val _hasUnreadMail = MutableStateFlow(false)
    val hasUnreadMail: StateFlow<Boolean> = _hasUnreadMail.asStateFlow()

    private val _hasUnreadSms = MutableStateFlow(false)
    val hasUnreadSms: StateFlow<Boolean> = _hasUnreadSms.asStateFlow()

    private val _hasUnreadWhatsApp = MutableStateFlow(false)
    val hasUnreadWhatsApp: StateFlow<Boolean> = _hasUnreadWhatsApp.asStateFlow()

    /** Packages that currently have at least one tracked active notification (for folder badges). */
    private val _packagesWithUnread = MutableStateFlow<Set<String>>(emptySet())
    val packagesWithUnread: StateFlow<Set<String>> = _packagesWithUnread.asStateFlow()

    fun snapshotActiveMap(): Map<String, StatusBarNotification> =
        synchronized(lock) { HashMap(activeByKey) }

    fun replaceAllActive(active: Array<StatusBarNotification>?) {
        scope.launch {
            synchronized(lock) {
                activeByKey.clear()
                iconBadgePackagesByKey.clear()
                mailKeys.clear()
                smsKeys.clear()
                waKeys.clear()
                val list = active ?: emptyArray()
                for (sbn in list) {
                    if (shouldTrackIconBadge(sbn)) {
                        iconBadgePackagesByKey[sbn.key] = sbn.packageName
                    }
                    if (!shouldTrack(sbn)) continue
                    val key = sbn.key
                    activeByKey[key] = sbn
                    addKeyToBucket(key, sbn.packageName)
                }
                publishAll()
            }
        }
    }

    fun onPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        scope.launch {
            synchronized(lock) {
                if (!shouldTrack(sbn)) {
                    // Drop stale entry if a previously tracked notification no longer qualifies
                    val key = sbn.key
                    if (activeByKey.remove(key) != null) {
                        removeKeyFromAllBuckets(key)
                    }
                    if (shouldTrackIconBadge(sbn)) {
                        iconBadgePackagesByKey[key] = sbn.packageName
                    } else {
                        iconBadgePackagesByKey.remove(key)
                    }
                    publishAll()
                    return@synchronized
                }
                val key = sbn.key
                activeByKey[key] = sbn
                if (shouldTrackIconBadge(sbn)) {
                    iconBadgePackagesByKey[key] = sbn.packageName
                } else {
                    iconBadgePackagesByKey.remove(key)
                }
                removeKeyFromAllBuckets(key)
                addKeyToBucket(key, sbn.packageName)
                publishAll()
            }
        }
    }

    /**
     * Optimistically clears badge for [pkg] when the user opens the app directly.
     * The badge returns naturally when a new notification arrives via [onPosted].
     */
    fun clearForPackage(pkg: String) {
        scope.launch {
            synchronized(lock) {
                val keysToRemove = iconBadgePackagesByKey.entries
                    .filter { it.value == pkg }
                    .map { it.key }
                if (keysToRemove.isEmpty()) return@synchronized
                for (key in keysToRemove) {
                    iconBadgePackagesByKey.remove(key)
                    activeByKey.remove(key)
                    removeKeyFromAllBuckets(key)
                }
                publishAll()
            }
        }
    }

    fun onRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        scope.launch {
            synchronized(lock) {
                val key = sbn.key
                activeByKey.remove(key)
                iconBadgePackagesByKey.remove(key)
                removeKeyFromAllBuckets(key)
                publishAll()
            }
        }
    }

    private fun addKeyToBucket(key: String, pkg: String) {
        when {
            pkg == WHATSAPP_PACKAGE -> waKeys.add(key)
            pkg in SMS_PACKAGES -> smsKeys.add(key)
            isMailLikePackage(pkg) -> mailKeys.add(key)
        }
    }

    /** True for known mail apps + broad package-name heuristic (excludes SMS / WhatsApp). */
    fun isMailLikePackage(pkg: String): Boolean {
        if (pkg == WHATSAPP_PACKAGE || pkg in SMS_PACKAGES) return false
        if (pkg in MAIL_PACKAGES) return true
        return mailHeuristicPackage(pkg)
    }

    /**
     * Catches Samsung Email, K-9, Aqua Mail, etc. without listing every package.
     * Kept conservative to avoid matching unrelated apps.
     */
    private fun mailHeuristicPackage(pkg: String): Boolean {
        val l = pkg.lowercase()
        if (l.contains("messaging")) return false
        return l.contains("mail") ||
            l.contains("email") ||
            l.contains("inbox") ||
            l.contains("hub") ||
            l.contains("outlook") ||
            l.contains("gmail") ||
            l.contains("yahoo") ||
            l.contains("proton") ||
            l.contains("thunderbird") ||
            l.contains("k9") ||
            l.contains("aquamail") ||
            l.contains("bluemail") ||
            (l.contains("edison") && l.contains("mail")) ||
            l.contains("readdle")
    }

    private fun isAllowedForBadge(pkg: String): Boolean =
        pkg == WHATSAPP_PACKAGE || pkg in SMS_PACKAGES || isMailLikePackage(pkg)

    private fun removeKeyFromAllBuckets(key: String) {
        mailKeys.remove(key)
        smsKeys.remove(key)
        waKeys.remove(key)
    }

    private fun publishAll() {
        val mail = mailKeys.isNotEmpty()
        val sms = smsKeys.isNotEmpty()
        val wa = waKeys.isNotEmpty()
        if (_hasUnreadMail.value != mail) _hasUnreadMail.value = mail
        if (_hasUnreadSms.value != sms) _hasUnreadSms.value = sms
        if (_hasUnreadWhatsApp.value != wa) _hasUnreadWhatsApp.value = wa
        val pkgs = iconBadgePackagesByKey.values.toSet()
        if (_packagesWithUnread.value != pkgs) _packagesWithUnread.value = pkgs
    }

    private fun shouldTrackIconBadge(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        if (isGroupSummary(n)) return false
        @Suppress("DEPRECATION")
        return (n.flags and Notification.FLAG_ONGOING_EVENT) == 0
    }

    fun shouldTrack(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        val pkg = sbn.packageName
        if (!isAllowedForBadge(pkg)) return false
        if (isBackgroundOrSilent(sbn, n)) return false
        if (isMailLikePackage(pkg)) {
            return true
        }
        if (isGroupSummary(n)) return false
        if (!passesCategoryFilter(n)) return false
        return true
    }

    private fun isGroupSummary(n: Notification): Boolean {
        @Suppress("DEPRECATION")
        return (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0
    }

    private fun isBackgroundOrSilent(sbn: StatusBarNotification, n: Notification): Boolean {
        if (isMailLikePackage(sbn.packageName)) {
            // Primary / Social / Promotions / low-importance — all count; do not drop by priority.
            return false
        }
        @Suppress("DEPRECATION")
        return n.priority <= Notification.PRIORITY_LOW
    }

    private fun passesCategoryFilter(n: Notification): Boolean {
        val cat = n.category ?: return true
        return cat == Notification.CATEGORY_MESSAGE ||
            cat == Notification.CATEGORY_EMAIL ||
            cat == Notification.CATEGORY_CALL
    }
}
