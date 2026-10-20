package com.phicdy.redraft.shared.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsValidationTest {
    @Test
    fun acceptsHttpsUrls() {
        listOf(
            "https://api.openai.com/v1",
            "https://llm-proxy.example.com/openai/v1/",
            "  https://localhost:8443  ",
            "HTTPS://10.0.0.1:8080/v1",
        ).forEach { url -> assertNull(validateBaseUrl(url), url) }
    }

    @Test
    fun rejectsInvalidUrls() {
        assertEquals(BaseUrlError.Blank, validateBaseUrl("  "))
        assertEquals(BaseUrlError.NotHttps, validateBaseUrl("http://api.openai.com/v1"))
        assertEquals(BaseUrlError.NotHttps, validateBaseUrl("api.openai.com/v1"))
        assertEquals(BaseUrlError.Invalid, validateBaseUrl("https://"))
        assertEquals(BaseUrlError.Invalid, validateBaseUrl("https:///v1"))
        assertEquals(BaseUrlError.Invalid, validateBaseUrl("https://api openai.com"))
        assertEquals(BaseUrlError.Invalid, validateBaseUrl("https://user:pass@example.com/v1"))
        assertEquals(BaseUrlError.Invalid, validateBaseUrl("https://example.com/v1?key=1"))
        assertEquals(BaseUrlError.Invalid, validateBaseUrl("https://example.com:port/v1"))
    }

    @Test
    fun normalizesBaseUrl() {
        assertEquals("https://example.com/v1", normalizeBaseUrl(" https://example.com/v1// "))
    }

    @Test
    fun validatesModel() {
        assertNull(validateModel(" gpt-6.1-sol "))
        assertEquals(ModelError.Blank, validateModel(" "))
        assertEquals(ModelError.ContainsWhitespace, validateModel("gpt 6"))
    }

    @Test
    fun validatesApiKey() {
        assertNull(validateApiKey("sk-proj-AbC_123-xyz"))
        assertEquals(ApiKeyError.InvalidCharacters, validateApiKey("sk proj"))
        assertEquals(ApiKeyError.InvalidCharacters, validateApiKey("sk-\"x\""))
        assertEquals(ApiKeyError.InvalidCharacters, validateApiKey("ｓｋ－ａｂｃ"))
        assertEquals(ApiKeyError.TooLong, validateApiKey("a".repeat(1025)))
    }
}
