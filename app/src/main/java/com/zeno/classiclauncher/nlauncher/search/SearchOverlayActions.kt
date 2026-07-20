package com.zeno.classiclauncher.nlauncher.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.zeno.classiclauncher.nlauncher.apps.AppsRepository
import com.zeno.classiclauncher.nlauncher.apps.CustomIconStore
import com.zeno.classiclauncher.nlauncher.apps.LauncherActions
import com.zeno.classiclauncher.nlauncher.prefs.LauncherPrefsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Side effects for the overlay's results — launching an app or a settings screen, then dismissing. */
internal object SearchOverlayActions {

    /** Handled in `MainActivity.onNewIntent` — brings the launcher to the drawer's hidden-apps view. */
    const val ACTION_SHOW_HIDDEN_APPS = "com.zeno.classiclauncher.nlauncher.SHOW_HIDDEN_APPS"

    /** Handled in `MainActivity.onNewIntent` — Quick Switch's "⋮" menu's only action that can't
     *  stay in-place: reordering means dragging tiles in the actual home/drawer grid, which only
     *  exists on the launcher itself. */
    const val ACTION_ENTER_REORDER_MODE = "com.zeno.classiclauncher.nlauncher.ENTER_REORDER_MODE"

    /** Outlives the overlay's own ComposeView — [applyCustomIconFromPicker] runs after the overlay
     *  (and its Compose coroutine scope) has already been torn down, since picking an image takes
     *  the user through a separate proxy Activity first. */
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun launchAndHide(context: Context, packageName: String) {
        runCatching { LauncherActions(context).launchApp(packageName) }
        SearchOverlayController.hide()
    }

    fun openSettingsAndHide(context: Context, action: String, fallbackAction: String) {
        val launched = runCatching {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (!launched) {
            runCatching {
                context.startActivity(Intent(fallbackAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        SearchOverlayController.hide()
    }

    fun showHiddenAppsAndHide(context: Context) {
        val intent = Intent(context, com.zeno.classiclauncher.nlauncher.MainActivity::class.java)
            .setAction(ACTION_SHOW_HIDDEN_APPS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
        SearchOverlayController.hide()
    }

    fun openWebSearchAndHide(context: Context, query: String) {
        openWebSearch(context, query)
        SearchOverlayController.hide()
    }

    fun openPlayStoreSearchAndHide(context: Context, query: String) {
        openPlayStoreSearch(context, query)
        SearchOverlayController.hide()
    }

    fun openContactAndHide(context: Context, contact: ContactResult) {
        openContact(context, contact)
        SearchOverlayController.hide()
    }

    fun openAppInfoAndHide(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
        SearchOverlayController.hide()
    }

    /** The only Quick Switch "⋮" action that can't stay in-place — see [ACTION_ENTER_REORDER_MODE]. */
    fun openArrangeModeAndHide(context: Context) {
        val intent = Intent(context, com.zeno.classiclauncher.nlauncher.MainActivity::class.java)
            .setAction(ACTION_ENTER_REORDER_MODE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
        SearchOverlayController.hide()
    }

    /**
     * "Change icon" from Quick Switch's "⋮" menu: the overlay is already hidden and its own
     * Compose scope torn down by the time [IconPickerProxyActivity] returns a picked image, so
     * this runs on [ioScope] instead — fresh repo instances, same pattern [SearchOverlayController]
     * already uses for its one-shot prefs/apps reads.
     */
    fun applyCustomIconFromPicker(context: Context, packageName: String, uri: Uri) {
        ioScope.launch {
            val ok = CustomIconStore.save(context, packageName, uri)
            if (ok) {
                val prefsRepo = LauncherPrefsRepository(context)
                prefsRepo.addCustomIconPackage(packageName)
                AppsRepository(context, prefsRepo).invalidateAndRefresh(packageName)
            }
        }
    }
}
