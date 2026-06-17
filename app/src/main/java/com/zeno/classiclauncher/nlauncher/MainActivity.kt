package com.zeno.classiclauncher.nlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zeno.classiclauncher.nlauncher.R
import com.zeno.classiclauncher.nlauncher.locale.LauncherLocale
import com.zeno.classiclauncher.nlauncher.minimalmode.MinimalModeScreen
import com.zeno.classiclauncher.nlauncher.ui.BbTheme
import com.zeno.classiclauncher.nlauncher.ui.LauncherScreen
import com.zeno.classiclauncher.nlauncher.ui.LauncherViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LauncherLocale.apply(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Allow content to extend into status-bar / navigation-bar areas.
        // statusBarsPadding() / navigationBarsPadding() in each screen handle the insets.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        handlePinShortcutIntent(intent)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        setContent {
            BbTheme {
                val prefs by viewModel.prefs.collectAsStateWithLifecycle()
                if (prefs.minimalModeEnabled) {
                    MinimalModeScreen(vm = viewModel)
                } else {
                    LauncherScreen(vm = viewModel)
                }
            }
        }
        // React to Minimal Mode toggle at runtime (e.g. user switches in settings overlay).
        // Also gates NotificationRepository so badge classification is skipped while the
        // Normal Mode dock is not composed (its StateFlow consumers don't exist in Minimal Mode).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.prefs.collect { prefs ->
                    applyStatusBarVisibility(prefs.minimalModeEnabled)
                    com.zeno.classiclauncher.nlauncher.badges.NotificationRepository
                        .minimalModeActive = prefs.minimalModeEnabled
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val currentPrefs = viewModel.prefs.value
        applyStatusBarVisibility(currentPrefs.minimalModeEnabled)
        com.zeno.classiclauncher.nlauncher.badges.NotificationRepository
            .minimalModeActive = currentPrefs.minimalModeEnabled
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyStatusBarVisibility(viewModel.prefs.value.minimalModeEnabled)
    }

    /**
     * Hides or shows only the status bar based on Minimal Mode state.
     * Navigation bar is never touched.
     * BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPING keeps the notification shade accessible
     * via a downward swipe even when the status bar is hidden.
     * Requires API 30+; no-op on older devices (minSdk=26, but target devices are API 34/36).
     */
    private fun applyStatusBarVisibility(minimalModeActive: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val controller = window.insetsController ?: return
        if (minimalModeActive) {
            controller.hide(WindowInsets.Type.statusBars())
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsets.Type.statusBars())
        }
    }

    /** Catch KEYCODE_CALL at the Activity level — Compose onPreviewKeyEvent may not receive it
     *  if the system (PhoneWindowManager) intercepts or delivers it only on ACTION_UP. */
    @Suppress("RestrictedApi")
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.keyCode == android.view.KeyEvent.KEYCODE_CALL &&
            event.action == android.view.KeyEvent.ACTION_UP
        ) {
            if (openDefaultDialer()) return true
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
                    getString(R.string.home_shortcuts_full),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

}
