package io.paritytech.polkadotapp.feature_chats_impl.data.repository

import io.paritytech.polkadotapp.common.domain.model.AccountId
import io.paritytech.polkadotapp.common.domain.model.X25519PublicKey
import io.paritytech.polkadotapp.common.domain.model.intoAccountId
import io.paritytech.polkadotapp.common.domain.model.toDataByteArray
import io.paritytech.polkadotapp.common.utils.mapListNotNull
import io.paritytech.polkadotapp.database.dao.ContactDeviceDao
import io.paritytech.polkadotapp.database.model.ContactDeviceLocal
import io.paritytech.polkadotapp.feature_chats_api.domain.model.ContactAccountId
import io.paritytech.polkadotapp.feature_chats_api.domain.model.ContactDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

interface ContactDevicesRepository {
    suspend fun getDevices(contactAccountId: ContactAccountId): List<ContactDevice>

    fun subscribeDevices(contactAccountId: ContactAccountId): Flow<List<ContactDevice>>

    fun subscribeAllDevices(): Flow<Map<ContactAccountId, List<ContactDevice>>>

    suspend fun addDevice(device: ContactDevice)

    suspend fun removeDevice(contactAccountId: ContactAccountId, statementAccountId: AccountId)

    suspend fun clearDevicesFor(contactAccountId: ContactAccountId)
}

class RealContactDevicesRepository @Inject constructor(
    private val dao: ContactDeviceDao
) : ContactDevicesRepository {
    override suspend fun getDevices(contactAccountId: ContactAccountId): List<ContactDevice> {
        return dao.getByContact(contactAccountId.value).mapNotNull { it.toDomainOrNull() }
    }

    override fun subscribeDevices(contactAccountId: ContactAccountId): Flow<List<ContactDevice>> {
        return dao.subscribeByContact(contactAccountId.value)
            .mapListNotNull { it.toDomainOrNull() }
    }

    override fun subscribeAllDevices(): Flow<Map<ContactAccountId, List<ContactDevice>>> {
        return dao.subscribeAll()
            .mapListNotNull { it.toDomainOrNull() }
            .map { devices -> devices.groupBy { it.contactAccountId } }
    }

    override suspend fun addDevice(device: ContactDevice) {
        dao.upsert(device.toLocal())
    }

    override suspend fun removeDevice(
        contactAccountId: ContactAccountId,
        statementAccountId: AccountId
    ) {
        dao.delete(contactAccountId.value, statementAccountId.value)
    }

    override suspend fun clearDevicesFor(contactAccountId: ContactAccountId) {
        dao.deleteAllForContact(contactAccountId.value)
    }
}

/**
 * Maps a stored device, or returns `null` when its encryption key is not a valid X25519 public
 * key. These rows are read inside Room flows, where a throw would crash the app; the peer
 * re-announces the device with a valid key.
 */
private fun ContactDeviceLocal.toDomainOrNull(): ContactDevice? {
    val parsedKey = X25519PublicKey.fromBytes(encryptionPublicKey.toDataByteArray()).getOrElse {
        Timber.w(
            "Skipping contact device row with an unparseable encryption key: " +
                "contact=${contactAccountId.intoAccountId()}, device=${statementAccountId.intoAccountId()}, " +
                "storedKeyLength=${encryptionPublicKey.size}, expected=${X25519PublicKey.SIZE_BYTES}"
        )
        return null
    }

    return ContactDevice(
        contactAccountId = contactAccountId.intoAccountId(),
        statementAccountId = statementAccountId.intoAccountId(),
        encryptionPublicKey = parsedKey,
    )
}

private fun ContactDevice.toLocal(): ContactDeviceLocal {
    return ContactDeviceLocal(
        contactAccountId = contactAccountId.value,
        statementAccountId = statementAccountId.value,
        encryptionPublicKey = encryptionPublicKey.bytes.value,
    )
}
