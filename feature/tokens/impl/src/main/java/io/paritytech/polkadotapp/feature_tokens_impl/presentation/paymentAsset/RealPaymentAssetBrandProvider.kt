package io.paritytech.polkadotapp.feature_tokens_impl.presentation.paymentAsset

import io.paritytech.polkadotapp.common.data.memory.ComputationalScope
import io.paritytech.polkadotapp.common.presentation.AppInitializer
import io.paritytech.polkadotapp.common.utils.CurrencyConfig
import io.paritytech.polkadotapp.common.utils.logFailure
import io.paritytech.polkadotapp.feature_tokens_api.presentation.paymentAsset.PaymentAssetBrand
import io.paritytech.polkadotapp.feature_tokens_api.presentation.paymentAsset.PaymentAssetBrandProvider
import io.paritytech.polkadotapp.feature_tokens_api.presentation.paymentAsset.PaymentAssetLogo
import io.paritytech.polkadotapp.feature_tokens_impl.data.paymentAsset.PaymentAssetConfigProvider
import io.paritytech.polkadotapp.feature_tokens_impl.domain.paymentAsset.PaymentAssetConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RealPaymentAssetBrandProvider @Inject constructor(
    private val configProvider: PaymentAssetConfigProvider,
    private val logoPrefetcher: PaymentAssetLogoPrefetcher,
) : PaymentAssetBrandProvider, AppInitializer {
    override val brand = MutableStateFlow(PaymentAssetBrand.bundled(CurrencyConfig.symbol))

    context(scope: ComputationalScope)
    override fun initialize(): Result<Unit> = runCatching {
        scope.launch { applyPublishedBrand() }
    }

    suspend fun applyPublishedBrand() {
        configProvider.paymentAssetConfig()
            .logFailure("Payment asset config unavailable, keeping the bundled brand")
            .onSuccess { config -> apply(config) }
    }

    private suspend fun apply(config: PaymentAssetConfig?) {
        val symbol = config?.symbol ?: CurrencyConfig.defaultSymbol
        CurrencyConfig.updateSymbol(symbol)
        brand.value = PaymentAssetBrand.bundled(symbol)

        if (config != null) {
            brand.value = coroutineScope {
                val squareLogo = async { config.squareLogoUrl.toLogo() }
                val wideLogo = async { config.wideLogoUrl.toLogo() }

                PaymentAssetBrand(symbol, squareLogo.await(), wideLogo.await())
            }
        }
    }

    private suspend fun String?.toLogo(): PaymentAssetLogo {
        if (this == null) return PaymentAssetLogo.Bundled

        return logoPrefetcher.prefetch(this)
            .logFailure("Payment asset logo unavailable at $this, keeping the bundled one")
            .fold(
                onSuccess = { PaymentAssetLogo.Remote(this) },
                onFailure = { PaymentAssetLogo.Bundled },
            )
    }
}
