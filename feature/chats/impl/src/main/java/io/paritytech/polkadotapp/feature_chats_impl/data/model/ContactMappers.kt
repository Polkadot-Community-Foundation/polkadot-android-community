package io.paritytech.polkadotapp.feature_chats_impl.data.model

import io.paritytech.polkadotapp.common.data.os.OperatingSystem
import io.paritytech.polkadotapp.common.domain.model.X25519PublicKey
import io.paritytech.polkadotapp.common.domain.model.intoAccountId
import io.paritytech.polkadotapp.common.domain.model.toDataByteArray
import io.paritytech.polkadotapp.database.model.ContactLocal
import io.paritytech.polkadotapp.database.model.ContactWithChatRequestLocal
import io.paritytech.polkadotapp.database.model.ContactWithRequestTimestampLocal
import io.paritytech.polkadotapp.feature_account_api.domain.model.SharedSecretDerivationDomain
import io.paritytech.polkadotapp.feature_chats_api.domain.model.Contact
import io.paritytech.polkadotapp.feature_chats_api.domain.model.ContactWithChatRequest
import io.paritytech.polkadotapp.feature_chats_impl.domain.models.toDomain
import timber.log.Timber
import kotlin.time.Instant
import io.paritytech.polkadotapp.feature_chats_api.domain.model.ContactWithRequestTimestamp as DomainContactWithRequestTimestamp

/**
 * PCF FORK-LOCAL — keep on upstream conflict; see `LegacyEcdhKeyCleanup` for why.
 *
 * Maps a stored contact, or `null` when its chat key cannot be parsed.
 *
 * `contacts.chatKey` is a plain BLOB, so it can hold bytes that are not a valid X25519 public key —
 * concretely, the 65-byte uncompressed P-256 point that pre-X25519 builds copied here from the
 * peer's on-chain `ConsumerInfo.identifierKey`. No schema migration rewrote them, because the
 * column's *structure* never changed, only the rule about its *value*. This mapper feeds Room
 * Flows, where a throw escapes onto a background dispatcher, cannot be caught by any collector, and
 * kills the process; so an unparseable row is reported as "no such contact" instead.
 *
 * ## What this costs, on purpose
 *
 * On an install upgraded across that change, *every* contact is affected, so the app comes up with
 * an empty contact and chat list rather than crashing. That is the deliberate trade — do not read
 * it as a recoverable state:
 *
 * - The stored bytes are a P-256 point. Nothing can convert one into an X25519 key, so there is no
 *   "later value migration" that brings these contacts back.
 * - The rows are nevertheless **not** deleted (unlike `contact_devices` / `sso_sessions`, see
 *   `LegacyEcdhKeyCleanup`): `contacts` is the FK parent of the chat history, and dropping a row
 *   would cascade the user's messages away for no gain.
 * - A contact returns when its chat key is written afresh with a peer that has itself migrated:
 *   a new incoming chat request (`IncomingChatRequestProcessor`), or
 *   `AddContactUseCase.addAlreadyEstablishedContactsById`, which re-reads `identifierKey` from the
 *   People chain. Neither runs on its own for an existing contact — there is no self-heal here.
 *
 * Do not simplify this back into a non-null mapper.
 */
fun ContactLocal.toDomainOrNull(): Contact? {
    val parsedChatKey = X25519PublicKey.fromBytes(chatKey.toDataByteArray()).getOrElse {
        Timber.w(
            "Skipping contact ${accountId.intoAccountId()}: stored chatKey is ${chatKey.size} bytes, " +
                "expected ${X25519PublicKey.SIZE_BYTES}"
        )
        return null
    }

    return Contact(
        accountId = accountId.intoAccountId(),
        username = username,
        chatKey = parsedChatKey,
        ourMetaAccountId = ourMetaAccountId,
        sharedSecretDerivationDomain = SharedSecretDerivationDomain(sharedSecretDerivationPath),
        pin = pin,
        pushId = pushId?.toDataByteArray(),
        pushToken = pushToken?.toDataByteArray(),
        voipPushToken = voipPushToken?.toDataByteArray(),
        lastSharedPushToken = lastSharedPushToken,
        operatingSystem = operatingSystem.toDomain(),
        isPeerLeft = isPeerLeft,
        isBlocked = isBlocked,
        avatarUrl = avatar,
        origin = origin,
        pendingChatRequestId = chatRequestId,
        pendingDevicesFanOut = pendingDevicesFanOut,
        addedAt = Instant.fromEpochMilliseconds(addedAt),
        establishedAt = establishedAt?.let(Instant::fromEpochMilliseconds),
    )
}

fun Contact.toLocal(): ContactLocal {
    return ContactLocal(
        accountId = accountId.value,
        username = username,
        chatKey = chatKey.bytes.value,
        ourMetaAccountId = ourMetaAccountId,
        sharedSecretDerivationPath = sharedSecretDerivationDomain.domain,
        avatar = avatarUrl,
        pin = pin,
        pushId = pushId?.value,
        pushToken = pushToken?.value,
        voipPushToken = voipPushToken?.value,
        lastSharedPushToken = lastSharedPushToken,
        operatingSystem = operatingSystem.toLocal(),
        isPeerLeft = isPeerLeft,
        isBlocked = isBlocked,
        origin = origin,
        chatRequestId = pendingChatRequestId,
        pendingDevicesFanOut = pendingDevicesFanOut,
        addedAt = addedAt.toEpochMilliseconds(),
        establishedAt = establishedAt?.toEpochMilliseconds(),
    )
}

fun OperatingSystem.toLocal(): ContactLocal.OperatingSystem? = when (this) {
    OperatingSystem.ANDROID -> ContactLocal.OperatingSystem.ANDROID
    OperatingSystem.IOS -> ContactLocal.OperatingSystem.IOS
    OperatingSystem.UNKNOWN -> null
}

fun ContactLocal.OperatingSystem?.toDomain() = when (this) {
    ContactLocal.OperatingSystem.ANDROID -> OperatingSystem.ANDROID
    ContactLocal.OperatingSystem.IOS -> OperatingSystem.IOS
    null -> OperatingSystem.UNKNOWN
}

/** Null for the same reason as [ContactLocal.toDomainOrNull] — the embedded contact is unmappable. */
fun ContactWithRequestTimestampLocal.toDomainOrNull(): DomainContactWithRequestTimestamp? {
    val contact = contact.toDomainOrNull() ?: return null

    return DomainContactWithRequestTimestamp(
        contact = contact,
        requestTimestamp = requestTimestamp
    )
}

/** Null for the same reason as [ContactLocal.toDomainOrNull] — the embedded contact is unmappable. */
fun ContactWithChatRequestLocal.toDomainOrNull(): ContactWithChatRequest? {
    val contact = contact.toDomainOrNull() ?: return null

    return ContactWithChatRequest(
        contact = contact,
        pendingChatRequest = chatRequest?.toDomain()
    )
}
