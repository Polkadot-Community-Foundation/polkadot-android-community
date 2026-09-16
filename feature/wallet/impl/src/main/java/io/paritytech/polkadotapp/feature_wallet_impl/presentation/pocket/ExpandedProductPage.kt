package io.paritytech.polkadotapp.feature_wallet_impl.presentation.pocket

import io.paritytech.polkadotapp.common.data.memory.ComputationalScope
import io.paritytech.polkadotapp.feature_products_api.presentation.spaHost.SpaHostSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.job

/**
 * The product hosted under an expanded card. One runs at a time: expanding another card closes the
 * first, and collapsing tears the WebView down with the scope it lives in.
 */
class ExpandedProductPage(
    private val parentScope: CoroutineScope,
    private val openSession: (ComputationalScope, String) -> SpaHostSession,
) {
    private val openSessionState = MutableStateFlow<SpaHostSession?>(null)
    private var openScope: CoroutineScope? = null
    private var openUrl: String? = null

    val session: StateFlow<SpaHostSession?> = openSessionState.asStateFlow()

    /** Asking for the product already hosted changes nothing, so a redraw cannot restart its WebView. */
    fun open(url: String) {
        if (url == openUrl) return
        close()
        openUrl = url

        // A child of the parent, so a screen that goes away takes the WebView with it.
        val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext.job))
        openScope = scope
        openSessionState.value = openSession(ComputationalScope(scope), url)
    }

    fun close() {
        openScope?.cancel()
        openScope = null
        openUrl = null
        openSessionState.value = null
    }
}
