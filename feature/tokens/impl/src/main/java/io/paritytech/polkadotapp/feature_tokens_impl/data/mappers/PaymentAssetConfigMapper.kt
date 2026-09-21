package io.paritytech.polkadotapp.feature_tokens_impl.data.mappers

import io.paritytech.polkadotapp.feature_tokens_impl.data.paymentAsset.PaymentAssetConfigRemote
import io.paritytech.polkadotapp.feature_tokens_impl.domain.paymentAsset.PaymentAssetConfig
import java.net.URI

private val WEB_SCHEMES = setOf("http", "https")

internal fun PaymentAssetConfigRemote.toDomain(): PaymentAssetConfig = PaymentAssetConfig(
    symbol = symbol?.trim()?.takeIf(String::isNotEmpty),
    squareLogoUrl = iconSquareUrl?.toWebUrlOrNull(),
    wideLogoUrl = iconWideUrl?.toWebUrlOrNull(),
)

private fun String.toWebUrlOrNull(): String? {
    val trimmed = trim()
    val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
    val isWebUrl = uri.scheme?.lowercase() in WEB_SCHEMES && uri.host != null

    return trimmed.takeIf { isWebUrl }
}
