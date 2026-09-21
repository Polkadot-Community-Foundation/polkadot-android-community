package io.paritytech.polkadotapp.common.utils

import io.paritytech.polkadotapp.common.BuildConfig

object CurrencyConfig {
    val defaultSymbol: String = BuildConfig.CURRENCY_SYMBOL

    @Volatile
    var symbol: String = defaultSymbol
        private set

    fun updateSymbol(symbol: String) {
        this.symbol = symbol
    }
}
