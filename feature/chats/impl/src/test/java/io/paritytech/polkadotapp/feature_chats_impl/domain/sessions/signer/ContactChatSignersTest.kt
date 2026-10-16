package io.paritytech.polkadotapp.feature_chats_impl.domain.sessions.signer

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair
import io.paritytech.polkadotapp.common.domain.model.toDataByteArray
import io.paritytech.polkadotapp.common.utils.CoroutineDispatchers
import io.paritytech.polkadotapp.feature_chats_api.domain.model.Contact
import io.paritytech.polkadotapp.feature_chats_impl.data.repository.ContactsRepository
import io.paritytech.polkadotapp.feature_chats_impl.domain.identitySigning.IdentitySigningGate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactChatSignersTest {
    private val contactAccountId = ByteArray(32) { 1 }.toDataByteArray()
    private val chatPublicKey = ByteArray(32) { 2 }
    private val chatKeypair: Sr25519Keypair = mockk { every { publicKey } returns chatPublicKey }
    private val privateSigner = ContactChatSigner(chatKeypair, ChatSignerKind.PRIVATE)

    private val resolver: ContactChatSignerResolver = mockk()
    private val slotAllocator = RecordingSlotAllocator()
    private val contactsRepository: ContactsRepository = mockk()
    private val identitySigningGate: IdentitySigningGate = mockk(relaxed = true)
    private val storedContact = MutableStateFlow<Contact?>(contact(pendingChatRequestId = null))

    @Before
    fun setUp() {
        every { contactsRepository.subscribeContact(contactAccountId) } returns storedContact
    }

    @Test
    fun `concurrent callers resolve an established chat once per process`() = runTest {
        val signers = createSigners()
        val contact = contact()
        coEvery { resolver.resolve(contact) } returns Result.success(privateSigner)

        val keypairs = List(3) { async { signers.keypairFor(contact) } }.awaitAll()

        keypairs.forEach { assertSame(chatKeypair, it) }
        coVerify(exactly = 1) { resolver.resolve(contact) }
    }

    @Test
    fun `transient failure is retried instead of downgrading to username signer`() = runTest {
        val signers = createSigners()
        val contact = contact()
        val attempts = listOf(Result.failure<ContactChatSigner>(IllegalStateException()), Result.success(privateSigner))
        coEvery { resolver.resolve(contact) } returnsMany attempts

        val keypair = signers.keypairFor(contact)

        assertSame(chatKeypair, keypair)
    }

    @Test
    fun `release stops renewing the chat slot and resolves again on next use`() = runTest {
        val signers = createSigners()
        val contact = contact()
        coEvery { resolver.resolve(contact) } returns Result.success(privateSigner)

        signers.keypairFor(contact)
        signers.release(contactAccountId)
        signers.keypairFor(contact)

        assertEquals(listOf(chatPublicKey.toDataByteArray()), slotAllocator.deallocatedTargets)
        coVerify(exactly = 2) { resolver.resolve(contact) }
    }

    @Test
    fun `a pending chat claims no slot until it is accepted`() = runTest {
        val signers = createSigners()
        val contact = contact()
        coEvery { resolver.resolve(contact) } returns Result.success(privateSigner)
        storedContact.value = contact(pendingChatRequestId = "request")

        val keypair = async { signers.keypairFor(contact) }
        coVerify(exactly = 0) { resolver.resolve(any()) }

        storedContact.value = contact(pendingChatRequestId = null)

        assertSame(chatKeypair, keypair.await())
    }

    private fun TestScope.createSigners(): RealContactChatSigners {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val dispatchers: CoroutineDispatchers = mockk { every { io } returns testDispatcher }
        return RealContactChatSigners(resolver, slotAllocator, contactsRepository, identitySigningGate, dispatchers)
    }

    private fun contact(pendingChatRequestId: String? = null): Contact = mockk {
        every { accountId } returns contactAccountId
        every { username } returns "alice"
        every { this@mockk.pendingChatRequestId } returns pendingChatRequestId
        every { isBlocked } returns false
        every { isPeerLeft } returns false
    }
}
