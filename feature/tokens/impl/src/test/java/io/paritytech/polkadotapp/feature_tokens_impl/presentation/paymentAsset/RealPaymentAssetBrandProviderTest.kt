package io.paritytech.polkadotapp.feature_tokens_impl.presentation.paymentAsset

import io.paritytech.polkadotapp.common.utils.CurrencyConfig
import io.paritytech.polkadotapp.feature_tokens_api.presentation.paymentAsset.PaymentAssetBrand
import io.paritytech.polkadotapp.feature_tokens_api.presentation.paymentAsset.PaymentAssetLogo
import io.paritytech.polkadotapp.feature_tokens_impl.data.paymentAsset.PaymentAssetConfigProvider
import io.paritytech.polkadotapp.feature_tokens_impl.domain.paymentAsset.PaymentAssetConfig
import io.paritytech.polkadotapp.test_shared.any
import io.paritytech.polkadotapp.test_shared.whenever
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

private const val SQUARE_URL = "https://cdn.example.com/square.svg"
private const val WIDE_URL = "https://cdn.example.com/wide.svg"

class RealPaymentAssetBrandProviderTest {
    private val configProvider: PaymentAssetConfigProvider = mock(PaymentAssetConfigProvider::class.java)
    private val logoPrefetcher: PaymentAssetLogoPrefetcher = mock(PaymentAssetLogoPrefetcher::class.java)

    private val provider = RealPaymentAssetBrandProvider(configProvider, logoPrefetcher)

    @After
    fun tearDown() {
        CurrencyConfig.updateSymbol(CurrencyConfig.defaultSymbol)
    }

    @Test
    fun `should start from the bundled brand with the default symbol`() {
        assertEquals(PaymentAssetBrand.bundled(CurrencyConfig.defaultSymbol), provider.brand.value)
    }

    @Test
    fun `should apply the published symbol and the fetched logos`() = runBlocking<Unit> {
        withPublishedConfig(PaymentAssetConfig("USD", SQUARE_URL, WIDE_URL))
        withLogoAvailable(SQUARE_URL)
        withLogoAvailable(WIDE_URL)

        provider.applyPublishedBrand()

        assertEquals(
            PaymentAssetBrand("USD", PaymentAssetLogo.Remote(SQUARE_URL), PaymentAssetLogo.Remote(WIDE_URL)),
            provider.brand.value
        )
        assertEquals("USD", CurrencyConfig.symbol)
    }

    @Test
    fun `should apply the symbol before the logos are fetched`() = runBlocking<Unit> {
        withPublishedConfig(PaymentAssetConfig("USD", SQUARE_URL, null))
        val recordingPrefetcher = BrandRecordingPrefetcher()
        val provider = RealPaymentAssetBrandProvider(configProvider, recordingPrefetcher)
        recordingPrefetcher.brand = provider.brand

        provider.applyPublishedBrand()

        assertEquals(PaymentAssetBrand.bundled("USD"), recordingPrefetcher.brandWhileFetching)
    }

    @Test
    fun `should keep the bundled logo when its fetch fails`() = runBlocking<Unit> {
        withPublishedConfig(PaymentAssetConfig(null, SQUARE_URL, WIDE_URL))
        withLogoUnavailable(SQUARE_URL)
        withLogoAvailable(WIDE_URL)

        provider.applyPublishedBrand()

        assertEquals(
            PaymentAssetBrand(
                symbol = CurrencyConfig.defaultSymbol,
                squareLogo = PaymentAssetLogo.Bundled,
                wideLogo = PaymentAssetLogo.Remote(WIDE_URL)
            ),
            provider.brand.value
        )
    }

    @Test
    fun `should keep the bundled brand when the object is not published`() = runBlocking<Unit> {
        withPublishedConfig(null)

        provider.applyPublishedBrand()

        assertEquals(PaymentAssetBrand.bundled(CurrencyConfig.defaultSymbol), provider.brand.value)
        verifyNoLogoFetched()
    }

    @Test
    fun `should keep the bundled brand when the config cannot be read`() = runBlocking<Unit> {
        withConfigReadFailure()

        provider.applyPublishedBrand()

        assertEquals(PaymentAssetBrand.bundled(CurrencyConfig.defaultSymbol), provider.brand.value)
        assertEquals(CurrencyConfig.defaultSymbol, CurrencyConfig.symbol)
        verifyNoLogoFetched()
    }

    private suspend fun withPublishedConfig(config: PaymentAssetConfig?) {
        whenever(configProvider.paymentAssetConfig()).thenReturn(Result.success(config))
    }

    private suspend fun withConfigReadFailure() {
        whenever(configProvider.paymentAssetConfig()).thenReturn(Result.failure(IllegalStateException("not synced")))
    }

    private suspend fun withLogoAvailable(url: String) {
        whenever(logoPrefetcher.prefetch(url)).thenReturn(Result.success(Unit))
    }

    private suspend fun withLogoUnavailable(url: String) {
        whenever(logoPrefetcher.prefetch(url)).thenReturn(Result.failure(IllegalStateException("404")))
    }

    private suspend fun verifyNoLogoFetched() {
        verify(logoPrefetcher, never()).prefetch(any())
    }

    /** Mockito cannot answer a suspend call with a value class; records the brand seen mid-fetch instead. */
    private class BrandRecordingPrefetcher : PaymentAssetLogoPrefetcher {
        lateinit var brand: StateFlow<PaymentAssetBrand>

        var brandWhileFetching: PaymentAssetBrand? = null

        override suspend fun prefetch(url: String): Result<Unit> {
            brandWhileFetching = brand.value
            return Result.success(Unit)
        }
    }
}
