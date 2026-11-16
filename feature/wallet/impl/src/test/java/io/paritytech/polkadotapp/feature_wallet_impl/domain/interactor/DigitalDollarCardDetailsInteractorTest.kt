package io.paritytech.polkadotapp.feature_wallet_impl.domain.interactor

import io.paritytech.polkadotapp.common.data.network.TestnetEnvironment
import io.paritytech.polkadotapp.feature_account_api.data.repository.AccountRepository
import io.paritytech.polkadotapp.feature_coinage_api.domain.service.CoinageBackupService
import io.paritytech.polkadotapp.feature_coinage_api.domain.usecase.CoinageBalanceConverterUseCase
import io.paritytech.polkadotapp.feature_coinage_api.domain.usecase.CoinageHoldingsUseCase
import io.paritytech.polkadotapp.feature_coinage_api.domain.usecase.CoinageTestnetFundUseCase
import io.paritytech.polkadotapp.feature_coinage_api.domain.usecase.ShareCoinageLogsUseCase
import io.paritytech.polkadotapp.feature_dotns_api.domain.DotNsTld
import io.paritytech.polkadotapp.feature_dotns_api.domain.DotNsTldProvider
import io.paritytech.polkadotapp.feature_products_api.domain.FundingDomainProvider
import io.paritytech.polkadotapp.feature_tokens_api.domain.ChainAssetProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class DigitalDollarCardDetailsInteractorTest {
    @Test
    fun `the faucet URL names the network's faucet product and pays the given address`() {
        val url = DigitalDollarCardDetailsInteractor.faucetUrl(
            tld = requireNotNull(DotNsTld.parse("dot")),
            recipient = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
        )

        assertEquals("faucet.dot/?address=5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY", url)
    }

    @Test
    fun `only the devnet builds top up through the faucet`() {
        val opensFaucet = TestnetEnvironment.entries.associateWith { interactorIn(it).topUpOpensFaucet() }

        assertEquals(
            mapOf(
                TestnetEnvironment.TESTNET to false,
                TestnetEnvironment.NIGHTLY to true,
                TestnetEnvironment.DEV to true,
                TestnetEnvironment.PRODUCTION to false,
            ),
            opensFaucet,
        )
    }

    private fun interactorIn(environment: TestnetEnvironment) = DigitalDollarCardDetailsInteractor(
        chainAssetProvider = mock(ChainAssetProvider::class.java),
        environment = environment,
        coinageTestnetFundUseCase = mock(CoinageTestnetFundUseCase::class.java),
        coinageBackupService = mock(CoinageBackupService::class.java),
        shareCoinageLogsUseCase = mock(ShareCoinageLogsUseCase::class.java),
        fundingDomainProvider = mock(FundingDomainProvider::class.java),
        coinageHoldingsUseCase = mock(CoinageHoldingsUseCase::class.java),
        coinageBalanceConverterUseCase = mock(CoinageBalanceConverterUseCase::class.java),
        accountRepository = mock(AccountRepository::class.java),
        dotNsTldProvider = mock(DotNsTldProvider::class.java),
    )
}
