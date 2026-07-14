package com.zeno.classiclauncher.nlauncher.search

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.WindowManager
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

/**
 * Owns the floating search overlay window — a single [ComposeView] added directly to the
 * system [WindowManager], independent of any Activity. There is no Activity backing this
 * window, so [ComposeView] needs its own [LifecycleOwner]/[ViewModelStoreOwner]/
 * [SavedStateRegistryOwner] wired up manually; that's what [OverlayLifecycleOwner] is for.
 *
 * A plain top-level `object` (rather than a Service) is deliberate: the only thing that needs
 * a lifecycle here is the overlay window itself, and [SearchOverlayAccessibilityService] already
 * provides the long-lived process context that triggers [show]/[hide].
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

    val isVisible: Boolean get() = rootView != null

    fun toggle(serviceContext: Context) {
        if (isVisible) hide() else show(serviceContext)
    }

    fun show(serviceContext: Context) {
        if (isVisible) return
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
                SearchOverlayContent(
                    onDismiss = ::hide,
                    onLaunchApp = { pkg ->
                        SearchOverlayActions.launchAndHide(appContext, pkg)
                    },
                    onLaunchSettings = { action, fallbackAction ->
                        SearchOverlayActions.openSettingsAndHide(appContext, action, fallbackAction)
                    },
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
