package com.phicdy.redraft.shared.data

import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryApiKeyStoreTest {
    private val store = InMemoryApiKeyStore()

    @Test
    fun storesKeysPerProvider(): TestResult = runTest {
        store.put("openai", "sk-one")
        store.put("work", "sk-two")

        assertEquals("sk-one", store.get("openai"))
        assertEquals("sk-two", store.get("work"))
        assertTrue(store.contains("openai"))
    }

    @Test
    fun putReplacesAndRemoveDeletes(): TestResult = runTest {
        store.put("openai", "sk-old")
        store.put("openai", "sk-new")
        assertEquals("sk-new", store.get("openai"))

        store.remove("openai")

        assertNull(store.get("openai"))
        assertFalse(store.contains("openai"))
    }

    @Test
    fun removingAMissingKeyIsANoOp(): TestResult = runTest {
        store.remove("openai")

        assertNull(store.get("openai"))
    }
}
