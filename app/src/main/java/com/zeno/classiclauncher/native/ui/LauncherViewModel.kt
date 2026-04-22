@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package com.zeno.classiclauncher.nlauncher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.pm.LauncherApps
import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import com.zeno.classiclauncher.nlauncher.apps.AppsRepository
import com.zeno.classiclauncher.nlauncher.apps.LauncherActions
import com.zeno.classiclauncher.nlauncher.apps.homeShortcutStorageToken
import com.zeno.classiclauncher.nlauncher.apps.parseHomeShortcutToken
import com.zeno.classiclauncher.nlauncher.folders.DrawerGridCell
import com.zeno.classiclauncher.nlauncher.folders.FolderIds
import com.zeno.classiclauncher.nlauncher.folders.buildDrawerGridCells
import com.zeno.classiclauncher.nlauncher.badges.NotificationRepository
import com.zeno.classiclauncher.nlauncher.prefs.GridPreset
import com.zeno.classiclauncher.nlauncher.prefs.GlanceCalendarRange
import com.zeno.classiclauncher.nlauncher.prefs.GlanceWeatherLocationMode
import com.zeno.classiclauncher.nlauncher.prefs.GlanceWeatherUnit
import com.zeno.classiclauncher.nlauncher.prefs.HomeGroup
import com.zeno.classiclauncher.nlauncher.prefs.HomeGroupIds
import com.zeno.classiclauncher.nlauncher.prefs.HomeGroupSide
import com.zeno.classiclauncher.nlauncher.prefs.AppIconShape
import com.zeno.classiclauncher.nlauncher.prefs.DockIconStyle
import com.zeno.classiclauncher.nlauncher.prefs.LauncherPrefs
import com.zeno.classiclauncher.nlauncher.prefs.LauncherPrefsRepository
import com.zeno.classiclauncher.nlauncher.prefs.STRIP_TOTAL_SLOTS
import com.zeno.classiclauncher.nlauncher.prefs.canAddHomeStripItem
import com.zeno.classiclauncher.nlauncher.prefs.effectiveHomeStripOrder
import com.zeno.classiclauncher.nlauncher.prefs.effectiveHomeStripSlotOrder
import com.zeno.classiclauncher.nlauncher.prefs.MailBadgeCandidates
import com.zeno.classiclauncher.nlauncher.prefs.SecondShortcutTarget
import com.zeno.classiclauncher.nlauncher.prefs.DEFAULT_THEME_JSON
import com.zeno.classiclauncher.nlauncher.prefs.LauncherBackup
import com.zeno.classiclauncher.nlauncher.theme.LauncherThemePalette
import com.zeno.classiclauncher.nlauncher.usage.UsageStatsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.text.Collator
import java.util.Locale
import kotlin.math.abs

