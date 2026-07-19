package com.zeno.classiclauncher.nlauncher.search

import android.content.Context
import android.content.Intent
import com.zeno.classiclauncher.nlauncher.apps.LauncherActions

/** Side effects for the overlay's results — launching an app or a settings screen, then dismissing. */
internal object SearchOverlayActions {

    /** Handled in `MainActivity.onNewIntent` — brings the launcher to the drawer's hidden-apps view. */
    const val ACTION_SHOW_HIDDEN_APPS = "com.zeno.classiclauncher.nlauncher.SHOW_HIDDEN_APPS"

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
}
