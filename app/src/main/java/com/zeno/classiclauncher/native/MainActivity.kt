package com.zeno.classiclauncher.nlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.zeno.classiclauncher.nlauncher.ui.BbTheme
import com.zeno.classiclauncher.nlauncher.ui.LauncherScreen
import com.zeno.classiclauncher.nlauncher.ui.LauncherViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // QS / edge-to-edge experiment: lay out under system bars (status bar stays visible;
        // content can extend behind it). Off by default — flip to true to retest.
        if (ENABLE_EDGE_TO_EDGE_UNDER_SYSTEM_BARS &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        ) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        handlePinShortcutIntent(intent)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        setContent {
            BbTheme {
                LauncherScreen()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            viewModel.requestDismissLauncherQuickSettings()
        }
    }

    /** Catch KEYCODE_ENDCALL at the Activity level — Compose onPreviewKeyEvent may not receive it
     *  if the system (PhoneWindowManager) intercepts or delivers it only on ACTION_UP. */
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.keyCode == android.view.KeyEvent.KEYCODE_CALL &&
            event.action == android.view.KeyEvent.ACTION_UP
        ) {
            if (openDefaultDialer()) return true
        }
        if (event.keyCode == android.view.KeyEvent.KEYCODE_ENDCALL &&
            event.action == android.view.KeyEvent.ACTION_UP
        ) {
            viewModel.requestNavigateHome()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun openDefaultDialer(): Boolean {
        val telecom = getSystemService(TelecomManager::class.java)
        val defaultDialerPkg = telecom?.defaultDialerPackage.orEmpty()
        if (defaultDialerPkg.isNotEmpty()) {
            val explicitDial = Intent(Intent.ACTION_DIAL).apply {
                setPackage(defaultDialerPkg)
            }
            if (packageManager.resolveActivity(explicitDial, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                return runCatching {
                    startActivity(explicitDial)
                    true
                }.getOrDefault(false)
            }
        }
        val fallback = Intent(Intent.ACTION_DIAL)
        return runCatching {
            startActivity(fallback)
            true
        }.getOrDefault(false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePinShortcutIntent(intent)
        // Navigate to home page only when the HOME intent arrives while the launcher is already
        // in the foreground (RESUMED). When the launcher is being *restored* from the background
        // (e.g. back press from recents), onNewIntent fires while the lifecycle is still STARTED —
        // in that case we keep whatever page the user was on.
        if (intent.action == Intent.ACTION_MAIN &&
            intent.hasCategory(Intent.CATEGORY_HOME) &&
            lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
        ) {
            viewModel.requestNavigateHome()
        }
    }

    private fun handlePinShortcutIntent(intent: Intent) {
        val req = getSystemService(LauncherApps::class.java)?.getPinItemRequest(intent) ?: return
        if (req.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) return
        if (req.shortcutInfo == null) return
        lifecycleScope.launch {
            val ok = viewModel.consumePinShortcutRequest(req)
            if (ok) {
                req.accept()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Home shortcuts full (max 3)",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private companion object {
        private const val ENABLE_EDGE_TO_EDGE_UNDER_SYSTEM_BARS = true
    }
}
