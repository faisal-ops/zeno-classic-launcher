package com.zeno.classiclauncher.nlauncher.ui

import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import com.zeno.classiclauncher.nlauncher.apps.AppsRepository
import com.zeno.classiclauncher.nlauncher.folders.DrawerGridCell
import com.zeno.classiclauncher.nlauncher.folders.FolderIds
import com.zeno.classiclauncher.nlauncher.folders.buildDrawerGridCells
import com.zeno.classiclauncher.nlauncher.prefs.LauncherPrefsRepository
import kotlinx.coroutines.flow.first
import java.text.Collator
import java.util.Locale

private val PRIVATE_PREFIX_REGEX = Regex("^private\\s+", RegexOption.IGNORE_CASE)

internal fun strictAlphabeticalDrawerOrder(
    installed: List<AppEntry>,
    folderContents: Map<String, List<String>>,
    folderNames: Map<String, String>,
): List<String> {
    val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
    val byPkg = installed.associateBy { it.packageName }
    val folderMemberPkgs = folderContents.values.flatten().toSet()
    data class TokenSortKey(
        val token: String,
        val label: String,
    )

    val topLevelApps = installed
        .asSequence()
        .filter { it.packageName !in folderMemberPkgs }
        .map { app -> TokenSortKey(token = app.packageName, label = app.label) }
        .toMutableList()

    val topLevelFolders = folderContents
        .asSequence()
        .filter { (_, members) -> members.isNotEmpty() }
        .map { (folderId, members) ->
            val custom = folderNames[folderId]?.trim()?.takeIf { it.isNotEmpty() }
            val fallback = members.firstNotNullOfOrNull { byPkg[it]?.label } ?: "Folder"
            TokenSortKey(token = folderId, label = custom ?: fallback)
        }.toList()

    val allTopLevel = (topLevelApps + topLevelFolders).toMutableList()
    allTopLevel.sortWith { a, b ->
        val c = collator.compare(a.label, b.label)
        if (c != 0) c else a.token.compareTo(b.token)
    }
    return allTopLevel.map { it.token }
}

/**
 * Same cell ordering as [LauncherViewModel.filteredGridCells] for a hypothetical grid state (used when
 * inserting a new drawer folder at the on-screen position where the app was).
 */
internal fun filteredDrawerCellsSnapshot(
    installed: List<AppEntry>,
    orderedPackages: List<String>,
    folderContents: Map<String, List<String>>,
    folderNames: Map<String, String>,
    hiddenPackages: Set<String>,
    searchQueryRaw: String,
    sortByUsage: Boolean,
    usageStats: Map<String, Long>,
): List<DrawerGridCell> {
    val query = searchQueryRaw.trim()
    val ql = query.lowercase()
    val (privateMode, privateQuery) = when {
        ql == "private" -> true to ""
        PRIVATE_PREFIX_REGEX.containsMatchIn(query) -> true to PRIVATE_PREFIX_REGEX.replaceFirst(query, "").trim()
        else -> false to ""
    }
    val normalQuery = if (privateMode) "" else query
    val cells = buildDrawerGridCells(
        installed = installed,
        orderedPackages = orderedPackages,
        folderContents = folderContents,
        folderNames = folderNames,
        hiddenPackages = hiddenPackages,
        privateMode = privateMode,
        privateQuery = privateQuery,
        normalQuery = normalQuery,
    )
    val sorted = if (sortByUsage && usageStats.isNotEmpty()) {
        cells.sortedByDescending { cell ->
            when (cell) {
                is DrawerGridCell.App -> usageStats[cell.entry.packageName] ?: 0L
                is DrawerGridCell.Folder -> cell.members.maxOfOrNull { usageStats[it.packageName] ?: 0L } ?: 0L
            }
        }
    } else {
        cells
    }
    return sorted
}

/**
 * Returns an index in [orderedPackages] such that, after [buildDrawerGridCells], the released app sits at
 * the same position it would in a purely A→Z list of visible app tiles (folders skipped). If no placement
 * in [orderedPackages] can achieve that (e.g. the correct spot is only inside the tail), returns **-1** so
 * the caller leaves the package out of [orderedPackages] and it appears in the tail with correct ordering.
 */
