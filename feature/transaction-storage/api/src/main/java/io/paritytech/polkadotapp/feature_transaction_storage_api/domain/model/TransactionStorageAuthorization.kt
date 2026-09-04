package io.paritytech.polkadotapp.feature_transaction_storage_api.domain.model

import io.novasama.substrate_sdk_android.koltinx_serialization_scale.serializers.BigIntegerSerializable
import io.paritytech.polkadotapp.chains.network.binding.BlockNumber
import io.paritytech.polkadotapp.common.utils.InformationSize
import io.paritytech.polkadotapp.common.utils.atLeastZero
import kotlinx.serialization.Serializable
import java.math.BigInteger

@Serializable
data class TransactionStorageAuthorization(
    val extent: TransactionStorageExtent,
    val expiration: BlockNumber,
)

// Decoded by name, not by position: the SDK turns SCALE into a name-keyed
// `Struct.Instance` and then `StructDecoder` looks up each declared field via
// `descriptor.getElementName(index)`, throwing `IllegalStateException("… is not
// found in the …")` when a name is absent. Fields present on chain but not
// declared here are simply never looked up, so omitting one is safe.
//
// `bytes_permanent` is therefore deliberately NOT declared. Bulletin v0.0.26
// (paseo bulletin runtime spec 2_004_000) splits it out of the extent and into
// `AuthorizationExtent::extra: PermanentExtent { bytes_permanent }`. The SCALE
// layout is unchanged — `extra` occupies the slot the flat field had — but the
// *name* moves a level down, so a declared `bytesPermanent` stops resolving and
// every authorization read throws. Nothing in this app reads the value.
@Serializable
data class TransactionStorageExtent(
    val transactions: BigIntegerSerializable,
    val transactionsAllowance: BigIntegerSerializable,
    val bytes: BigIntegerSerializable,
    val bytesAllowance: BigIntegerSerializable
)

val TransactionStorageExtent.remainingTransactions: BigInteger
    get() = (transactionsAllowance - transactions).atLeastZero()

val TransactionStorageExtent.remainingBytes: BigInteger
    get() = (bytesAllowance - bytes).atLeastZero()

fun TransactionStorageAuthorization.hasExpiredAt(blockNumber: BlockNumber): Boolean {
    return blockNumber > expiration
}

fun TransactionStorageAuthorization.hasCapacityFor(size: InformationSize): Boolean {
    return extent.remainingTransactions > BigInteger.ZERO && extent.remainingBytes.toLong() >= size.inWholeBytes
}

fun TransactionStorageAuthorization.storedTransactionAfter(previousTransactionsCount: BigInteger): Boolean {
    return extent.transactionsAllowance < previousTransactionsCount
}

fun TransactionStorageAuthorization.increasedAllocationAfter(previousTransactionsCount: BigInteger): Boolean {
    return extent.transactionsAllowance > previousTransactionsCount
}
