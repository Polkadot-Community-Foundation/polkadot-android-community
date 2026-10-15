package io.paritytech.polkadotapp.feature_swap_impl.domain

import io.paritytech.polkadotapp.chains.multiNetwork.chain.model.Chain
import io.paritytech.polkadotapp.chains.network.binding.Balance
import io.paritytech.polkadotapp.feature_account_api.domain.model.MetaAccount
import io.paritytech.polkadotapp.feature_balances_api.data.type.TokenBalanceTypeRegistry
import javax.inject.Inject

interface AssetInAdditionalSwapDeductionUseCase {
    suspend fun invoke(
        assetIn: Chain.Asset,
        assetOut: Chain.Asset,
        metaAccount: MetaAccount,
    ): Balance
}

class RealAssetInAdditionalSwapDeductionUseCase @Inject constructor(
    private val balanceTypeRegistry: TokenBalanceTypeRegistry,
) : AssetInAdditionalSwapDeductionUseCase {
    override suspend fun invoke(
        assetIn: Chain.Asset,
        assetOut: Chain.Asset,
        metaAccount: MetaAccount,
    ): Balance {
        val assetInBalanceType = balanceTypeRegistry.typeFor(assetIn)

        // The swap withdraws the input with Preservation::Preserve, so the runtime keeps its
        // existential deposit even when a sufficient output asset would keep the account alive.
        // Sizing the swap to the full balance makes it fail with Token::NotExpendable.
        return assetInBalanceType.minimumBalance()
    }
}
