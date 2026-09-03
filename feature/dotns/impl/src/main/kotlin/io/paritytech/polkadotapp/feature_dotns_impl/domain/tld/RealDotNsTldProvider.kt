package io.paritytech.polkadotapp.feature_dotns_impl.domain.tld

import io.paritytech.polkadotapp.common.utils.CoroutineDispatchers
import io.paritytech.polkadotapp.common.utils.logFailure
import io.paritytech.polkadotapp.feature_dotns_api.domain.DotNsTld
import io.paritytech.polkadotapp.feature_dotns_api.domain.DotNsTldProvider
import io.paritytech.polkadotapp.feature_dotns_impl.data.contract.DotNsContractApi
import io.paritytech.polkadotapp.feature_dotns_impl.data.storage.DotNsTldStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RealDotNsTldProvider @Inject constructor(
    private val contractApi: DotNsContractApi,
    private val tldStorage: DotNsTldStorage,
    private val dispatchers: CoroutineDispatchers
) : DotNsTldProvider, CoroutineScope {
    override val coroutineContext = dispatchers.io + SupervisorJob()

    private val fetchMutex = Mutex()
    private val persistedTld by lazy { tldStorage.getTld() }
    private val settledTld = MutableStateFlow<DotNsTld?>(null)

    override fun currentTldOrNull(): DotNsTld? {
        settledTld.value?.let { return it }
        kickRefresh()
        return persistedTld
    }

    override suspend fun getTld(): Result<DotNsTld> {
        settledTld.value?.let { return Result.success(it) }

        return fetchMutex.withLock { fetchAndSettle() }
    }

    private fun kickRefresh() {
        if (fetchMutex.isLocked) return

        launch {
            getTld()
        }
    }

    private suspend fun fetchAndSettle(): Result<DotNsTld> {
        settledTld.value?.let { return Result.success(it) }

        return contractApi.readTld()
            .logFailure("Failed to read dotNS TLD from the protocol registry")
            .map(::settle)
    }

    private fun settle(rawSuffix: String?): DotNsTld {
        val parsed = rawSuffix?.removePrefix(".")?.let(DotNsTld::parse)
        if (parsed == null) {
            Timber.w("dotNS deployment reported no usable TLD (raw=$rawSuffix); falling back to ${DotNsTld.FALLBACK}")
        }

        val tld = parsed ?: DotNsTld.FALLBACK
        settledTld.value = tld
        tldStorage.putTld(tld)
        return tld
    }
}
