package com.phicdy.redraft.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Totals of one week compared with the week before, computed from history without an LLM.
 *
 * Stored as JSON with each weekly report and sent to the LLM as input, so the serial names
 * must never be renamed. Counts of the week before are [CountDelta.previous]; properties
 * without a backing field (such as [CountDelta.delta]) are not serialized.
 */
@Serializable
data class WeeklyStats(
    /** Number of entries per mode; every mode is present. */
    @SerialName("entry_counts")
    val entryCounts: List<ModeCount>,
    /** Proofread issues per (category, severity), most frequent this week first. */
    @SerialName("issue_counts")
    val issueCounts: List<IssueCountDelta>,
    /** Categories with the most proofread issues this week (both severities), most frequent first. */
    @SerialName("top_categories")
    val topCategories: List<CategoryCountDelta>,
    /** Translation grammar points per category, most frequent this week first. */
    @SerialName("grammar_point_counts")
    val grammarPointCounts: List<CategoryCountDelta>,
    /** Expressions learned from translations this week, oldest first, without duplicates. */
    @SerialName("expressions")
    val expressions: List<WeeklyExpression>,
) {
    /** Entries of all modes this week. */
    val totalEntries: Int
        get() = entryCounts.sumOf { it.current }

    @Serializable
    data class ModeCount(
        @SerialName("mode")
        val mode: InputMode,
        @SerialName("current")
        override val current: Int,
        @SerialName("previous")
        override val previous: Int,
    ) : CountDelta

    @Serializable
    data class IssueCountDelta(
        @SerialName("category")
        val category: ErrorCategory,
        @SerialName("severity")
        val severity: Severity,
        @SerialName("current")
        override val current: Int,
        @SerialName("previous")
        override val previous: Int,
    ) : CountDelta

    @Serializable
    data class CategoryCountDelta(
        @SerialName("category")
        val category: ErrorCategory,
        @SerialName("current")
        override val current: Int,
        @SerialName("previous")
        override val previous: Int,
    ) : CountDelta

    @Serializable
    data class WeeklyExpression(
        @SerialName("expression")
        val expression: String,
        @SerialName("type")
        val type: TranslationResult.ExpressionType,
        @SerialName("meaning")
        val meaning: String,
    )
}

/** A count this week and the week before. */
interface CountDelta {
    val current: Int
    val previous: Int

    /** Positive when it went up compared with the week before. */
    val delta: Int
        get() = current - previous
}
