package io.paritytech.polkadotapp.feature_products_api.domain.pocket

import io.paritytech.polkadotapp.feature_products_api.model.JsWidget
import kotlinx.coroutines.flow.Flow

/** Where a card's face tree comes from. */
interface PocketFaceSource {
    /**
     * The face for [key], starting with the cached tree. The backing product's worker is kept
     * running for as long as the flow is collected, so collect it only while the face is on screen.
     */
    fun observeFace(key: PocketCardKey): Flow<JsWidget>
}
