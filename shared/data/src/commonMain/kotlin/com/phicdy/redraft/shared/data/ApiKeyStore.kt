package com.phicdy.redraft.shared.data

/**
 * Secure storage for API keys, keyed by provider config id.
 *
 * Implementations: Keychain on macOS, Android Keystore on Android
 * (EncryptedSharedPreferences is deprecated and must not be used).
 * Keys never leave the device: they are not part of any exported settings.
 */
interface ApiKeyStore {
    /** Returns the stored key, or null when none is stored or it can no longer be read. */
    suspend fun get(providerId: String): String?

    /** Whether a key is stored, without handing the key to the caller. */
    suspend fun contains(providerId: String): Boolean

    suspend fun put(providerId: String, apiKey: String)

    suspend fun remove(providerId: String)
}
