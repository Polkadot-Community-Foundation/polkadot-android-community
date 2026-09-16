package io.paritytech.polkadotapp.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.paritytech.polkadotapp.database.model.PocketCardLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface PocketCardDao {
    @Query("SELECT * FROM pocket_cards")
    fun observeAll(): Flow<List<PocketCardLocal>>

    @Query("SELECT * FROM pocket_cards WHERE productId = :productId AND cardId = :cardId")
    suspend fun get(productId: String, cardId: String): PocketCardLocal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: PocketCardLocal)

    @Query("DELETE FROM pocket_cards WHERE productId = :productId AND cardId = :cardId")
    suspend fun delete(productId: String, cardId: String): Int
}
