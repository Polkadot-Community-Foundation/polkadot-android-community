package io.paritytech.polkadotapp.app.internal

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.paritytech.polkadotapp.common.presentation.AppInitializer

@Module
@InstallIn(SingletonComponent::class)
interface InternalBuildModule {
    @Binds
    @IntoSet
    fun bindInternalBuildProvider(impl: InternalBuildProvider): AppInitializer
}
