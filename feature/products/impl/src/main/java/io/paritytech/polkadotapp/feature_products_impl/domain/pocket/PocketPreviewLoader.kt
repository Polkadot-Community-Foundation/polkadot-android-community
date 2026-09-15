package io.paritytech.polkadotapp.feature_products_impl.domain.pocket

import io.paritytech.polkadotapp.common.utils.flatMap
import io.paritytech.polkadotapp.feature_products_api.model.JsWidget
import io.paritytech.polkadotapp.feature_products_api.model.PocketCardDefinition
import io.paritytech.polkadotapp.feature_products_api.model.ProductId
import io.paritytech.polkadotapp.feature_products_impl.domain.product.ProductWorkerArchive
import javax.inject.Inject

/** Reads a published card's static face out of the product's worker archive. */
class PocketPreviewLoader @Inject constructor(
    private val archive: ProductWorkerArchive,
    private val faceDecoder: PocketFaceJsonDecoder,
) {
    suspend fun load(productId: ProductId, definition: PocketCardDefinition): Result<JsWidget> =
        archive.file(productId, definition.preview)
            .mapCatching { it.readText() }
            .flatMap(faceDecoder::decode)
}
