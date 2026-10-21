package io.paritytech.polkadotapp.feature_transactions_impl.data.extensions

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicVersion
import io.novasama.substrate_sdk_android.runtime.metadata.TransactionExtensionMetadata
import io.paritytech.polkadotapp.chains.multiNetwork.ChainRegistry
import io.paritytech.polkadotapp.chains.multiNetwork.runtime.RuntimeProvider
import io.paritytech.polkadotapp.feature_transactions_impl.data.DefaultExtrinsicVersionProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RealTxPayloadExtensionsResolverTest {
    private val versionProvider = mockk<DefaultExtrinsicVersionProvider>()
    private val chainRegistry = mockk<ChainRegistry>()

    private val resolver = RealTxPayloadExtensionsResolver(
        defaultExtrinsicVersionProvider = versionProvider,
        chainRegistry = chainRegistry,
    )

    @Before
    fun setUp() {
        val extension = mockk<TransactionExtensionMetadata> { every { id } returns "CheckNonce" }
        val runtime = mockk<RuntimeSnapshot> {
            every { metadata.extrinsic.transactionExtensions(any()) } returns listOf(extension)
        }
        coEvery { chainRegistry.getRuntimeProvider(CHAIN) } returns mockk<RuntimeProvider> { coEvery { get() } returns runtime }
    }

    @Test
    fun `a version 0 request takes the extrinsic version that carries pipeline 0, not the configured pipeline`() = runTest {
        coEvery { versionProvider.getDefaultExtrinsicVersion(CHAIN, isSigned = true) } returns Result.success(ExtrinsicVersion.V5(1))
        coEvery { versionProvider.getExtrinsicVersion(CHAIN, extensionVersion = 0, isSigned = true) } returns ExtrinsicVersion.V4

        val resolved = resolver.resolve(emptyList(), txExtVersion = 0u, chainId = CHAIN, isSigned = true).getOrThrow()

        assertEquals(ExtrinsicVersion.V4, resolved.submissionOptions.extrinsicVersion)
    }

    @Test
    fun `a version 0 request stays v4 when the chain defaults to v4`() = runTest {
        coEvery { versionProvider.getDefaultExtrinsicVersion(CHAIN, isSigned = false) } returns Result.success(ExtrinsicVersion.V4)

        val resolved = resolver.resolve(emptyList(), txExtVersion = 0u, chainId = CHAIN, isSigned = false).getOrThrow()

        assertEquals(ExtrinsicVersion.V4, resolved.submissionOptions.extrinsicVersion)
        coVerify(exactly = 0) { versionProvider.getExtrinsicVersion(any(), any(), any()) }
    }

    @Test
    fun `an explicit version goes general on that pipeline`() = runTest {
        val resolved = resolver.resolve(emptyList(), txExtVersion = 1u, chainId = CHAIN, isSigned = true).getOrThrow()

        val version = resolved.submissionOptions.extrinsicVersion
        assertTrue("expected a general transaction, got $version", version is ExtrinsicVersion.V5)
        assertEquals(1.toByte(), (version as ExtrinsicVersion.V5).extensionVersion)
    }

    private companion object {
        const val CHAIN = "people"
    }
}
