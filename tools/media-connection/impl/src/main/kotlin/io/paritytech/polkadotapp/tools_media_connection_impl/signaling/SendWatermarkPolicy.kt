package io.paritytech.polkadotapp.tools_media_connection_impl.signaling

import io.paritytech.polkadotapp.common.utils.InformationSize

class SendWatermarkPolicy(
    private val highWatermark: InformationSize,
    private val lowWatermark: InformationSize,
) {
    init {
        require(lowWatermark < highWatermark) { "lowWatermark must be below highWatermark" }
    }

    fun shouldWait(bufferedAmount: InformationSize, messageSize: InformationSize, wasThrottled: Boolean): Boolean {
        // An oversized message never fits under the high watermark: isolate it by waiting for an empty buffer.
        if (messageSize > highWatermark) return bufferedAmount > InformationSize.ZERO

        // Once throttled, wait for the low watermark so sends don't pause and resume around the high one.
        if (wasThrottled && bufferedAmount > lowWatermark) return true

        return bufferedAmount + messageSize > highWatermark
    }
}
