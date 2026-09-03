package io.paritytech.polkadotapp.feature_sso_impl.data.repository

import io.paritytech.polkadotapp.common.domain.model.intoAccountId
import io.paritytech.polkadotapp.common.domain.model.requireX25519PublicKey
import io.paritytech.polkadotapp.database.dao.SsoSessionDao
import io.paritytech.polkadotapp.database.model.SsoSessionLocal
import io.paritytech.polkadotapp.database.model.SsoSessionMetadataLocal
import io.paritytech.polkadotapp.database.model.SsoSessionWithMetadata
import io.paritytech.polkadotapp.feature_sso_api.domain.model.DeviceStatus
import io.paritytech.polkadotapp.feature_sso_api.domain.model.HandshakeMetadata
import io.paritytech.polkadotapp.feature_sso_impl.domain.model.SsoSessionData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the read boundary of [SsoSessionRepository] against rows written before the P-256 -> X25519
 * migration.
 *
 * `sso_sessions.sharedSecretPublicKey` is a plain BLOB, so no schema migration rewrote the 65-byte
 * uncompressed P-256 keys that older builds stored there for a linked device. Parsing them strictly
 * throws out of a Room Flow, on a background dispatcher, where no collector can catch it — which
 * kills the process on launch, since `observeSessions` is collected from the device-sync
 * initializer. Every read path here must skip such a row instead.
 */
class SsoSessionRepositoryTest {
    private val dao = FakeSsoSessionDao()

    private val repository = SsoSessionRepository(dao)

    @Test
    fun `observeSessions skips a legacy 65-byte key instead of throwing`() = runBlocking<Unit> {
        dao.setRows(legacySessionRow())

        assertEquals(emptyList<SsoSessionData>(), repository.observeSessions().first())
    }

    @Test
    fun `getSessions skips a legacy 65-byte key instead of throwing`() = runBlocking<Unit> {
        dao.setRows(legacySessionRow())

        assertEquals(emptyList<SsoSessionData>(), repository.getSessions())
    }

    @Test
    fun `getSessionByStatementAccountId skips a legacy 65-byte key instead of throwing`() = runBlocking<Unit> {
        dao.setRows(legacySessionRow())

        assertNull(repository.getSessionByStatementAccountId(LEGACY_STATEMENT_ACCOUNT_ID.intoAccountId()))
    }

    @Test
    fun `observeSessions surfaces a valid 32-byte key unchanged`() = runBlocking<Unit> {
        dao.setRows(validSessionRow())

        val sessions = repository.observeSessions().first()

        assertEquals(1, sessions.size)
        assertValidSession(sessions.single())
    }

    @Test
    fun `getSessions returns only the valid rows of a mixed set`() = runBlocking<Unit> {
        dao.setRows(legacySessionRow(), validSessionRow())

        val sessions = repository.getSessions()

        assertEquals(1, sessions.size)
        assertValidSession(sessions.single())
    }

    @Test
    fun `getSessionByStatementAccountId surfaces a valid 32-byte key unchanged`() = runBlocking<Unit> {
        dao.setRows(legacySessionRow(), validSessionRow())

        val session = repository.getSessionByStatementAccountId(VALID_STATEMENT_ACCOUNT_ID.intoAccountId())

        assertValidSession(requireNotNull(session))
    }

    @Test
    fun `metadata of a valid row survives the null-degrading mapper`() = runBlocking<Unit> {
        dao.setRows(validSessionRow(metadata = listOf(hostNameMetadata())))

        assertEquals("desktop", repository.observeSessions().first().single().name)
    }

    @Test
    fun `a session written through the repository round-trips`() = runBlocking<Unit> {
        dao.setRows(legacySessionRow())

        repository.saveSession(validSessionData())

        val sessions = repository.getSessions()

        assertEquals(1, sessions.size)
        assertValidSession(sessions.single())
    }

    private fun assertValidSession(session: SsoSessionData) {
        assertArrayEquals(VALID_X25519_KEY, session.sharedSecretPublicKey.bytes.value)
        assertArrayEquals(VALID_STATEMENT_ACCOUNT_ID, session.statementStorePublicKey.value)
    }

    private fun validSessionData() = SsoSessionData(
        sharedSecretPublicKey = VALID_X25519_KEY.requireX25519PublicKey(),
        statementStorePublicKey = VALID_STATEMENT_ACCOUNT_ID.intoAccountId(),
        metadata = HandshakeMetadata(emptyMap()),
        addedAt = 0,
        status = DeviceStatus.ACTIVE,
        lastUpdate = 0,
        outgoingUpdateTime = null,
        lastSyncOfferId = null,
    )

