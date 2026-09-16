package io.paritytech.polkadotapp.feature_chats_impl.data.repository

import io.paritytech.polkadotapp.common.domain.model.intoAccountId
import io.paritytech.polkadotapp.common.domain.model.requireX25519PublicKey
import io.paritytech.polkadotapp.database.dao.ContactDeviceDao
import io.paritytech.polkadotapp.database.model.ContactDeviceLocal
import io.paritytech.polkadotapp.feature_chats_api.domain.model.ContactDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the read boundary of [RealContactDevicesRepository] against rows written before the
 * P-256 -> X25519 migration.
 *
 * `contact_devices.encryptionPublicKey` is a plain BLOB, so no schema migration rewrote the
 * 65-byte uncompressed P-256 keys that older builds stored there. Parsing them strictly used to
 * throw out of a Room Flow, on a background dispatcher, where no collector can catch it — which
 * killed the process on launch. Every read path here must skip such a row instead.
 */
class RealContactDevicesRepositoryTest {
    private val dao = FakeContactDeviceDao()

    private val repository = RealContactDevicesRepository(dao)

    @Test
    fun `getDevices skips a legacy 65-byte key instead of throwing`() = runBlocking<Unit> {
        dao.setRows(legacyDeviceRow())

        assertEquals(emptyList<ContactDevice>(), repository.getDevices(CONTACT_ACCOUNT_ID.intoAccountId()))
    }

    @Test
    fun `subscribeDevices skips a legacy 65-byte key instead of throwing`() = runBlocking<Unit> {
        dao.setRows(legacyDeviceRow())

        val devices = repository.subscribeDevices(CONTACT_ACCOUNT_ID.intoAccountId()).first()

        assertEquals(emptyList<ContactDevice>(), devices)
    }

    @Test
    fun `subscribeAllDevices skips a legacy 65-byte key instead of throwing`() = runBlocking<Unit> {
        dao.setRows(legacyDeviceRow())

        val devicesByContact = repository.subscribeAllDevices().first()

        assertEquals(emptyMap<Any, Any>(), devicesByContact)
    }

    @Test
    fun `getDevices surfaces a valid 32-byte key unchanged`() = runBlocking<Unit> {
        dao.setRows(validDeviceRow())

        val devices = repository.getDevices(CONTACT_ACCOUNT_ID.intoAccountId())

        assertEquals(1, devices.size)
        assertValidDevice(devices.single())
    }

    @Test
    fun `subscribeDevices surfaces a valid 32-byte key unchanged`() = runBlocking<Unit> {
        dao.setRows(validDeviceRow())

        val devices = repository.subscribeDevices(CONTACT_ACCOUNT_ID.intoAccountId()).first()

        assertEquals(1, devices.size)
        assertValidDevice(devices.single())
    }

    @Test
    fun `subscribeAllDevices surfaces a valid 32-byte key unchanged`() = runBlocking<Unit> {
        dao.setRows(validDeviceRow())

        val devicesByContact = repository.subscribeAllDevices().first()

        assertEquals(setOf(CONTACT_ACCOUNT_ID.intoAccountId()), devicesByContact.keys)
        assertValidDevice(devicesByContact.getValue(CONTACT_ACCOUNT_ID.intoAccountId()).single())
    }

    @Test
    fun `getDevices returns only the valid rows of a mixed set`() = runBlocking<Unit> {
        dao.setRows(legacyDeviceRow(), validDeviceRow())

        val devices = repository.getDevices(CONTACT_ACCOUNT_ID.intoAccountId())

        assertEquals(1, devices.size)
        assertValidDevice(devices.single())
    }

    @Test
    fun `subscribeDevices returns only the valid rows of a mixed set`() = runBlocking<Unit> {
        dao.setRows(legacyDeviceRow(), validDeviceRow())

        val devices = repository.subscribeDevices(CONTACT_ACCOUNT_ID.intoAccountId()).first()

        assertEquals(1, devices.size)
        assertValidDevice(devices.single())
    }

    @Test
    fun `subscribeAllDevices still groups the surviving rows by contact`() = runBlocking<Unit> {
        // A contact whose only device is legacy must disappear from the map entirely, rather than
        // appear with an empty list — groupBy runs over the already-filtered list.
        dao.setRows(
            legacyDeviceRow(),
            validDeviceRow(),
            legacyDeviceRow(contactAccountId = OTHER_CONTACT_ACCOUNT_ID),
        )

        val devicesByContact = repository.subscribeAllDevices().first()

        assertEquals(setOf(CONTACT_ACCOUNT_ID.intoAccountId()), devicesByContact.keys)
        assertValidDevice(devicesByContact.getValue(CONTACT_ACCOUNT_ID.intoAccountId()).single())
    }

