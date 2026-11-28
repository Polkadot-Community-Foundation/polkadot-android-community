package io.paritytech.polkadotapp.app.internal

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import io.sentry.android.core.SentryAndroid

/**
 * Starts Sentry before Application.onCreate, as the SDK's own provider would, so early crashes are
 * still reported, but with [InternalSentryScrubber] installed. The DSN and the remaining options come
 * from the manifest; an empty DSN leaves Sentry disabled.
 */
class InternalSentryInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val context = context ?: return false
        SentryAndroid.init(context) { options -> InternalSentryScrubber.applyTo(options) }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
