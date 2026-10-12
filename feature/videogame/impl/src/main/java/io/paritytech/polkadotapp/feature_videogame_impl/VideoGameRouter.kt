package io.paritytech.polkadotapp.feature_videogame_impl

import io.paritytech.polkadotapp.common.presentation.navigation.ReturnableRouter
import io.paritytech.polkadotapp.feature_products_api.model.ProductId

interface VideoGameRouter : ReturnableRouter {
    suspend fun openGameProduct(productId: ProductId)
}
