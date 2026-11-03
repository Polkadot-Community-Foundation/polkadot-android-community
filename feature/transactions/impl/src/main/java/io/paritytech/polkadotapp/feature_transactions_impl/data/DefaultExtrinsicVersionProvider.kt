package io.paritytech.polkadotapp.feature_transactions_impl.data

import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicVersion
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.verifySignature.VerifySignature
import io.paritytech.polkadotapp.chains.multiNetwork.ChainRegistry
import io.paritytech.polkadotapp.chains.multiNetwork.KnownChains
import io.paritytech.polkadotapp.chains.multiNetwork.chain.model.ChainId
import io.paritytech.polkadotapp.chains.multiNetwork.getRuntime
import io.paritytech.polkadotapp.feature_transactions.api.data.TransactionExtensionVersions
import io.paritytech.polkadotapp.tools_remoteconfig_api.RemoteConfigService
import io.paritytech.polkadotapp.tools_remoteconfig_api.getSyncedJsonObject
import javax.inject.Inject

interface DefaultExtrinsicVersionProvider {
    suspend fun getDefaultExtrinsicVersion(chainId: ChainId, isSigned: Boolean): Result<ExtrinsicVersion>

    /**
     * The extrinsic version that carries a transaction built against pipeline [extensionVersion]: v4 when the
     * transaction is signed and that pipeline cannot verify a general signature, a general transaction otherwise.
     */
    suspend fun getExtrinsicVersion(chainId: ChainId, extensionVersion: Byte, isSigned: Boolean): ExtrinsicVersion
}

class RealDefaultExtrinsicVersionProvider @Inject constructor(
    private val knownChains: KnownChains,
    private val remoteConfigService: RemoteConfigService,
    private val chainRegistry: ChainRegistry,
) : DefaultExtrinsicVersionProvider, TransactionExtensionVersions {
    private companion object {
        const val TX_EXTENSION_VERSIONS_KEY = "transaction_extension_versions"
        const val TX_EXTENSION_VERSION_DEFAULT = 0
    }

    override suspend fun getDefaultExtrinsicVersion(chainId: ChainId, isSigned: Boolean): Result<ExtrinsicVersion> {
        if (chainId !in generalTransactionChains()) return Result.success(ExtrinsicVersion.V4)

        return getTransactionExtensionVersions().mapCatching { versions ->
            getExtrinsicVersion(chainId, highestDeclaredVersionUpTo(chainId, versions.versionOf(chainId)), isSigned)
        }
    }

    override suspend fun getExtrinsicVersion(chainId: ChainId, extensionVersion: Byte, isSigned: Boolean): ExtrinsicVersion {
        return if (isSigned && !verifiesGeneralSignatures(chainId, extensionVersion)) {
            ExtrinsicVersion.V4
        } else {
            ExtrinsicVersion.V5(extensionVersion)
        }
    }

    // A signed general transaction carries its signature in VerifySignature, so a pipeline without it cannot take
    // one. Such runtimes still accept v4 — but v4 always runs pipeline 0, whatever extensions later versions add.
    private suspend fun verifiesGeneralSignatures(chainId: ChainId, extensionVersion: Byte): Boolean {
        return chainRegistry.getRuntime(chainId).metadata.extrinsic
            .transactionExtensions(extensionVersion.toInt())
            .any { it.id == VerifySignature.ID }
    }

    // The configured version may be one the runtime does not declare yet, e.g. ahead of the upgrade that adds it.
    private suspend fun highestDeclaredVersionUpTo(chainId: ChainId, configuredVersion: Byte): Byte {
        return chainRegistry.getRuntime(chainId).metadata.extrinsic
            .transactionExtensionsByVersion.keys
            .filter { it <= configuredVersion }
            .maxOrNull()
            ?.toByte()
            ?: configuredVersion
    }

    override suspend fun forGeneralTransactionChains(): Result<Map<ChainId, Byte>> {
        return getTransactionExtensionVersions().map { versions ->
            generalTransactionChains().associateWith { chainId -> versions.versionOf(chainId) }
        }
    }

    private fun generalTransactionChains(): List<ChainId> = listOf(knownChains.people, knownChains.assetHub)

    // Gson receives only the raw Class, so map values arrive as Double regardless of the declared type
    private suspend fun getTransactionExtensionVersions(): Result<Map<String, Number>> {
        return remoteConfigService.getSyncedJsonObject<Map<String, Number>>(TX_EXTENSION_VERSIONS_KEY)
    }

    private fun Map<String, Number>.versionOf(chainId: ChainId): Byte {
        return (this[chainId] ?: TX_EXTENSION_VERSION_DEFAULT).toByte()
    }
}
