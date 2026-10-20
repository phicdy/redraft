package com.phicdy.redraft.shared.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Stores API keys in the user's default (login) Keychain as generic passwords
 * (service [SERVICE], account = provider id), using the system `/usr/bin/security` tool.
 *
 * The key is never passed as a command-line argument, where other processes could read it
 * (for example with `ps`). It is written to the standard input of `security -i`, which reads
 * commands line by line from stdin until EOF and splits them like a shell (quotes and backslash
 * escapes). Reading uses `find-generic-password -w`, which prints only the password to stdout.
 *
 * As documented in security(1), the application that creates an item is trusted to access it,
 * which here is `/usr/bin/security`. So the key is protected at rest by the Keychain, but other
 * programs running as the same user can read it through the same tool without a prompt.
 */
class MacKeychainApiKeyStore internal constructor(
    private val service: String,
    private val runner: SecurityCommandRunner,
) : ApiKeyStore {
    constructor() : this(service = SERVICE, runner = ProcessSecurityCommandRunner(SECURITY_EXECUTABLE))

    private val mutex = Mutex()

    init {
        require(isSafeToken(service)) { "Invalid Keychain service name" }
    }

    override suspend fun get(providerId: String): String? = locked {
        val result = runner.run(findCommand(providerId) + "-w", stdin = null)
        when (result.exitCode) {
            0 -> result.stdout.removeSuffix("\n").takeIf { it.isNotEmpty() }
            ITEM_NOT_FOUND_EXIT_CODE -> null
            // Do not include stdout: it may contain the password.
            else -> throw KeychainException("find-generic-password failed (exit ${result.exitCode})")
        }
    }

    override suspend fun contains(providerId: String): Boolean = locked { exists(providerId) }

    override suspend fun put(providerId: String, apiKey: String) {
        locked {
            require(isStorableApiKey(apiKey)) { "The API key contains characters that cannot be stored" }
            val result = runner.run(listOf("-i"), stdin = addCommandLine(providerId, apiKey))
            // In interactive mode a failed command reports "<command>: returned <status>" on stderr.
            val stderr = result.stderr.replace(apiKey, "***").trim()
            if (result.exitCode != 0 || stderr.contains(": returned ")) {
                throw KeychainException("add-generic-password failed (exit ${result.exitCode}): $stderr")
            }
            if (!exists(providerId)) {
                throw KeychainException("The API key was not found in the Keychain after saving")
            }
        }
    }

    override suspend fun remove(providerId: String) {
        locked {
            val result = runner.run(
                listOf("delete-generic-password", "-s", service, "-a", providerId),
                stdin = null,
            )
            if (result.exitCode != 0 && result.exitCode != ITEM_NOT_FOUND_EXIT_CODE) {
                throw KeychainException(
                    "delete-generic-password failed (exit ${result.exitCode}): ${result.stderr.trim()}",
                )
            }
        }
    }

    /** The line written to `security -i`. Only values checked by [isSafeToken] / [isStorableApiKey] reach it. */
    internal fun addCommandLine(providerId: String, apiKey: String): String {
        require(isSafeToken(providerId)) { "Invalid provider id" }
        return "add-generic-password -U -s \"$service\" -a \"$providerId\" -w \"$apiKey\"\n"
    }

    private fun exists(providerId: String): Boolean {
        val result = runner.run(findCommand(providerId), stdin = null)
        return when (result.exitCode) {
            0 -> true
            ITEM_NOT_FOUND_EXIT_CODE -> false
            else -> throw KeychainException(
                "find-generic-password failed (exit ${result.exitCode}): ${result.stderr.trim()}",
            )
        }
    }

    private fun findCommand(providerId: String): List<String> {
        require(isSafeToken(providerId)) { "Invalid provider id" }
        return listOf("find-generic-password", "-s", service, "-a", providerId)
    }

    private suspend fun <T> locked(block: () -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock { block() }
    }

    internal companion object {
        const val SERVICE: String = "com.phicdy.redraft"
        const val SECURITY_EXECUTABLE: String = "/usr/bin/security"

        /** `security` exits with 44 when the item does not exist (errSecItemNotFound). */
        const val ITEM_NOT_FOUND_EXIT_CODE: Int = 44

        /** `security -i` reads lines of at most 4096 bytes; keep well below that. */
        private const val MAX_API_KEY_LENGTH = 1024

        private val SAFE_TOKEN = Regex("[A-Za-z0-9._-]+")

        fun isSafeToken(value: String): Boolean = SAFE_TOKEN.matches(value)

        /**
         * Printable ASCII without whitespace, quotes or backslashes, so the key can be placed inside
         * double quotes on a `security -i` line without any escaping. Real API keys use far fewer characters.
         */
        fun isStorableApiKey(value: String): Boolean = value.isNotEmpty() &&
            value.length <= MAX_API_KEY_LENGTH &&
            value.all { it in '!'..'~' && it != '"' && it != '\'' && it != '\\' }
    }
}

class KeychainException(message: String) : IOException(message)

internal fun interface SecurityCommandRunner {
    /** Runs the `security` tool with [arguments] and writes [stdin], if any, to its standard input. */
    fun run(arguments: List<String>, stdin: String?): SecurityCommandResult
}

internal class SecurityCommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

internal class ProcessSecurityCommandRunner(private val executable: String) : SecurityCommandRunner {
    override fun run(arguments: List<String>, stdin: String?): SecurityCommandResult {
        val process = ProcessBuilder(listOf(executable) + arguments).start()
        // Closing stdin sends EOF, which ends `security -i`.
        process.outputStream.use { output ->
            if (stdin != null) output.write(stdin.encodeToByteArray())
        }
        // The outputs are a few lines at most, far below the pipe buffer, so reading them in turn cannot block.
        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        return SecurityCommandResult(exitCode = exitCode, stdout = stdout, stderr = stderr)
    }
}
