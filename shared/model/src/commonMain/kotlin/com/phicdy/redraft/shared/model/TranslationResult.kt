package com.phicdy.redraft.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Structured output of Japanese-to-English translation. */
@Serializable
data class TranslationResult(
    @SerialName("translation")
    val translation: String,
    @SerialName("alternatives")
    val alternatives: List<Alternative>,
    @SerialName("grammar_points")
    val grammarPoints: List<GrammarPoint>,
    @SerialName("expressions")
    val expressions: List<Expression>,
) {
    @Serializable
    data class Alternative(
        @SerialName("tone")
        val tone: String,
        @SerialName("text")
        val text: String,
    )

    @Serializable
    data class GrammarPoint(
        @SerialName("category")
        val category: ErrorCategory,
        @SerialName("title")
        val title: String,
        @SerialName("explanation")
        val explanation: String,
        @SerialName("example")
        val example: String,
    )

    @Serializable
    data class Expression(
        @SerialName("expression")
        val expression: String,
        @SerialName("type")
        val type: ExpressionType,
        @SerialName("meaning")
        val meaning: String,
        @SerialName("example")
        val example: String,
    )

    @Serializable
    enum class ExpressionType {
        @SerialName("word")
        Word,

        @SerialName("phrase")
        Phrase,

        @SerialName("idiom")
        Idiom,
    }
}
