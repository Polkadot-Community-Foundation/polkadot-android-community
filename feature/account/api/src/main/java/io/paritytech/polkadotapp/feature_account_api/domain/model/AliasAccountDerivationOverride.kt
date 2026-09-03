package io.paritytech.polkadotapp.feature_account_api.domain.model

import io.paritytech.polkadotapp.bandersnatch_crypto.BandersnatchContext

/**
 * [derivationPath] is deferred: paths that follow a product subtree depend on the network's
 * dotNS TLD, which is read from chain and may not be known at DI-graph build time.
 */
class AliasAccountDerivationOverride(
    val context: BandersnatchContext,
    val derivationPath: suspend () -> String
)
