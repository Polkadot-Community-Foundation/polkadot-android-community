package io.paritytech.polkadotapp.feature_products_impl.domain.truapi

import io.parity.truapi.PocketHostBridge
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCard
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCardId
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCardKey
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketRemoveError
import io.paritytech.polkadotapp.feature_products_api.model.ProductId
import io.paritytech.polkadotapp.feature_products_impl.domain.pocket.PocketCardStore
import io.paritytech.polkadotapp.feature_products_impl.domain.pocket.PocketRemoval
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import uniffi.truapi_server.NativePocketRemoval
import java.util.concurrent.atomic.AtomicReference
import uniffi.truapi.PocketCard as NativePocketCard

/**
 * Serves one product's slice of the collection to the core. Both callbacks run inline on the core's
 * dispatcher thread: the list is answered from a snapshot, and a removal completes before returning
 * because the core reads the list again right after it and republishes that answer.
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

    override fun removeCard(cardId: String): NativePocketRemoval {
        val removal = runBlocking { store.remove(PocketCardKey(productId, PocketCardId(cardId))) }
        return removal.fold(
            onSuccess = { outcome ->
                snapshot.updateAndGet { cards -> cards.filterNot { it.cardId == cardId } }
                outcome.toNative()
            },
            onFailure = { failure ->
                if (failure is PocketRemoveError.Privileged) NativePocketRemoval.PRIVILEGED else throw failure
            },
        )
    }

    private fun PocketRemoval.toNative() = when (this) {
        PocketRemoval.REMOVED -> NativePocketRemoval.REMOVED
        PocketRemoval.ABSENT -> NativePocketRemoval.ABSENT
    }

    private fun PocketCard.toNative() = NativePocketCard(cardId = key.cardId.value, privileged = privileged)
}
