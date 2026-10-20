package io.paritytech.polkadotapp.feature_products_impl.domain.hostApi

import io.paritytech.polkadotapp.common.utils.logFailure
import io.paritytech.polkadotapp.common.utils.runCancellableCatching
import io.paritytech.polkadotapp.design.theme.AppThemeSelector
import io.paritytech.polkadotapp.designsystem.themes.PolkadotAppTheme
import io.paritytech.polkadotapp.feature_products_impl.domain.jsRuntime.JsRuntime
import io.paritytech.polkadotapp.feature_products_impl.domain.jsRuntime.toJsStringLiteral
import kotlinx.coroutines.flow.first
import java.net.URI

class ThemeSeedingInjection(
    private val delegate: ContainerInjectionStrategy,
    private val appThemeSelector: AppThemeSelector,
    private val productUrl: String,
) : ContainerInjectionStrategy {
    override suspend fun setup(scriptPayload: ScriptPayload, runtime: JsRuntime, loadInitialPage: suspend () -> Unit) {
        runCancellableCatching { seedTheme(runtime) }
            .logFailure("Theme seed not registered")

        delegate.setup(scriptPayload, runtime, loadInitialPage)
    }

    private suspend fun seedTheme(runtime: JsRuntime) {
        val origin = originOf(productUrl) ?: return
        val theme = appThemeSelector.selectedTheme.first()
        runtime.injectDocumentStartScript(seedScript(theme), setOf(origin))
    }

    private fun originOf(url: String): String? {
        val uri = URI(url)
        val host = uri.host ?: return null
        val port = if (uri.port == -1) "" else ":${uri.port}"
        return "${uri.scheme}://$host$port"
    }

    private fun seedScript(theme: PolkadotAppTheme): String =
        "try{localStorage.setItem('$STORAGE_KEY',${theme.designSystemId.toJsStringLiteral()})}catch(e){}"

    private val PolkadotAppTheme.designSystemId: String
        get() = name.replace(WORD_BOUNDARY, "-").lowercase()

    private companion object {
        // Polkadot design-system key that a product's head script reads before its first paint.
        const val STORAGE_KEY = "pds-theme"

        val WORD_BOUNDARY = Regex("(?<=[a-z])(?=[A-Z])")
    }
}
