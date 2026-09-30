package io.paritytech.polkadotapp.feature_chats_impl.presentation.search.scan

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import io.paritytech.polkadotapp.chains.multiNetwork.ChainRegistry
import io.paritytech.polkadotapp.common.data.memory.ComputationalScope
import io.paritytech.polkadotapp.common.domain.model.intoAccountId
import io.paritytech.polkadotapp.feature_account_api.data.repository.AccountRepository
import io.paritytech.polkadotapp.feature_account_api.data.repository.getWalletAccountIdIn
import io.paritytech.polkadotapp.feature_chats_api.domain.model.ChatId
import io.paritytech.polkadotapp.feature_chats_impl.ChatsRouter
import io.paritytech.polkadotapp.feature_chats_impl.data.repository.ChatSearchRecentsRepository
import io.paritytech.polkadotapp.feature_chats_impl.domain.models.RecentChat
import io.paritytech.polkadotapp.feature_chats_impl.domain.models.StartChatData
import io.paritytech.polkadotapp.feature_chats_impl.domain.usecase.StartChatDataUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContactAddressScanContentParserTest {
    private val accountRepository: AccountRepository = mockk()
    private val startChatDataUseCase: StartChatDataUseCase = mockk()
    private val recents = RecordingRecentsRepository()

    private val parser = ContactAddressScanContentParser(
        knownChains = mockk(relaxed = true),
        chainRegistry = mockk<ChainRegistry>(relaxed = true),
        accountRepository = accountRepository,
        startChatDataUseCase = startChatDataUseCase,
        chatSearchRecentsRepository = recents,
        router = mockk<ChatsRouter>(relaxed = true),
    )

    @Before
    fun mockOwnAccount() {
        mockkStatic(ACCOUNT_REPOSITORY_FILE)
        coEvery { accountRepository.getWalletAccountIdIn(any()) } returns OWN_ACCOUNT_ID
    }

    @After
    fun unmockOwnAccount() = unmockkStatic(ACCOUNT_REPOSITORY_FILE)

    @Test
    fun `a scanned contact is added to recents`() = runBlocking<Unit> {
        coEvery { startChatDataUseCase(SCANNED_ACCOUNT_ID) } returns Result.success(mockk<StartChatData.ExistingChat>())

        val result = handle(SCANNED_ADDRESS)

        assertTrue(result.isSuccess)
        assertEquals(listOf(ChatId.fromContact(SCANNED_ACCOUNT_ID)), recents.added)
    }

    @Test
    fun `a contact that cannot be resolved is not added to recents`() = runBlocking<Unit> {
        coEvery { startChatDataUseCase(any()) } returns Result.failure(IllegalStateException("no chat key"))

        val result = handle(SCANNED_ADDRESS)

        assertTrue(result.isFailure)
        assertEquals(emptyList<ChatId>(), recents.added)
    }

    private suspend fun CoroutineScope.handle(content: String) = with(TestComputationalScope(this)) {
        parser.handle(content)
    }

    private class TestComputationalScope(scope: CoroutineScope) : ComputationalScope, CoroutineScope by scope

    private class RecordingRecentsRepository : ChatSearchRecentsRepository {
        val added = mutableListOf<ChatId>()

        override fun observeRecents(): Flow<List<RecentChat>> = emptyFlow()

        override suspend fun addRecent(chatId: ChatId) {
            added += chatId
        }

        override suspend fun removeRecent(chatId: ChatId) = Unit

        override suspend fun clearRecents() = Unit
    }

    private companion object {
        const val ACCOUNT_REPOSITORY_FILE = "io.paritytech.polkadotapp.feature_account_api.data.repository.AccountRepositoryKt"
        const val SCANNED_ADDRESS = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY"

        val SCANNED_ACCOUNT_ID = SCANNED_ADDRESS.toAccountId().intoAccountId()
        val OWN_ACCOUNT_ID = ByteArray(32) { 1 }.intoAccountId()
    }
}
