package io.paritytech.polkadotapp.common.data.keypair

import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair
import io.paritytech.polkadotapp.test_shared.FakeEncryptedPreferences
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class RealClientKeypairStoreTest {
    private val installKeypair = keypair(seed = 1)
    private val walletKeypair = keypair(seed = 2)

    @Test
    fun `reads the persisted keypair`() {
        val store = RealClientKeypairStore(prefsWithInstallKey())

        assertArrayEquals(installKeypair.publicKey, store.getOrGenerate().publicKey)
    }

    @Test
    fun `adopt replaces the auth identity for subsequent reads`() {
        val store = RealClientKeypairStore(prefsWithInstallKey())
        store.getOrGenerate()

        store.adopt(walletKeypair)

        assertArrayEquals(walletKeypair.publicKey, store.getOrGenerate().publicKey)
    }

    @Test
    fun `adopt survives a new store over the same preferences`() {
        val prefs = prefsWithInstallKey()
        RealClientKeypairStore(prefs).adopt(walletKeypair)

        // A fresh instance (process restart) must read the adopted key, so the
        // JWT minted after a restart still authenticates as the wallet.
        assertArrayEquals(walletKeypair.publicKey, RealClientKeypairStore(prefs).getOrGenerate().publicKey)
    }

    @Test
    fun `adopting the current keypair does not rewrite storage`() {
        val prefs = prefsWithInstallKey()
        val store = RealClientKeypairStore(prefs)
        store.getOrGenerate()
        val writesBefore = prefs.writes

        store.adopt(installKeypair)

        assertEquals(writesBefore, prefs.writes)
    }

    // Pre-seed the persisted key so no test path has to generate a fresh
    // keypair — that calls native sr25519 code, unavailable on the JVM.
    private fun prefsWithInstallKey() = FakeEncryptedPreferences().apply {
        seed("jwt_client_keypair_v1", installKeypair.toBlob())
    }

    private fun Sr25519Keypair.toBlob() =
        listOf(publicKey, privateKey, nonce).joinToString(":") { bytes ->
            bytes.joinToString("") { "%02x".format(it) }
        }

    private fun keypair(seed: Byte) = Sr25519Keypair(
        publicKey = ByteArray(32) { seed },
        privateKey = ByteArray(32) { (seed + 10).toByte() },
        nonce = ByteArray(32) { (seed + 20).toByte() },
    )
}
