package io.paritytech.polkadotapp.app.internal

import android.os.Build
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.Window
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import io.paritytech.polkadotapp.app.R
import timber.log.Timber
import java.io.File

/** First-run notice that blocks the app until the tester acknowledges it, once per install. */
internal class InternalBuildNotice(private val acknowledgement: File) {
    fun showIfNeeded(activity: ComponentActivity, buildInfo: InternalBuildInfo) {
        if (acknowledgement.exists()) return

        lateinit var overlay: NoticeOverlay
        overlay = NoticeOverlay(
            activity = activity,
            buildInfo = buildInfo,
            onContinue = {
                acknowledge()
                overlay.dismiss()
            },
            onCloseApp = activity::finishAffinity,
        )
        overlay.show()
    }

    private fun acknowledge() {
        runCatching { acknowledgement.createNewFile() }
            .onFailure { Timber.w(it, "Could not store the internal build notice acknowledgement") }
    }
}

/**
 * Drawn inside the activity's own window, above its content. Back is intercepted ahead of the
 * activity's OnBackPressedDispatcher, whose callbacks registered later by screens would otherwise
 * win: as a key event where the app has not opted into predictive back, and with an overlay-priority
 * OnBackInvokedCallback where it has.
 */
private class NoticeOverlay(
    private val activity: ComponentActivity,
    buildInfo: InternalBuildInfo,
    onContinue: () -> Unit,
    onCloseApp: () -> Unit,
) {
    private val view = ComposeView(activity).apply {
        // A fixed id lets the ticked boxes survive the activity being recreated.
        id = R.id.internal_build_notice
        consumeWindowInsets = false
        // Swallows touches the notice itself does not handle, so nothing underneath reacts.
        isClickable = true
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            InternalBuildNoticeScreen(
                isDebuggable = buildInfo.isDebuggable,
                hasDebugMenu = buildInfo.hasDebugMenu,
                hasGoogleBackup = buildInfo.hasGoogleBackup,
                sentryStatus = buildInfo.sentryStatus,
                onContinue = onContinue,
                onCloseApp = onCloseApp,
            )
        }
    }

    private val backKeyInterceptor = BackKeyInterceptor(activity.window.callback)

    private val overlayBackCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        OverlayBackCallback(activity)
    } else {
        null
    }

    fun show() {
        activity.addContentView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        activity.window.callback = backKeyInterceptor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) overlayBackCallback?.register()
    }

    fun dismiss() {
        (view.parent as? ViewGroup)?.removeView(view)
        if (activity.window.callback === backKeyInterceptor) {
            activity.window.callback = backKeyInterceptor.delegate
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) overlayBackCallback?.unregister()
    }
}

private class BackKeyInterceptor(val delegate: Window.Callback) : Window.Callback by delegate {
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        event.keyCode == KeyEvent.KEYCODE_BACK || delegate.dispatchKeyEvent(event)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class OverlayBackCallback(private val activity: ComponentActivity) {
    private val callback = OnBackInvokedCallback { }

    fun register() {
        activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_OVERLAY, callback)
    }

    fun unregister() {
        activity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(callback)
    }
}
