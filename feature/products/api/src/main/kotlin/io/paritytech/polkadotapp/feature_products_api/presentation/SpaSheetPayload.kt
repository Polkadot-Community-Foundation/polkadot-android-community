package io.paritytech.polkadotapp.feature_products_api.presentation

import android.net.Uri
import android.os.Parcelable
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCardKey
import io.paritytech.polkadotapp.feature_products_api.model.ProductId
import kotlinx.parcelize.Parcelize

/**
 * Navigation arg for the SPA sheet: which product to host for the sheet's lifetime, and the query
 * string its launch URL carries, if any.
 */
@Parcelize
data class SpaSheetPayload(
    val productId: String,
    val launchQuery: String?,
) : Parcelable {
    companion object {
        fun forProduct(productId: ProductId) = SpaSheetPayload(productId.value, launchQuery = null)

        /** The card is named in the launch URL, so the product opens on the card the user tapped. */
        fun forPocketCard(key: PocketCardKey) =
            SpaSheetPayload(key.productId.value, launchQuery = "card=${Uri.encode(key.cardId.value)}")
    }
}
