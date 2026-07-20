package com.zeno.classiclauncher.nlauncher.search

import android.content.Context
import android.content.Intent
import com.zeno.classiclauncher.nlauncher.apps.LauncherActions

/** Side effects for the overlay's results — launching an app or a settings screen, then dismissing. */
internal object SearchOverlayActions {

    /** Handled in `MainActivity.onNewIntent` — brings the launcher to the drawer's hidden-apps view. */
    const val ACTION_SHOW_HIDDEN_APPS = "com.zeno.classiclauncher.nlauncher.SHOW_HIDDEN_APPS"

    /** Handled in `MainActivity.onNewIntent` — opens the real AppContextMenu for [EXTRA_APP_MENU_PACKAGE]. */
    const val ACTION_SHOW_APP_MENU = "com.zeno.classiclauncher.nlauncher.SHOW_APP_MENU"
    const val EXTRA_APP_MENU_PACKAGE = "app_menu_package"

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

    /**
     * Quick Switch's "⋮" app-row action brings the launcher to the foreground and opens the exact
     * same AppContextMenu that home/drawer search uses (see LauncherViewModel.requestShowAppMenu /
     * MainActivity.onNewIntent) — this tokenless overlay window has no access to that root-level
     * Compose state directly, but the launcher's own Activity does.
     */
    fun openAppMenuAndHide(context: Context, packageName: String) {
        val intent = Intent(context, com.zeno.classiclauncher.nlauncher.MainActivity::class.java)
            .setAction(ACTION_SHOW_APP_MENU)
            .putExtra(EXTRA_APP_MENU_PACKAGE, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
        SearchOverlayController.hide()
    }
}
