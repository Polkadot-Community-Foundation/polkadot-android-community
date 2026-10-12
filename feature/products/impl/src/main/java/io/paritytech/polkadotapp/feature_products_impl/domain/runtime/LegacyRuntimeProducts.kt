package io.paritytech.polkadotapp.feature_products_impl.domain.runtime

import java.net.URI

/**
 * Products that still speak the legacy wire, so they run on the native host even when the
 * TrUAPI core is on. Get CASH is also opened by production builds, which only carry the native host.
 */
object LegacyRuntimeProducts {
    private val labels = setOf("getcash")

    fun runsNative(url: String): Boolean {
        val host = runCatching { URI(if ("://" in url) url else "https://$url").host }.getOrNull() ?: return false
        return host.substringBefore('.') in labels
    }
}
