package io.paritytech.polkadotapp.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the Pocket card tables.
 *
 * Both `main` and `truapi-dev` shipped schema version 66, with different entity sets: only the
 * latter carries the Pocket card collection. Merging the two therefore yields a version-66 schema
 * that matches neither, and Room refuses to open a database written by either side ("cannot verify
 * the data integrity", identity-hash mismatch). Bumping to 67 and creating the two tables here
 * gives the union schema its own version, so a device sitting on either 66 upgrades cleanly instead
 * of being wiped.
 *
 * Purely additive: two new tables, no existing row touched.
 */
class Migration66To67 : Migration(66, 67) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pocket_cards` " +
                "(`productId` TEXT NOT NULL, `cardId` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                "PRIMARY KEY(`productId`, `cardId`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pocket_card_faces` " +
                "(`productId` TEXT NOT NULL, `cardId` TEXT NOT NULL, `faceJson` TEXT NOT NULL, " +
                "PRIMARY KEY(`productId`, `cardId`))"
        )
    }
}
