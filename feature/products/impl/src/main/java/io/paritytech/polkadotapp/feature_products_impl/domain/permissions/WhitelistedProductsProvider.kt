package io.paritytech.polkadotapp.feature_products_impl.domain.permissions

import io.paritytech.polkadotapp.common.utils.FeatureOption
import io.paritytech.polkadotapp.common.utils.isDisabled
import io.paritytech.polkadotapp.common.utils.isEnabled
import io.paritytech.polkadotapp.common.utils.logFailure
import io.paritytech.polkadotapp.feature_products_api.domain.FundingDomainProvider
import io.paritytech.polkadotapp.feature_products_api.model.ProductId
import javax.inject.Inject

interface WhitelistedProductsProvider {
    suspend fun whitelistedProducts(): Set<ProductId>
}

class RealWhitelistedProductsProvider @Inject constructor(
    private val fundingDomainProvider: FundingDomainProvider,
) : WhitelistedProductsProvider {
    // The funding product is whitelisted only while there is no product settings UI to grant it
    // permissions through, and only while Get CASH is enabled at all — while it is gated off
    // (FeatureOption.GET_CASH_PRODUCT) nothing should be pre-granted on its behalf.
    override suspend fun whitelistedProducts(): Set<ProductId> {
        if (FeatureOption.PRODUCT_SETTINGS.isEnabled) return emptySet()
        if (FeatureOption.GET_CASH_PRODUCT.isDisabled) return emptySet()

        return fundingDomainProvider.getFundingProductId()
            .logFailure("Failed to resolve the funding product to whitelist")
            .fold({ setOf(it) }, { emptySet() })
    }
}
