package com.phicdy.redraft.shared.llm.prompt

import com.phicdy.redraft.shared.model.ProofreadResult
import com.phicdy.redraft.shared.model.WeeklyStats
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** What the weekly report is written from. */
data class WeeklyReportInput(
    val stats: WeeklyStats,
    /** Proofread issues of the week, newest first. Any number; they are sampled and shortened before sending. */
    val issues: List<ProofreadResult.Issue>,
    /** The week has not ended yet, so this week's counts are partial. */
    val isInProgress: Boolean,
)

/**
 * The user message of the weekly report request. The data is sent as JSON so the model can tell
 * numbers and quoted text apart from instructions. Every list and text is capped, which keeps the
 * request size bounded however much was written during the week.
 */
internal object WeeklyReportPromptInput {
    const val MAX_ISSUE_SAMPLES: Int = 30
    const val MAX_EXPRESSIONS: Int = 40
    const val MAX_SPAN_LENGTH: Int = 160
    const val MAX_EXPLANATION_LENGTH: Int = 300
    const val MAX_MEANING_LENGTH: Int = 120

    private val json = Json { encodeDefaults = true }

    fun encode(input: WeeklyReportInput): String {
        val stats = input.stats.copy(
            expressions = input.stats.expressions.take(MAX_EXPRESSIONS).map { expression ->
                expression.copy(
                    expression = expression.expression.shorten(MAX_SPAN_LENGTH),
                    meaning = expression.meaning.shorten(MAX_MEANING_LENGTH),
                )
            },
        )
        val payload = Payload(
            weekStatus = if (input.isInProgress) WEEK_IN_PROGRESS else WEEK_COMPLETED,
            stats = stats,
            issueSamples = sampleIssues(input.issues),
        )
        return json.encodeToString(Payload.serializer(), payload)
    }

    /**
     * At most [MAX_ISSUE_SAMPLES] issues, taken in turns from each category (most frequent category
     * first), so a single frequent category cannot crowd out the others. Texts are shortened.
     */
    fun sampleIssues(issues: List<ProofreadResult.Issue>): List<ProofreadResult.Issue> {
        val byCategory = issues.groupBy { it.category }.values.sortedByDescending { it.size }
        val samples = mutableListOf<ProofreadResult.Issue>()
        var round = 0
        while (samples.size < MAX_ISSUE_SAMPLES) {
            val picked = byCategory.mapNotNull { it.getOrNull(round) }
            if (picked.isEmpty()) break
            samples += picked.take(MAX_ISSUE_SAMPLES - samples.size)
            round++
        }
        return samples.map { issue ->
            issue.copy(
                original = issue.original.shorten(MAX_SPAN_LENGTH),
                suggestion = issue.suggestion.shorten(MAX_SPAN_LENGTH),
                explanation = issue.explanation.shorten(MAX_EXPLANATION_LENGTH),
            )
        }
    }

    const val WEEK_COMPLETED: String = "completed"
    const val WEEK_IN_PROGRESS: String = "in_progress"

    private fun String.shorten(maxLength: Int): String =
        if (length <= maxLength) this else take(maxLength - 1) + "…"

    @Serializable
    private class Payload(
        @SerialName("week_status")
        val weekStatus: String,
        @SerialName("stats")
        val stats: WeeklyStats,
        @SerialName("issue_samples")
        val issueSamples: List<ProofreadResult.Issue>,
    )
}
