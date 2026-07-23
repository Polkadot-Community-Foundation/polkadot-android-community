package io.paritytech.polkadotapp.tools_ipfs_impl.data

import io.ipfs.multihash.Multihash
import io.paritytech.polkadotapp.tools_ipfs_api.Cid
import io.paritytech.polkadotapp.tools_ipfs_api.IpfsContentLookup
import io.paritytech.polkadotapp.tools_ipfs_api.getDefaultRawLink
import io.paritytech.polkadotapp.tools_ipfs_impl.data.config.IpfsGatewayUrlProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

internal class RealIpfsContentLookup @Inject constructor(
    private val gatewayUrlProvider: IpfsGatewayUrlProvider,
    private val httpClient: OkHttpClient,
) : IpfsContentLookup {
    override suspend fun getIpfsLinkFor(hash: Multihash, codec: Cid.Codec): Result<String> {
        val cid = Cid.buildCidV1(codec, hash.type, hash.hash).toString()
        return getIpfsLinkFor(cid)
    }

    override suspend fun getIpfsLinkFor(cid: String): Result<String> {
        return gatewayUrlProvider.getGatewayUrl()
            // The configured gateway URL carries a trailing slash (iOS resolves the CID
            // against it RFC-3986-relative, which drops the last path segment without one).
            // Trim it here so we request /ipfs/<cid> directly instead of /ipfs//<cid> and
            // relying on the gateway to normalise the double slash away.
            .map { gatewayUrl -> "${gatewayUrl.trimEnd('/')}/$cid" }
    }

    override suspend fun lookupRawHash(hash: ByteArray): Result<ByteArray> {
        return getDefaultRawLink(hash).mapCatching { url ->
            withContext(Dispatchers.IO) {
                val request = Request.Builder().url(url).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("IPFS fetch failed: HTTP ${response.code} for $url")
                    }
                    response.body.bytes()
                }
            }
        }
    }
}
