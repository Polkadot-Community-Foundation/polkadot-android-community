package io.paritytech.polkadotapp.feature_products_api.presentation.widget

import androidx.compose.runtime.compositionLocalOf
import io.paritytech.polkadotapp.feature_products_api.model.JsImageSource

/** Turns an image source into something the image loader can fetch, or null for one the host cannot. */
fun interface JsImageResolver {
    suspend fun resolve(source: JsImageSource): Any?
}

val LocalJsImageResolver = compositionLocalOf<JsImageResolver> { JsImageResolver { null } }
