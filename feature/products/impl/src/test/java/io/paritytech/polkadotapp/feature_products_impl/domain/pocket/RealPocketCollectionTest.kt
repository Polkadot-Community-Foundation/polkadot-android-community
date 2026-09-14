package io.paritytech.polkadotapp.feature_products_impl.domain.pocket

import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketRemoveError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealPocketCollectionTest {
    private val humanity = pinnedCard(personhoodProduct, "humanity")
    private val loyalty = addedCard(gameProduct, "loyalty")
    private val repository = InMemoryPocketCardRepository()
    private val collection = RealPocketCollection(FakePinnedPocketCards(listOf(humanity)), repository)

    @Test
    fun `pinned cards come first and are present before anything was added`() = runTest {
        assertEquals(listOf(humanity.card), collection.observeCards().first())

        collection.addCard(loyalty)

        assertEquals(listOf(humanity.card, loyalty.card), collection.observeCards().first())
    }

    @Test
    fun `removing a privileged card is refused and changes nothing`() = runTest {
        val outcome = collection.removeCard(humanity.card.key)

        assertEquals(PocketRemoveError.Privileged, outcome.exceptionOrNull())
        assertEquals(listOf(humanity.card), collection.observeCards().first())
        assertEquals(humanity.face, collection.cachedFace(humanity.card.key))
    }

    @Test
    fun `removing an added card drops it and its cached face`() = runTest {
        collection.addCard(loyalty)

        assertTrue(collection.removeCard(loyalty.card.key).isSuccess)

        assertEquals(listOf(humanity.card), collection.observeCards().first())
        assertNull(collection.cachedFace(loyalty.card.key))
    }

    @Test
    fun `removing an absent card succeeds, so a product retry is harmless`() = runTest {
        assertTrue(collection.removeCard(cardKey(gameProduct, "never-added")).isSuccess)
    }

    @Test
    fun `a card added twice keeps one entry with the newest face`() = runTest {
        collection.addCard(loyalty)
        val refreshed = loyalty.copy(face = faceOf("newer"))

        collection.addCard(refreshed)

        assertEquals(listOf(humanity.card, loyalty.card), collection.observeCards().first())
        assertEquals(refreshed.face, collection.cachedFace(loyalty.card.key))
    }

    @Test
    fun `only the host places privileged cards`() = runTest {
        val outcome = runCatching { collection.addCard(humanity) }

        assertTrue(outcome.exceptionOrNull() is IllegalArgumentException)
    }
}
