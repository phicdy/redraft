package com.phicdy.redraft.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Structured output of English proofreading. */
@Serializable
data class ProofreadResult(
    @SerialName("corrected_text")
    val correctedText: String,
    @SerialName("issues")
    val issues: List<Issue>,
    @SerialName("natural_alternatives")
    val naturalAlternatives: List<NaturalAlternative>,
    @SerialName("overall_comment")
    val overallComment: String,
) {
    /**
     * A single finding. [original] is located in the input text by string search,
     * not by character offsets, because LLM-reported offsets are unreliable.
     */
    @Serializable
    data class Issue(
        @SerialName("original")
        val original: String,
        @SerialName("suggestion")
        val suggestion: String,
        @SerialName("category")
        val category: ErrorCategory,
        @SerialName("severity")
        val severity: Severity,
        @SerialName("explanation")
        val explanation: String,
    )

    @Serializable
    data class NaturalAlternative(
        @SerialName("text")
        val text: String,
        @SerialName("nuance")
        val nuance: String,
    )
}
