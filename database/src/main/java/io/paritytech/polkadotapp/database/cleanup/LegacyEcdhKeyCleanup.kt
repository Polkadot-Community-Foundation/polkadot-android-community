package io.paritytech.polkadotapp.database.cleanup

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import io.paritytech.polkadotapp.common.domain.model.X25519PublicKey
import timber.log.Timber

/**
 * PCF FORK-LOCAL — this is not upstream code. On the next upstream sync, resolve conflicts in
 * favour of keeping it until the retirement condition below is met.
 *
 * Drops rows whose stored ECDH public key is not a 32-byte X25519 public key: `contact_devices`
 * (a peer's device roster) and `sso_sessions` (our own linked desktop/web devices).
 *
 * ## Why this exists
 *
 * Builds released before the chat / multi-device encryption migration from P-256 (secp256r1) to
 * X25519 stored these keys as an uncompressed P-256 point — 65 bytes. That migration tightened the
 * *value* rule (32-byte X25519, enforced by `X25519PublicKey.fromBytes`) while the *columns* stayed
 * plain `ByteArray` -> SQLite BLOB: same tables, same columns, same affinity. Room's schema diff
 * compares structure, not row values, so it saw no change between the two schema versions and
 * demanded no migration; `@AutoMigration` never inspects data. The incompatibility is **semantic**,
 * and no schema migration could have caught it. On the device that reproduced this, the schema had
 * migrated cleanly (`user_version` 47 -> 54) while all four `contact_devices` rows still held
 * 65-byte keys.
 *
 * The symptom on an install upgraded in place across that change was a crash on launch:
 *
 * ```
 * FATAL EXCEPTION: DefaultDispatcher-worker-2
 * java.lang.IllegalArgumentException: X25519PublicKey must be 32 bytes, got 65
 *     at X25519KeysKt.sizeChecked(X25519Keys.kt:71)
 *     at X25519PublicKey$Companion.fromBytes(X25519Keys.kt:19)
 *     at ContactDevicesRepositoryKt.toDomain(ContactDevicesRepository.kt:67)
 *     at RealContactDevicesRepository$subscribeAllDevices$$inlined$mapList$1$2.emit
 *     at androidx.room.coroutines.FlowUtil$createFlow$$inlined$map$1$2.emit
 * ```
 *
 * — the throw escaped a Room Flow on a background dispatcher, where no collector can catch it, so
 * it killed the process. `sso_sessions` had the identical shape of the problem one layer over
 * (`SsoSessionRepository.observeSessions`, collected from the device-sync initializer). A fresh
 * install never reproduces either, because both tables start empty; only an upgrade of an install
 * that had already established a 1:1 chat, or linked a device, does. CI installs fresh, which is
 * why this shipped.
 *
 * ## Why deleting these rows is safe
 *
 * A 65-byte P-256 key is unusable by the X25519 code — ECDH cannot consume it — so such a row
 * confers nothing that still functions.
 *
 * - `contact_devices` is a cache of *other people's* device rosters; it is re-populated whenever a
 *   peer re-announces a device (`DeviceAdded` / `DeviceChatAccepted` over chat), and
 *   `ContactDeviceDao.upsert` replaces the row on its `(contactAccountId, statementAccountId)`
 *   primary key. Deleting the FK child triggers no cascade of its own (the cascade runs `contacts`
 *   -> `contact_devices`, not the reverse).
 * - `sso_sessions` describes a link to one of our own devices that can no longer be spoken to; the
 *   user re-pairs it. Its `sso_session_metadata` children are deleted explicitly first rather than
 *   relying on `ON DELETE CASCADE`, so the outcome does not depend on whether `PRAGMA foreign_keys`
 *   happens to be on at this point of database open. Leaving the row instead would strand it
 *   forever: re-pairing mints a fresh `sharedSecretPublicKey`, so it inserts a *new* primary key
 *   rather than replacing the stale one.
 *
 * Contact rows are deliberately **not** touched — see `ContactMappers.toDomainOrNull`.
 *
 * ## Why this is not a Migration
 *
 * The Room schema version is a namespace **shared with upstream**. Defining our own 55 would stamp
 * users' databases with a version that upstream may later define differently; those users would
 * then fail Room's schema-hash validation at open — a worse crash than the one being fixed. This
 * change needs no structural change, so it stays out of that namespace entirely and runs from
 * [RoomDatabase.Callback.onOpen], which is ordered before any DAO read can observe a row. It is
 * idempotent (after the first run it matches nothing) and cheap (both tables are bounded by
 * contacts x their devices, and by linked devices; the reproducing device had four rows).
 *
 * A failure here must never be worse than the crash it prevents, so the statements run inside a
 * catching boundary: this is hygiene, and hygiene may not fail database open. The read boundaries
 * remain correct on their own if it is skipped.
 *
 * ## Retirement condition
 *
 * This exists **solely** for installs upgraded across the P-256 -> X25519 change. It can and should
 * be deleted once no such install can still be in the field — in practice, once every device has
 * either reinstalled or opened the database at least once on a build containing this cleanup; the
 * absence of the warn logs below in bug reports is the signal. It is not a permanent invariant. The
 * permanent invariant — that a stored device key is 32 bytes — is enforced by
 * `X25519PublicKey.fromBytes` at the read boundaries in `ContactDevicesRepository` and
 * `SsoSessionRepository`, and those read boundaries must survive this class's removal.
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
