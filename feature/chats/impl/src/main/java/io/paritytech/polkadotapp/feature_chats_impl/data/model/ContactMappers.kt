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
 * Maps a stored contact, or returns `null` when its chat key is not a valid X25519 public key,
 * such as a legacy 65-byte P-256 key. These mappers feed Room flows, where a throw would crash the
 * app. The row is kept because `contacts` is the parent of the chat history.
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
