package io.paritytech.polkadotapp.common.data.network

// Should be kept in-tact with common/build.gradle
enum class TestnetEnvironment {
    // DEV = the public io.pcf.polkadotapp.dev build. Network-wise it mirrors NIGHTLY
    // (same chains_v2 Remote Config key + nightly-* chain ids); kept as a distinct value
    // so the dev build is identifiable in-app and can diverge from nightly later.
    TESTNET, NIGHTLY, DEV, PRODUCTION;

    companion object {
        private val DEFAULT = NIGHTLY

        fun fromNameOrDefault(name: String): TestnetEnvironment {
            return runCatching { valueOf(name) }
                .getOrDefault(DEFAULT)
        }
    }
}