    @Test
    fun `a device written through the repository round-trips`() = runBlocking<Unit> {
        val device = ContactDevice(
            contactAccountId = CONTACT_ACCOUNT_ID.intoAccountId(),
            statementAccountId = VALID_DEVICE_ACCOUNT_ID.intoAccountId(),
            encryptionPublicKey = VALID_X25519_KEY.requireX25519PublicKey(),
        )

        repository.addDevice(device)

        assertEquals(listOf(device), repository.getDevices(CONTACT_ACCOUNT_ID.intoAccountId()))
    }

    @Test
    fun `subscribeAllDevices keeps emitting after a legacy row is replaced by a valid one`() = runBlocking<Unit> {
        dao.setRows(legacyDeviceRow())

        assertTrue(repository.subscribeAllDevices().first().isEmpty())

        // What happens in the field once the peer re-announces the device: the upsert replaces the
        // legacy row on the (contactAccountId, statementAccountId) primary key.
        repository.addDevice(
            ContactDevice(
                contactAccountId = CONTACT_ACCOUNT_ID.intoAccountId(),
                statementAccountId = LEGACY_DEVICE_ACCOUNT_ID.intoAccountId(),
                encryptionPublicKey = VALID_X25519_KEY.requireX25519PublicKey(),
            )
        )

        val devices = repository.subscribeAllDevices().first().getValue(CONTACT_ACCOUNT_ID.intoAccountId())

        assertEquals(1, devices.size)
        assertArrayEquals(VALID_X25519_KEY, devices.single().encryptionPublicKey.bytes.value)
    }

    private fun assertValidDevice(device: ContactDevice) {
        assertArrayEquals(CONTACT_ACCOUNT_ID, device.contactAccountId.value)
        assertArrayEquals(VALID_DEVICE_ACCOUNT_ID, device.statementAccountId.value)
        assertArrayEquals(VALID_X25519_KEY, device.encryptionPublicKey.bytes.value)
    }

    private fun legacyDeviceRow(
        contactAccountId: ByteArray = CONTACT_ACCOUNT_ID,
        statementAccountId: ByteArray = LEGACY_DEVICE_ACCOUNT_ID,
    ) = ContactDeviceLocal(
        contactAccountId = contactAccountId,
        statementAccountId = statementAccountId,
        encryptionPublicKey = LEGACY_P256_KEY,
    )

    private fun validDeviceRow() = ContactDeviceLocal(
        contactAccountId = CONTACT_ACCOUNT_ID,
        statementAccountId = VALID_DEVICE_ACCOUNT_ID,
        encryptionPublicKey = VALID_X25519_KEY,
    )
}

private val CONTACT_ACCOUNT_ID = ByteArray(32) { 1 }
private val OTHER_CONTACT_ACCOUNT_ID = ByteArray(32) { 9 }
private val LEGACY_DEVICE_ACCOUNT_ID = ByteArray(32) { 2 }
private val VALID_DEVICE_ACCOUNT_ID = ByteArray(32) { 3 }

/** Uncompressed P-256 point (`0x04 || X || Y`), exactly as pre-X25519 builds stored it. */
private val LEGACY_P256_KEY = ByteArray(65) { index -> if (index == 0) 4 else index.toByte() }

private val VALID_X25519_KEY = ByteArray(32) { index -> (index + 100).toByte() }

private class FakeContactDeviceDao : ContactDeviceDao() {
    private val rows = MutableStateFlow<List<ContactDeviceLocal>>(emptyList())

    fun setRows(vararg newRows: ContactDeviceLocal) {
        rows.value = newRows.toList()
    }

    override suspend fun upsert(device: ContactDeviceLocal) {
        rows.update { current -> current.filterNot { it.key() == device.key() } + device }
    }

    override suspend fun getByContact(contactAccountId: ByteArray): List<ContactDeviceLocal> {
        return rows.value.filter { it.contactAccountId.contentEquals(contactAccountId) }
    }

    override fun subscribeByContact(contactAccountId: ByteArray): Flow<List<ContactDeviceLocal>> {
        return rows.map { current -> current.filter { it.contactAccountId.contentEquals(contactAccountId) } }
    }

    override fun subscribeAll(): Flow<List<ContactDeviceLocal>> = rows

    override suspend fun delete(contactAccountId: ByteArray, statementAccountId: ByteArray) {
        rows.update { current ->
            current.filterNot {
                it.contactAccountId.contentEquals(contactAccountId) &&
                    it.statementAccountId.contentEquals(statementAccountId)
            }
        }
    }

    override suspend fun deleteAllForContact(contactAccountId: ByteArray) {
        rows.update { current -> current.filterNot { it.contactAccountId.contentEquals(contactAccountId) } }
    }

    private fun ContactDeviceLocal.key(): String {
        return contactAccountId.joinToString(separator = ",") + "/" + statementAccountId.joinToString(separator = ",")
    }
}
