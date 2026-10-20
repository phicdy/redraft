package com.phicdy.redraft.shared.ui.settings

enum class BaseUrlError {
    Blank,
    NotHttps,
    Invalid,
}

enum class ModelError {
    Blank,
    ContainsWhitespace,
}

enum class ApiKeyError {
    /** Spaces, quotes, non-ASCII or similar; real API keys never contain them. */
    InvalidCharacters,
    TooLong,
}

private const val HTTPS_PREFIX = "https://"
private const val MAX_API_KEY_LENGTH = 1024

/** Host name or IPv4 address with an optional port. User info, IPv6 literals and spaces are rejected. */
private val HOST_AND_PORT = Regex("[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?(:[0-9]{1,5})?")

/**
 * Accepts `https://host[:port][/path]`. A query or fragment is rejected because the
 * adapter appends `/responses` to the base URL.
 */
fun validateBaseUrl(input: String): BaseUrlError? {
    val url = input.trim()
    if (url.isEmpty()) return BaseUrlError.Blank
    if (!url.startsWith(HTTPS_PREFIX, ignoreCase = true)) return BaseUrlError.NotHttps
    val rest = url.substring(HTTPS_PREFIX.length)
    if (rest.any { it.isWhitespace() || it == '?' || it == '#' || it == '\\' }) return BaseUrlError.Invalid
    val hostAndPort = rest.substringBefore('/')
    return if (HOST_AND_PORT.matches(hostAndPort)) null else BaseUrlError.Invalid
}

/** Trims spaces and trailing slashes so the stored value is canonical. Call after [validateBaseUrl]. */
fun normalizeBaseUrl(input: String): String = input.trim().trimEnd('/')

fun validateModel(input: String): ModelError? {
    val model = input.trim()
    return when {
        model.isEmpty() -> ModelError.Blank
        model.any { it.isWhitespace() } -> ModelError.ContainsWhitespace
        else -> null
    }
}

/** Validates a newly entered key (already trimmed). An empty input means "keep the saved key". */
fun validateApiKey(input: String): ApiKeyError? = when {
    input.length > MAX_API_KEY_LENGTH -> ApiKeyError.TooLong
    input.any { it !in '!'..'~' || it == '"' || it == '\'' || it == '\\' } -> ApiKeyError.InvalidCharacters
    else -> null
}
