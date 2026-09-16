package io.paritytech.polkadotapp.database.migrations

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.paritytech.polkadotapp.common.data.storage.preferences.Editor
import io.paritytech.polkadotapp.common.data.storage.preferences.InitialValueProducer
import io.paritytech.polkadotapp.common.data.storage.preferences.Preferences
import io.paritytech.polkadotapp.database.AppDatabase
import io.paritytech.polkadotapp.database.AppDatabase.Companion.addAppCallbacks
import io.paritytech.polkadotapp.database.AppDatabase.Companion.addAppMigrations
import io.paritytech.polkadotapp.feature_chats_impl.data.migrations.createChatMessageMigration1to2
import io.paritytech.polkadotapp.feature_chats_impl.data.migrations.createChatMessageMigration2to3
import io.paritytech.polkadotapp.feature_chats_impl.data.migrations.createChatMessageMigration3to4
import io.paritytech.polkadotapp.feature_chats_impl.data.migrations.createChatMessageMigration4to5
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrateAll() {
        helper.createDatabase(TEST_DB, 1).close()

        openAppDatabase().apply {
            openHelper.writableDatabase.close()
            close()
        }
    }

    /**
     * PCF FORK-LOCAL. Covers [io.paritytech.polkadotapp.database.cleanup.LegacyEcdhKeyCleanup], the
     * one destructive step in the fork's P-256 -> X25519 repair, against the real schema: it is
     * installed by [addAppCallbacks] and swallows its own failures, so a table or column typo in
     * its SQL would otherwise be silent until a user's device came up with a stale row.
     *
     * The seeded rows are what an install upgraded across that change actually holds: a 65-byte
     * uncompressed P-256 key in a BLOB column that no schema migration rewrote.
     */
    @Test
    fun purgesLegacyEcdhKeysOnOpen() {
        helper.createDatabase(TEST_DB, LATEST_VERSION).apply {
            // Foreign keys are off on the helper's connection, so the FK parents can be skipped.
            execSQL(
                "INSERT INTO contact_devices (contactAccountId, statementAccountId, encryptionPublicKey) VALUES (?, ?, ?)",
                arrayOf(ACCOUNT_ID_A, ACCOUNT_ID_B, LEGACY_P256_KEY)
            )
            execSQL(
                "INSERT INTO contact_devices (contactAccountId, statementAccountId, encryptionPublicKey) VALUES (?, ?, ?)",
                arrayOf(ACCOUNT_ID_A, ACCOUNT_ID_C, VALID_X25519_KEY)
            )
            execSQL(
                "INSERT INTO sso_sessions (sharedSecretPublicKey, statementStorePublicKey) VALUES (?, ?)",
                arrayOf(LEGACY_P256_KEY, ACCOUNT_ID_B)
            )
            execSQL(
                "INSERT INTO sso_session_metadata (sessionSharedSecretPublicKey, `key`, value) VALUES (?, ?, ?)",
                arrayOf(LEGACY_P256_KEY, "HostName", "desktop")
            )
            execSQL(
                "INSERT INTO sso_sessions (sharedSecretPublicKey, statementStorePublicKey) VALUES (?, ?)",
                arrayOf(VALID_X25519_KEY, ACCOUNT_ID_C)
            )
            close()
        }

        openAppDatabase().apply {
            val db = openHelper.writableDatabase

            assertEquals(1, db.countOf("contact_devices"))
            assertEquals(1, db.countOf("sso_sessions"))
            assertEquals(0, db.countOf("sso_session_metadata"))

            db.close()
            close()
        }
    }

    private fun openAppDatabase(): AppDatabase {
        return Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            TEST_DB
        )
            .addAppMigrations(
                NoOpPreferences(),
                setOf(createChatMessageMigration1to2(), createChatMessageMigration2to3(), createChatMessageMigration3to4(), createChatMessageMigration4to5())
            )
            .addAppCallbacks()
            .build()
    }

    private fun SupportSQLiteDatabase.countOf(table: String): Int {
        return query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"

        /** Must track `AppDatabase`'s `@Database(version = ...)`, which is shared with upstream. */
        private const val LATEST_VERSION = 54

        private val ACCOUNT_ID_A = ByteArray(32) { 1 }
        private val ACCOUNT_ID_B = ByteArray(32) { 2 }
        private val ACCOUNT_ID_C = ByteArray(32) { 3 }

        /** Uncompressed P-256 point (`0x04 || X || Y`), exactly as pre-X25519 builds stored it. */
        private val LEGACY_P256_KEY = ByteArray(65) { index -> if (index == 0) 4 else index.toByte() }

        private val VALID_X25519_KEY = ByteArray(32) { index -> (index + 100).toByte() }
    }
}

private class NoOpPreferences : Preferences {
    override fun contains(field: String) = false
    override fun putString(field: String, value: String?) = Unit
    override fun getString(field: String, defaultValue: String) = defaultValue
    override fun getString(field: String): String? = null
    override fun putBoolean(field: String, value: Boolean) = Unit
    override fun getBoolean(field: String, defaultValue: Boolean) = defaultValue
    override fun putInt(field: String, value: Int) = Unit
    override fun getInt(field: String, defaultValue: Int) = defaultValue
    override fun putLong(field: String, value: Long) = Unit
    override fun getLong(field: String, defaultValue: Long) = defaultValue
    override fun putStringSet(field: String, value: Set<String>) = Unit
    override fun getStringSet(field: String): Set<String> = emptySet()
    override fun removeField(field: String) = Unit
    override fun stringFlow(field: String, initialValueProducer: InitialValueProducer<String>?): Flow<String?> = emptyFlow()
    override fun booleanFlow(field: String, defaultValue: Boolean): Flow<Boolean> = emptyFlow()
    override fun longFlow(field: String, defaultValue: Long): Flow<Long> = emptyFlow()
    override fun intFlow(field: String, defaultValue: Int): Flow<Int> = emptyFlow()
    override fun stringSetFlow(field: String): Flow<Set<String>> = emptyFlow()
    override fun keyFlow(key: String): Flow<String> = emptyFlow()
    override fun keysFlow(vararg keys: String): Flow<List<String>> = emptyFlow()
    override fun edit(): Editor = NoOpEditor()
}

private class NoOpEditor : Editor {
    override fun putString(field: String, value: String?) = Unit
    override fun putBoolean(field: String, value: Boolean) = Unit
    override fun putInt(field: String, value: Int) = Unit
    override fun putLong(field: String, value: Long) = Unit
    override fun remove(field: String) = Unit
    override fun apply() = Unit
}