internal fun alphabeticalDrawerInsertIndex(
    orderWithoutApp: List<String>,
    packageName: String,
    installed: List<AppEntry>,
    folderContents: Map<String, List<String>>,
    folderNames: Map<String, String>,
    hiddenPackages: Set<String>,
): Int {
    val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
    val released = installed.find { it.packageName == packageName } ?: return -1
    val baseCells = buildDrawerGridCells(
        installed = installed,
        orderedPackages = orderWithoutApp,
        folderContents = folderContents,
        folderNames = folderNames,
        hiddenPackages = hiddenPackages,
        privateMode = false,
        privateQuery = "",
        normalQuery = "",
    )
    val targetIdx = baseCells.appEntries().count { e ->
        val c = collator.compare(e.label, released.label)
        c < 0 || (c == 0 && e.packageName < released.packageName)
    }
    for (insertAt in 0..orderWithoutApp.size) {
        val trial = orderWithoutApp.toMutableList().apply { add(insertAt, packageName) }
        val trialCells = buildDrawerGridCells(
            installed = installed,
            orderedPackages = trial,
            folderContents = folderContents,
            folderNames = folderNames,
            hiddenPackages = hiddenPackages,
            privateMode = false,
            privateQuery = "",
            normalQuery = "",
        )
        val idx = trialCells.appEntries().indexOfFirst { it.packageName == packageName }
        if (idx == targetIdx) return insertAt
    }
    return -1
}

private fun List<DrawerGridCell>.appEntries(): List<AppEntry> = mapNotNull { cell ->
    (cell as? DrawerGridCell.App)?.entry
}

/**
 * Shared "New Group" implementation — single source of truth for both [LauncherViewModel]'s own
 * long-press menu (home/drawer) and Quick Switch's self-contained "⋮" menu (see
 * `SearchOverlayActions` in the `search` package), so this non-trivial grid-ordering logic can't
 * drift between the two call sites. Callers own their own locking (e.g. LauncherViewModel's
 * gridHomeMutex) — this is plain read-modify-write against [prefsRepo], nothing more.
 */
