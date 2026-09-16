package io.paritytech.polkadotapp.feature_products_impl.domain.truapi.worker

import io.parity.truapi.TrUAPIProductExecution
import io.paritytech.polkadotapp.common.utils.logFailure
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCardKey
import io.paritytech.polkadotapp.feature_products_api.model.JsWidget
import io.paritytech.polkadotapp.feature_products_impl.domain.pocket.PocketFaceStreams
import io.paritytech.polkadotapp.feature_products_impl.domain.truapi.TrUAPIHostRuntimeProvider
import io.paritytech.polkadotapp.feature_products_impl.domain.truapi.renderer.toJsWidget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import uniffi.truapi.HostRendererActionSubscribeItem
import uniffi.truapi.ProductRendererRenderRequest
import uniffi.truapi.RenderContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Faces over the core: one worker reference is taken for the collection, the product's worker is
 * awaited, and `render` is opened on the card's context. A render stream that ends or fails leaves
 * the last face on screen; a worker that restarts gets a fresh stream.
 */
class TrUAPIPocketFaceStreams @Inject constructor(
    private val runtimeProvider: TrUAPIHostRuntimeProvider,
    private val workers: TrUAPIWorkerSupervisor,
) : PocketFaceStreams {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun renderFaces(key: PocketCardKey): Flow<JsWidget> = flow {
        val runtime = runtimeProvider.runtime()
            .logFailure("TrUAPI runtime unavailable; Pocket face ${key.cardId.value} stays static")
            .getOrElse { return@flow }

        runtime.acquireWorker(key.productId.value)
        try {
            emitAll(
                workers.execution(key.productId)
                    .filterNotNull()
                    .flatMapLatest { execution -> execution.faces(key) },
            )
        } finally {
            runtime.releaseWorker(key.productId.value)
        }
    }

    override fun sendAction(key: PocketCardKey, actionId: String, payload: ByteArray) {
        val execution = workers.currentExecution(key.productId) ?: return
        runCatching { execution.publishRendererAction(HostRendererActionSubscribeItem(key.renderContext(), actionId, payload)) }
            .logFailure("truapi.renderer.action '$actionId' for ${key.cardId.value}")
    }

    private fun TrUAPIProductExecution.faces(key: PocketCardKey): Flow<JsWidget> =
        render(ProductRendererRenderRequest(key.renderContext(), payload = ByteArray(0)))
            .retryWhileConnecting(CONNECT_ATTEMPTS, CONNECT_RETRY_DELAY)
            .map { it.toJsWidget() }
            .catch { Timber.w(it, "Pocket face stream for %s ended", key.cardId.value) }

    private fun PocketCardKey.renderContext() = RenderContext.PocketCard(cardId.value)

    private companion object {
        const val CONNECT_ATTEMPTS = 40L
        val CONNECT_RETRY_DELAY = 250.milliseconds
    }
}
