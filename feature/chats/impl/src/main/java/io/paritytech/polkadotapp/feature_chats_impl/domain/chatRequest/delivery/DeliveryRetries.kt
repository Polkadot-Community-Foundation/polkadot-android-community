package io.paritytech.polkadotapp.feature_chats_impl.domain.chatRequest.delivery

import io.paritytech.polkadotapp.feature_chats_impl.domain.identitySigning.IdentitySigningDeclined
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val MAX_BACKOFF_DOUBLINGS = 8
private val FIRST_RETRY_DELAY = 2.seconds
private val MAX_RETRY_DELAY = 5.minutes

internal fun deliveryRetryDelay(attempt: Int): Duration {
    val exponential = FIRST_RETRY_DELAY * (1 shl attempt.coerceAtMost(MAX_BACKOFF_DOUBLINGS))
    return exponential.coerceAtMost(MAX_RETRY_DELAY)
}

/** Retrying cannot help: the user declined the identity signer, or the request id cannot be derived from. */
internal fun Throwable.endsDelivery(): Boolean {
    return this is IdentitySigningDeclined || this is InvalidRequestIdJunction
}
