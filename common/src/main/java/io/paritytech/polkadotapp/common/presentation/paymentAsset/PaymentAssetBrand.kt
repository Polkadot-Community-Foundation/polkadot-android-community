package io.paritytech.polkadotapp.common.presentation.paymentAsset

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import io.paritytech.polkadotapp.common.utils.CurrencyConfig

// Static on purpose: the brand changes at most once per app start, and text formatted outside the composition
// (TokenAmountFormatter) only picks up the published symbol when the whole subtree recomposes.
val LocalPaymentAssetBrand = staticCompositionLocalOf { PaymentAssetBrand.bundled(CurrencyConfig.defaultSymbol) }

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

    /** A published logo; screens load it through Coil at their own size and show the bundled mark until it arrives or if it fails. */
    data class Remote(val url: String) : PaymentAssetLogo
}