private val PRIVATE_PREFIX_REGEX = Regex("^private\\s+", RegexOption.IGNORE_CASE)

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val prefsRepo = LauncherPrefsRepository(app.applicationContext)
    private val appsRepo = AppsRepository(app.applicationContext)
    private val actions = LauncherActions(app.applicationContext)

    val prefs: StateFlow<LauncherPrefs> =
        prefsRepo.prefsFlow
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, LauncherPrefs())

    /** Incremented each time the end-call / red button is pressed — UI observes and navigates home. */
    private val _navigateHomeEvent = MutableStateFlow(0)
    val navigateHomeEvent: StateFlow<Int> = _navigateHomeEvent.asStateFlow()

    fun requestNavigateHome() { _navigateHomeEvent.value++ }

    /** Window lost focus (e.g. system notification / QS shade) — UI closes launcher quick settings overlay. */
    private val _dismissLauncherQsEvent = MutableStateFlow(0)
    val dismissLauncherQsEvent: StateFlow<Int> = _dismissLauncherQsEvent.asStateFlow()
    private val _newAppAddedToast = MutableStateFlow<String?>(null)
    val newAppAddedToast: StateFlow<String?> = _newAppAddedToast.asStateFlow()

    fun requestDismissLauncherQuickSettings() {
        _dismissLauncherQsEvent.value++
    }
    fun consumeNewAppAddedToast() {
        _newAppAddedToast.value = null
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _reorderMode = MutableStateFlow(false)
    private val _movingPackage = MutableStateFlow<String?>(null)
    private val _sortByUsage = MutableStateFlow(false)
    val sortByUsage: StateFlow<Boolean> = _sortByUsage.asStateFlow()

    fun toggleSortByUsage() {
        val next = !_sortByUsage.value
        _sortByUsage.value = next
        if (next) {
            refreshUsageStats()
        } else {
            // Stop showing usage ordering data when sorting is disabled.
            _usageStats.value = emptyMap()
        }
    }

    val apps: StateFlow<List<AppEntry>> =
        appsRepo.appsFlow()
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun hasUsagePermission(): Boolean = UsageStatsRepository.hasPermission(getApplication())

    private val _usageStats = MutableStateFlow<Map<String, Long>>(emptyMap())
    val usageStats: StateFlow<Map<String, Long>> = _usageStats.asStateFlow()
    private fun isStrictDrawerAlphabeticalMode(): Boolean = !_reorderMode.value && !_sortByUsage.value

    /** Force an immediate refresh of usage stats (call after returning from Usage Access settings). */
    fun refreshUsageStats() {
        viewModelScope.launch {
            _usageStats.value = UsageStatsRepository.getLast24hUsage(getApplication())
        }
    }

    private val _baseCells = combine(apps, prefs, _searchQuery) { list, pr, q ->
        val query = q.trim()
        val ql = query.lowercase()
        val (privateMode, privateQuery) = when {
            ql == "private" -> true to ""
            PRIVATE_PREFIX_REGEX.containsMatchIn(query) -> true to PRIVATE_PREFIX_REGEX.replaceFirst(query, "").trim()
            else -> false to ""
        }
        val normalQuery = if (privateMode) "" else query
        buildDrawerGridCells(
            installed = list,
            orderedPackages = pr.orderedPackages,
            folderContents = pr.folderContents,
            folderNames = pr.folderNames,
            hiddenPackages = pr.hiddenPackages,
            privateMode = privateMode,
            privateQuery = privateQuery,
            normalQuery = normalQuery,
        )
    }

    val filteredGridCells: StateFlow<List<DrawerGridCell>> =
        combine(_baseCells, usageStats, _sortByUsage) { cells, usage, sort ->
            pinInternalSettingsFirst(
                if (sort && usage.isNotEmpty()) {
                    cells.sortedByDescending { cell ->
                        when (cell) {
                            is DrawerGridCell.App -> usage[cell.entry.packageName] ?: 0L
                            is DrawerGridCell.Folder -> cell.members.maxOfOrNull { usage[it.packageName] ?: 0L } ?: 0L
                        }
                    }
                } else {
                    cells
                },
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hasUnreadMail: StateFlow<Boolean> = NotificationRepository.hasUnreadMail
    val hasUnreadSms: StateFlow<Boolean> = NotificationRepository.hasUnreadSms
    val hasUnreadWhatsApp: StateFlow<Boolean> = NotificationRepository.hasUnreadWhatsApp
    val packagesWithUnread: StateFlow<Set<String>> = NotificationRepository.packagesWithUnread
    private var lastPrunePackageSnapshot: Set<String>? = null
    private var lastObservedInstalledPackages: Set<String>? = null

    /** Serializes read–modify–write on drawer grid + home groups so rapid folder edits and prune cannot clobber each other. */
    private val gridHomeMutex = Mutex()

    init {
        viewModelScope.launch {
            combine(apps, prefsRepo.prefsFlow) { list, p -> list to p }
                .debounce(150)
                .collect { (list, p) ->
                    // [apps] starts as emptyList() until AppsRepository emits; pruning with an empty set
                    // would wipe all folder members and home-group apps (persisted empty).
                    if (list.isEmpty()) {
                        lastPrunePackageSnapshot = null
                        return@collect
                    }
                    val pkgs = list.mapTo(HashSet(list.size)) { it.packageName }
                    // Guard against transient package snapshots (PackageManager churn) that can otherwise wipe
                    // whole folders/groups. Only prune after the same non-empty snapshot repeats.
                    if (lastPrunePackageSnapshot != pkgs) {
                        lastPrunePackageSnapshot = pkgs
                        return@collect
                    }
                    val newFolders = HashMap<String, List<String>>(p.folderContents.size)
                    for ((id, members) in p.folderContents) {
                        val filtered = members.filter { it in pkgs }
                        if (filtered.isNotEmpty()) newFolders[id] = filtered
                    }
                    val newNames = p.folderNames.filterKeys { newFolders.containsKey(it) }
                    val newOrder = if (isStrictDrawerAlphabeticalMode()) {
                        strictAlphabeticalDrawerOrder(
                            installed = list,
                            folderContents = newFolders,
                            folderNames = newNames,
                        )
                    } else {
                        p.orderedPackages.filter { token ->
                            !FolderIds.isFolderId(token) || newFolders.containsKey(token)
                        }
                    }
                    val pruned = p.homeGroups.map { g ->
                        g.copy(packageNames = g.packageNames.filter { it in pkgs })
                    }
                    val gridChanged =
                        newFolders != p.folderContents || newOrder != p.orderedPackages || newNames != p.folderNames
                    val groupsChanged = pruned != p.homeGroups
                    if (gridChanged || groupsChanged) {
                        gridHomeMutex.withLock {
                            prefsRepo.writeGridAndHomeGroupsState(newOrder, newFolders, newNames, pruned)
                        }
                    }
                }
        }
        viewModelScope.launch {
            apps
                .debounce(250)
                .collect { list ->
                    if (list.isEmpty()) {
                        lastObservedInstalledPackages = null
                        return@collect
                    }
                    val pkgs = list.mapTo(HashSet(list.size)) { it.packageName }
                    val prev = lastObservedInstalledPackages
                    lastObservedInstalledPackages = pkgs
                    if (prev == null || prev == pkgs) return@collect
                    val added = pkgs - prev
                    if (added.isEmpty()) return@collect

                    val byPkg = list.associateBy { it.packageName }
                    val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
                    val addedLabel = added
                        .mapNotNull { pkg -> byPkg[pkg]?.label }
                        .minWithOrNull { a, b -> collator.compare(a, b) }
                    if (!addedLabel.isNullOrBlank()) {
                        _newAppAddedToast.value = "New app added: $addedLabel"
                    }

                    if (!isStrictDrawerAlphabeticalMode()) return@collect
                    gridHomeMutex.withLock {
                        val snap = prefs.value
                        val folders = snap.folderContents
                            .mapValues { (_, members) -> members.filter { it in pkgs } }
                            .filterValues { it.isNotEmpty() }
                        val names = snap.folderNames.filterKeys { folders.containsKey(it) }
                        val finalOrder = strictAlphabeticalDrawerOrder(
                            installed = list,
                            folderContents = folders,
                            folderNames = names,
                        )
                        val changed =
                            finalOrder != snap.orderedPackages || folders != snap.folderContents || names != snap.folderNames
                        if (changed) {
                            prefsRepo.writeGridState(finalOrder, folders, names)
                        }
                    }
                }
        }
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    val isReorderMode: StateFlow<Boolean> = _reorderMode.asStateFlow()
    val moving: StateFlow<String?> = _movingPackage.asStateFlow()

    private val _reorderFromHomeStrip = MutableStateFlow(false)

    fun toggleReorderMode() {
        _reorderMode.value = !_reorderMode.value
        if (!_reorderMode.value) {
            _movingPackage.value = null
            _reorderFromHomeStrip.value = false
        }
    }

    fun startMove(packageName: String) {
        _movingPackage.value = packageName
        _reorderFromHomeStrip.value = false
    }

    /** Drag/reorder gesture started from the home strip (slot tokens, not drawer grid). */
    fun startStripMove(token: String) {
        _movingPackage.value = token
        _reorderFromHomeStrip.value = true
    }

    fun clearMove() {
        _movingPackage.value = null
        _reorderFromHomeStrip.value = false
    }

    fun finishReorderDrop(targetPackage: String?) {
        if (_reorderFromHomeStrip.value) {
            finishHomeStripReorderDrop(targetPackage)
            return
        }
        val moving = _movingPackage.value ?: return
        if (targetPackage == null || targetPackage == moving) {
            clearMove()
            return
        }
        if (FolderIds.isFolderId(targetPackage) && !FolderIds.isFolderId(moving)) {
            viewModelScope.launch { addAppToFolder(moving, targetPackage) }
            clearMove()
            return
        }
        // clearMove() is intentionally called AFTER setOrderedPackages so that movingSlotId
        // stays non-null until the new order is committed. This prevents displaySlice from
        // reverting to the old grid order before gridCells reflects the new order.
        viewModelScope.launch {
            gridHomeMutex.withLock {
                val currentOrder = prefs.value.orderedPackages.toMutableList()
                if (!currentOrder.contains(moving)) currentOrder.add(moving)
                if (!currentOrder.contains(targetPackage)) currentOrder.add(targetPackage)
                val origMovingIdx = currentOrder.indexOf(moving)
                val origTargetIdx = currentOrder.indexOf(targetPackage)
                currentOrder.remove(moving)
                val targetIndex = currentOrder.indexOf(targetPackage).coerceAtLeast(0)
                // For forward drags the target shifted left by 1 after removal; +1 restores it to
                // its original slot, matching what displaySlice showed during the drag preview.
                val insertIndex = if (origMovingIdx < origTargetIdx) targetIndex + 1 else targetIndex
                currentOrder.add(insertIndex.coerceAtMost(currentOrder.size), moving)
                prefsRepo.setOrderedPackages(currentOrder)
            }
            clearMove()
        }
    }

    private fun finishHomeStripReorderDrop(targetKey: String?) {
        val moving = _movingPackage.value ?: return
        if (targetKey == null || targetKey == moving || targetKey.startsWith("strip_empty_")) {
            clearMove()
            return
        }
        viewModelScope.launch {
            gridHomeMutex.withLock {
                val snap = prefs.value
                val slots = snap.effectiveHomeStripSlotOrder().toMutableList()
                if (slots.size != STRIP_TOTAL_SLOTS) return@withLock
                val fromIdx = slots.indexOfFirst { it == moving }
                val toIdx = slots.indexOfFirst { it == targetKey }
                if (fromIdx < 0 || toIdx < 0 || fromIdx == toIdx) return@withLock
                val tmp = slots[fromIdx]
                slots[fromIdx] = slots[toIdx]
                slots[toIdx] = tmp
                prefsRepo.setHomeStripSlots(slots)
                prefsRepo.setHomeStripOrder(slots.filterNotNull())
            }
            clearMove()
        }
    }

    fun moveTo(targetSlotId: String) {
        if (_reorderFromHomeStrip.value) return
        val moving = _movingPackage.value ?: return
        if (moving == targetSlotId) return

        if (FolderIds.isFolderId(targetSlotId) && !FolderIds.isFolderId(moving)) {
            viewModelScope.launch { addAppToFolder(moving, targetSlotId) }
            return
        }

        viewModelScope.launch {
            gridHomeMutex.withLock {
                val currentOrder = prefs.value.orderedPackages.toMutableList()
                if (!currentOrder.contains(moving)) currentOrder.add(moving)
                if (!currentOrder.contains(targetSlotId)) currentOrder.add(targetSlotId)
                val origMovingIdx = currentOrder.indexOf(moving)
                val origTargetIdx = currentOrder.indexOf(targetSlotId)
                currentOrder.remove(moving)
                val targetIndex = currentOrder.indexOf(targetSlotId).coerceAtLeast(0)
                val insertIndex = if (origMovingIdx < origTargetIdx) targetIndex + 1 else targetIndex
                currentOrder.add(insertIndex.coerceAtMost(currentOrder.size), moving)
                prefsRepo.setOrderedPackages(currentOrder)
            }
        }
    }

    /**
     * Creates a drawer group containing [packageName]. The new folder is placed at the **same visual index**
     * as that app’s tile (same grid slot, including Zeno settings pin and usage sort). When the app lived
     * in the drawer **tail** (not in [orderedPackages]), we rewrite [orderedPackages] from the visible
     * slot list so the folder can stay on later pages; otherwise a single-token insert can only place
     * folders before the tail, which wrongly pulled them onto page 1.
     *
     * @param visualCellIndex optional index from the UI; used only if the app is not found in a fresh
     * snapshot of the drawer (race edge case).
     */
    fun createFolderFromApp(packageName: String, title: String, visualCellIndex: Int = -1) {
        if (packageName == AppsRepository.INTERNAL_SETTINGS_PACKAGE) return
        viewModelScope.launch {
            gridHomeMutex.withLock {
            val snap = prefs.value
            val installed = apps.value
            val searchRaw = _searchQuery.value
            val sortUsage = _sortByUsage.value
            val usage = _usageStats.value

            val cellsBefore = filteredDrawerCellsSnapshot(
                installed = installed,
                orderedPackages = snap.orderedPackages.filter { tok ->
                    !FolderIds.isFolderId(tok) || snap.folderContents.containsKey(tok)
                },
                folderContents = snap.folderContents,
                folderNames = snap.folderNames.filterKeys { snap.folderContents.containsKey(it) },
                hiddenPackages = snap.hiddenPackages,
                searchQueryRaw = searchRaw,
                sortByUsage = sortUsage,
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

            if (isStrictDrawerAlphabeticalMode()) {
                val finalOrder = strictAlphabeticalDrawerOrder(
                    installed = installed,
                    folderContents = folders,
                    folderNames = names,
                )
                prefsRepo.writeGridState(finalOrder, folders, names)
                return@withLock
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
                    searchQueryRaw = searchRaw,
                    sortByUsage = sortUsage,
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
                        val d = abs(folderIdx - targetSlotIndex)
                        if (d < bestDist) {
                            bestDist = d
                            best = trial
                        }
                    }
                }
                return best ?: order.toMutableList().apply { add(id) }
            }

            val finalOrder: MutableList<String> = when {
                targetSlotIndex >= 0 -> {
                    orderFromMaterializedGrid() ?: orderByInsertScan()
                }
                posBeforeRemove >= 0 -> order.toMutableList().apply { add(posBeforeRemove, id) }
                else -> order.toMutableList().apply { add(id) }
            }

            prefsRepo.writeGridState(finalOrder, folders, names)
            }
        }
    }

    fun renameFolder(folderId: String, newTitle: String) {
        if (!FolderIds.isFolderId(folderId)) return
        viewModelScope.launch {
            gridHomeMutex.withLock {
            val snap = prefs.value
            if (folderId !in snap.folderContents.keys) return@withLock
            val label = newTitle.trim().ifBlank { "Folder" }
            val names = snap.folderNames.toMutableMap()
            names[folderId] = label
            val prunedNames = names.filterKeys { snap.folderContents.containsKey(it) }
            val finalOrder = if (isStrictDrawerAlphabeticalMode()) {
                strictAlphabeticalDrawerOrder(
                    installed = apps.value,
                    folderContents = snap.folderContents,
                    folderNames = prunedNames,
                )
            } else {
                snap.orderedPackages
            }
            prefsRepo.writeGridState(
                finalOrder,
                snap.folderContents,
                prunedNames,
            )
            }
        }
    }

    fun addAppToFolder(packageName: String, folderId: String) {
        if (!FolderIds.isFolderId(folderId) || packageName == AppsRepository.INTERNAL_SETTINGS_PACKAGE) return
        viewModelScope.launch {
            gridHomeMutex.withLock {
            val snap = prefs.value
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
            val finalOrder = if (isStrictDrawerAlphabeticalMode()) {
                strictAlphabeticalDrawerOrder(
                    installed = apps.value,
                    folderContents = folders,
                    folderNames = names,
                )
            } else {
                order
            }
            prefsRepo.writeGridState(finalOrder, folders, names)
            }
        }
    }

    fun removeAppFromFolder(packageName: String, folderId: String) {
        if (!FolderIds.isFolderId(folderId)) return
        viewModelScope.launch {
            gridHomeMutex.withLock {
            val snap = prefs.value
            val folders = snap.folderContents.toMutableMap()
            val list = folders[folderId]?.toMutableList() ?: return@withLock
            list.remove(packageName)
            val order = snap.orderedPackages.toMutableList()
            var names = snap.folderNames.filterKeys { folders.containsKey(it) }
            if (list.isEmpty()) {
                folders.remove(folderId)
                order.removeAll { it == folderId }
                names = names.filterKeys { it != folderId }
            } else {
                folders[folderId] = list
            }
            order.removeAll { it == packageName }
            if (isStrictDrawerAlphabeticalMode()) {
                val finalOrder = strictAlphabeticalDrawerOrder(
                    installed = apps.value,
                    folderContents = folders,
                    folderNames = names,
                )
                prefsRepo.writeGridState(finalOrder, folders, names)
                return@withLock
            }
            val usageReorderActive = _sortByUsage.value && _usageStats.value.isNotEmpty()
            if (usageReorderActive) {
                order.add(packageName)
            } else {
                when (
                    val insertAt = alphabeticalDrawerInsertIndex(
                        orderWithoutApp = order,
                        packageName = packageName,
                        installed = apps.value,
                        folderContents = folders,
                        folderNames = names,
                        hiddenPackages = snap.hiddenPackages,
                    )
                ) {
                    -1 -> Unit // Not in orderedPackages: appears in tail with correct A→Z among unplaced apps.
                    else -> order.add(insertAt, packageName)
                }
            }
            prefsRepo.writeGridState(order, folders, names)
            }
        }
    }

    fun dissolveFolder(folderId: String) {
        if (!FolderIds.isFolderId(folderId)) return
        viewModelScope.launch {
            gridHomeMutex.withLock {
            val snap = prefs.value
            val members = snap.folderContents[folderId] ?: return@withLock
            val folders = snap.folderContents.filterKeys { it != folderId }
            val names = snap.folderNames.filterKeys { folders.containsKey(it) }
            if (isStrictDrawerAlphabeticalMode()) {
                val finalOrder = strictAlphabeticalDrawerOrder(
                    installed = apps.value,
                    folderContents = folders,
                    folderNames = names,
                )
                prefsRepo.writeGridState(finalOrder, folders, names)
                return@withLock
            }
            val order = snap.orderedPackages.toMutableList()
            val fi = order.indexOf(folderId)
            order.removeAll { it == folderId }
            if (fi >= 0) {
                order.addAll(fi, members)
            } else {
                members.forEach { order.add(it) }
            }
            prefsRepo.writeGridState(order, folders, names)
            }
        }
    }

    fun foldersForAddMenu(): List<Pair<String, String>> {
        val byPkg = apps.value.associateBy { it.packageName }
        return prefs.value.folderContents.mapNotNull { (id, members) ->
            if (members.isEmpty()) null
            else {
                val custom = prefs.value.folderNames[id]?.trim()?.takeIf { it.isNotEmpty() }
                val label = custom ?: (members.firstNotNullOfOrNull { byPkg[it]?.label } ?: "Folder")
                id to label
            }
        }
    }

    fun createHomeGroup(title: String) {
        viewModelScope.launch {
            gridHomeMutex.withLock {
            if (!prefs.value.canAddHomeStripItem()) return@withLock
            val label = title.trim().ifBlank { "Group" }
            val usedSides = prefs.value.homeGroups.map { it.side }.toSet()
            val side = HomeGroupSide.entries.firstOrNull { it !in usedSides } ?: HomeGroupSide.RIGHT
            val id = HomeGroupIds.newId()
            val next = prefs.value.homeGroups + HomeGroup(
                id = id,
                title = label,
                packageNames = emptyList(),
                side = side,
            )
            prefsRepo.setHomeGroups(next)
            }
        }
    }

    fun setHomeGroupSide(groupId: String, side: HomeGroupSide) {
        viewModelScope.launch {
            gridHomeMutex.withLock {
            if (!HomeGroupIds.isHomeGroupId(groupId)) return@withLock
            val groups = prefs.value.homeGroups
            val target = groups.find { it.id == groupId } ?: return@withLock
            if (target.side == side) return@withLock
            val other = groups.firstOrNull { it.id != groupId && it.side == side }
            val next = groups.map { g ->
                when {
                    g.id == groupId -> g.copy(side = side)
                    other != null && g.id == other.id -> g.copy(side = target.side)
                    else -> g
                }
            }
            prefsRepo.setHomeGroups(next)
            }
        }
    }

    fun renameHomeGroup(groupId: String, newTitle: String) {
        if (!HomeGroupIds.isHomeGroupId(groupId)) return
        viewModelScope.launch {
            gridHomeMutex.withLock {
            val label = newTitle.trim().ifBlank { "Group" }
            val next = prefs.value.homeGroups.map { g ->
                if (g.id == groupId) g.copy(title = label) else g
            }
            if (next != prefs.value.homeGroups) prefsRepo.setHomeGroups(next)
            }
        }
    }

    fun deleteHomeGroup(groupId: String) {
        if (!HomeGroupIds.isHomeGroupId(groupId)) return
        viewModelScope.launch {
            gridHomeMutex.withLock {
            prefsRepo.setHomeGroups(prefs.value.homeGroups.filter { it.id != groupId })
            }
        }
    }

    fun addPackageToHomeGroup(packageName: String, groupId: String) {
        if (!HomeGroupIds.isHomeGroupId(groupId) || packageName == AppsRepository.INTERNAL_SETTINGS_PACKAGE) return
        viewModelScope.launch {
            gridHomeMutex.withLock {
            val next = prefs.value.homeGroups.map { g ->
                if (g.id != groupId) {
                    g
                } else {
                    val list = g.packageNames.toMutableList()
                    if (packageName !in list) list.add(packageName)
                    g.copy(packageNames = list)
                }
            }
            prefsRepo.setHomeGroups(next)
            }
        }
    }

    fun removePackageFromHomeGroup(packageName: String, groupId: String) {
        if (!HomeGroupIds.isHomeGroupId(groupId)) return
        viewModelScope.launch {
            gridHomeMutex.withLock {
            val next = prefs.value.homeGroups.map { g ->
                if (g.id != groupId) g else g.copy(packageNames = g.packageNames.filter { it != packageName })
            }
            prefsRepo.setHomeGroups(next)
            }
        }
    }

    fun setHidden(packageName: String, hidden: Boolean) {
        viewModelScope.launch { prefsRepo.setHidden(packageName, hidden) }
    }

    fun setGridPreset(preset: GridPreset) {
        viewModelScope.launch { prefsRepo.setGridPreset(preset) }
    }

    fun setSecondShortcutTarget(target: SecondShortcutTarget) {
        viewModelScope.launch { prefsRepo.setSecondShortcutTarget(target) }
    }

    fun cycleMailBadgePackage() {
        viewModelScope.launch {
            val candidates = MailBadgeCandidates.installedCandidates(apps.value)
            val options = (listOf("") + candidates.map { it.packageName })
            val cur = prefs.value.mailBadgePackage
            val idx = options.indexOf(cur).takeIf { it >= 0 } ?: 0
            val next = options[(idx + 1) % options.size]
            prefsRepo.setMailBadgePackage(next)
        }
    }

    fun launchFromDock(slot: DockSlot) {
        when (slot) {
            DockSlot.Mail -> {
                val pkg = prefs.value.dockMailPackage.trim()
                if (pkg.isEmpty()) {
                    actions.launchMail()
                } else {
                    if (!actions.launchApp(pkg)) actions.launchMail()
                }
            }
            DockSlot.Shortcut -> {
                val pkg = prefs.value.dockSecondPackage.trim()
                if (pkg.isEmpty()) {
                    val target = prefs.value.secondShortcutTarget
                    if (target == SecondShortcutTarget.WHATSAPP) {
                        if (!actions.launchApp("com.whatsapp")) actions.launchMessages()
                    } else {
                        actions.launchMessages()
                    }
                } else {
                    if (!actions.launchApp(pkg)) actions.launchMessages()
                }
            }
            DockSlot.Camera -> {
                val pkg = prefs.value.dockCameraPackage.trim()
                if (pkg.isEmpty()) {
                    actions.launchCamera()
                } else {
                    if (!actions.launchApp(pkg)) actions.launchCamera()
                }
            }
        }
    }

    fun setDockCameraPackage(packageName: String) {
        viewModelScope.launch { prefsRepo.setDockCameraPackage(packageName.trim()) }
    }

    fun setDockMailPackage(packageName: String) {
        viewModelScope.launch { prefsRepo.setDockMailPackage(packageName.trim()) }
    }

    fun setDockSecondPackage(packageName: String) {
        viewModelScope.launch { prefsRepo.setDockSecondPackage(packageName.trim()) }
    }

    fun setDockSecondEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDockSecondEnabled(enabled) }
    }

    fun setDockSlotTitle(slot: DockSlot, title: String) {
        viewModelScope.launch {
            when (slot) {
                DockSlot.Mail -> prefsRepo.setDockMailTitle(title)
                DockSlot.Shortcut -> prefsRepo.setDockSecondTitle(title)
                DockSlot.Camera -> prefsRepo.setDockThirdTitle(title)
            }
        }
    }

    fun setDockIconStyle(style: DockIconStyle) {
        viewModelScope.launch { prefsRepo.setDockIconStyle(style) }
    }

    fun addHomeShortcut(packageName: String) {
        viewModelScope.launch {
            val snap = prefs.value
            val pkg = packageName.trim()
            if (pkg.isEmpty()) return@launch
            val cur = snap.homeShortcutPackages
            if (!snap.canAddHomeStripItem() || cur.contains(pkg)) return@launch
            prefsRepo.setHomeShortcutPackages(cur + pkg)
        }
    }

    fun removeHomeShortcutToken(token: String) {
        viewModelScope.launch {
            val snap = prefs.value
            val t = token.trim()
            if (t.isEmpty()) return@launch
            prefsRepo.setHomeShortcutPackages(snap.homeShortcutPackages.filter { it != t })
        }
    }

    fun moveHomeShortcut(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val snap = prefs.value
            val list = snap.homeShortcutPackages.toMutableList()
            if (fromIndex !in list.indices || toIndex !in list.indices) return@launch
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            prefsRepo.setHomeShortcutPackages(list)
        }
    }

    fun launchHomeShortcutFromToken(token: String) {
        val (pkg, sid) = parseHomeShortcutToken(token)
        if (pkg.isEmpty()) return
        if (sid != null) {
            if (actions.startShortcut(pkg, sid)) return
        }
        actions.launchApp(pkg)
    }

    suspend fun consumePinShortcutRequest(request: LauncherApps.PinItemRequest): Boolean {
        if (request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) return false
        val info = request.shortcutInfo ?: return false
        val token = homeShortcutStorageToken(info.`package`, info.id)
        val cur = prefs.value.homeShortcutPackages
        if (!prefs.value.canAddHomeStripItem()) return false
        if (token in cur) return true
        prefsRepo.setHomeShortcutPackages(cur + token)
        return true
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setHapticsEnabled(enabled) }
    }

    fun setHapticIntensity(intensity: Int) {
        viewModelScope.launch { prefsRepo.setHapticIntensity(intensity) }
    }

    fun setGlanceEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setGlanceEnabled(enabled) }
    }

    fun setGlanceShowFlashlight(show: Boolean) {
        viewModelScope.launch { prefsRepo.setGlanceShowFlashlight(show) }
    }

    fun setGlanceShowBattery(show: Boolean) {
        viewModelScope.launch { prefsRepo.setGlanceShowBattery(show) }
    }

    fun setGlanceShowCalendar(show: Boolean) {
        viewModelScope.launch { prefsRepo.setGlanceShowCalendar(show) }
    }

    fun setGlanceShowAlarm(show: Boolean) {
        viewModelScope.launch { prefsRepo.setGlanceShowAlarm(show) }
    }

    fun setGlanceCalendarRange(range: GlanceCalendarRange) {
        viewModelScope.launch { prefsRepo.setGlanceCalendarRange(range) }
    }

    fun setGlanceWeatherUnit(unit: GlanceWeatherUnit) {
        viewModelScope.launch { prefsRepo.setGlanceWeatherUnit(unit) }
    }

    fun setGlanceWeatherLocationMode(mode: GlanceWeatherLocationMode) {
        viewModelScope.launch { prefsRepo.setGlanceWeatherLocationMode(mode) }
    }

    fun setGlanceWeatherManualLatitude(latitude: String) {
        viewModelScope.launch { prefsRepo.setGlanceWeatherManualLatitude(latitude) }
    }

    fun setGlanceWeatherManualLongitude(longitude: String) {
        viewModelScope.launch { prefsRepo.setGlanceWeatherManualLongitude(longitude) }
    }

    fun launchApp(packageName: String) {
        NotificationRepository.clearForPackage(packageName)
        actions.launchApp(packageName)
    }

    fun openAppInfo(packageName: String) {
        actions.openAppInfo(packageName)
    }

    fun exportAppOrder(): List<String> = prefs.value.orderedPackages

    fun importAppOrder(packages: List<String>) {
        viewModelScope.launch {
            gridHomeMutex.withLock {
            val snap = prefs.value
            val names = snap.folderNames.filterKeys { snap.folderContents.containsKey(it) }
            prefsRepo.writeGridState(packages, snap.folderContents, names)
            }
        }
    }

    fun exportThemeJson(): String =
        LauncherThemePalette.toExportJson(LauncherThemePalette.fromJson(prefs.value.themeJson))

    fun importThemeJson(themeJson: String) {
        viewModelScope.launch {
            runCatching {
                JSONObject(themeJson.trim())
            }.onSuccess {
                LauncherThemePalette.fromJson(themeJson.trim())
                prefsRepo.setThemeJson(themeJson.trim())
            }
        }
    }

    fun resetTheme() {
        viewModelScope.launch { prefsRepo.setThemeJson(DEFAULT_THEME_JSON) }
    }

    fun setNotificationBadgesEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setNotificationBadgesEnabled(enabled) }
    }

    fun setDoubleTapToSleepEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDoubleTapToSleepEnabled(enabled) }
    }

    fun setSwipeUpPackage(pkg: String) {
        viewModelScope.launch { prefsRepo.setSwipeUpPackage(pkg) }
    }

    fun setDoubleTapPackage(pkg: String) {
        viewModelScope.launch { prefsRepo.setDoubleTapPackage(pkg) }
    }

    fun setShowUsageStatsBadge(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setShowUsageStatsBadge(enabled) }
    }

    fun setShowIconNotifBadge(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setShowIconNotifBadge(enabled) }
    }

    fun setShowShortcutApps(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setShowShortcutApps(enabled) }
    }

    fun setShowHomeGroups(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setShowHomeGroups(enabled) }
    }

    fun setHomeStripEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setHomeStripEnabled(enabled) }
    }

    fun setCustomQuickSettingsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setCustomQuickSettingsEnabled(enabled) }
    }

    fun setClassicMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setClassicMode(enabled) }
    }

    fun setAppIconShape(shape: AppIconShape) {
        viewModelScope.launch { prefsRepo.setAppIconShape(shape) }
    }

    fun setShowAppCardBackground(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setShowAppCardBackground(enabled) }
    }

    fun setSwipeDownAppSpotlight(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setSwipeDownAppSpotlight(enabled) }
    }

    fun exportBackupJson(): String = LauncherBackup.toJson(prefs.value)

    fun importBackupJson(json: String): Boolean {
        val restored = LauncherBackup.fromJson(json).getOrNull() ?: return false
        viewModelScope.launch { prefsRepo.applyFullBackup(restored) }
        return true
    }

}

private fun strictAlphabeticalDrawerOrder(
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
private fun pinInternalSettingsFirst(cells: List<DrawerGridCell>): List<DrawerGridCell> {
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
private fun filteredDrawerCellsSnapshot(
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
private fun alphabeticalDrawerInsertIndex(
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

enum class DockSlot { Mail, Shortcut, Camera }
