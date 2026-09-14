package io.paritytech.polkadotapp.database.model

import androidx.room.Entity

/**
 * A Pocket card the user added, with the face tree it was approved with. No foreign key to
 * `products`: a card's product need not be installed for the card to stay in the collection.
 */
@Entity(
    tableName = "pocket_cards",
    primaryKeys = ["productId", "cardId"],
)
class PocketCardLocal(
    val productId: String,
    val cardId: String,
    val title: String,
    val faceJson: String,
)
