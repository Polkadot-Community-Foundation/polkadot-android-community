package io.paritytech.polkadotapp.feature_chats_impl.data.model

import io.paritytech.polkadotapp.common.data.os.OperatingSystem
import io.paritytech.polkadotapp.database.model.ContactLocal
import io.paritytech.polkadotapp.database.model.ContactWithChatRequestLocal
import io.paritytech.polkadotapp.database.model.ContactWithRequestTimestampLocal
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the read boundary of the `contacts` mappers against rows written before the
 * P-256 -> X25519 migration.
 *
 * `contacts.chatKey` is a plain BLOB, so older builds' 65-byte keys survive the schema migration
 * untouched. This mapper feeds Room Flows, so a strict parse here throws onto a background
 * dispatcher and kills the process; an unparseable row must be reported as "no such contact"
 * instead. The row is only hidden, never deleted — deleting it would destroy the chat and cascade
 * away its devices.
 */
class ContactMappersTest {
    @Test
    fun `a legacy 65-byte chatKey maps to null instead of throwing`() {
        assertNull(contactLocal(chatKey = LEGACY_P256_KEY).toDomainOrNull())
    }

    @Test
    fun `a valid 32-byte chatKey maps to a contact with the key intact`() {
        val contact = contactLocal(chatKey = VALID_X25519_KEY).toDomainOrNull()

        assertNotNull(contact)
        assertArrayEquals(VALID_X25519_KEY, contact!!.chatKey.bytes.value)
    }

    @Test
    fun `a valid contact keeps its other fields`() {
        val contact = contactLocal(chatKey = VALID_X25519_KEY).toDomainOrNull()!!

        assertArrayEquals(CONTACT_ACCOUNT_ID, contact.accountId.value)
        assertEquals("alice", contact.username)
        assertEquals(7L, contact.ourMetaAccountId)
        assertEquals("chat", contact.sharedSecretDerivationDomain.domain)
        assertEquals(OperatingSystem.ANDROID, contact.operatingSystem)
        assertEquals(ADDED_AT_MILLIS, contact.addedAt.toEpochMilliseconds())
    }

    @Test
    fun `a legacy chatKey hides the contact from the with-timestamp wrapper`() {
        val wrapper = ContactWithRequestTimestampLocal(
            contact = contactLocal(chatKey = LEGACY_P256_KEY),
            requestTimestamp = ADDED_AT_MILLIS,
        )

        assertNull(wrapper.toDomainOrNull())
    }

    @Test
    fun `a valid chatKey surfaces through the with-timestamp wrapper`() {
        val wrapper = ContactWithRequestTimestampLocal(
            contact = contactLocal(chatKey = VALID_X25519_KEY),
            requestTimestamp = ADDED_AT_MILLIS,
        )

        val mapped = wrapper.toDomainOrNull()

        assertNotNull(mapped)
        assertEquals(ADDED_AT_MILLIS, mapped!!.requestTimestamp)
        assertArrayEquals(VALID_X25519_KEY, mapped.contact.chatKey.bytes.value)
    }

    @Test
    fun `a legacy chatKey hides the contact from the with-chat-request wrapper`() {
        val wrapper = ContactWithChatRequestLocal(
            contact = contactLocal(chatKey = LEGACY_P256_KEY),
            chatRequest = null,
        )

        assertNull(wrapper.toDomainOrNull())
    }

    @Test
    fun `a valid chatKey surfaces through the with-chat-request wrapper`() {
        val wrapper = ContactWithChatRequestLocal(
            contact = contactLocal(chatKey = VALID_X25519_KEY),
            chatRequest = null,
        )

        val mapped = wrapper.toDomainOrNull()

        assertNotNull(mapped)
        assertNull(mapped!!.pendingChatRequest)
        assertArrayEquals(VALID_X25519_KEY, mapped.contact.chatKey.bytes.value)
    }
}

private const val ADDED_AT_MILLIS = 1_700_000_000_000L

private val CONTACT_ACCOUNT_ID = ByteArray(32) { 1 }

/** Uncompressed P-256 point (`0x04 || X || Y`), exactly as pre-X25519 builds stored it. */
private val LEGACY_P256_KEY = ByteArray(65) { index -> if (index == 0) 4 else index.toByte() }

private val VALID_X25519_KEY = ByteArray(32) { index -> (index + 100).toByte() }

private fun contactLocal(chatKey: ByteArray) = ContactLocal(
    accountId = CONTACT_ACCOUNT_ID,
    username = "alice",
    chatKey = chatKey,
    ourMetaAccountId = 7L,
    sharedSecretDerivationPath = "chat",
    avatar = null,
    pin = null,
    pushId = null,
    pushToken = null,
    lastSharedPushToken = null,
    operatingSystem = ContactLocal.OperatingSystem.ANDROID,
    voipPushToken = null,
    chatRequestId = null,
    addedAt = ADDED_AT_MILLIS,
)
