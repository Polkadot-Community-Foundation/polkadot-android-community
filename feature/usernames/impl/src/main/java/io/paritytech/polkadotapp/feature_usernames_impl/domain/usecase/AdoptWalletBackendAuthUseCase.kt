package io.paritytech.polkadotapp.feature_usernames_impl.domain.usecase

import io.paritytech.polkadotapp.common.data.keypair.ClientKeypairStore
import io.paritytech.polkadotapp.feature_account_api.data.repository.AccountRepository
import io.paritytech.polkadotapp.feature_account_api.data.storage.accountSecrets.AccountSecretsStorage
import io.paritytech.polkadotapp.feature_account_api.data.storage.accountSecrets.getMetaAccountSr25519Keypair
import io.paritytech.polkadotapp.tools_jwt_auth_api.JwtAuthTokenInvalidator
import javax.inject.Inject

/**
 * Make the wallet's own sr25519 keypair the backend-auth identity.
 *
 * The device-uniqueness-backend refuses a username registration whose
 * `candidateAccountId` is not the authenticated subject (403,
 * device-uniqueness-backend #77). `substrateAccountId` IS the wallet's public
 * key (see MetaAccount), so a JWT minted with this keypair has the wallet as
 * its subject — exactly the candidate a claim registers for.
 *
 * Must run on a normal coroutine, never inside the auth interceptor: that path
 * is entered from `BearerTokenAuthenticator`'s `runBlocking { validToken() }`,
 * so reading account secrets from it risks deadlocking the token mint.
 */
interface AdoptWalletBackendAuthUseCase {
    suspend operator fun invoke()
}

class RealAdoptWalletBackendAuthUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val accountSecretsStorage: AccountSecretsStorage,
    private val clientKeypairStore: ClientKeypairStore,
    private val jwtAuthTokenInvalidator: JwtAuthTokenInvalidator,
) : AdoptWalletBackendAuthUseCase {
    /**
     * No-op when the keypair is already adopted; tokens are only dropped when
     * the identity actually changed, so a retried claim doesn't force a
     * needless re-attestation.
     */
    override suspend operator fun invoke() {
        if (!accountRepository.areAccountsInitialized()) return
        val wallet = accountRepository.getWalletAccount()
        val walletKeypair = accountSecretsStorage.getMetaAccountSr25519Keypair(wallet.id)
        if (clientKeypairStore.getOrGenerate().publicKey.contentEquals(walletKeypair.publicKey)) return

        clientKeypairStore.adopt(walletKeypair)
        // Both tokens: a refresh would resurrect the pre-wallet subject
        // instead of re-attesting as the wallet.
        jwtAuthTokenInvalidator.invalidate()
    }
}
