package io.paritytech.polkadotapp.feature_wallet_impl.presentation.enterAmount.compose.components

import org.junit.Assert.assertEquals
import org.junit.Test

class LockupPlacementTest {
    @Test
    fun `amount that fits keeps its own width and is centred`() {
        val placement = lockupPlacement(boxWidth = 1000, widths = LockupWidths(fiatSymbol = 50f, amount = 300f, ticker = 150f))

        assertEquals(LockupPlacement(amountSlot = 300f, offsetX = 250f, amountOverflows = false), placement)
    }

    @Test
    fun `amount wider than eighty percent is clamped to the remaining slot`() {
        val placement = lockupPlacement(boxWidth = 1000, widths = LockupWidths(fiatSymbol = 50f, amount = 900f, ticker = 150f))

        assertEquals(LockupPlacement(amountSlot = 600f, offsetX = 100f, amountOverflows = true), placement)
    }

    @Test
    fun `amount exactly at the limit does not overflow`() {
        val placement = lockupPlacement(boxWidth = 1000, widths = LockupWidths(fiatSymbol = 50f, amount = 600f, ticker = 150f))

        assertEquals(LockupPlacement(amountSlot = 600f, offsetX = 100f, amountOverflows = false), placement)
    }

    @Test
    fun `eighty percent limit rounds to the nearest pixel`() {
        val placement = lockupPlacement(boxWidth = 936, widths = LockupWidths(fiatSymbol = 50f, amount = 549f, ticker = 150f))

        assertEquals(false, placement.amountOverflows)
        assertEquals(549f, placement.amountSlot)
    }

    @Test
    fun `symbol and ticker wider than the limit give a zero slot, not a negative one`() {
        val placement = lockupPlacement(boxWidth = 100, widths = LockupWidths(fiatSymbol = 50f, amount = 10f, ticker = 150f))

        assertEquals(0f, placement.amountSlot)
        assertEquals(0f, placement.offsetX)
    }

    @Test
    fun `zero box gives zero slot and offset`() {
        val placement = lockupPlacement(boxWidth = 0, widths = LockupWidths(fiatSymbol = 0f, amount = 0f, ticker = 0f))

        assertEquals(LockupPlacement(amountSlot = 0f, offsetX = 0f, amountOverflows = false), placement)
    }
}
