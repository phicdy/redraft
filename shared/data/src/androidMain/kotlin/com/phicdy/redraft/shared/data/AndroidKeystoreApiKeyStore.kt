package com.phicdy.redraft.shared.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.ProviderException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts API keys with an AES-256-GCM key that lives in the Android Keystore and never
 * leaves it. Only the ciphertext and its random IV are written to app-private
 * SharedPreferences (the app sets `android:allowBackup="false"`, so they are not backed up).
 *
 * The provider id is bound as associated data, so a ciphertext cannot be moved to another id.
 * If the Keystore key is gone or decryption fails (for example after the Keystore was reset),
 * the stored value is treated as missing and cleared instead of crashing.
 */
class AndroidKeystoreApiKeyStore(context: Context) : ApiKeyStore {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    override suspend fun get(providerId: String): String? = withContext(Dispatchers.IO) {
        mutex.withLock { read(providerId) }
    }

    override suspend fun contains(providerId: String): Boolean = get(providerId) != null

    override suspend fun put(providerId: String, apiKey: String) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val encoded = encrypt(providerId, apiKey)
                check(preferences.edit().putString(preferenceKey(providerId), encoded).commit()) {
                    "Failed to write the encrypted API key"
                }
            }
        }
    }

    override suspend fun remove(providerId: String) {
        withContext(Dispatchers.IO) {
            mutex.withLock { clear(providerId) }
        }
    }

    private fun read(providerId: String): String? {
        val stored = preferences.getString(preferenceKey(providerId), null) ?: return null
        return try {
            decrypt(providerId, stored)
        } catch (e: GeneralSecurityException) {
            // Covers a missing or invalidated Keystore key and AEADBadTagException.
            clear(providerId)
            null
        } catch (e: ProviderException) {
            // Keystore failures that surface as runtime exceptions.
            clear(providerId)
            null
        } catch (e: IllegalArgumentException) {
            // Malformed stored value (bad format or Base64).
            clear(providerId)
            null
        }
    }

    private fun clear(providerId: String) {
        preferences.edit().remove(preferenceKey(providerId)).commit()
    }

    private fun encrypt(providerId: String, apiKey: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        try {
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        } catch (e: InvalidKeyException) {
            // The existing key cannot be used any more; replace it once. Old ciphertexts become unreadable
            // and are cleared on their next read.
            deleteKey()
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        // The Keystore generates a random 12-byte IV for each encryption.
        val iv = cipher.iv
        cipher.updateAAD(providerId.encodeToByteArray())
        val ciphertext = cipher.doFinal(apiKey.encodeToByteArray())
        val encoder = Base64.getEncoder()
        return listOf(FORMAT_VERSION, encoder.encodeToString(iv), encoder.encodeToString(ciphertext))
            .joinToString(SEPARATOR)
    }

    private fun decrypt(providerId: String, stored: String): String {
        val parts = stored.split(SEPARATOR)
        require(parts.size == 3 && parts[0] == FORMAT_VERSION) { "Unknown stored key format" }
        val decoder = Base64.getDecoder()
        val iv = decoder.decode(parts[1])
        val ciphertext = decoder.decode(parts[2])
        val key = existingKey() ?: throw InvalidKeyException("The Keystore key does not exist")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        cipher.updateAAD(providerId.encodeToByteArray())
        return cipher.doFinal(ciphertext).decodeToString()
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun existingKey(): SecretKey? = keyStore().getKey(KEY_ALIAS, null) as? SecretKey

    private fun getOrCreateKey(): SecretKey = existingKey() ?: generateKey()

    private fun generateKey(): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .build()
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(spec)
        return generator.generateKey()
    }

    private fun deleteKey() {
        keyStore().deleteEntry(KEY_ALIAS)
    }

    private fun preferenceKey(providerId: String): String = "api_key.$providerId"

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "redraft_api_key_encryption"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val GCM_TAG_LENGTH_BITS = 128
        const val PREFERENCES_NAME = "redraft_api_keys"
        const val FORMAT_VERSION = "v1"
        const val SEPARATOR = ":"
    }
}