internal suspend fun applyCreateFolderFromApp(
    prefsRepo: LauncherPrefsRepository,
    installed: List<AppEntry>,
    packageName: String,
    title: String,
    searchQueryRaw: String,
    sortByUsage: Boolean,
    usage: Map<String, Long>,
    strictAlphabetical: Boolean,
    visualCellIndex: Int = -1,
) {
    if (packageName == AppsRepository.INTERNAL_SETTINGS_PACKAGE) return
    val snap = prefsRepo.prefsFlow.first()

    val cellsBefore = filteredDrawerCellsSnapshot(
        installed = installed,
        orderedPackages = snap.orderedPackages.filter { tok ->
            !FolderIds.isFolderId(tok) || snap.folderContents.containsKey(tok)
        },
        folderContents = snap.folderContents,
        folderNames = snap.folderNames.filterKeys { snap.folderContents.containsKey(it) },
        hiddenPackages = snap.hiddenPackages,
        searchQueryRaw = searchQueryRaw,
        sortByUsage = sortByUsage,
        usageStats = usage,
    )
    val resolvedIdx = cellsBefore.indexOfFirst { cell ->
        cell is DrawerGridCell.App && cell.entry.packageName == packageName
    }
    val targetSlotIndex = when {
        resolvedIdx >= 0 -> resolvedIdx
        visualCellIndex >= 0 -> visualCellIndex
        else -> -1
    }

    val id = FolderIds.newId()
    val folders = snap.folderContents
        .mapValues { (_, m) -> m.filter { it != packageName } }
        .filterValues { it.isNotEmpty() }
        .toMutableMap()
    folders[id] = listOf(packageName)
    val label = title.trim().ifBlank { "Group" }
    val names = snap.folderNames
        .filterKeys { folders.containsKey(it) }
        .toMutableMap()
    names[id] = label

    if (strictAlphabetical) {
        val finalOrder = strictAlphabeticalDrawerOrder(
            installed = installed,
            folderContents = folders,
            folderNames = names,
        )
        prefsRepo.writeGridState(finalOrder, folders, names)
        return
    }

    val folderMembers = folders.values.flatten().toSet()

    val order = snap.orderedPackages
        .filter { tok -> !FolderIds.isFolderId(tok) || folders.containsKey(tok) }
        .toMutableList()
    val posBeforeRemove = order.indexOf(packageName)
    order.removeAll { it == packageName }

    fun cellsForTrial(trialOrder: List<String>): List<DrawerGridCell> =
        filteredDrawerCellsSnapshot(
            installed = installed,
            orderedPackages = trialOrder,
            folderContents = folders,
            folderNames = names,
            hiddenPackages = snap.hiddenPackages,
            searchQueryRaw = searchQueryRaw,
            sortByUsage = sortByUsage,
            usageStats = usage,
        )

    /**
     * Folder tokens only exist in [orderedPackages]; apps in the "tail" are not, so inserting a
     * single folder id can never land in the middle of the tail — only before/within the ordered
     * prefix. Rebuild [orderedPackages] from the current visible top-level slot order instead.
     */
    fun orderFromMaterializedGrid(): MutableList<String>? {
        val cellAt = cellsBefore.getOrNull(targetSlotIndex) ?: return null
        if (cellAt !is DrawerGridCell.App || cellAt.entry.packageName != packageName) return null
        val slotIds = cellsBefore.map { it.slotId }.toMutableList()
        slotIds[targetSlotIndex] = id
        val cleaned = slotIds.filter { tok ->
            when {
                FolderIds.isFolderId(tok) -> folders.containsKey(tok)
                else -> !folderMembers.contains(tok)
            }
        }.toMutableList()
        val verifyCells = cellsForTrial(cleaned)
        val folderIdx = verifyCells.indexOfFirst { cell ->
            cell is DrawerGridCell.Folder && cell.id == id
        }
        return if (folderIdx == targetSlotIndex) cleaned else null
    }

    fun orderByInsertScan(): MutableList<String> {
        var best: MutableList<String>? = null
        var bestDist = Int.MAX_VALUE
        for (insertAt in 0..order.size) {
            val trial = order.toMutableList().apply { add(insertAt, id) }
            val cells = cellsForTrial(trial)
            val folderIdx = cells.indexOfFirst { cell ->
                cell is DrawerGridCell.Folder && cell.id == id
            }
            if (folderIdx == targetSlotIndex) return trial
            if (folderIdx >= 0) {
                val d = kotlin.math.abs(folderIdx - targetSlotIndex)
                if (d < bestDist) {
                    bestDist = d
                    best = trial
                }
            }
        }
        return best ?: order.toMutableList().apply { add(id) }
    }

    val finalOrder: MutableList<String> = when {
        targetSlotIndex >= 0 -> orderFromMaterializedGrid() ?: orderByInsertScan()
        posBeforeRemove >= 0 -> order.toMutableList().apply { add(posBeforeRemove, id) }
        else -> order.toMutableList().apply { add(id) }
    }

    prefsRepo.writeGridState(finalOrder, folders, names)
}

/** Shared "Add to [existing folder]" implementation — see [createFolderFromApp]'s doc. */
internal suspend fun applyAddAppToFolder(
    prefsRepo: LauncherPrefsRepository,
    installed: List<AppEntry>,
    packageName: String,
    folderId: String,
    strictAlphabetical: Boolean,
) {
    if (!FolderIds.isFolderId(folderId) || packageName == AppsRepository.INTERNAL_SETTINGS_PACKAGE) return
    val snap = prefsRepo.prefsFlow.first()
    val folders = snap.folderContents
        .mapValues { (_, m) -> m.filter { it != packageName } }
        .filterValues { it.isNotEmpty() }
        .toMutableMap()
    val order = snap.orderedPackages
        .filter { tok -> !FolderIds.isFolderId(tok) || folders.containsKey(tok) }
        .toMutableList()
    order.removeAll { it == packageName }
    val list = folders.getOrElse(folderId) { emptyList() }.toMutableList()
    if (packageName !in list) list.add(packageName)
    folders[folderId] = list
    val names = snap.folderNames.filterKeys { folders.containsKey(it) }
    val finalOrder = if (strictAlphabetical) {
        strictAlphabeticalDrawerOrder(
            installed = installed,
            folderContents = folders,
            folderNames = names,
        )
    } else {
        order
    }
    prefsRepo.writeGridState(finalOrder, folders, names)
}
