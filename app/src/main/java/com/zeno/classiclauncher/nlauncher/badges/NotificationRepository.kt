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
 * **Mail badge scoping:** [dockMailPackage] restricts which app's notifications actually light up
 * the dock Mail badge — a mail-like notification only counts if it matches the app pinned to the
 * dock's Mail slot. When no app is explicitly pinned, [dockMailPackage] is set (by MainActivity)
 * to [com.zeno.classiclauncher.nlauncher.apps.resolveDefaultMailPackage] — the same resolution
 * [com.zeno.classiclauncher.nlauncher.apps.LauncherActions.launchMail] uses — so the badge still
 * tracks whichever app a tap would actually open. Only falls back to the broad heuristic (any
 * mail-like app) if that resolution itself comes up empty.
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

    /**
     * Set to true while Minimal Mode is active. Skips all badge classification work because
     * the Normal Mode dock (the only consumer of these StateFlows) is not composed in that state.
     * Wired from MainActivity immediately after the mode StateFlow emits.
     */
    @Volatile
    var minimalModeActive: Boolean = false

    /**
     * Mirrors the notificationBadgesEnabled pref. When false, skips all classification work —
     * BadgeNotificationListener still receives events (it's a system service), but they are
     * discarded here rather than burning CPU on shouldTrack / addKeyToBucket / publishAll.
     * Wired from MainActivity immediately after the prefs StateFlow emits.
     */
    @Volatile
    var badgesEnabled: Boolean = true

    /**
     * Mirrors prefs.dockMailPackage. Scopes the mail badge to only the app pinned to the dock's
     * Mail slot — empty means no app is pinned, so the broad mail-like heuristic is used instead.
     * Wired from MainActivity immediately after the prefs StateFlow emits.
     */
    @Volatile
    var dockMailPackage: String = ""

    private val lock = Any()

    /** Spec: map of active, filtered notifications by system key. */
    private val activeByKey = HashMap<String, StatusBarNotification>()

    /** The bits of a [StatusBarNotification] the icon-dot badges and status-bar notification
     *  glyph need — [smallIcon] is the app-supplied monochrome glyph built for exactly this
     *  context (matches what the real Android status bar shows), not the full launcher icon. */
    private data class IconBadgeEntry(val packageName: String, val postTime: Long, val smallIcon: android.graphics.drawable.Icon?)

    /** All active notifications used for icon-dot badges (broader scope than dock badge buckets). */
    private val iconBadgeEntryByKey = HashMap<String, IconBadgeEntry>()

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

    /** Up to 3 most-recently-posted unread packages (most recent first), for the status bar's
     *  reserved notification-icon slots — empty when there are none. Capped to 3 distinct
     *  packages; a package with several active notifications only occupies one slot.
     *  [smallIcon] is that package's most recent notification's own status-bar glyph — the
     *  same one Android's own status bar shows — used in preference to the app's launcher icon. */
    data class RecentNotifyIcon(val packageName: String, val smallIcon: android.graphics.drawable.Icon?)

    private val _recentUnreadPackages = MutableStateFlow<List<RecentNotifyIcon>>(emptyList())
    val recentUnreadPackages: StateFlow<List<RecentNotifyIcon>> = _recentUnreadPackages.asStateFlow()

    /** Keys of currently-active notifications with category CATEGORY_MISSED_CALL — independent
     *  of the mail/SMS/WhatsApp buckets above (those are keyed by package, this by category). */
    private val missedCallKeys = HashSet<String>()

    /** True exactly while the phone/dialer app's own missed-call notification is still active —
     *  stays true until the user dismisses/clears it (matches it disappearing), not on a timer. */
    private val _hasMissedCall = MutableStateFlow(false)
    val hasMissedCall: StateFlow<Boolean> = _hasMissedCall.asStateFlow()

    fun snapshotActiveMap(): Map<String, StatusBarNotification> =
        synchronized(lock) { HashMap(activeByKey) }

    fun replaceAllActive(active: Array<StatusBarNotification>?) {
        if (!badgesEnabled) return
        scope.launch {
            synchronized(lock) {
                activeByKey.clear()
                iconBadgeEntryByKey.clear()
                mailKeys.clear()
                smsKeys.clear()
                waKeys.clear()
                missedCallKeys.clear()
                val list = active ?: emptyArray()
                for (sbn in list) {
                    if (shouldTrackIconBadge(sbn)) {
                        iconBadgeEntryByKey[sbn.key] =
                            IconBadgeEntry(sbn.packageName, sbn.postTime, sbn.notification.smallIcon)
                    }
                    if (isMissedCall(sbn)) missedCallKeys.add(sbn.key)
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
        if (sbn == null || !badgesEnabled) return
        scope.launch {
            synchronized(lock) {
                if (isMissedCall(sbn)) missedCallKeys.add(sbn.key) else missedCallKeys.remove(sbn.key)
                if (!shouldTrack(sbn)) {
                    // Drop stale entry if a previously tracked notification no longer qualifies
                    val key = sbn.key
                    if (activeByKey.remove(key) != null) {
                        removeKeyFromAllBuckets(key)
                    }
                    if (shouldTrackIconBadge(sbn)) {
                        iconBadgeEntryByKey[key] =
                            IconBadgeEntry(sbn.packageName, sbn.postTime, sbn.notification.smallIcon)
                    } else {
                        iconBadgeEntryByKey.remove(key)
                    }
                    publishAll()
                    return@synchronized
                }
                val key = sbn.key
                activeByKey[key] = sbn
                if (shouldTrackIconBadge(sbn)) {
                    iconBadgeEntryByKey[key] =
                        IconBadgeEntry(sbn.packageName, sbn.postTime, sbn.notification.smallIcon)
                } else {
                    iconBadgeEntryByKey.remove(key)
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
                val keysToRemove = iconBadgeEntryByKey.entries
                    .filter { it.value.packageName == pkg }
                    .map { it.key }
                if (keysToRemove.isEmpty()) return@synchronized
                for (key in keysToRemove) {
                    iconBadgeEntryByKey.remove(key)
                    activeByKey.remove(key)
                    removeKeyFromAllBuckets(key)
                }
                publishAll()
            }
        }
    }

    fun onRemoved(sbn: StatusBarNotification?) {
        if (sbn == null || !badgesEnabled) return
        scope.launch {
            synchronized(lock) {
                val key = sbn.key
                activeByKey.remove(key)
                iconBadgeEntryByKey.remove(key)
                missedCallKeys.remove(key)
                removeKeyFromAllBuckets(key)
                publishAll()
            }
        }
    }

    private fun addKeyToBucket(key: String, pkg: String) {
        when {
            pkg == WHATSAPP_PACKAGE -> waKeys.add(key)
            pkg in SMS_PACKAGES -> smsKeys.add(key)
            isMailLikePackage(pkg) && matchesDockMailPackage(pkg) -> mailKeys.add(key)
        }
    }

    /** True if [pkg] should count toward the dock Mail badge — see [dockMailPackage]. */
    internal fun matchesDockMailPackage(pkg: String): Boolean {
        val configured = dockMailPackage
        return configured.isEmpty() || pkg == configured
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
        val missedCall = missedCallKeys.isNotEmpty()
        if (_hasMissedCall.value != missedCall) _hasMissedCall.value = missedCall
        val pkgs = iconBadgeEntryByKey.values.map { it.packageName }.toSet()
        if (_packagesWithUnread.value != pkgs) _packagesWithUnread.value = pkgs
        val recentIcons = iconBadgeEntryByKey.values
            .groupBy { it.packageName }
            .map { (_, entries) -> entries.maxBy { it.postTime } }
            .sortedByDescending { it.postTime }
            .take(3)
            .map { RecentNotifyIcon(it.packageName, it.smallIcon) }
        if (_recentUnreadPackages.value != recentIcons) _recentUnreadPackages.value = recentIcons
    }

    private fun shouldTrackIconBadge(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        if (isGroupSummary(n)) return false
        @Suppress("DEPRECATION")
        return (n.flags and Notification.FLAG_ONGOING_EVENT) == 0
    }

    /** The dialer/phone app's own missed-call notification — independent of [shouldTrack]'s
     *  mail/SMS/WhatsApp package filtering, since the missed-call indicator isn't scoped to a
     *  specific app the way those badges are. */
    private fun isMissedCall(sbn: StatusBarNotification): Boolean =
        sbn.notification.category == Notification.CATEGORY_MISSED_CALL

    fun shouldTrack(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        val pkg = sbn.packageName
        if (!isAllowedForBadge(pkg)) return false
        if (isBackgroundOrSilent(sbn, n)) return false
        if (isMailLikePackage(pkg)) {
            // Group summaries from Gmail/Outlook are silent background bookkeeping notifications
            // that are invisible to the user — excluding them prevents the badge from getting stuck
            // after the user clears all visible mail notifications.
            if (isGroupSummary(n)) return false
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
