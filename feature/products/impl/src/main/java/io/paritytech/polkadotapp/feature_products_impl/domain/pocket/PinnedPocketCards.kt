package io.paritytech.polkadotapp.feature_products_impl.domain.pocket

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import io.paritytech.polkadotapp.feature_dotns_api.domain.DotNsTld
import io.paritytech.polkadotapp.feature_dotns_api.domain.DotNsTldProvider
import io.paritytech.polkadotapp.feature_dotns_api.domain.getTldRetrying
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCard
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCardId
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCardKey
import io.paritytech.polkadotapp.feature_products_api.model.ProductId
import io.paritytech.polkadotapp.feature_products_api.model.derivation.ReservedProductIds
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import io.paritytech.polkadotapp.common.R as RCommon

/** The cards the host itself places: present on first run, removable by nobody. */
interface PinnedPocketCards {
    suspend fun cards(): List<CachedPocketCard>
}

/**
 * Humanity, backed by the governance-reserved personhood product and drawn from a face bundled with
 * the app until that product streams its own.
 */
@Singleton
class AssetPinnedPocketCards @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dotNsTldProvider: DotNsTldProvider,
    private val faceDecoder: PocketFaceJsonDecoder,
) : PinnedPocketCards {
    private class Definition(
        val cardId: String,
        @StringRes val titleRes: Int,
        val backingProduct: (DotNsTld) -> ProductId,
    )

    private val definitions = listOf(
        Definition("humanity", RCommon.string.pocket_pinned_card_humanity, ReservedProductIds::personhood),
    )

    private val loading = Mutex()
    private var loaded: List<CachedPocketCard>? = null

    override suspend fun cards(): List<CachedPocketCard> = loading.withLock {
        loaded ?: load().also { loaded = it }
    }

    private suspend fun load(): List<CachedPocketCard> {
        val tld = dotNsTldProvider.getTldRetrying()
        return definitions.map { definition ->
            CachedPocketCard(
                card = PocketCard(
                    key = PocketCardKey(definition.backingProduct(tld), PocketCardId(definition.cardId)),
                    title = context.getString(definition.titleRes),
                    privileged = true,
                ),
                face = bundledFace(definition.cardId),
            )
        }
    }

    // Bundled with the app, so a failure here is a build defect rather than product input.
    private fun bundledFace(cardId: String) = faceDecoder
        .decode(context.assets.open("pocket/$cardId.json").bufferedReader().readText())
        .getOrElse { throw IllegalStateException("bundled Pocket face '$cardId' is invalid", it) }
}
