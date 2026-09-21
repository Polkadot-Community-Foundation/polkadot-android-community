package io.paritytech.polkadotapp.feature_tokens_api.presentation.paymentAsset.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.paritytech.polkadotapp.design.components.image.NovaAsyncImage
import io.paritytech.polkadotapp.feature_tokens_api.presentation.paymentAsset.PaymentAssetLogo

/**
 * Draws a [PaymentAssetLogo.Remote] from the image cache and composes [fallback] for a bundled logo or when the
 * image cannot be loaded.
 */
@Composable
fun PaymentAssetLogoImage(
    modifier: Modifier = Modifier,
    logo: PaymentAssetLogo,
    fallback: @Composable () -> Unit,
) {
    var loadFailed by remember(logo) { mutableStateOf(false) }

    if (logo is PaymentAssetLogo.Remote && !loadFailed) {
        NovaAsyncImage(
            modifier = modifier,
            model = logo.url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onError = { loadFailed = true },
        )
    } else {
        fallback()
    }
}
