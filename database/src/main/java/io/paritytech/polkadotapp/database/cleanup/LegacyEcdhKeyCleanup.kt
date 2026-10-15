package io.paritytech.polkadotapp.database.cleanup

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import io.paritytech.polkadotapp.common.domain.model.X25519PublicKey
import timber.log.Timber

/**
 * Deletes `contact_devices` and `sso_sessions` rows whose stored key is not a 32-byte X25519 public
 * key: the 65-byte P-256 keys written by builds before the X25519 change, which ECDH cannot use.
 * Peers re-announce their devices, and linked devices can be paired again.
 *
 * Runs on open rather than as a migration because only row values change, not the schema.
 * Failures are logged and ignored: the read boundaries in `ContactDevicesRepository` and
 * `SsoSessionRepository` skip these rows on their own.
 */
object LegacyEcdhKeyCleanup : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        runCatching { db.purgeLegacyKeys() }
            .onFailure { Timber.e(it, "Legacy pre-X25519 key cleanup failed; read boundaries will skip the rows instead") }
    }

    private fun SupportSQLiteDatabase.purgeLegacyKeys() {
        val removedDevices = executeUpdateDelete(SQL_DELETE_LEGACY_CONTACT_DEVICES)

        if (removedDevices > 0) {
            Timber.w(
                "Dropped $removedDevices contact_devices row(s) whose encryptionPublicKey was not " +
                    "${X25519PublicKey.SIZE_BYTES} bytes — legacy pre-X25519 device keys. " +
                    "The affected peers will re-announce their devices."
            )
        }

        // Children first, so the result does not depend on foreign keys being enabled at open.
        executeUpdateDelete(SQL_DELETE_LEGACY_SSO_SESSION_METADATA)
        val removedSessions = executeUpdateDelete(SQL_DELETE_LEGACY_SSO_SESSIONS)

        if (removedSessions > 0) {
            Timber.w(
                "Dropped $removedSessions sso_sessions row(s) whose sharedSecretPublicKey was not " +
                    "${X25519PublicKey.SIZE_BYTES} bytes — legacy pre-X25519 session keys. " +
                    "The affected devices have to be linked again."
            )
        }
    }

    private fun SupportSQLiteDatabase.executeUpdateDelete(sql: String): Int {
        return compileStatement(sql).use { it.executeUpdateDelete() }
    }
}

private const val KEY_SIZE = X25519PublicKey.SIZE_BYTES

private const val SQL_DELETE_LEGACY_CONTACT_DEVICES =
    "DELETE FROM contact_devices WHERE length(encryptionPublicKey) != $KEY_SIZE"

private const val SQL_DELETE_LEGACY_SSO_SESSION_METADATA =
    "DELETE FROM sso_session_metadata WHERE length(sessionSharedSecretPublicKey) != $KEY_SIZE"

private const val SQL_DELETE_LEGACY_SSO_SESSIONS =
    "DELETE FROM sso_sessions WHERE length(sharedSecretPublicKey) != $KEY_SIZE"
