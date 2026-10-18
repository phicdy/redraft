package com.phicdy.redraft.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Fixed list of categories the LLM must choose from.
 *
 * The serial names are stored in history and used for report aggregation,
 * so existing entries must never be renamed or removed. Adding new ones is fine.
 */
@Serializable
enum class ErrorCategory(val group: ErrorCategoryGroup) {
    @SerialName("article")
    Article(ErrorCategoryGroup.Grammar),

    @SerialName("number")
    Number(ErrorCategoryGroup.Grammar),

    @SerialName("tense")
    Tense(ErrorCategoryGroup.Grammar),

    @SerialName("agreement")
    Agreement(ErrorCategoryGroup.Grammar),

    @SerialName("preposition")
    Preposition(ErrorCategoryGroup.Grammar),

    @SerialName("verb_form")
    VerbForm(ErrorCategoryGroup.Grammar),

    @SerialName("word_order")
    WordOrder(ErrorCategoryGroup.Grammar),

    @SerialName("sentence_structure")
    SentenceStructure(ErrorCategoryGroup.Grammar),

    @SerialName("pronoun")
    Pronoun(ErrorCategoryGroup.Grammar),

    @SerialName("word_choice")
    WordChoice(ErrorCategoryGroup.Vocabulary),

    @SerialName("collocation")
    Collocation(ErrorCategoryGroup.Vocabulary),

    @SerialName("literal_translation")
    LiteralTranslation(ErrorCategoryGroup.Vocabulary),

    @SerialName("register")
    Register(ErrorCategoryGroup.Style),

    @SerialName("conciseness")
    Conciseness(ErrorCategoryGroup.Style),

    @SerialName("clarity")
    Clarity(ErrorCategoryGroup.Style),

    @SerialName("naturalness")
    Naturalness(ErrorCategoryGroup.Style),

    @SerialName("spelling_punctuation")
    SpellingPunctuation(ErrorCategoryGroup.Mechanics),
}

enum class ErrorCategoryGroup {
    Grammar,
    Vocabulary,
    Style,
    Mechanics,
}

@Serializable
enum class Severity {
    /** Grammatically wrong. */
    @SerialName("error")
    Error,

    /** Not wrong, but could be better. */
    @SerialName("improvement")
    Improvement,
}
