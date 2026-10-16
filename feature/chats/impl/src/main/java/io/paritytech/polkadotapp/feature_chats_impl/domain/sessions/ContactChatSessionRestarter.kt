package io.paritytech.polkadotapp.feature_chats_impl.domain.sessions

import io.paritytech.polkadotapp.common.domain.model.AccountId

interface ContactChatSessionRestarter {
    /** Replaces the running session of [accountId], dropping whatever it still had queued in memory. */
    fun restartSession(accountId: AccountId)
}
