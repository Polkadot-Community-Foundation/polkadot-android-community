package io.paritytech.polkadotapp.feature_wallet_impl.presentation.pocket

import io.paritytech.polkadotapp.common.presentation.sharing.SharingManager
import io.paritytech.polkadotapp.feature_coinage_api.domain.model.BackupProgress
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCard
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCardId
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCardKey
import io.paritytech.polkadotapp.feature_products_api.model.ProductId
import io.paritytech.polkadotapp.feature_products_api.presentation.spaHost.SpaHost
import io.paritytech.polkadotapp.feature_tokens_api.presentation.formatter.TokenAmountFormatter
import io.paritytech.polkadotapp.feature_tokens_api.presentation.mapper.TokenAmountMapper
import io.paritytech.polkadotapp.feature_videogame_api.domain.collectibles.CollectiblesUrlResolver
import io.paritytech.polkadotapp.feature_wallet_impl.PocketRouter
import io.paritytech.polkadotapp.feature_wallet_impl.domain.interactor.PocketInteractor
import io.paritytech.polkadotapp.feature_wallet_impl.domain.model.PocketRank
import io.paritytech.polkadotapp.feature_wallet_impl.presentation.pocket.models.PocketCardUiModel
import io.paritytech.polkadotapp.test_shared.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.stubbing.Answer

class PocketViewModelTest {
    // Every flow the screen combines answers empty unless a test says otherwise, so each test names
    // only the source it is about. observeRank is answered here rather than stubbed because its
    // context parameter cannot be named from a call site that does not have one.
    private val quietFlows = Answer { invocation ->
        when {
            invocation.method.name == "observeRank" -> flowOf(PocketRank.Basic)
            invocation.method.returnType == Flow::class.java -> emptyFlow<Any>()
            else -> null
        }
    }

    private val interactor: PocketInteractor = mock(PocketInteractor::class.java, quietFlows)

    private fun createViewModel() = PocketViewModel(
        interactor = interactor,
        tokenAmountMapper = mock(TokenAmountMapper::class.java),
        tokenAmountFormatter = mock(TokenAmountFormatter::class.java),
        router = mock(PocketRouter::class.java),
        collectiblesUrlResolver = mock(CollectiblesUrlResolver::class.java),
        idShareImageRenderer = mock(IdShareImageRenderer::class.java),
        sharingManager = mock(SharingManager::class.java),
        spaHost = mock(SpaHost::class.java),
    )

    // The card list is assembled off the main thread, so the tests wait on it rather than driving a
    // virtual clock that the background dispatcher does not share.
    private fun PocketViewModel.awaitCards(count: Int) = runBlocking {
        withTimeout(AWAIT_MILLIS) { cards.first { it.size == count } }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        whenever(interactor.observeUsername()).thenReturn(flowOf("alicent"))
        whenever(interactor.observeAddress()).thenReturn(flowOf("15oF4u"))
        whenever(interactor.observeBackupProgress()).thenReturn(flowOf(BackupProgress.Unknown))
        whenever(interactor.observeAccountBackupPending()).thenReturn(flowOf(false))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun productCard(cardId: String) = PocketCard(
        key = PocketCardKey(ProductId.fromStoredValue("game.dot"), PocketCardId(cardId)),
        title = cardId,
        privileged = false,
    )

    // The collection is stored, decoded and served by a product's worker, so it has failure modes
    // the balance and identity cards do not share. Before the product cards joined this screen
    // nothing product-side could empty it; that must stay true.
    @Test
    fun `a failing product collection costs the product cards alone, not the native ones`() {
        whenever(interactor.observeProductCards()).thenReturn(flow { throw IllegalStateException("unreadable") })

        val cards = createViewModel().awaitCards(count = 2)

        assertEquals(listOf("digital_dollar_card", "id_card"), cards.map { it.id })
    }

    @Test
    fun `product cards follow the native ones once the collection loads`() {
        whenever(interactor.observeProductCards()).thenReturn(flowOf(listOf(productCard("loyalty"))))

        val cards = createViewModel().awaitCards(count = 3)

        assertEquals(
            listOf("digital_dollar_card", "id_card", "product_card:game.dot:loyalty"),
            cards.map { it.id },
        )
        assertEquals("loyalty", cards.filterIsInstance<PocketCardUiModel.ProductCard>().single().title)
    }

    private companion object {
        const val AWAIT_MILLIS = 5_000L
    }
}
