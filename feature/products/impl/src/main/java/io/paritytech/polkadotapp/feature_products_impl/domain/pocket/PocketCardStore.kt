package io.paritytech.polkadotapp.feature_products_impl.domain.pocket

import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCardKey
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCollection
import io.paritytech.polkadotapp.feature_products_api.model.JsWidget

/** The collection as the host's own flows see it: the public read side plus the writes only the host makes. */
interface PocketCardStore : PocketCollection {
    /** Inserts a card the user approved, replacing the face of one already present. */
    suspend fun addCard(card: CachedPocketCard)

    /** The newest face held for [key]: bundled for a pinned card, approved for an added one. */
    suspend fun cachedFace(key: PocketCardKey): JsWidget?
}
