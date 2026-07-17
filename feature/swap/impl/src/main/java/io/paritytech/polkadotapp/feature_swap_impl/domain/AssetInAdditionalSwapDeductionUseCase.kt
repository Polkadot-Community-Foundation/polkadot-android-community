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

        // The AssetConversion swap withdraws the input asset with a keep-alive
        // (Preservation::Preserve) guarantee. The runtime enforces that as "the
        // input balance must stay >= its existential deposit" at withdrawal time,
        // *regardless* of whether a sufficient output asset would later keep the
        // account alive — a same-chain sufficient output does NOT lift this
        // constraint (verified on-chain: swapping the full balance fails with
        // Token::NotExpendable / FundsUnavailable even when the account already
        // holds a sufficient asset). So we must always leave the input asset's
        // existential deposit aside; otherwise the swap gets sized to the full
        // balance and reverts, which on the auto-convert ("Get Cash") path shows
        // up as an endless retry loop.
        return assetInBalanceType.minimumBalance()
    }
}
