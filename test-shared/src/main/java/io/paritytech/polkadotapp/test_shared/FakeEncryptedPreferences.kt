package io.paritytech.polkadotapp.test_shared

import io.paritytech.polkadotapp.common.data.storage.preferences.encrypted.EncryptedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory [EncryptedPreferences] for tests.
 *
 * [writes] counts `putEncryptedString` calls, so a test can assert that a
 * no-op path did not touch storage.
 */
class FakeEncryptedPreferences : EncryptedPreferences {
    private val storage = mutableMapOf<String, String>()

    var writes: Int = 0
        private set

    override fun putEncryptedString(field: String, value: String) {
        storage[field] = value
        writes++
    }

    override fun getDecryptedString(field: String): String? = storage[field]

    override fun hasKey(field: String): Boolean = storage.containsKey(field)

    override fun removeKey(field: String) {
        storage.remove(field)
    }

    override fun decryptedStringFlow(field: String): Flow<String?> = flowOf(storage[field])

    /** Seed a value without counting it as a write. */
    fun seed(field: String, value: String) {
        storage[field] = value
    }
}
