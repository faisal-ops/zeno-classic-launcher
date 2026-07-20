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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import android.os.Build
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
import com.zeno.classiclauncher.nlauncher.ui.LocalReservedStatusBarHeightPx
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LauncherLocale.apply(newBase))
    }

    // Real status bar height in px, captured from the live WindowInsets the first time they're
    // dispatched (before we ever hide the bar) — never hardcoded, so it's correct on any device/
    // density. Confirmed on the Q25 via `adb shell dumpsys window displays`: InsetsSource
    // type=statusBars frame=[0,0][720,31], i.e. exactly 31px at this device's 720x720/208dpi
    // panel — this field reads that same 31px at runtime.
    private var measuredStatusBarHeightPx = 0
    private var reserveStatusBarSpace = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Allow content to extend into status-bar / navigation-bar areas.
        // statusBarsPadding() / navigationBarsPadding() in each screen handle the insets.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        handlePinShortcutIntent(intent)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // Freezes the statusBars inset at its real measured height whenever
        // reserveStatusBarSpace is on, regardless of what hide()/show() reports — every existing
        // `.statusBarsPadding()` consumer across the app keeps reserving exactly the same space
        // whether or not the real bar is actually drawn, so ZenoStatusBar can occupy that exact
        // strip without anything else in the layout shifting. Installed before setContent() so
        // it intercepts insets before they ever reach the ComposeView.
        var reservedHeightState by androidx.compose.runtime.mutableIntStateOf(0)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val real = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            if (real.top > 0) measuredStatusBarHeightPx = real.top
            reservedHeightState = if (reserveStatusBarSpace) measuredStatusBarHeightPx else 0

            // Re-assert the hide whenever the system bar comes back while our own bar is active.
            // ModalBottomSheet/AlertDialog each render in their OWN window, which has its own
            // WindowInsetsController and does not inherit this Activity window's hidden state —
            // so opening a folder, the home-actions sheet, or Zeno settings made the real bar
            // reappear on top of ZenoStatusBar (two clocks, doubled icons). Confirmed on-device.
            // Re-hiding centrally here fixes every sheet/dialog at once instead of patching each.
            if (reserveStatusBarSpace &&
                insets.isVisible(WindowInsetsCompat.Type.statusBars())
            ) {
                applyStatusBarVisibility(true)
            }

            if (reserveStatusBarSpace && measuredStatusBarHeightPx > 0) {
                WindowInsetsCompat.Builder(insets)
                    .setInsets(
                        WindowInsetsCompat.Type.statusBars(),
                        Insets.of(real.left, measuredStatusBarHeightPx, real.right, real.bottom),
                    )
                    .build()
            } else {
                insets
            }
        }
        ViewCompat.requestApplyInsets(window.decorView)

        setContent {
            BbTheme {
                CompositionLocalProvider(LocalReservedStatusBarHeightPx provides reservedHeightState) {
                    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
                    if (prefs.minimalModeEnabled) {
                        MinimalModeScreen(vm = viewModel)
                    } else {
                        LauncherScreen(vm = viewModel)
                    }
                }
            }
        }
        // Sync NotificationRepository flags and status-bar visibility with prefs at runtime.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.prefs.collect { prefs ->
                    reserveStatusBarSpace = prefs.minimalModeEnabled || prefs.customStatusBarEnabled || prefs.classicMode
                    ViewCompat.requestApplyInsets(window.decorView)
                    applyStatusBarVisibility(prefs.minimalModeEnabled || prefs.customStatusBarEnabled || prefs.classicMode)
                    com.zeno.classiclauncher.nlauncher.badges.NotificationRepository
                        .minimalModeActive = prefs.minimalModeEnabled
                    com.zeno.classiclauncher.nlauncher.badges.NotificationRepository
                        .badgesEnabled = prefs.notificationBadgesEnabled
                    com.zeno.classiclauncher.nlauncher.badges.NotificationRepository
                        .dockMailPackage = prefs.dockMailPackage.ifEmpty {
                            com.zeno.classiclauncher.nlauncher.apps.resolveDefaultMailPackage(this@MainActivity)
                        }
                }
            }
        }
        // setComponentEnabled is a PackageManager write — only call it when the pref
        // actually flips. Calling it redundantly risks a spurious service rebind that
        // clears the notification cache via onListenerConnected.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.prefs
                    .map { it.notificationBadgesEnabled }
                    .distinctUntilChanged()
                    .collect { enabled ->
                        com.zeno.classiclauncher.nlauncher.badges.BadgeNotificationListener
                            .setComponentEnabled(this@MainActivity, enabled)
                    }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val currentPrefs = viewModel.prefs.value
        reserveStatusBarSpace = currentPrefs.minimalModeEnabled || currentPrefs.customStatusBarEnabled || currentPrefs.classicMode
        ViewCompat.requestApplyInsets(window.decorView)
        applyStatusBarVisibility(currentPrefs.minimalModeEnabled || currentPrefs.customStatusBarEnabled || currentPrefs.classicMode)
        com.zeno.classiclauncher.nlauncher.badges.NotificationRepository
            .minimalModeActive = currentPrefs.minimalModeEnabled
        com.zeno.classiclauncher.nlauncher.badges.NotificationRepository
            .badgesEnabled = currentPrefs.notificationBadgesEnabled
        com.zeno.classiclauncher.nlauncher.badges.NotificationRepository
            .dockMailPackage = currentPrefs.dockMailPackage.ifEmpty {
                com.zeno.classiclauncher.nlauncher.apps.resolveDefaultMailPackage(this@MainActivity)
            }
        com.zeno.classiclauncher.nlauncher.search.LauncherForegroundState.isForeground = true
    }

    override fun onPause() {
        super.onPause()
        com.zeno.classiclauncher.nlauncher.search.LauncherForegroundState.isForeground = false
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val p = viewModel.prefs.value
            reserveStatusBarSpace = p.minimalModeEnabled || p.customStatusBarEnabled || p.classicMode
            ViewCompat.requestApplyInsets(window.decorView)
            applyStatusBarVisibility(p.minimalModeEnabled || p.customStatusBarEnabled || p.classicMode)
        }
    }

    /**
     * Hides or shows only the status bar — while Minimal Mode is on, or Zeno/Classic Mode's
     * optional custom status bar is on. Navigation bar is never touched.
     * BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPING keeps the notification shade accessible
     * via a downward swipe even when the status bar is hidden.
     * Requires API 30+; no-op on older devices (minSdk=26, but target devices are API 34/36).
     *
     * Safe to extend to customStatusBarEnabled now that reserveStatusBarSpace freezes the
     * statusBars inset at its real measured height (see the window-insets listener in
     * onCreate()) — every other screen's `.statusBarsPadding()` keeps reserving the same space
     * whether or not the real bar is actually drawn, so nothing else shifts or overlaps.
     * (Confirmed the overlap on-device before this fix: MinimalModeSettingsOverlay's header and
     * home-screen widgets both collapsed toward the hidden bar's now-zero inset and rendered on
     * top of ZenoStatusBar. Freezing the inset — rather than auditing every affected call site —
     * fixes all of them at once.)
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
        if (intent.action == com.zeno.classiclauncher.nlauncher.search.SearchOverlayActions.ACTION_SHOW_HIDDEN_APPS) {
            viewModel.requestShowHiddenApps()
        }
        if (intent.action == com.zeno.classiclauncher.nlauncher.search.SearchOverlayActions.ACTION_ENTER_REORDER_MODE) {
            viewModel.requestReorderMode()
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
