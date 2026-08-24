package io.paritytech.polkadotapp.tools_jwt_auth_impl.data.manager

import io.paritytech.polkadotapp.tools_jwt_auth_api.JwtAuthTokenInvalidator
import io.paritytech.polkadotapp.tools_jwt_auth_impl.data.store.JWTTokenStore
import javax.inject.Inject

class RealJwtAuthTokenInvalidator @Inject constructor(
    private val tokenStore: JWTTokenStore,
) : JwtAuthTokenInvalidator {
    override suspend fun invalidate() {
        // Both tokens: keeping the refresh token would resurrect the old
        // subject on the next refresh instead of forcing re-attestation.
        tokenStore.deleteAll()
    }
}
