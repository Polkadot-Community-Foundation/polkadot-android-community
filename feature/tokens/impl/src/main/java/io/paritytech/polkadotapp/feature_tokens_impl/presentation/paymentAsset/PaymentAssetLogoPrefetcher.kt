package io.paritytech.polkadotapp.feature_tokens_impl.presentation.paymentAsset

import android.content.Context
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal interface PaymentAssetLogoPrefetcher {
    suspend fun prefetch(url: String): Result<Unit>
}

internal class CoilPaymentAssetLogoPrefetcher @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
) : PaymentAssetLogoPrefetcher {
    override suspend fun prefetch(url: String): Result<Unit> {
        val request = ImageRequest.Builder(context)
            .data(url)
            // Decoded larger than any on-screen use, so the screens are served from the memory cache.
            .size(PREFETCH_SIZE_PX)
            .build()

        return when (val result = imageLoader.execute(request)) {
            is SuccessResult -> Result.success(Unit)
            is ErrorResult -> Result.failure(result.throwable)
        }
    }

    private companion object {
        const val PREFETCH_SIZE_PX = 512
    }
}
