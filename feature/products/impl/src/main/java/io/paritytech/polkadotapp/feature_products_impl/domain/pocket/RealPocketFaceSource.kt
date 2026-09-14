package io.paritytech.polkadotapp.feature_products_impl.domain.pocket

import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCardKey
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketFaceSource
import io.paritytech.polkadotapp.feature_products_api.model.JsWidget
import io.paritytech.polkadotapp.feature_products_impl.domain.worker.ProductWorkerRefCounter
import io.paritytech.polkadotapp.feature_products_impl.domain.worker.withWorkerAcquired
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Serves the cached face and holds one worker reference per collected face. A live render stream
 * from the worker is merged here later, without the screen noticing.
 */
class RealPocketFaceSource @Inject constructor(
    private val store: PocketCardStore,
    private val refCounter: ProductWorkerRefCounter,
) : PocketFaceSource {
    override fun observeFace(key: PocketCardKey): Flow<JsWidget> = flow {
        refCounter.withWorkerAcquired(key.productId, "pocket:${key.cardId.value}") {
            store.cachedFace(key)?.let { emit(it) }
            awaitCancellation()
        }
    }
}
