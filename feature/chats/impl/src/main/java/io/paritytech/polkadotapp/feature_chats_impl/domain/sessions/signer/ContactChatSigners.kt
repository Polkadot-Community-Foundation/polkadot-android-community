package io.paritytech.polkadotapp.feature_chats_impl.domain.sessions.signer

import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair
import io.paritytech.polkadotapp.common.domain.model.AccountId
import io.paritytech.polkadotapp.common.domain.model.toDataByteArray
import io.paritytech.polkadotapp.common.utils.CoroutineDispatchers
import io.paritytech.polkadotapp.common.utils.exponentialRetryDelay
import io.paritytech.polkadotapp.feature_chats_api.domain.model.Contact
import io.paritytech.polkadotapp.feature_chats_impl.data.repository.ContactsRepository
import io.paritytech.polkadotapp.feature_chats_impl.domain.identitySigning.IdentitySigningGate
import io.paritytech.polkadotapp.feature_chats_impl.domain.sessions.isActiveChat
import io.paritytech.polkadotapp.feature_statement_store_api.domain.slotAllocator.StatementStoreSlotAllocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// A private signer is kept for the whole process; a username one only for a while, so the chat returns to its own
// account once a slot frees up (chat-spec RFC-0005 §4).
private val USERNAME_SIGNER_TTL = 1.hours

interface ContactChatSigners {
    // Suspends while the chat is not active (pending, blocked or left), so only active chats ever claim a slot.
    suspend fun keypairFor(contact: Contact): Sr25519Keypair

    fun warmUp(contact: Contact)

    /** Forgets the chat's signer and drops its slot when this process resolved it; [ChatSignerSlotReconciler] drops the rest. */
    suspend fun release(contactAccountId: AccountId): Result<Unit>
}

@Singleton
class RealContactChatSigners @Inject constructor(
    private val resolver: ContactChatSignerResolver,
    private val slotAllocator: StatementStoreSlotAllocator,
    private val contactsRepository: ContactsRepository,
    private val identitySigningGate: IdentitySigningGate,
    dispatchers: CoroutineDispatchers,
) : ContactChatSigners, CoroutineScope {
    override val coroutineContext = dispatchers.io + SupervisorJob()

    private val retryDelay = exponentialRetryDelay(initialDelay = 2.seconds, maxDelay = 5.minutes)
    private val resolutions = ConcurrentHashMap<AccountId, Deferred<ContactChatSigner>>()

    override suspend fun keypairFor(contact: Contact): Sr25519Keypair {
        awaitActiveChat(contact.accountId)

        val signer = resolutionFor(contact).await()
        return signer.keypair
    }

    override fun warmUp(contact: Contact) {
        resolutionFor(contact)
    }

    override suspend fun release(contactAccountId: AccountId): Result<Unit> {
        val forgotten = forget(contactAccountId) ?: return Result.success(Unit)
        if (forgotten.kind != ChatSignerKind.PRIVATE) return Result.success(Unit)

        return slotAllocator.deallocateAllSlots(forgotten.keypair.publicKey.toDataByteArray())
    }

    private suspend fun awaitActiveChat(contactAccountId: AccountId) {
        contactsRepository.subscribeContact(contactAccountId).first { it?.isActiveChat() == true }
    }

    // Cancelled before deallocating so an in-flight allocation cannot re-insert a slot row after release.
    private suspend fun forget(contactAccountId: AccountId): ContactChatSigner? {
        val resolution = resolutions.remove(contactAccountId)
        val resolved = resolution?.takeIf { it.isCompleted }?.let { runCatching { it.await() }.getOrNull() }
        resolution?.cancelAndJoin()

        return resolved
    }

    private fun resolutionFor(contact: Contact): Deferred<ContactChatSigner> {
        return resolutions.computeIfAbsent(contact.accountId) {
            async { resolveUntilSuccess(contact) }.also { resolution -> expireIfUsername(contact.accountId, resolution) }
        }
    }

    private fun expireIfUsername(contactAccountId: AccountId, resolution: Deferred<ContactChatSigner>) = launch {
        if (resolution.await().kind != ChatSignerKind.USERNAME) return@launch

        delay(USERNAME_SIGNER_TTL)
        resolutions.remove(contactAccountId, resolution)
    }

    private suspend fun resolveUntilSuccess(contact: Contact): ContactChatSigner {
        var attempt = 0

        while (true) {
            val outcome = resolver.resolve(contact)
            val signer = outcome.getOrNull()
            if (signer != null) {
                Timber.i("ContactChatSigners: chat with ${contact.username} resolved to ${signer.kind}")
                return signer
            }

            attempt++
            identitySigningGate.awaitRetry(contact.accountId, outcome.exceptionOrNull(), retryDelay.delayFor(attempt))
        }
    }
}
