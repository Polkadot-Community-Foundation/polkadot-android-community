package io.paritytech.polkadotapp.feature_products_impl.presentation.deeplink

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.paritytech.polkadotapp.common.data.memory.ComputationalScope
import io.paritytech.polkadotapp.common.presentation.deeplink.DeepLinkHandler
import io.paritytech.polkadotapp.common.presentation.deeplink.DeepLinkHandler.Companion.WEB_HTTPS_SCHEME
import io.paritytech.polkadotapp.common.presentation.deeplink.DeeplinkProcessingOutcome
import io.paritytech.polkadotapp.common.utils.CoroutineDispatchers
import io.paritytech.polkadotapp.common.utils.flatMap
import io.paritytech.polkadotapp.feature_dotns_api.domain.DotNsTld
import io.paritytech.polkadotapp.feature_dotns_api.domain.DotNsTldProvider
import io.paritytech.polkadotapp.feature_dotns_api.domain.DotNsUtils
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCardKey
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCollection
import io.paritytech.polkadotapp.feature_products_api.model.ProductId
import io.paritytech.polkadotapp.feature_products_api.presentation.PocketAddCardPayload
import io.paritytech.polkadotapp.feature_products_impl.domain.pocket.PocketCardIdentifier
import io.paritytech.polkadotapp.feature_products_impl.domain.pocket.PocketPublishError
import io.paritytech.polkadotapp.feature_products_impl.domain.pocket.PublishedPocketCards
import io.paritytech.polkadotapp.feature_products_impl.domain.truapi.PocketDeeplink
import io.paritytech.polkadotapp.feature_products_impl.domain.truapi.PocketDeeplinkAction
import io.paritytech.polkadotapp.feature_products_impl.domain.truapi.PocketDeeplinkParser
import io.paritytech.polkadotapp.feature_products_impl.presentation.productBotManagement.ProductsRouter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import io.paritytech.polkadotapp.common.R as RCommon

/**
 * `polkadotapp://<product>.<tld>/-/pocket/{add,open}?card=<id>`. The first segment `-` is reserved
 * for host-handled targets, so the product App handler leaves these alone.
 */
internal class PocketDeepLinkHandler @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val dotNsTldProvider: DotNsTldProvider,
    private val parser: PocketDeeplinkParser,
    private val publishedCards: PublishedPocketCards,
    private val collection: PocketCollection,
    private val router: ProductsRouter,
    @param:ApplicationContext private val context: Context,
) : DeepLinkHandler {
    override suspend fun canHandle(data: Uri): Boolean {
        val tld = dotNsTldProvider.currentTldOrNull() ?: return false
        return DotNsUtils.isDotDomain(data.asWebUri(), tld) &&
            data.pathSegments.take(2) == listOf(RESERVED_SEGMENT, POCKET_SEGMENT)
    }

    context(scope: ComputationalScope)
    override suspend fun handle(data: Uri): Result<DeeplinkProcessingOutcome> = withContext(dispatchers.io) {
        dotNsTldProvider.getTld().flatMap { tld ->
            val normalized = DotNsUtils.normalize(data.asWebUri(), tld)
                ?: return@flatMap Result.failure(IllegalArgumentException("Not a $tld domain: $data"))
            val deeplink = parser.parse(normalized.toString())
                ?: return@flatMap Result.failure(IllegalArgumentException("Not a Pocket deeplink: $data"))

            ProductId.fromString(deeplink.productHost, tld).flatMap { productId -> dispatch(productId, deeplink) }
        }
    }

    private suspend fun dispatch(productId: ProductId, deeplink: PocketDeeplink): Result<DeeplinkProcessingOutcome> {
        val key = runCatching { PocketCardKey(productId, PocketCardIdentifier.screen(deeplink.cardId)) }
            .getOrElse { return Result.failure(it) }
        val present = collection.observeCards().first().any { it.key == key }

        return when {
            // A present card is opened whichever action asked; adding it again has nothing to add.
            present -> Result.success(DeeplinkProcessingOutcome.Navigate { router.openPocketCard(key) })
            deeplink.action == PocketDeeplinkAction.OPEN -> Result.success(hostError(PocketPublishError.UnknownCard))
            else -> offerToAdd(key)
        }
    }

    private suspend fun offerToAdd(key: PocketCardKey): Result<DeeplinkProcessingOutcome> {
        val offer: Result<DeeplinkProcessingOutcome> = publishedCards.find(key.productId, key.cardId).map {
            DeeplinkProcessingOutcome.Navigate {
                router.openPocketAddCard(PocketAddCardPayload(key.productId.value, key.cardId.value))
            }
        }
        return offer.recoverCatching { failure ->
            if (failure is PocketPublishError) hostError(failure) else throw failure
        }
    }

    private fun hostError(error: PocketPublishError) = DeeplinkProcessingOutcome.ShowMessage(
        context.getString(
            when (error) {
                PocketPublishError.NoPocket -> RCommon.string.pocket_deeplink_no_pocket
                PocketPublishError.UnknownCard -> RCommon.string.pocket_deeplink_unknown_card
            },
        ),
    )

    // Swaps the scheme rather than prefixing it: ensureHttpsProtocol would mangle a polkadotapp:// deeplink.
    private fun Uri.asWebUri(): Uri = buildUpon().scheme(WEB_HTTPS_SCHEME).build()

    private companion object {
        const val RESERVED_SEGMENT = "-"
        const val POCKET_SEGMENT = "pocket"
    }
}
