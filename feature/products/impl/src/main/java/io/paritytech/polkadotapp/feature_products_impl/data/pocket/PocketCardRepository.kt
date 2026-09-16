package io.paritytech.polkadotapp.feature_products_impl.data.pocket

import io.paritytech.polkadotapp.database.dao.PocketCardDao
import io.paritytech.polkadotapp.database.model.PocketCardLocal
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCard
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCardId
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCardKey
import io.paritytech.polkadotapp.feature_products_api.model.JsWidget
import io.paritytech.polkadotapp.feature_products_api.model.ProductId
import io.paritytech.polkadotapp.feature_products_impl.domain.pocket.CachedPocketCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Cards the user added, with the face each was approved with. Host-placed cards never land here. */
interface PocketCardRepository {
    fun observeCards(): Flow<List<CachedPocketCard>>

    suspend fun get(key: PocketCardKey): CachedPocketCard?

    suspend fun insert(card: CachedPocketCard)

    /** Whether a card was held under [key]. */
    suspend fun delete(key: PocketCardKey): Boolean
}

@Singleton
class RealPocketCardRepository @Inject constructor(
    private val dao: PocketCardDao,
) : PocketCardRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override fun observeCards(): Flow<List<CachedPocketCard>> =
        dao.observeAll().map { cards -> cards.map { it.toDomain() } }

    override suspend fun get(key: PocketCardKey): CachedPocketCard? =
        dao.get(key.productId.value, key.cardId.value)?.toDomain()

    override suspend fun insert(card: CachedPocketCard) = dao.insert(card.toLocal())

    override suspend fun delete(key: PocketCardKey): Boolean = dao.delete(key.productId.value, key.cardId.value) > 0

    private fun PocketCardLocal.toDomain() = CachedPocketCard(
        card = PocketCard(
            key = PocketCardKey(ProductId.fromStoredValue(productId), PocketCardId(cardId)),
            title = title,
            privileged = false,
        ),
        face = json.decodeFromString(JsWidget.serializer(), faceJson),
    )

    private fun CachedPocketCard.toLocal() = PocketCardLocal(
        productId = card.key.productId.value,
        cardId = card.key.cardId.value,
        title = card.title,
        faceJson = json.encodeToString(JsWidget.serializer(), face),
    )
}
