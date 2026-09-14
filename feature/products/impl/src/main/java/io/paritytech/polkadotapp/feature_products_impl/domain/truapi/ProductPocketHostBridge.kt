package io.paritytech.polkadotapp.feature_products_impl.domain.truapi

import io.parity.truapi.PocketHostBridge
import io.paritytech.polkadotapp.common.utils.logFailure
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCard
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCardId
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCardKey
import io.paritytech.polkadotapp.feature_products_api.model.ProductId
import io.paritytech.polkadotapp.feature_products_impl.domain.pocket.PocketCardStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import uniffi.truapi.PocketCard as NativePocketCard

/**
 * Serves one product's slice of the collection to the core. Both callbacks run inline on the core's
 * dispatcher thread, so the list is answered from a snapshot and a removal is handed to [scope];
 * the product sees it land through the republished list.
 */
class ProductPocketHostBridge(
    private val productId: ProductId,
    private val store: PocketCardStore,
    private val scope: CoroutineScope,
) : PocketHostBridge {
    private val snapshot = AtomicReference<List<NativePocketCard>>(emptyList())

    /** Keeps the snapshot current and republishes the product's cards on every collection change. */
    fun start(republish: (List<NativePocketCard>) -> Unit) {
        scope.launch {
            store.observeCards()
                .map { cards -> cards.filter { it.key.productId == productId }.map { it.toNative() } }
                .collect { cards ->
                    snapshot.set(cards)
                    republish(cards)
                }
        }
    }

    override fun listCards(): List<NativePocketCard> = snapshot.get()

    override fun removeCard(cardId: String) {
        scope.launch {
            store.removeCard(PocketCardKey(productId, PocketCardId(cardId)))
                .logFailure("truapi.pocket.remove_card: $cardId")
        }
    }

    private fun PocketCard.toNative() = NativePocketCard(cardId = key.cardId.value, privileged = privileged)
}
