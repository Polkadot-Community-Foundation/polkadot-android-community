package io.paritytech.polkadotapp.feature_products_impl.domain.pocket

import io.paritytech.polkadotapp.feature_products_api.model.JsWidget
import io.paritytech.polkadotapp.feature_products_api.model.ProductId
import io.paritytech.polkadotapp.feature_products_impl.domain.worker.ProductWorker
import io.paritytech.polkadotapp.feature_products_impl.domain.worker.ProductWorkerRefCounter
import io.paritytech.polkadotapp.feature_products_impl.domain.worker.ProductWorkerReference
import io.paritytech.polkadotapp.feature_products_impl.domain.worker.WorkerModalityApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class RealPocketFaceSourceTest {
    private class FakeRefCounter : ProductWorkerRefCounter {
        val acquired = mutableListOf<String>()
        var released = 0

        override suspend fun acquire(productId: ProductId, label: String): ProductWorkerReference {
            acquired += "$productId|$label"
            return object : ProductWorkerReference {
                private val done = AtomicBoolean(false)
                override suspend fun worker(): ProductWorker = error("unused")
                override suspend fun enableModalityApi(api: WorkerModalityApi) = error("unused")
                override fun release() {
                    if (done.compareAndSet(false, true)) released++
                }
            }
        }
    }

    private val loyalty = addedCard(gameProduct, "loyalty")
    private val repository = InMemoryPocketCardRepository()
    private val store = RealPocketCollection(FakePinnedPocketCards(emptyList()), repository)
    private val refCounter = FakeRefCounter()
    private val source = RealPocketFaceSource(store, refCounter)

    @Test
    fun `a collected face holds exactly one reference labelled for the card and releases it when it leaves`() = runTest {
        store.addCard(loyalty)
        val faces = mutableListOf<JsWidget>()

        val onScreen = source.observeFace(loyalty.card.key).onEach { faces += it }.launchIn(this)
        advanceUntilIdle()

        assertEquals(listOf(loyalty.face), faces)
        assertEquals(listOf("$gameProduct|pocket:loyalty"), refCounter.acquired)
        assertEquals(0, refCounter.released)

        onScreen.cancel()
        advanceUntilIdle()

        assertEquals(1, refCounter.released)
    }

    @Test
    fun `two faces of one product hold two references, so the worker outlives either alone`() = runTest {
        store.addCard(loyalty)
        store.addCard(addedCard(gameProduct, "receipt"))

        val first = source.observeFace(loyalty.card.key).launchIn(this)
        val second = source.observeFace(cardKey(gameProduct, "receipt")).launchIn(this)
        advanceUntilIdle()
        first.cancel()
        advanceUntilIdle()

        assertEquals(2, refCounter.acquired.size)
        assertEquals(1, refCounter.released)

        second.cancel()
        advanceUntilIdle()
        assertEquals(2, refCounter.released)
    }

    @Test
    fun `a card with no cached face still keeps its worker while on screen`() = runTest {
        val onScreen = source.observeFace(cardKey(gameProduct, "unknown")).launchIn(this)
        advanceUntilIdle()

        assertEquals(1, refCounter.acquired.size)

        onScreen.cancel()
        advanceUntilIdle()
        assertEquals(1, refCounter.released)
    }
}
