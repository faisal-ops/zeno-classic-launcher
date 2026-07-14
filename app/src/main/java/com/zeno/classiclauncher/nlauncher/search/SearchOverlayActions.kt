package com.zeno.classiclauncher.nlauncher.search

import android.content.Context
import android.content.Intent
import com.zeno.classiclauncher.nlauncher.apps.LauncherActions

/** Side effects for the overlay's results — launching an app or a settings screen, then dismissing. */
internal object SearchOverlayActions {

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
}
