package io.paritytech.polkadotapp.tools_integrity_impl.di

import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * When the app binds this, a gp build attests with key attestation instead of Play Integrity, so a
 * build installed outside Play can still authenticate. This module has no internal build type, so
 * the choice has to come from the app's own source set.
 *
 * Deliberately not constructor-injectable: an @Inject constructor would make the binding, and with
 * it key attestation, present in every build.
 */
object KeyAttestationOptIn

@Module
@InstallIn(SingletonComponent::class)
interface KeyAttestationOptInModule {
    @BindsOptionalOf
    fun optionalKeyAttestationOptIn(): KeyAttestationOptIn
}

/** Only the chosen factory runs, so the other attestation path is never set up. */
internal fun <T> selectAttestation(
    keyAttestationOptedIn: Boolean,
    playIntegrity: () -> T,
    keyAttestation: () -> T,
): T = if (keyAttestationOptedIn) keyAttestation() else playIntegrity()
