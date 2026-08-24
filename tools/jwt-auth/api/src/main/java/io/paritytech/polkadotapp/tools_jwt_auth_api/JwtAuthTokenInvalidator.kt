package io.paritytech.polkadotapp.tools_jwt_auth_api

/**
 * Drops the cached access AND refresh tokens so the next authenticated call
 * re-attests from scratch (challenge → integrity → token).
 *
 * Needed when the auth identity changes mid-session: tokens minted before a
 * wallet existed carry the install-scoped client key as subject, while the
 * backend requires username registration to be authenticated AS the
 * candidate account (device-uniqueness-backend #77). Deleting only the
 * access token would not help — a refresh keeps the original subject.
 */
interface JwtAuthTokenInvalidator {
    suspend fun invalidate()
}
