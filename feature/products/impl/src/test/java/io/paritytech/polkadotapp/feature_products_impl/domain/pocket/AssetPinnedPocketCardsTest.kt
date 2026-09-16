package io.paritytech.polkadotapp.feature_products_impl.domain.pocket

import android.content.Context
import android.content.res.AssetManager
import io.paritytech.polkadotapp.feature_dotns_api.domain.DotNsTld
import io.paritytech.polkadotapp.feature_dotns_api.domain.DotNsTldProvider
import io.paritytech.polkadotapp.feature_products_api.model.derivation.ReservedProductIds
import io.paritytech.polkadotapp.feature_products_impl.domain.truapi.renderer.RendererNodeJsonDecoder
import org.mockito.ArgumentMatchers.anyInt
import io.paritytech.polkadotapp.test_shared.whenever
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import java.io.ByteArrayInputStream

private const val FACE = """{"tag":"Text","value":{"modifiers":[],"props":{"style":"TitleMediumRegular",
    "color":"FgPrimary"},"children":[{"tag":"String","value":{"text":"Humanity"}}]}}"""

class AssetPinnedPocketCardsTest {
    private val tld = requireNotNull(DotNsTld.parse("testnet"))
    private val context: Context = mock()
    private val assets: AssetManager = mock()
    private val tldProvider: DotNsTldProvider = mock()

    private suspend fun pinnedCards(): List<CachedPocketCard> {
        whenever(context.assets).thenReturn(assets)
        whenever(assets.open("pocket/humanity.json")).thenReturn(ByteArrayInputStream(FACE.toByteArray()))
        whenever(context.getString(anyInt())).thenReturn("Humanity")
        whenever(tldProvider.getTld()).thenReturn(Result.success(tld))

        return AssetPinnedPocketCards(context, tldProvider, PocketFaceJsonDecoder(RendererNodeJsonDecoder())).cards()
    }

    // Balance and Scarcity were dropped: the products behind them publish no Pocket cards, so pinning
    // them left two placeholder cards that duplicated the host's own native ones.
    @Test
    fun `the host pins Humanity alone, on the personhood product`() = runBlocking {
        val cards = pinnedCards()

        assertEquals(1, cards.size)
        assertEquals(ReservedProductIds.personhood(tld), cards.single().card.key.productId)
        assertEquals("humanity", cards.single().card.key.cardId.value)
    }

    @Test
    fun `a host-placed card is privileged, so nothing can remove it`() = runBlocking {
        assertTrue(pinnedCards().single().card.privileged)
    }
}
