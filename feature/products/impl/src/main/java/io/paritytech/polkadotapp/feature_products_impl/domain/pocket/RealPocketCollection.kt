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
        val pinnedKeys = pinned.map { it.key }.toSet()

        // A pinned card is kept for its face, not as a card of its own, so its row is not a second card.
        emitAll(
            repository.observeCards().map { stored ->
                pinned + stored.map { it.card }.filterNot { it.key in pinnedKeys }
            }
        )
    }

    override suspend fun removeCard(key: PocketCardKey): Result<Unit> = remove(key).map {}

    override suspend fun remove(key: PocketCardKey): Result<PocketRemoval> {
        if (pinned(key) != null) return Result.failure(PocketRemoveError.Privileged)
        return runCatching { if (repository.delete(key)) PocketRemoval.REMOVED else PocketRemoval.ABSENT }
    }

    override suspend fun addCard(card: CachedPocketCard) {
        require(!card.card.privileged) { "only the host places privileged cards" }
        repository.insert(card)
    }

    /** The newest face the product drew, else the one bundled with a pinned card for its first run. */
    override suspend fun cachedFace(key: PocketCardKey): JsWidget? =
        repository.get(key)?.face ?: pinned(key)?.face

    override suspend fun cacheFace(key: PocketCardKey, face: JsWidget) {
        val held = repository.get(key) ?: pinned(key) ?: return
        repository.insert(held.copy(face = face))
    }

    private suspend fun pinned(key: PocketCardKey): CachedPocketCard? =
        pinnedPocketCards.cards().firstOrNull { it.card.key == key }
}
