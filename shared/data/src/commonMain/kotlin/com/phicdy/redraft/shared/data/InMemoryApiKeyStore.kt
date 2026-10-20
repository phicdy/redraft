package com.phicdy.redraft.shared.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps keys in process memory only, so they are gone when the app exits.
 *
 * Used on desktop platforms other than macOS (Redraft ships for macOS; Linux is only
 * used by CI) and in tests. Nothing is written to disk, so there is no plaintext key
 * file to protect.
 */
class InMemoryApiKeyStore : ApiKeyStore {
    private val mutex = Mutex()
    private val keys = mutableMapOf<String, String>()

    override suspend fun get(providerId: String): String? = mutex.withLock { keys[providerId] }

    override suspend fun contains(providerId: String): Boolean = mutex.withLock { providerId in keys }

    override suspend fun put(providerId: String, apiKey: String) {
        mutex.withLock { keys[providerId] = apiKey }
    }

    override suspend fun remove(providerId: String) {
        mutex.withLock { keys.remove(providerId) }
    }
}
