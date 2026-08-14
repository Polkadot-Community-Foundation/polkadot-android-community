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

// Decoded by name (novasama StructDecoder), so field order here is irrelevant and
// chain-side fields we do not declare are simply never looked up. `bytes_permanent`
// is deliberately absent: pallet-bulletin-transaction-storage split renewal out into
// pallet-bulletin-data-renewal, and that slot became the opaque `extra`
// (PermanentExtent). Declaring it would make StructDecoder throw on every
// Authorizations read once the chain upgrades — nothing here consumed it anyway.
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
