package com.phicdy.redraft.shared.llm.prompt

import com.phicdy.redraft.shared.llm.schema.OutputSchemas
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PromptsTest {
    @Test
    fun translationRequestUsesTranslationSchema() {
        val request = Prompts.translation("明日の会議を延期したいです", WritingContext(), model = "m")

        assertEquals(OutputSchemas.translation, request.schema)
        assertEquals("明日の会議を延期したいです", request.input)
        assertEquals("m", request.model)
    }

    @Test
    fun proofreadRequestUsesProofreadSchema() {
        val request = Prompts.proofread("I look forward to see you.", WritingContext(), model = "m")

        assertEquals(OutputSchemas.proofread, request.schema)
    }

    @Test
    fun instructionsStateCountLimitsLanguageAndCategories() {
        val translation = Prompts.translationInstructions(WritingContext())
        val proofread = Prompts.proofreadInstructions(WritingContext())

        assertTrue("at most ${Prompts.MAX_EXPRESSIONS}" in translation)
        assertTrue("at most ${Prompts.MAX_NATURAL_ALTERNATIVES}" in proofread)
        listOf(translation, proofread).forEach { instructions ->
            assertTrue("American English" in instructions)
            assertTrue("in Japanese" in instructions)
            assertTrue("- spelling_punctuation:" in instructions)
            assertTrue("- literal_translation:" in instructions)
        }
    }

    @Test
    fun instructionsReflectWritingContext() {
        val email = Prompts.proofreadInstructions(WritingContext(WritingPurpose.Email, Politeness.Formal))
        val chat = Prompts.translationInstructions(WritingContext(WritingPurpose.Chat, Politeness.Casual))

        assertTrue("business email" in email)
        assertTrue("formal and polite" in email)
        assertTrue("work chat" in chat)
        assertTrue("casual and friendly" in chat)
    }
}
