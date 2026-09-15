package io.paritytech.polkadotapp.feature_products_impl.domain.pocket

import android.net.Uri
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCardKey
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketFaceSource
import io.paritytech.polkadotapp.feature_products_api.model.JsImageSource
import io.paritytech.polkadotapp.feature_products_api.model.JsWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** Shows the cached face at once, then every live face the product draws, caching the newest. */
class RealPocketFaceSource @Inject constructor(
    private val store: PocketCardStore,
    private val streams: PocketFaceStreams,
    private val images: PocketImageResolver,
) : PocketFaceSource {
    override fun observeFace(key: PocketCardKey): Flow<JsWidget> = flow {
        store.cachedFace(key)?.let { emit(it) }
        streams.renderFaces(key).collect { face ->
            store.cacheFace(key, face)
            emit(face)
        }
    }

    override fun sendAction(key: PocketCardKey, actionId: String, payload: ByteArray) =
        streams.sendAction(key, actionId, payload)

    override suspend fun resolveImage(key: PocketCardKey, source: JsImageSource): Result<Uri> =
        images.resolve(key.productId, source)
}
