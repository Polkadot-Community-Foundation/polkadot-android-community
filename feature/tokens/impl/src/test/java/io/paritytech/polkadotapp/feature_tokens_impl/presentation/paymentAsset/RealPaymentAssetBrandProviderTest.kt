package io.paritytech.polkadotapp.feature_tokens_impl.presentation.paymentAsset

import io.paritytech.polkadotapp.common.presentation.paymentAsset.PaymentAssetBrand
import io.paritytech.polkadotapp.common.presentation.paymentAsset.PaymentAssetLogo
import io.paritytech.polkadotapp.common.utils.CurrencyConfig
import io.paritytech.polkadotapp.feature_tokens_impl.data.paymentAsset.PaymentAssetConfigProvider
import io.paritytech.polkadotapp.feature_tokens_impl.domain.paymentAsset.PaymentAssetConfig
import io.paritytech.polkadotapp.test_shared.whenever
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

private const val SQUARE_URL = "https://cdn.example.com/square.svg"
private const val WIDE_URL = "https://cdn.example.com/wide.svg"

class RealPaymentAssetBrandProviderTest {
    private val configProvider: PaymentAssetConfigProvider = mock(PaymentAssetConfigProvider::class.java)

    private val provider = RealPaymentAssetBrandProvider(configProvider)

    @Test
    fun `should start from the bundled brand with the default symbol`() {
        assertEquals(PaymentAssetBrand.bundled(CurrencyConfig.defaultSymbol), provider.brand.value)
    }

    @Test
    fun `should expose the published symbol and remote logos at once`() = runBlocking<Unit> {
        withPublishedConfig(PaymentAssetConfig("USD", SQUARE_URL, WIDE_URL))

        provider.applyPublishedBrand()

        assertEquals(
            PaymentAssetBrand("USD", PaymentAssetLogo.Remote(SQUARE_URL), PaymentAssetLogo.Remote(WIDE_URL)),
            provider.brand.value
        )
    }

    @Test
    fun `should keep the default symbol when only logos are published`() = runBlocking<Unit> {
        withPublishedConfig(PaymentAssetConfig(null, SQUARE_URL, WIDE_URL))

        provider.applyPublishedBrand()

        assertEquals(
            PaymentAssetBrand(
                symbol = CurrencyConfig.defaultSymbol,
                squareLogo = PaymentAssetLogo.Remote(SQUARE_URL),
                wideLogo = PaymentAssetLogo.Remote(WIDE_URL)
            ),
            provider.brand.value
        )
    }

    @Test
    fun `should keep the bundled logo for a variant that is not published`() = runBlocking<Unit> {
        withPublishedConfig(PaymentAssetConfig("USD", SQUARE_URL, null))

        provider.applyPublishedBrand()

        assertEquals(
            PaymentAssetBrand("USD", PaymentAssetLogo.Remote(SQUARE_URL), PaymentAssetLogo.Bundled),
            provider.brand.value
        )
    }

    @Test
    fun `should keep the bundled brand when the object is not published`() = runBlocking<Unit> {
        withPublishedConfig(null)

        provider.applyPublishedBrand()

        assertEquals(PaymentAssetBrand.bundled(CurrencyConfig.defaultSymbol), provider.brand.value)
    }

    @Test
    fun `should keep the bundled brand when the config cannot be read`() = runBlocking<Unit> {
        withConfigReadFailure()

        provider.applyPublishedBrand()

        assertEquals(PaymentAssetBrand.bundled(CurrencyConfig.defaultSymbol), provider.brand.value)
    }

    private suspend fun withPublishedConfig(config: PaymentAssetConfig?) {
        whenever(configProvider.paymentAssetConfig()).thenReturn(Result.success(config))
    }

    private suspend fun withConfigReadFailure() {
        whenever(configProvider.paymentAssetConfig()).thenReturn(Result.failure(IllegalStateException("not synced")))
    }
}
