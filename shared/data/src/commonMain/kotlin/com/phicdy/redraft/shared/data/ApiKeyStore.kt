package com.phicdy.redraft.shared.data

/**
 * Secure storage for API keys, keyed by provider config id.
 *
 * Implementations: Keychain on macOS, Android Keystore on Android
 * (EncryptedSharedPreferences is deprecated and must not be used).
 */
interface ApiKeyStore {
    suspend fun get(providerId: String): String?

    suspend fun put(providerId: String, apiKey: String)

    suspend fun remove(providerId: String)
}
