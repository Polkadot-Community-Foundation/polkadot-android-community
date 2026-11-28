package io.paritytech.polkadotapp.app.internal

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.paritytech.polkadotapp.common.presentation.AppInitializer
import io.paritytech.polkadotapp.tools_integrity_impl.di.KeyAttestationOptIn

@Module
@InstallIn(SingletonComponent::class)
interface InternalBuildModule {
    @Binds
    @IntoSet
    fun bindInternalBuildProvider(impl: InternalBuildProvider): AppInitializer

    companion object {
        // This build is not installed from Play, so it attests with key attestation.
        @Provides
        fun provideKeyAttestationOptIn(): KeyAttestationOptIn = KeyAttestationOptIn
    }
}
