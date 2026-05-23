package com.zeno.classiclauncher.nlauncher.prefs

import com.zeno.classiclauncher.nlauncher.apps.AppEntry

/**
 * Preferred order for dock mail badge when the user picks a fixed app (matches heuristic priority).
 */
object MailBadgeCandidates {
    val preferredPackageOrder: List<String> = listOf(
        "com.blackberry.hub",
        "com.google.android.gm",
        "com.android.email",
        "com.microsoft.office.outlook",
        "com.yahoo.mobile.client.android.mail",
        "com.samsung.android.email.provider",
    )

    private fun heuristicPackage(pkg: String): Boolean =
        pkg.contains("mail", ignoreCase = true) ||
            pkg.contains("email", ignoreCase = true) ||
            pkg.contains("hub", ignoreCase = true)

    /** Installed apps that can be chosen for the mail dock badge (no internal tiles). */
    fun installedCandidates(apps: List<AppEntry>): List<AppEntry> {
        val real = apps.filter { !it.internal }
        val byPkg = real.associateBy { it.packageName }
        val ordered = preferredPackageOrder.mapNotNull { byPkg[it] }
        val used = ordered.map { it.packageName }.toSet()
        val extra = real
            .filter { it.packageName !in used && heuristicPackage(it.packageName) }
            .sortedBy { it.label.lowercase() }
        return ordered + extra
    }
}
