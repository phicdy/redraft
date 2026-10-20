package com.phicdy.redraft.shared.data

import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Checks the commands sent to `security`; the real Keychain cannot be used on CI. */
class MacKeychainApiKeyStoreTest {
    private class Call(val arguments: List<String>, val stdin: String?)

    private val calls = mutableListOf<Call>()

    private fun store(respond: (List<String>) -> SecurityCommandResult): MacKeychainApiKeyStore =
        MacKeychainApiKeyStore(
            service = "com.phicdy.redraft",
            runner = SecurityCommandRunner { arguments, stdin ->
                calls += Call(arguments, stdin)
                respond(arguments)
            },
        )

    private fun result(exitCode: Int = 0, stdout: String = "", stderr: String = ""): SecurityCommandResult =
        SecurityCommandResult(exitCode = exitCode, stdout = stdout, stderr = stderr)

    @Test
    fun putSendsTheKeyOnStdinOnly(): TestResult = runTest {
        val store = store { result() }

        store.put("openai", "sk-proj-AbC_123-xyz")

        val add = calls.first()
        assertEquals(listOf("-i"), add.arguments)
        assertEquals(
            "add-generic-password -U -s \"com.phicdy.redraft\" -a \"openai\" -w \"sk-proj-AbC_123-xyz\"\n",
            add.stdin,
        )
        assertTrue(calls.none { call -> call.arguments.any { "sk-proj" in it } })
        // Verifies the item exists afterwards, without reading the password back.
        assertEquals(listOf("find-generic-password", "-s", "com.phicdy.redraft", "-a", "openai"), calls[1].arguments)
    }

    @Test
    fun putFailsWhenTheInteractiveCommandFails(): TestResult = runTest {
        val store = store { arguments ->
            if (arguments == listOf("-i")) result(stderr = "add-generic-password: returned 45\n") else result()
        }

        assertFailsWith<KeychainException> { store.put("openai", "sk-test") }
    }

    @Test
    fun putRejectsKeysThatWouldNeedEscaping(): TestResult = runTest {
        val store = store { result() }

        for (key in listOf("sk test", "sk\"x", "sk'x", "sk\\x", "sk\nx", "", "ｓｋ-全角")) {
            assertFailsWith<IllegalArgumentException> { store.put("openai", key) }
        }
        assertTrue(calls.isEmpty())
    }

    @Test
    fun getReadsThePasswordFromStdout(): TestResult = runTest {
        val store = store { result(stdout = "sk-test\n") }

        assertEquals("sk-test", store.get("openai"))
        assertEquals(
            listOf("find-generic-password", "-s", "com.phicdy.redraft", "-a", "openai", "-w"),
            calls.single().arguments,
        )
    }

    @Test
    fun missingItemIsNull(): TestResult = runTest {
        val notFound = MacKeychainApiKeyStore.ITEM_NOT_FOUND_EXIT_CODE
        val store = store {
            result(exitCode = notFound, stderr = "security: SecKeychainSearchCopyNext: The specified item could not be found in the keychain.\n")
        }

        assertNull(store.get("openai"))
        assertFalse(store.contains("openai"))
        store.remove("openai")
    }

    @Test
    fun otherFailuresAreErrors(): TestResult = runTest {
        val store = store { result(exitCode = 1) }

        assertFailsWith<KeychainException> { store.get("openai") }
        assertFailsWith<KeychainException> { store.remove("openai") }
    }

    @Test
    fun rejectsUnsafeProviderIds(): TestResult = runTest {
        val store = store { result() }

        assertFailsWith<IllegalArgumentException> { store.get("open ai") }
        assertFailsWith<IllegalArgumentException> { store.put("a\" -A \"b", "sk-test") }
        assertTrue(calls.isEmpty())
    }
}
