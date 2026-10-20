package io.paritytech.polkadotapp.feature_products_impl.presentation.spaSheet

import android.graphics.Color
import android.webkit.WebView
import androidx.lifecycle.SavedStateHandle
import io.paritytech.polkadotapp.common.data.memory.ComputationalScope
import io.paritytech.polkadotapp.common.presentation.screens.MessageDisplay
import io.paritytech.polkadotapp.feature_products_api.presentation.SpaSheetPayload
import io.paritytech.polkadotapp.feature_products_api.presentation.spaHost.SpaHost
import io.paritytech.polkadotapp.feature_products_api.presentation.spaHost.SpaHostSession
import io.paritytech.polkadotapp.feature_products_impl.presentation.productBotManagement.ProductsRouter
import io.paritytech.polkadotapp.test_shared.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class SpaSheetViewModelTest {
    private val webView: WebView = mock()
    private val session: SpaHostSession = mock()
    private val router: ProductsRouter = mock()
    private val sessionWebView = MutableStateFlow<WebView?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(session.webView).thenReturn(sessionWebView)
        whenever(session.loadProgress).thenReturn(emptyFlow())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `exposes the WebView with a transparent background when the session emits it`() {
        val viewModel = createViewModel()

        sessionWebView.value = webView

        assertSame(webView, viewModel.webView.value)
        verifyTransparentBackground(webView)
    }

    private fun verifyTransparentBackground(webView: WebView) {
        verify(webView).setBackgroundColor(Color.TRANSPARENT)
    }

    private fun createViewModel() = SpaSheetViewModel(
        spaHost = SessionSpaHost(session),
        router = router,
        savedStateHandle = SavedStateHandle(mapOf(SpaSheetPayload::class.java.name to SpaSheetPayload(PRODUCT_URL))),
    )

    private class SessionSpaHost(private val session: SpaHostSession) : SpaHost {
        context(scope: ComputationalScope, messageDisplay: MessageDisplay)
        override fun createSession(initialUrl: String): SpaHostSession = session
    }

    private companion object {
        const val PRODUCT_URL = "https://onramp.dot"
    }
}
