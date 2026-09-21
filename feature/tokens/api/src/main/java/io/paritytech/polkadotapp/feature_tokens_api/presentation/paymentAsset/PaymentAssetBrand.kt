package io.paritytech.polkadotapp.feature_tokens_api.presentation.paymentAsset

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import io.paritytech.polkadotapp.common.utils.CurrencyConfig

val LocalPaymentAssetBrand = compositionLocalOf { PaymentAssetBrand.bundled(CurrencyConfig.symbol) }

@Immutable
data class PaymentAssetBrand(
    val symbol: String,
    val squareLogo: PaymentAssetLogo,
    val wideLogo: PaymentAssetLogo,
) {
    companion object {
        fun bundled(symbol: String) = PaymentAssetBrand(
            symbol = symbol,
            squareLogo = PaymentAssetLogo.Bundled,
            wideLogo = PaymentAssetLogo.Bundled,
        )
    }
}

@Immutable
sealed interface PaymentAssetLogo {
    /** The mark shipped with the app. */
    data object Bundled : PaymentAssetLogo

    /** A published logo that has already been fetched into the image cache, so screens can draw it at once. */
    data class Remote(val url: String) : PaymentAssetLogo
}
