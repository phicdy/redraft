package com.phicdy.redraft.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Structured output of the weekly report: what the LLM writes from [WeeklyStats] and samples of
 * the week's proofread issues. The serial names are the LLM JSON schema and are stored with
 * each report, so never rename them.
 */
@Serializable
data class WeeklyReportNarrative(
    @SerialName("summary")
    val summary: String,
    @SerialName("recurring_mistakes")
    val recurringMistakes: List<RecurringMistake>,
    @SerialName("improvements")
    val improvements: List<String>,
    @SerialName("focus_next_week")
    val focusNextWeek: List<String>,
    @SerialName("expressions_to_review")
    val expressionsToReview: List<ExpressionToReview>,
) {
    /** A mistake that came up more than once this week. */
    @Serializable
    data class RecurringMistake(
        @SerialName("category")
        val category: ErrorCategory,
        /** What keeps going wrong, in Japanese. */
        @SerialName("pattern")
        val pattern: String,
        @SerialName("examples")
        val examples: List<MistakeExample>,
        /** How to avoid it, in Japanese. */
        @SerialName("advice")
        val advice: String,
    )

    /** A real issue from the week, as written and as corrected. */
    @Serializable
    data class MistakeExample(
        @SerialName("original")
        val original: String,
        @SerialName("suggestion")
        val suggestion: String,
    )

    @Serializable
    data class ExpressionToReview(
        @SerialName("expression")
        val expression: String,
        /** Why and how to use it, in Japanese. */
        @SerialName("note")
        val note: String,
    )
}
