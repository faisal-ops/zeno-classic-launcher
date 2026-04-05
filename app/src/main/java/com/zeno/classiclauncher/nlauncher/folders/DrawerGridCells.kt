package com.zeno.classiclauncher.nlauncher.folders

import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import com.zeno.classiclauncher.nlauncher.apps.AppsRepository

private fun folderDisplayTitle(folderId: String, members: List<AppEntry>, folderNames: Map<String, String>): String {
    folderNames[folderId]?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    if (members.isEmpty()) return "Folder"
    if (members.size == 1) return members.first().label
    return "Folder (${members.size})"
}

fun buildDrawerGridCells(
    installed: List<AppEntry>,
    orderedPackages: List<String>,
    folderContents: Map<String, List<String>>,
    folderNames: Map<String, String>,
    hiddenPackages: Set<String>,
    privateMode: Boolean,
    privateQuery: String,
    normalQuery: String,
): List<DrawerGridCell> {
    val byPkg = HashMap<String, AppEntry>(installed.size)
    for (app in installed) byPkg[app.packageName] = app

    val folderMemberPkgs = HashSet<String>(folderContents.size * 4)
    for ((_, members) in folderContents) folderMemberPkgs.addAll(members)

    val orderIndex = HashMap<String, Int>(orderedPackages.size)
    for (i in orderedPackages.indices) orderIndex[orderedPackages[i]] = i

    val privateLower = if (privateMode && privateQuery.isNotEmpty()) privateQuery.lowercase() else ""
    val normalLower = if (!privateMode && normalQuery.isNotEmpty()) normalQuery.lowercase() else ""

    fun appMatchesFilter(a: AppEntry): Boolean {
        if (privateMode) {
            if (a.packageName !in hiddenPackages) return false
            if (privateLower.isEmpty()) return true
            return a.label.lowercase().contains(privateLower) || a.packageName.contains(privateLower)
        }
        if (a.packageName in hiddenPackages) return false
        if (normalLower.isEmpty()) return true
        return a.label.lowercase().contains(normalLower) || a.packageName.contains(normalLower)
    }

    fun membersForFolderCell(folderId: String, forSearch: Boolean): List<AppEntry> {
        val pkgs = folderContents[folderId] ?: return emptyList()
        val out = ArrayList<AppEntry>(pkgs.size)
        for (pkg in pkgs) {
            val app = byPkg[pkg] ?: continue
            if (forSearch || privateMode) {
                if (appMatchesFilter(app)) out.add(app)
            } else {
                if (app.packageName !in hiddenPackages) out.add(app)
            }
        }
        return out
    }

    val searching = (privateMode && privateQuery.isNotEmpty()) || (!privateMode && normalQuery.isNotEmpty())

    val result = ArrayList<DrawerGridCell>(installed.size)
    val placedTopLevelPkgs = HashSet<String>(orderedPackages.size)

    for (token in orderedPackages) {
        if (FolderIds.isFolderId(token)) {
            val members = membersForFolderCell(token, forSearch = searching || privateMode)
            if (members.isNotEmpty()) {
                val title = folderDisplayTitle(token, members, folderNames)
                result.add(DrawerGridCell.Folder(token, members, title))
            }
        } else {
            if (token in folderMemberPkgs) continue
            val app = byPkg[token] ?: continue
            if (!appMatchesFilter(app)) continue
            result.add(DrawerGridCell.App(app))
            placedTopLevelPkgs.add(token)
        }
    }

    val tail = installed
        .asSequence()
        .filter { appMatchesFilter(it) }
        .filter { it.packageName !in folderMemberPkgs }
        .filter { it.packageName !in placedTopLevelPkgs }
        .sortedWith(TAIL_COMPARATOR(orderIndex))
        .map { DrawerGridCell.App(it) }
        .toList()

    result.addAll(tail)
    return result
}

@Suppress("FunctionName")
private fun TAIL_COMPARATOR(orderIndex: Map<String, Int>): Comparator<AppEntry> =
    compareBy<AppEntry> {
        if (it.packageName == AppsRepository.INTERNAL_SETTINGS_PACKAGE) 0 else 1
    }.thenBy {
        orderIndex[it.packageName] ?: Int.MAX_VALUE
    }.thenBy {
        it.label.lowercase()
    }
