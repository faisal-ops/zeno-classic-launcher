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
import com.zeno.classiclauncher.nlauncher.prefs.HomeGroup
import com.zeno.classiclauncher.nlauncher.prefs.HomeGroupIds
import com.zeno.classiclauncher.nlauncher.prefs.HomeGroupSide
import com.zeno.classiclauncher.nlauncher.prefs.AppIconShape
import com.zeno.classiclauncher.nlauncher.prefs.LauncherPrefs
import com.zeno.classiclauncher.nlauncher.prefs.LauncherPrefsRepository
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
import org.json.JSONObject

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
            if (sort && usage.isNotEmpty()) {
                cells.sortedByDescending { cell ->
                    when (cell) {
                        is DrawerGridCell.App -> usage[cell.entry.packageName] ?: 0L
                        is DrawerGridCell.Folder -> cell.members.maxOfOrNull { usage[it.packageName] ?: 0L } ?: 0L
                    }
                }
            } else cells
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hasUnreadMail: StateFlow<Boolean> = NotificationRepository.hasUnreadMail
    val hasUnreadSms: StateFlow<Boolean> = NotificationRepository.hasUnreadSms
    val hasUnreadWhatsApp: StateFlow<Boolean> = NotificationRepository.hasUnreadWhatsApp
    val packagesWithUnread: StateFlow<Set<String>> = NotificationRepository.packagesWithUnread

    init {
        viewModelScope.launch {
            combine(apps, prefsRepo.prefsFlow) { list, p -> list to p }
                .debounce(150)
                .collect { (list, p) ->
                    // [apps] starts as emptyList() until AppsRepository emits; pruning with an empty set
                    // would wipe all folder members and home-group apps (persisted empty).
                    if (list.isEmpty()) return@collect
                    val pkgs = list.mapTo(HashSet(list.size)) { it.packageName }
                    val newFolders = HashMap<String, List<String>>(p.folderContents.size)
                    for ((id, members) in p.folderContents) {
                        val filtered = members.filter { it in pkgs }
                        if (filtered.isNotEmpty()) newFolders[id] = filtered
                    }
                    val newOrder = p.orderedPackages.filter { token ->
                        !FolderIds.isFolderId(token) || newFolders.containsKey(token)
                    }
                    val newNames = p.folderNames.filterKeys { newFolders.containsKey(it) }
                    val pruned = p.homeGroups.map { g ->
                        g.copy(packageNames = g.packageNames.filter { it in pkgs })
                    }
                    val gridChanged =
                        newFolders != p.folderContents || newOrder != p.orderedPackages || newNames != p.folderNames
                    val groupsChanged = pruned != p.homeGroups
                    if (gridChanged || groupsChanged) {
                        prefsRepo.writeGridAndHomeGroupsState(newOrder, newFolders, newNames, pruned)
                    }
                }
        }
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    val isReorderMode: StateFlow<Boolean> = _reorderMode.asStateFlow()
    val moving: StateFlow<String?> = _movingPackage.asStateFlow()

    fun toggleReorderMode() {
        _reorderMode.value = !_reorderMode.value
        if (!_reorderMode.value) _movingPackage.value = null
    }

    fun startMove(packageName: String) {
        _movingPackage.value = packageName
    }

    fun clearMove() {
        _movingPackage.value = null
    }

    fun finishReorderDrop(targetPackage: String?) {
        val moving = _movingPackage.value ?: return
        if (targetPackage != null && targetPackage != moving) {
            moveTo(targetPackage)
        }
        clearMove()
    }

    fun moveTo(targetSlotId: String) {
        val moving = _movingPackage.value ?: return
        if (moving == targetSlotId) return

        if (FolderIds.isFolderId(targetSlotId) && !FolderIds.isFolderId(moving)) {
            viewModelScope.launch { addAppToFolder(moving, targetSlotId) }
            return
        }

        val currentOrder = prefs.value.orderedPackages.toMutableList()
        if (!currentOrder.contains(moving)) currentOrder.add(moving)
        if (!currentOrder.contains(targetSlotId)) currentOrder.add(targetSlotId)

        currentOrder.remove(moving)
        val targetIndex = currentOrder.indexOf(targetSlotId).coerceAtLeast(0)
        currentOrder.add(targetIndex, moving)

        viewModelScope.launch { prefsRepo.setOrderedPackages(currentOrder) }
    }

    fun createFolderFromApp(packageName: String, title: String) {
        if (packageName == AppsRepository.INTERNAL_SETTINGS_PACKAGE) return
        viewModelScope.launch {
            val snap = prefs.value
            val id = FolderIds.newId()
            val folders = snap.folderContents
                .mapValues { (_, m) -> m.filter { it != packageName } }
                .filterValues { it.isNotEmpty() }
                .toMutableMap()
            val order = snap.orderedPackages
                .filter { tok -> !FolderIds.isFolderId(tok) || folders.containsKey(tok) }
                .toMutableList()
            val idx = order.indexOf(packageName)
            if (idx >= 0) order[idx] = id else order.add(id)
            folders[id] = listOf(packageName)
            val names = snap.folderNames
                .filterKeys { folders.containsKey(it) }
                .toMutableMap()
            val label = title.trim().ifBlank {
                apps.value.find { it.packageName == packageName }?.label?.let { "$it folder" } ?: "Folder"
            }
            names[id] = label
            prefsRepo.writeGridState(order, folders, names)
        }
    }

    fun renameFolder(folderId: String, newTitle: String) {
        if (!FolderIds.isFolderId(folderId)) return
        viewModelScope.launch {
            val snap = prefs.value
            if (folderId !in snap.folderContents.keys) return@launch
            val label = newTitle.trim().ifBlank { "Folder" }
            val names = snap.folderNames.toMutableMap()
            names[folderId] = label
            prefsRepo.writeGridState(
                snap.orderedPackages,
                snap.folderContents,
                names.filterKeys { snap.folderContents.containsKey(it) },
            )
        }
    }

    fun addAppToFolder(packageName: String, folderId: String) {
        if (!FolderIds.isFolderId(folderId) || packageName == AppsRepository.INTERNAL_SETTINGS_PACKAGE) return
        viewModelScope.launch {
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
            prefsRepo.writeGridState(order, folders, names)
        }
    }

    fun removeAppFromFolder(packageName: String, folderId: String) {
        if (!FolderIds.isFolderId(folderId)) return
        viewModelScope.launch {
            val snap = prefs.value
            val folders = snap.folderContents.toMutableMap()
            val list = folders[folderId]?.toMutableList() ?: return@launch
            list.remove(packageName)
            val order = snap.orderedPackages.toMutableList()
            var names = snap.folderNames.filterKeys { folders.containsKey(it) }
            if (list.isEmpty()) {
                folders.remove(folderId)
                order.removeAll { it == folderId }
                names = names.filterKeys { it != folderId }
            } else {
                folders[folderId] = list
                val fi = order.indexOf(folderId)
                if (fi >= 0) {
                    order.add(fi + 1, packageName)
                } else {
                    val existingMember = list.firstOrNull { it != packageName }
                    val anchor = existingMember?.let { order.indexOf(it) } ?: -1
                    if (anchor >= 0) order.add(anchor + 1, packageName) else order.add(packageName)
                }
            }
            prefsRepo.writeGridState(order, folders, names)
        }
    }

    fun dissolveFolder(folderId: String) {
        if (!FolderIds.isFolderId(folderId)) return
        viewModelScope.launch {
            val snap = prefs.value
            val members = snap.folderContents[folderId] ?: return@launch
            val folders = snap.folderContents.filterKeys { it != folderId }
            val order = snap.orderedPackages.toMutableList()
            val fi = order.indexOf(folderId)
            order.removeAll { it == folderId }
            if (fi >= 0) {
                order.addAll(fi, members)
            } else {
                members.forEach { order.add(it) }
            }
            val names = snap.folderNames.filterKeys { folders.containsKey(it) }
            prefsRepo.writeGridState(order, folders, names)
        }
    }

    fun foldersForAddMenu(): List<Pair<String, String>> {
        val byPkg = apps.value.associateBy { it.packageName }
        return prefs.value.folderContents.mapNotNull { (id, members) ->
            if (members.isEmpty()) null
            else {
                val custom = prefs.value.folderNames[id]?.trim()?.takeIf { it.isNotEmpty() }
                val label = custom ?: (members.firstNotNullOfOrNull { byPkg[it]?.label } ?: "Folder")
                id to "$label (${members.size})"
            }
        }
    }

    fun createHomeGroup(title: String) {
        viewModelScope.launch {
            if (prefs.value.homeGroups.size >= 2) return@launch
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

    fun setHomeGroupSide(groupId: String, side: HomeGroupSide) {
        viewModelScope.launch {
            if (!HomeGroupIds.isHomeGroupId(groupId)) return@launch
            val groups = prefs.value.homeGroups
            val target = groups.find { it.id == groupId } ?: return@launch
            if (target.side == side) return@launch
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

    fun renameHomeGroup(groupId: String, newTitle: String) {
        if (!HomeGroupIds.isHomeGroupId(groupId)) return
        viewModelScope.launch {
            val label = newTitle.trim().ifBlank { "Group" }
            val next = prefs.value.homeGroups.map { g ->
                if (g.id == groupId) g.copy(title = label) else g
            }
            if (next != prefs.value.homeGroups) prefsRepo.setHomeGroups(next)
        }
    }

    fun deleteHomeGroup(groupId: String) {
        if (!HomeGroupIds.isHomeGroupId(groupId)) return
        viewModelScope.launch {
            prefsRepo.setHomeGroups(prefs.value.homeGroups.filter { it.id != groupId })
        }
    }

    fun addPackageToHomeGroup(packageName: String, groupId: String) {
        if (!HomeGroupIds.isHomeGroupId(groupId) || packageName == AppsRepository.INTERNAL_SETTINGS_PACKAGE) return
        viewModelScope.launch {
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

    fun removePackageFromHomeGroup(packageName: String, groupId: String) {
        if (!HomeGroupIds.isHomeGroupId(groupId)) return
        viewModelScope.launch {
            val next = prefs.value.homeGroups.map { g ->
                if (g.id != groupId) g else g.copy(packageNames = g.packageNames.filter { it != packageName })
            }
            prefsRepo.setHomeGroups(next)
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
            DockSlot.Mail -> actions.launchMail()
            DockSlot.Shortcut -> {
                val target = prefs.value.secondShortcutTarget
                if (target == SecondShortcutTarget.WHATSAPP) {
                    if (!actions.launchApp("com.whatsapp")) actions.launchMessages()
                } else {
                    actions.launchMessages()
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

    fun addHomeShortcut(packageName: String) {
        viewModelScope.launch {
            val snap = prefs.value
            val pkg = packageName.trim()
            if (pkg.isEmpty()) return@launch
            val cur = snap.homeShortcutPackages
            if (cur.size >= 3 || pkg in cur) return@launch
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
        if (cur.size >= 3) return false
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

    fun launchApp(packageName: String) {
        actions.launchApp(packageName)
    }

    fun openAppInfo(packageName: String) {
        actions.openAppInfo(packageName)
    }

    fun exportAppOrder(): List<String> = prefs.value.orderedPackages

    fun importAppOrder(packages: List<String>) {
        viewModelScope.launch {
            val snap = prefs.value
            val names = snap.folderNames.filterKeys { snap.folderContents.containsKey(it) }
            prefsRepo.writeGridState(packages, snap.folderContents, names)
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

    fun setClassicMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setClassicMode(enabled) }
    }

    fun setAppIconShape(shape: AppIconShape) {
        viewModelScope.launch { prefsRepo.setAppIconShape(shape) }
    }

    fun exportBackupJson(): String = LauncherBackup.toJson(prefs.value)

    fun importBackupJson(json: String): Boolean {
        val restored = LauncherBackup.fromJson(json).getOrNull() ?: return false
        viewModelScope.launch { prefsRepo.applyFullBackup(restored) }
        return true
    }

}

enum class DockSlot { Mail, Shortcut, Camera }
