package com.zeno.classiclauncher.nlauncher.search

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.zeno.classiclauncher.nlauncher.apps.AppEntry
import com.zeno.classiclauncher.nlauncher.apps.AppsRepository
import com.zeno.classiclauncher.nlauncher.prefs.LauncherPrefsRepository
import kotlinx.coroutines.flow.first

/**
 * Owns the floating search overlay window — a single [ComposeView] added directly to the
 * system [WindowManager], independent of any Activity. There is no Activity backing this
 * window, so [ComposeView] needs its own [LifecycleOwner]/[ViewModelStoreOwner]/
 * [SavedStateRegistryOwner] wired up manually; that's what [OverlayLifecycleOwner] is for.
 *
 * A plain top-level `object` (rather than a Service) is deliberate: the only thing that needs
 * a lifecycle here is the overlay window itself, and [com.zeno.classiclauncher.nlauncher.power.LockScreenAccessibilityService]
 * already provides the long-lived process context that triggers [show]/[hide].
 *
 * [show] must be called with the accessibility service's own `Context` (not applicationContext)
 * — [WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY] is only permitted for a
 * `WindowManager` obtained from a context tied to a bound accessibility service, which is
 * exactly what lets this overlay skip the separate "Display over other apps" permission
 * entirely: the same Accessibility grant the user already made covers both.
 */
internal object SearchOverlayController {

    private const val TAG = "ZenoSearchOverlay"

    private var windowManager: WindowManager? = null
    private var rootView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    // Retained across a hide()/show() cycle so startVoiceSearch can re-show the overlay after the
    // proxy activity returns — TYPE_ACCESSIBILITY_OVERLAY requires the accessibility service's own
    // Context (not applicationContext), and that service instance outlives the overlay itself.
    private var lastServiceContext: Context? = null

    val isVisible: Boolean get() = rootView != null

    fun toggle(serviceContext: Context) {
        if (isVisible) hide() else show(serviceContext)
    }

    fun show(serviceContext: Context, initialQuery: String = "") {
        if (isVisible) return
        lastServiceContext = serviceContext
        val appContext = serviceContext.applicationContext
        val wm = serviceContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

        val owner = OverlayLifecycleOwner().apply {
            performRestore()
            handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            handleLifecycleEvent(Lifecycle.Event.ON_START)
            handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        val composeView = ComposeView(serviceContext).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                var query by remember { mutableStateOf(initialQuery) }
                var allApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
                var hiddenPackages by remember { mutableStateOf<Set<String>>(emptySet()) }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    val prefsRepo = LauncherPrefsRepository(appContext)
                    hiddenPackages = prefsRepo.prefsFlow.first().hiddenPackages
                    allApps = AppsRepository(appContext, prefsRepo).appsFlow().first()
                }

                UniversalSearchOverlay(
                    query = query,
                    onQueryChange = { query = it },
                    onDismiss = ::hide,
                    allApps = allApps,
                    hiddenPackages = hiddenPackages,
                    onLaunchApp = { pkg -> SearchOverlayActions.launchAndHide(appContext, pkg) },
                    onLongPressApp = { app -> SearchOverlayActions.openAppMenuAndHide(appContext, app.packageName) },
                    onLaunchSettings = { action, fallbackAction ->
                        SearchOverlayActions.openSettingsAndHide(appContext, action, fallbackAction)
                    },
                    onShowHiddenApps = { SearchOverlayActions.showHiddenAppsAndHide(appContext) },
                    onLaunchContact = { contact -> SearchOverlayActions.openContactAndHide(appContext, contact) },
                    // Quick Switch now gets voice search too, via a tiny invisible proxy Activity
                    // that hosts the system speech-recognizer's result callback on this window's
                    // behalf (see VoiceSearchProxyActivity / startVoiceSearch below).
                    showMic = true,
                    onVoiceSearch = { startVoiceSearch(query) },
                    onOpenPlayStore = { q -> SearchOverlayActions.openPlayStoreSearchAndHide(appContext, q) },
                    onOpenWebSearch = { q -> SearchOverlayActions.openWebSearchAndHide(appContext, q) },
                )
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // Focusable (no FLAG_NOT_FOCUSABLE) — the search field needs to receive key input.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )

        val added = runCatching { wm.addView(composeView, params) }.isSuccess
        if (!added) {
            Log.w(TAG, "Failed to add overlay window — is the accessibility service actually bound?")
            owner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            return
        }

        windowManager = wm
        rootView = composeView
        lifecycleOwner = owner
    }

    fun hide() {
        val wm = windowManager ?: return
        val view = rootView ?: return
        lifecycleOwner?.apply {
            handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        runCatching { wm.removeView(view) }
        windowManager = null
        rootView = null
        lifecycleOwner = null
    }

    /**
     * Hides the overlay, launches [VoiceSearchProxyActivity] to run the system speech
     * recognizer, then re-shows the overlay with the recognized text once it returns (or the
     * original [currentQuery] if the user cancelled or no recognizer is available). The overlay
     * must be hidden first — its accessibility-overlay window would otherwise sit visually above
     * the recognizer's own UI, which is a normal task window.
     */
    fun startVoiceSearch(currentQuery: String) {
        val serviceContext = lastServiceContext ?: return
        val appContext = serviceContext.applicationContext
        hide()
        VoiceSearchProxyActivity.launch(appContext) { heard ->
            show(serviceContext, heard?.takeIf { it.isNotBlank() } ?: currentQuery)
        }
    }
}

/**
 * Minimal [LifecycleOwner]/[ViewModelStoreOwner]/[SavedStateRegistryOwner] for a [ComposeView]
 * that isn't hosted by an Activity/Fragment. State is never actually persisted across process
 * death (the overlay is transient and re-created fresh each time it's shown), so
 * [performRestore] always restores from a null bundle.
 */
private class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    override val viewModelStore = ViewModelStore()

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun performRestore() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}
