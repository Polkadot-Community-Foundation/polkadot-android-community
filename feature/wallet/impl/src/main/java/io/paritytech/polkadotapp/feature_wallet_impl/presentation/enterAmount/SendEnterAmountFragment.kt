package io.paritytech.polkadotapp.feature_wallet_impl.presentation.enterAmount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.paritytech.polkadotapp.common.presentation.screens.BaseComposeFragment
import io.paritytech.polkadotapp.feature_tokens_api.presentation.formatter.LocalTokenAmountFormatter
import io.paritytech.polkadotapp.feature_tokens_api.presentation.formatter.TokenAmountFormatter
import io.paritytech.polkadotapp.feature_tokens_api.presentation.paymentAsset.LocalPaymentAssetBrand
import io.paritytech.polkadotapp.feature_tokens_api.presentation.paymentAsset.PaymentAssetBrandProvider
import io.paritytech.polkadotapp.feature_wallet_impl.presentation.enterAmount.compose.SendEnterAmountScreen
import javax.inject.Inject

@AndroidEntryPoint
class SendEnterAmountFragment : BaseComposeFragment<SendEnterAmountViewModel>() {
    @Inject
    lateinit var tokenAmountFormatter: TokenAmountFormatter

    @Inject
    lateinit var paymentAssetBrandProvider: PaymentAssetBrandProvider

    override val viewModel: SendEnterAmountViewModel by viewModels()

    @Composable
    override fun Screen() {
        val paymentAssetBrand by paymentAssetBrandProvider.brand.collectAsStateWithLifecycle()

        CompositionLocalProvider(
            LocalTokenAmountFormatter provides tokenAmountFormatter,
            LocalPaymentAssetBrand provides paymentAssetBrand,
        ) {
            SendEnterAmountScreen(viewModel)
        }
    }
}
