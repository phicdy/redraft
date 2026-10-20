package com.phicdy.redraft.shared.llm.schema

import com.phicdy.redraft.shared.llm.OutputSchema
import com.phicdy.redraft.shared.model.ErrorCategory
import com.phicdy.redraft.shared.model.ProofreadResult
import com.phicdy.redraft.shared.model.Severity
import com.phicdy.redraft.shared.model.TranslationResult
import kotlinx.serialization.json.JsonObject

/**
 * JSON schemas for structured output. They mirror [TranslationResult] and [ProofreadResult]
 * field by field; `OutputSchemasTest` checks that they stay in sync with the serializers.
 *
 * Count limits (e.g. at most five expressions) are stated in the prompt, not here, and
 * descriptions only say which language a field is written in.
 */
object OutputSchemas {
    val translation: OutputSchema = OutputSchema(
        name = "translation_result",
        schema = objectSchema(
            "translation" to stringSchema("The English translation of the input."),
            "alternatives" to arraySchema(
                objectSchema(
                    "tone" to stringSchema("Japanese label for the tone, e.g. よりフォーマル / よりカジュアル."),
                    "text" to stringSchema("The alternative English translation."),
                ),
                "Other English renderings in a different tone. Empty if there are none worth showing.",
            ),
            "grammar_points" to arraySchema(
                objectSchema(
                    "category" to categorySchema(),
                    "title" to stringSchema("Short title of the grammar point, in Japanese."),
                    "explanation" to stringSchema("Explanation in Japanese."),
                    "example" to stringSchema("An English example sentence."),
                ),
                "Grammar worth learning from this translation. Empty if there is nothing notable.",
            ),
            "expressions" to arraySchema(
                objectSchema(
                    "expression" to stringSchema("The English word, phrase or idiom."),
                    "type" to enumSchema(
                        TranslationResult.ExpressionType.serializer().descriptor,
                        "Whether this is a single word, a phrase or an idiom.",
                    ),
                    "meaning" to stringSchema("Meaning and usage in Japanese."),
                    "example" to stringSchema("An English example sentence."),
                ),
                "Useful expressions from the translation. Empty if there are none.",
            ),
        ),
    )

    val proofread: OutputSchema = OutputSchema(
        name = "proofread_result",
        schema = objectSchema(
            "corrected_text" to stringSchema("The whole input with every issue fixed, in English."),
            "issues" to arraySchema(
                objectSchema(
                    "original" to stringSchema("The exact problematic span copied verbatim from the input."),
                    "suggestion" to stringSchema("The replacement for that span, in English."),
                    "category" to categorySchema(),
                    "severity" to enumSchema(
                        Severity.serializer().descriptor,
                        "error if it is wrong, improvement if it is correct but could be better.",
                    ),
                    "explanation" to stringSchema("Why it should change, in Japanese."),
                ),
                "Problems found in the input. Empty if there are none.",
            ),
            "natural_alternatives" to arraySchema(
                objectSchema(
                    "text" to stringSchema("A more natural way to say the whole text, in English."),
                    "nuance" to stringSchema("How its nuance differs, in Japanese."),
                ),
                "More natural rewrites. Empty if the text is already natural.",
            ),
            "overall_comment" to stringSchema("A short overall comment in Japanese."),
        ),
    )

    private fun categorySchema(): JsonObject = enumSchema(
        ErrorCategory.serializer().descriptor,
        "The category from the fixed list.",
    )
}
