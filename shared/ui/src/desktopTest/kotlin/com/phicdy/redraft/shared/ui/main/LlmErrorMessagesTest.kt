package com.phicdy.redraft.shared.ui.main

import com.phicdy.redraft.shared.llm.LlmError
import com.phicdy.redraft.shared.model.ErrorCategory
import com.phicdy.redraft.shared.model.ErrorCategoryGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LlmErrorMessagesTest {
    @Test
    fun everyErrorHasAMessage() {
        val errors = listOf(
            LlmError.MissingApiKey,
            LlmError.Unauthorized(message = "invalid key"),
            LlmError.RateLimited(retryAfterSeconds = null, message = null),
            LlmError.ServerError(status = 503, message = null),
            LlmError.HttpError(status = 400, message = null),
            LlmError.Timeout,
            LlmError.Network(message = null),
            LlmError.UnexpectedResponse(message = null),
            LlmError.Refused(message = "I can't help with that."),
            LlmError.Incomplete(reason = "max_output_tokens"),
            LlmError.GenerationFailed(code = null, message = null),
            LlmError.InvalidOutput(message = null),
        )
        val messages = errors.map { it.toUserMessage() }
        messages.forEach { assertTrue(it.isNotBlank()) }
        assertEquals(messages.size, messages.toSet().size, "Each error should read differently")
    }

    @Test
    fun rateLimitWithAndWithoutRetryAfter() {
        assertTrue("12秒後" in LlmError.RateLimited(retryAfterSeconds = 12, message = null).toUserMessage())
        assertTrue("秒" !in LlmError.RateLimited(retryAfterSeconds = null, message = null).toUserMessage())
    }

    @Test
    fun serverErrorShowsStatus() {
        assertTrue("503" in LlmError.ServerError(status = 503, message = null).toUserMessage())
    }

    @Test
    fun categoryNamesAreUniqueAndJapanese() {
        val names = ErrorCategory.entries.map { it.displayName() }
        assertEquals(17, names.toSet().size)
        assertEquals("冠詞", ErrorCategory.Article.displayName())
        assertEquals("スペル・句読点", ErrorCategory.SpellingPunctuation.displayName())
        assertEquals(ErrorCategoryGroup.entries.size, ErrorCategoryGroup.entries.map { it.displayName() }.toSet().size)
    }
}
