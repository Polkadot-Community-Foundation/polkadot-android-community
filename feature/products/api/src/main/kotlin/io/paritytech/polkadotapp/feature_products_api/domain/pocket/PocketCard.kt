package io.paritytech.polkadotapp.feature_products_api.domain.pocket

import io.paritytech.polkadotapp.feature_products_api.model.ProductId

/** The label a product declares for one of its cards, unique within that product. */
@JvmInline
value class PocketCardId(val value: String)

data class PocketCardKey(
    val productId: ProductId,
    val cardId: PocketCardId,
)

/** One card in the host's Pocket collection. A [privileged] card is host-placed and removable by nobody. */
data class PocketCard(
    val key: PocketCardKey,
    val title: String,
    val privileged: Boolean,
)
