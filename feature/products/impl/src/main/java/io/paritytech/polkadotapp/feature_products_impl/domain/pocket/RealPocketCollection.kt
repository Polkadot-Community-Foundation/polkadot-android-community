package io.paritytech.polkadotapp.feature_products_impl.domain.pocket

import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCard
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketCardKey
import io.paritytech.polkadotapp.feature_products_api.domain.pocket.PocketRemoveError
import io.paritytech.polkadotapp.feature_products_api.model.JsWidget
import io.paritytech.polkadotapp.feature_products_impl.data.pocket.PocketCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealPocketCollection @Inject constructor(
    private val pinnedPocketCards: PinnedPocketCards,
    private val repository: PocketCardRepository,
) : PocketCardStore {
    override fun observeCards(): Flow<List<PocketCard>> = flow {
        val pinned = pinnedPocketCards.cards().map { it.card }
        emitAll(repository.observeCards().map { added -> pinned + added.map { it.card } })
    }

    override suspend fun removeCard(key: PocketCardKey): Result<Unit> {
        if (pinned(key) != null) return Result.failure(PocketRemoveError.Privileged)
        return runCatching { repository.delete(key) }
    }

    override suspend fun addCard(card: CachedPocketCard) {
        require(!card.card.privileged) { "only the host places privileged cards" }
        repository.insert(card)
    }

    override suspend fun cachedFace(key: PocketCardKey): JsWidget? =
        pinned(key)?.face ?: repository.get(key)?.face

    private suspend fun pinned(key: PocketCardKey): CachedPocketCard? =
        pinnedPocketCards.cards().firstOrNull { it.card.key == key }
}