    private fun hostNameMetadata() = SsoSessionMetadataLocal(
        sessionSharedSecretPublicKey = VALID_X25519_KEY,
        key = "HostName",
        value = "desktop",
    )

    private fun legacySessionRow() = sessionRow(LEGACY_P256_KEY, LEGACY_STATEMENT_ACCOUNT_ID, emptyList())

    private fun validSessionRow(metadata: List<SsoSessionMetadataLocal> = emptyList()) =
        sessionRow(VALID_X25519_KEY, VALID_STATEMENT_ACCOUNT_ID, metadata)

    private fun sessionRow(
        sharedSecretPublicKey: ByteArray,
        statementStorePublicKey: ByteArray,
        metadata: List<SsoSessionMetadataLocal>,
    ) = SsoSessionWithMetadata(
        session = SsoSessionLocal(
            sharedSecretPublicKey = sharedSecretPublicKey,
            statementStorePublicKey = statementStorePublicKey,
            addedAt = 0,
            status = "ACTIVE",
            lastUpdate = 0,
            outgoingUpdateTime = null,
            lastSyncOfferId = null,
        ),
        metadata = metadata,
    )
}

private val LEGACY_STATEMENT_ACCOUNT_ID = ByteArray(32) { 1 }
private val VALID_STATEMENT_ACCOUNT_ID = ByteArray(32) { 2 }

/** Uncompressed P-256 point (`0x04 || X || Y`), exactly as pre-X25519 builds stored it. */
private val LEGACY_P256_KEY = ByteArray(65) { index -> if (index == 0) 4 else index.toByte() }

private val VALID_X25519_KEY = ByteArray(32) { index -> (index + 100).toByte() }

private class FakeSsoSessionDao : SsoSessionDao() {
    private val rows = MutableStateFlow<List<SsoSessionWithMetadata>>(emptyList())

    fun setRows(vararg newRows: SsoSessionWithMetadata) {
        rows.value = newRows.toList()
    }

    override fun observeAll(): Flow<List<SsoSessionWithMetadata>> = rows

    override fun observeSessionCount(): Flow<Int> = rows.map { it.size }

    override suspend fun getAll(): List<SsoSessionWithMetadata> = rows.value

    override suspend fun getByStatementStorePublicKey(statementStorePublicKey: ByteArray): SsoSessionWithMetadata? {
        return rows.value.firstOrNull { it.session.statementStorePublicKey.contentEquals(statementStorePublicKey) }
    }

    override suspend fun insertSession(session: SsoSessionLocal) {
        rows.value = rows.value.filterNot {
            it.session.sharedSecretPublicKey.contentEquals(session.sharedSecretPublicKey)
        } + SsoSessionWithMetadata(session, emptyList())
    }

    override suspend fun insertMetadata(entries: List<SsoSessionMetadataLocal>) {
        if (entries.isEmpty()) return

        rows.value = rows.value.map { row ->
            val forRow = entries.filter {
                it.sessionSharedSecretPublicKey.contentEquals(row.session.sharedSecretPublicKey)
            }

            if (forRow.isEmpty()) row else SsoSessionWithMetadata(row.session, row.metadata + forRow)
        }
    }

    override suspend fun deleteMetadataForSession(sessionSharedSecretPublicKey: ByteArray) {
        rows.value = rows.value.map { row ->
            if (row.session.sharedSecretPublicKey.contentEquals(sessionSharedSecretPublicKey)) {
                SsoSessionWithMetadata(row.session, emptyList())
            } else {
                row
            }
        }
    }

    override suspend fun delete(sharedSecretPublicKey: ByteArray) {
        rows.value = rows.value.filterNot { it.session.sharedSecretPublicKey.contentEquals(sharedSecretPublicKey) }
    }

    override suspend fun deleteByStatementStorePublicKey(statementStorePublicKey: ByteArray) {
        rows.value = rows.value.filterNot { it.session.statementStorePublicKey.contentEquals(statementStorePublicKey) }
    }

    override suspend fun getOutgoingUpdateTime(statementStorePublicKey: ByteArray): Long? = null

    override suspend fun updateOutgoingUpdateTime(statementStorePublicKey: ByteArray, timePoint: Long) = Unit

    override suspend fun getLastSyncOfferId(statementStorePublicKey: ByteArray): String? = null

    override suspend fun updateLastSyncOfferId(statementStorePublicKey: ByteArray, offerId: String) = Unit
}
