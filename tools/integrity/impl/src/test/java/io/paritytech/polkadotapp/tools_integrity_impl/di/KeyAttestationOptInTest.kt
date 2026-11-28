package io.paritytech.polkadotapp.tools_integrity_impl.di

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyAttestationOptInTest {

    @Test
    fun `without the opt-in, Play Integrity is used and key attestation is never built`() {
        val selected = selectAttestation(
            keyAttestationOptedIn = false,
            playIntegrity = { "play-integrity" },
            keyAttestation = { error("key attestation must not be built") },
        )

        assertEquals("play-integrity", selected)
    }

    @Test
    fun `with the opt-in, key attestation is used and Play Integrity is never built`() {
        val selected = selectAttestation(
            keyAttestationOptedIn = true,
            playIntegrity = { error("Play Integrity must not be built") },
            keyAttestation = { "key-attestation" },
        )

        assertEquals("key-attestation", selected)
    }

    // An @Inject constructor would give Dagger an implicit binding, making the opt-in present in every build.
    // Matched by name so both annotation packages Dagger accepts are covered without depending on either.
    @Test
    fun `the opt-in has no injectable constructor`() {
        val injectAnnotations = setOf("javax.inject.Inject", "jakarta.inject.Inject")

        assertTrue(
            KeyAttestationOptIn::class.java.declaredConstructors.none { constructor ->
                constructor.annotations.any { it.annotationClass.java.name in injectAnnotations }
            }
        )
    }
}
