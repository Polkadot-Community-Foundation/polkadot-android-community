package io.paritytech.polkadotapp.feature_products_impl.domain.hostApi

import io.paritytech.polkadotapp.design.theme.AppThemeSelector
import io.paritytech.polkadotapp.designsystem.themes.PolkadotAppTheme
import io.paritytech.polkadotapp.feature_products_impl.domain.jsRuntime.JsRuntime
import io.paritytech.polkadotapp.test_shared.any
import io.paritytech.polkadotapp.test_shared.eq
import io.paritytech.polkadotapp.test_shared.whenever
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class ThemeSeedingInjectionTest {
    private val runtime: JsRuntime = mock()
    private val delegate: ContainerInjectionStrategy = mock()
    private val appThemeSelector: AppThemeSelector = mock()
    private val scriptPayload = ScriptPayload(bridgeJs = "bridge", containerJs = "container")
    private val loadInitialPage: suspend () -> Unit = {}

    @Test
    fun `registers the design-system theme seed for the product origin before the delegate sets up`() = runBlocking<Unit> {
        withSelectedTheme(PolkadotAppTheme.BerlinNight)
        withDocumentStartSupported(true)

        createInjection(PRODUCT_URL).setup(scriptPayload, runtime, loadInitialPage)

        verifySeedRegisteredBeforeDelegate(themeId = "berlin-night", origin = PRODUCT_ORIGIN)
    }

    @Test
    fun `seeds the design-system id of each app theme`() = runBlocking<Unit> {
        withDocumentStartSupported(true)
        val expectedIds = mapOf(
            PolkadotAppTheme.BerlinDay to "berlin-day",
            PolkadotAppTheme.BerlinNight to "berlin-night",
            PolkadotAppTheme.Lisbon to "lisbon",
            PolkadotAppTheme.Malta to "malta",
            PolkadotAppTheme.Tokyo to "tokyo",
        )

        expectedIds.forEach { (theme, id) ->
            withSelectedTheme(theme)

            createInjection(PRODUCT_URL).setup(scriptPayload, runtime, loadInitialPage)

            verifySeedRegistered(themeId = id, origin = PRODUCT_ORIGIN)
        }
    }

    @Test
    fun `keeps the port in the origin rule when the product url has one`() = runBlocking<Unit> {
        withSelectedTheme(PolkadotAppTheme.Lisbon)
        withDocumentStartSupported(true)

        createInjection("http://localhost:8080/app/#/").setup(scriptPayload, runtime, loadInitialPage)

        verifySeedRegistered(themeId = "lisbon", origin = "http://localhost:8080")
    }

    @Test
    fun `skips the seed and still sets up the delegate when the product url has no host`() = runBlocking<Unit> {
        withSelectedTheme(PolkadotAppTheme.BerlinNight)

        createInjection("about:blank").setup(scriptPayload, runtime, loadInitialPage)

        verifyNoSeedRegistered()
        verifyDelegateSetUp()
    }

    @Test
    fun `still sets up the delegate when document-start scripts are unsupported`() = runBlocking<Unit> {
        withSelectedTheme(PolkadotAppTheme.BerlinNight)
        withDocumentStartSupported(false)

        createInjection(PRODUCT_URL).setup(scriptPayload, runtime, loadInitialPage)

        verifyDelegateSetUp()
    }

    @Test
    fun `still sets up the delegate when registering the seed fails`() = runBlocking<Unit> {
        withSelectedTheme(PolkadotAppTheme.BerlinNight)
        withSeedRegistrationFailing()

        createInjection(PRODUCT_URL).setup(scriptPayload, runtime, loadInitialPage)

        verifyDelegateSetUp()
    }

    private fun createInjection(productUrl: String) = ThemeSeedingInjection(
        delegate = delegate,
        appThemeSelector = appThemeSelector,
        productUrl = productUrl,
    )

    private fun withSelectedTheme(theme: PolkadotAppTheme) {
        whenever(appThemeSelector.selectedTheme).thenReturn(flowOf(theme))
    }

    private suspend fun withDocumentStartSupported(isSupported: Boolean) {
        whenever(runtime.injectDocumentStartScript(any(), any())).thenReturn(isSupported)
    }

    private suspend fun withSeedRegistrationFailing() {
        whenever(runtime.injectDocumentStartScript(any(), any())).thenThrow(IllegalArgumentException())
    }

    private suspend fun verifySeedRegistered(themeId: String, origin: String) {
        verify(runtime).injectDocumentStartScript(eq(seedScript(themeId)), eq(setOf(origin)))
    }

    private suspend fun verifySeedRegisteredBeforeDelegate(themeId: String, origin: String) {
        val order = inOrder(runtime, delegate)
        order.verify(runtime).injectDocumentStartScript(eq(seedScript(themeId)), eq(setOf(origin)))
        order.verify(delegate).setup(scriptPayload, runtime, loadInitialPage)
    }

    private suspend fun verifyNoSeedRegistered() {
        verify(runtime, never()).injectDocumentStartScript(any(), any())
    }

    private suspend fun verifyDelegateSetUp() {
        verify(delegate).setup(scriptPayload, runtime, loadInitialPage)
    }

    private fun seedScript(themeId: String) = "try{localStorage.setItem('pds-theme',\"$themeId\")}catch(e){}"

    private companion object {
        const val PRODUCT_URL = "https://getcash.paseo/#/"
        const val PRODUCT_ORIGIN = "https://getcash.paseo"
    }
}
