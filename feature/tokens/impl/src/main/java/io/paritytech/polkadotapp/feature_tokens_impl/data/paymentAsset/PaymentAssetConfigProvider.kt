package io.paritytech.polkadotapp.feature_tokens_impl.data.paymentAsset

import io.paritytech.polkadotapp.feature_tokens_impl.data.mappers.toDomain
import io.paritytech.polkadotapp.feature_tokens_impl.domain.paymentAsset.PaymentAssetConfig
import io.paritytech.polkadotapp.tools_remoteconfig_api.RemoteConfigService
import io.paritytech.polkadotapp.tools_remoteconfig_api.getSyncedJsonObject
import javax.inject.Inject

internal interface PaymentAssetConfigProvider {
    suspend fun paymentAssetConfig(): Result<PaymentAssetConfig?>
}

internal class RemoteConfigPaymentAssetConfigProvider @Inject constructor(
    private val remoteConfigService: RemoteConfigService,
) : PaymentAssetConfigProvider {
    override suspend fun paymentAssetConfig(): Result<PaymentAssetConfig?> {
        return remoteConfigService.getSyncedJsonObject<PaymentAssetConfigRemote>(CONFIG_KEY)
            // Gson bypasses Kotlin nullability: an unset key reads back as an empty string, which parses to null.
            .map { remote: PaymentAssetConfigRemote? -> remote?.toDomain() }
    }

    private companion object {
        const val CONFIG_KEY = "payment_asset_config"
    }
}
