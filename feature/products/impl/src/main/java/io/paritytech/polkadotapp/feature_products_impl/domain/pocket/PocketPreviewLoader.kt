package io.paritytech.polkadotapp.feature_products_impl.domain.pocket

import io.paritytech.polkadotapp.common.utils.flatMap
import io.paritytech.polkadotapp.feature_dotns_api.domain.DotNsResolver
import io.paritytech.polkadotapp.feature_dotns_api.domain.resolveToLocalFile
import io.paritytech.polkadotapp.feature_products_api.model.ExecutableKind
import io.paritytech.polkadotapp.feature_products_api.model.JsWidget
import io.paritytech.polkadotapp.feature_products_api.model.PocketCardDefinition
import io.paritytech.polkadotapp.feature_products_api.model.ProductId
import io.paritytech.polkadotapp.feature_products_impl.domain.product.ProductManifest
import java.io.File
import javax.inject.Inject

/** Reads a published card's static face out of the product's worker archive. */
class PocketPreviewLoader @Inject constructor(
    private val dotNsResolver: DotNsResolver,
    private val faceDecoder: PocketFaceJsonDecoder,
) {
    suspend fun load(productId: ProductId, definition: PocketCardDefinition): Result<JsWidget> {
        val workerHost = ProductManifest.hostOf(productId, ExecutableKind.WORKER)
        return dotNsResolver.resolveToLocalFile(workerHost.value)
            .mapCatching { archive -> archive.fileInside(definition.preview).readText() }
            .flatMap(faceDecoder::decode)
    }

    private fun File.fileInside(path: String): File {
        val file = File(this, path)
        require(file.canonicalPath.startsWith(canonicalPath + File.separator)) { "preview path escapes the archive" }
        require(file.isFile) { "preview '$path' is not in the archive" }
        return file
    }
}
