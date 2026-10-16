package io.paritytech.polkadotapp.feature_chats_impl.domain.sessions.signer

import io.paritytech.polkadotapp.common.data.memory.ComputationalScope
import io.paritytech.polkadotapp.common.domain.model.AccountId
import io.paritytech.polkadotapp.common.presentation.AppInitializer
import io.paritytech.polkadotapp.common.utils.coerceToUnit
import io.paritytech.polkadotapp.common.utils.flatMap
import io.paritytech.polkadotapp.common.utils.flattenResult
import io.paritytech.polkadotapp.common.utils.logFailure
import io.paritytech.polkadotapp.common.utils.mapToSet
import io.paritytech.polkadotapp.common.utils.runCancellableCatching
import io.paritytech.polkadotapp.feature_chats_impl.data.repository.ContactsRepository
import io.paritytech.polkadotapp.feature_chats_impl.domain.sessions.isActiveChat
import io.paritytech.polkadotapp.feature_statement_store_api.domain.slotAllocator.SlotPriority
import io.paritytech.polkadotapp.feature_statement_store_api.domain.slotAllocator.StatementStoreSlotAllocator
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drops every per-chat signer slot (the [SlotPriority.Critical] tier) whose account does not sign an active chat, so
 * the renewer never keeps one alive forever: a block, leave or removal interrupted by a crash, a removal synced from
 * another device and an account of an older derivation all end up here. Runs on start and on every change of the
 * active chats.
 */
@Singleton
class ChatSignerSlotReconciler @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val accountDerivation: ChatSignerAccountDerivation,
    private val slotAllocator: StatementStoreSlotAllocator,
) : AppInitializer {
    private val chatAccountIds = ConcurrentHashMap<AccountId, AccountId>()

    context(scope: ComputationalScope)
    override fun initialize(): Result<Unit> = runCancellableCatching {
        contactsRepository.subscribeContacts()
            .map { contacts -> contacts.filter { it.isActiveChat() }.mapToSet { it.accountId } }
            .distinctUntilChanged()
            .onEach { reconcile().logFailure("Failed to reconcile chat signer slots") }
            .launchIn(scope)
    }

    // Rows are read before the active chats: a row allocated after that read is left for the next run, so a chat
    // that turns active (and allocates) mid-run never loses its fresh row.
    private suspend fun reconcile(): Result<Unit> {
        return slotAllocator.allocatedTargets(SlotPriority.Critical).flatMap { allocated ->
            if (allocated.isEmpty()) return@flatMap Result.success(Unit)

            activeChatAccounts().flatMap { active -> deallocate(allocated - active) }
        }
    }

    private suspend fun activeChatAccounts(): Result<Set<AccountId>> {
        val activeContacts = contactsRepository.getContacts().filter { it.isActiveChat() }

        return activeContacts.map { chatAccountId(it.accountId) }.flattenResult().map { it.toSet() }
    }

    private suspend fun chatAccountId(contactAccountId: AccountId): Result<AccountId> {
        chatAccountIds[contactAccountId]?.let { return Result.success(it) }

        return accountDerivation.deriveAccountId(contactAccountId)
            .onSuccess { chatAccountIds[contactAccountId] = it }
    }

    private suspend fun deallocate(stale: Set<AccountId>): Result<Unit> {
        if (stale.isEmpty()) return Result.success(Unit)

        Timber.i("chatSignerSlots: dropping ${stale.size} slot(s) of chats that are no longer active")
        return stale.map { slotAllocator.deallocateAllSlots(it) }.flattenResult().coerceToUnit()
    }
}
