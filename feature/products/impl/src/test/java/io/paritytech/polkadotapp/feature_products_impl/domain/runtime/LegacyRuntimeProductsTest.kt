package io.paritytech.polkadotapp.feature_products_impl.domain.runtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyRuntimeProductsTest {
    @Test
    fun `get cash runs native on any network and page`() {
        assertTrue(LegacyRuntimeProducts.runsNative("https://getcash.dot/onramp"))
        assertTrue(LegacyRuntimeProducts.runsNative("https://getcash.paseo/#/withdraw"))
        assertTrue(LegacyRuntimeProducts.runsNative("getcash.dot/onramp"))
    }

    @Test
    fun `every other product keeps the selected runtime`() {
        assertFalse(LegacyRuntimeProducts.runsNative("https://browse.dot"))
        assertFalse(LegacyRuntimeProducts.runsNative("https://getcash-next.dot"))
        assertFalse(LegacyRuntimeProducts.runsNative("https://app.getcash.dot"))
        assertFalse(LegacyRuntimeProducts.runsNative("not a url"))
    }
}
