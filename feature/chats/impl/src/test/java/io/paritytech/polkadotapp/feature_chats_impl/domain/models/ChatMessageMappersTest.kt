package io.paritytech.polkadotapp.feature_chats_impl.domain.models

import io.novasama.substrate_sdk_android.koltinx_serialization_scale.binary.BinaryScale
import io.paritytech.polkadotapp.common.domain.model.toDataByteArray
import io.paritytech.polkadotapp.database.model.ChatMessageLocal
import io.paritytech.polkadotapp.feature_chats_api.domain.middleware.bot.CustomChatMessageRendererId
import io.paritytech.polkadotapp.feature_chats_api.domain.model.ChatMessage
import io.paritytech.polkadotapp.feature_chats_api.domain.model.ChatMessageOrigin
import io.paritytech.polkadotapp.feature_chats_impl.data.repository.CustomContentDecoder
import io.paritytech.polkadotapp.feature_chats_impl.domain.models.scale.ChatMessageContentLocal
import kotlinx.serialization.encodeToByteArray
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the mapping boundary of [ChatMessageLocal.toDomain] against messages persisted before the
 * P-256 -> X25519 migration.
 *
 * A legacy `DeviceAdded` row carries a 65-byte P-256 key inside its SCALE blob. The blob decodes
 * fine — `EncodedPublicKey` is just a `Vec<u8>` — and only the subsequent domain mapping rejects
 * the key. Since this mapper runs inside Room Flows, that throw escapes onto a background
 * dispatcher where no collector can catch it, so an unmappable stored message must degrade to
 * [ChatMessage.Content.Unsupported] rather than take the process down.
 */
class ChatMessageMappersTest {
    private val customContentDecoder = FailingCustomContentDecoder()

    @Test
    fun `a legacy DeviceAdded with a 65-byte key maps to Unsupported instead of throwing`() {
        val encoded = encodeDeviceAdded(encryptionPublicKey = LEGACY_P256_KEY)

        val content = chatMessageLocal(encoded).toDomain(customContentDecoder).content

        assertTrue(content is ChatMessage.Content.Unsupported)
        assertArrayEquals(encoded, (content as ChatMessage.Content.Unsupported).rawContent)
    }

    @Test
    fun `a message whose content is unmappable keeps its other fields`() {
        val message = chatMessageLocal(encodeDeviceAdded(encryptionPublicKey = LEGACY_P256_KEY))
            .toDomain(customContentDecoder)

        assertEquals(MESSAGE_ID, message.id)
        assertEquals(TIMESTAMP_MILLIS, message.timestamp)
        assertArrayEquals(CHAT_ID, message.chatId.value.value)
        assertEquals(ChatMessage.Status.IS_READ, message.status)
        assertEquals(ChatMessageOrigin.User, message.origin)
    }

    @Test
    fun `a DeviceAdded with a valid 32-byte key still maps to DeviceAdded`() {
        val encoded = encodeDeviceAdded(encryptionPublicKey = VALID_X25519_KEY)

        val content = chatMessageLocal(encoded).toDomain(customContentDecoder).content

        assertTrue(content is ChatMessage.Content.DeviceAdded)
        val deviceAdded = content as ChatMessage.Content.DeviceAdded
        assertArrayEquals(VALID_X25519_KEY, deviceAdded.encryptionPublicKey.bytes.value)
        assertArrayEquals(STATEMENT_ACCOUNT_ID, deviceAdded.statementAccountId.value)
    }

    @Test
    fun `an ordinary text message is unaffected`() {
        val encoded = BinaryScale.encodeToByteArray<ChatMessageContentLocal>(ChatMessageContentLocal.Text("hello"))

        val content = chatMessageLocal(encoded).toDomain(customContentDecoder).content

        assertTrue(content is ChatMessage.Content.Text)
        assertEquals("hello", (content as ChatMessage.Content.Text).text)
    }

    @Test
    fun `an undecodable SCALE blob still falls back to Unsupported`() {
        // The pre-existing decode guard; asserted here so widening the mapping guard cannot
        // silently displace it.
        val undecodable = byteArrayOf(0x7F, 0x7F, 0x7F)

        val content = chatMessageLocal(undecodable).toDomain(customContentDecoder).content

        assertTrue(content is ChatMessage.Content.Unsupported)
        assertArrayEquals(undecodable, (content as ChatMessage.Content.Unsupported).rawContent)
    }

    private fun encodeDeviceAdded(encryptionPublicKey: ByteArray): ByteArray {
        return BinaryScale.encodeToByteArray<ChatMessageContentLocal>(
            ChatMessageContentLocal.DeviceAdded(
                statementAccountId = STATEMENT_ACCOUNT_ID.toDataByteArray(),
                encryptionPublicKey = encryptionPublicKey.toDataByteArray(),
            )
        )
    }

    private fun chatMessageLocal(content: ByteArray) = ChatMessageLocal(
        id = MESSAGE_ID,
        chatId = CHAT_ID,
        timestamp = TIMESTAMP_MILLIS,
        updatedAt = TIMESTAMP_MILLIS,
        origin = ChatMessageLocal.Origin(type = ChatMessageLocal.OriginType.USER, key = null),
        status = ChatMessageLocal.Status.IS_READ,
        type = ChatMessageLocal.Type.DEVICE_ADDED,
        searchableContent = "",
        content = content,
        replyToMessageId = null,
        isInternal = true,
    )
}

private const val MESSAGE_ID = "message-1"
private const val TIMESTAMP_MILLIS = 1_700_000_000_000L

private val CHAT_ID = byteArrayOf(1, 2, 3)
private val STATEMENT_ACCOUNT_ID = ByteArray(32) { 5 }

/** Uncompressed P-256 point (`0x04 || X || Y`), exactly as pre-X25519 builds stored it. */
private val LEGACY_P256_KEY = ByteArray(65) { index -> if (index == 0) 4 else index.toByte() }

private val VALID_X25519_KEY = ByteArray(32) { index -> (index + 100).toByte() }

private class FailingCustomContentDecoder : CustomContentDecoder {
    override fun decode(rendererId: CustomChatMessageRendererId, content: ByteArray): Result<Any?> {
        return Result.failure(IllegalStateException("No renderer registered in this test"))
    }

    override fun encode(rendererId: CustomChatMessageRendererId, value: Any?): Result<ByteArray> {
        return Result.failure(IllegalStateException("No renderer registered in this test"))
    }
}
