package io.paritytech.polkadotapp.database.migrations

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Slot rows get a tag naming the consumer that allocated them. Before that, the per-chat signer slots were the only
 * Critical (level 2) rows, so those are tagged as chat signer slots and their cleanup keeps finding them.
 */
class Migration70To71Spec : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE statement_store_slot_allocations SET tag = 'chat-signer' WHERE priorityLevel = 2")
    }
}
