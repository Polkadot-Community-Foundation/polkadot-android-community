package io.paritytech.polkadotapp.feature_wallet_impl.presentation.pocket

import android.webkit.WebView
import io.paritytech.polkadotapp.feature_dotns_api.domain.DotNsLoadProgress
import io.paritytech.polkadotapp.feature_products_api.presentation.spaHost.SpaHostSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpandedProductPageTest {
    private class FakeSession : SpaHostSession {
        override val webView = MutableStateFlow<WebView?>(null)
        override val currentUrl = MutableStateFlow("")
        override val loadProgress = MutableStateFlow<DotNsLoadProgress>(DotNsLoadProgress.Idle)
        override val title = MutableStateFlow("")
        override fun pauseConnections() = Unit
        override fun resumeConnections() = Unit
    }

    private val opened = mutableListOf<Pair<String, CoroutineScope>>()

    // The screen's own scope, separate from the test's, so a page left open does not hold runTest open.
    private fun TestScope.screenScope() = CoroutineScope(StandardTestDispatcher(testScheduler))

    private fun page(screenScope: CoroutineScope) = ExpandedProductPage(screenScope) { scope, url ->
        opened += url to scope
        FakeSession()
    }

    @Test
    fun `opening a card hosts its product and hands the session to the screen`() = runTest {
        val page = page(screenScope())

        page.open("https://game.dot?card=loyalty")

        assertEquals(listOf("https://game.dot?card=loyalty"), opened.map { it.first })
        assertTrue(page.session.value is FakeSession)
    }

    // The session owns a WebView, so a page left running behind a closed card would leak one per card opened.
    @Test
    fun `closing the card tears its product down`() = runTest {
        val page = page(screenScope())
        page.open("https://game.dot?card=loyalty")

        page.close()

        assertFalse(opened.single().second.isActive)
        assertNull(page.session.value)
    }

    @Test
    fun `opening another card leaves only the new product running`() = runTest {
        val page = page(screenScope())
        page.open("https://game.dot?card=loyalty")

        page.open("https://shop.dot?card=points")

        val (first, second) = opened.map { it.second }
        assertFalse(first.isActive)
        assertTrue(second.isActive)
    }

    // The screen asks for the product once the card has settled, and it asks again on every
    // recomposition that follows. Re-opening would tear down a live WebView and start over.
    @Test
    fun `asking again for the product already hosted changes nothing`() = runTest {
        val page = page(screenScope())
        page.open("https://game.dot?card=loyalty")

        page.open("https://game.dot?card=loyalty")

        assertEquals(1, opened.size)
        assertTrue(opened.single().second.isActive)
    }

    @Test
    fun `a card reopened after being closed is hosted again`() = runTest {
        val page = page(screenScope())
        page.open("https://game.dot?card=loyalty")
        page.close()

        page.open("https://game.dot?card=loyalty")

        assertEquals(2, opened.size)
        assertTrue(opened.last().second.isActive)
    }

    @Test
    fun `closing a card that hosts nothing is harmless`() = runTest {
        val page = page(screenScope())

        page.close()

        assertTrue(opened.isEmpty())
        assertNull(page.session.value)
    }

    @Test
    fun `leaving the screen takes the open product with it`() = runTest {
        val screen = screenScope()
        page(screen).open("https://game.dot?card=loyalty")

        screen.cancel()

        assertFalse(opened.single().second.isActive)
    }
}
