package io.paritytech.polkadotapp.feature_wallet_impl

import io.paritytech.polkadotapp.common.presentation.navigation.ReturnableRouter
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCardKey
import io.paritytech.polkadotapp.feature_products_api.model.ProductId
import io.paritytech.polkadotapp.feature_wallet_api.presentation.enterAmount.SendEnterAmountPayload

interface PocketRouter : ReturnableRouter {
    fun openSendPayment()

    fun openSendEnterAmount(payload: SendEnterAmountPayload)

    fun openSendEnterAmountFromDeeplink(payload: SendEnterAmountPayload)

    fun openSuccess()

    fun openFailure()

    fun openScanAddressQr()

    fun openCollectibles()

    fun openProduct(productId: ProductId)

    /** Expands a Pocket card: the product opens with the card named in its launch URL. */
    fun openProductCard(key: PocketCardKey)
}
