package io.paritytech.polkadotapp.tools_integrity_impl.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.paritytech.polkadotapp.tools_integrity_impl.data.KeyAttestationIntegrityParamsInjector
import io.paritytech.polkadotapp.tools_integrity_impl.data.RealGPIntegrityParamsInjector
import io.paritytech.polkadotapp.tools_integrity_impl.data.integrity.IntegrityParamsInjector
import java.util.Optional
import javax.inject.Provider

@Module
@InstallIn(SingletonComponent::class)
object GPIntegrityModule {
    @Provides
    fun provideIntegrityParamsInjector(
        keyAttestationOptIn: Optional<KeyAttestationOptIn>,
        playIntegrity: Provider<RealGPIntegrityParamsInjector>,
        keyAttestation: Provider<KeyAttestationIntegrityParamsInjector>,
    ): IntegrityParamsInjector = selectAttestation(
        keyAttestationOptedIn = keyAttestationOptIn.isPresent,
        playIntegrity = playIntegrity::get,
        keyAttestation = keyAttestation::get,
    )
}
