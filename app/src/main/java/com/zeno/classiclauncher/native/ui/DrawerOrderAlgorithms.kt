package com.zeno.classiclauncher.nlauncher.ui

import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import com.zeno.classiclauncher.nlauncher.apps.AppsRepository
import com.zeno.classiclauncher.nlauncher.folders.DrawerGridCell
import com.zeno.classiclauncher.nlauncher.folders.buildDrawerGridCells
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
        val settingsFirst: Int,
    )

    val topLevelApps = installed
        .asSequence()
        .filter { it.packageName !in folderMemberPkgs }
        .map { app ->
            TokenSortKey(
                token = app.packageName,
                label = app.label,
                settingsFirst = if (app.packageName == AppsRepository.INTERNAL_SETTINGS_PACKAGE) 0 else 1,
            )
        }
        .toMutableList()

    val topLevelFolders = folderContents
        .asSequence()
        .filter { (_, members) -> members.isNotEmpty() }
        .map { (folderId, members) ->
            val custom = folderNames[folderId]?.trim()?.takeIf { it.isNotEmpty() }
            val fallback = members.firstNotNullOfOrNull { byPkg[it]?.label } ?: "Folder"
            TokenSortKey(
                token = folderId,
                label = custom ?: fallback,
                settingsFirst = 1,
            )
        }.toList()

    val allTopLevel = (topLevelApps + topLevelFolders).toMutableList()
    allTopLevel.sortWith { a, b ->
        when {
            a.settingsFirst != b.settingsFirst -> a.settingsFirst - b.settingsFirst
            else -> {
                val c = collator.compare(a.label, b.label)
                if (c != 0) c else a.token.compareTo(b.token)
            }
        }
    }
    return allTopLevel.map { it.token }
}

/** Zeno Classic settings is always the first drawer tile when it appears in the grid. */
internal fun pinInternalSettingsFirst(cells: List<DrawerGridCell>): List<DrawerGridCell> {
    val idx = cells.indexOfFirst { cell ->
        cell is DrawerGridCell.App && cell.entry.packageName == AppsRepository.INTERNAL_SETTINGS_PACKAGE
    }
    if (idx <= 0) return cells
    val settingsCell = cells[idx]
    return listOf(settingsCell) + cells.filterIndexed { i, _ -> i != idx }
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
    return pinInternalSettingsFirst(sorted)
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
    val targetIdx = baseCells.appKeysExcludingSettings().count { e ->
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
        val idx = trialCells.appKeysExcludingSettings().indexOfFirst { it.packageName == packageName }
        if (idx == targetIdx) return insertAt
    }
    return -1
}

private fun List<DrawerGridCell>.appKeysExcludingSettings(): List<AppEntry> = mapNotNull { cell ->
    when (cell) {
        is DrawerGridCell.App -> {
            if (cell.entry.packageName == AppsRepository.INTERNAL_SETTINGS_PACKAGE) null
            else cell.entry
        }
        is DrawerGridCell.Folder -> null
    }
}
