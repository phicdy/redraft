package com.phicdy.redraft.shared.llm.prompt

import com.phicdy.redraft.shared.llm.schema.OutputSchemas
import com.phicdy.redraft.shared.model.ErrorCategory
import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.model.ProofreadResult
import com.phicdy.redraft.shared.model.Severity
import com.phicdy.redraft.shared.model.TranslationResult
import com.phicdy.redraft.shared.model.WeeklyStats
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeeklyReportPromptTest {
    private val stats = WeeklyStats(
        entryCounts = listOf(
            WeeklyStats.ModeCount(InputMode.Translate, current = 3, previous = 1),
            WeeklyStats.ModeCount(InputMode.Proofread, current = 5, previous = 6),
        ),
        issueCounts = listOf(WeeklyStats.IssueCountDelta(ErrorCategory.Article, Severity.Error, current = 4, previous = 2)),
        topCategories = listOf(WeeklyStats.CategoryCountDelta(ErrorCategory.Article, current = 4, previous = 2)),
        grammarPointCounts = emptyList(),
        expressions = listOf(
            WeeklyStats.WeeklyExpression("touch base", TranslationResult.ExpressionType.Idiom, "連絡を取り合う"),
        ),
    )

    private fun issue(category: ErrorCategory, text: String = "a"): ProofreadResult.Issue = ProofreadResult.Issue(
        original = text,
        suggestion = "$text!",
        category = category,
        severity = Severity.Error,
        explanation = "説明",
    )

    private fun encode(input: WeeklyReportInput): JsonObject =
        Json.parseToJsonElement(Prompts.weeklyReport(input).input).jsonObject

    @Test
    fun requestUsesWeeklyReportSchema() {
        val request = Prompts.weeklyReport(WeeklyReportInput(stats, emptyList(), isInProgress = false))

        assertEquals(OutputSchemas.weeklyReport, request.schema)
    }

    @Test
    fun inputCarriesStatsAndWeekStatus() {
        val completed = encode(WeeklyReportInput(stats, listOf(issue(ErrorCategory.Article)), isInProgress = false))
        val inProgress = encode(WeeklyReportInput(stats, emptyList(), isInProgress = true))

        assertEquals("completed", completed.getValue("week_status").jsonPrimitive.content)
        assertEquals("in_progress", inProgress.getValue("week_status").jsonPrimitive.content)
        val entryCounts = completed.getValue("stats").jsonObject.getValue("entry_counts").jsonArray
        assertEquals("translate", entryCounts[0].jsonObject.getValue("mode").jsonPrimitive.content)
        assertEquals(3, entryCounts[0].jsonObject.getValue("current").jsonPrimitive.int)
        assertEquals(1, entryCounts[0].jsonObject.getValue("previous").jsonPrimitive.int)
        assertEquals(1, completed.getValue("issue_samples").jsonArray.size)
    }

    @Test
    fun samplesAreCappedAndTakenFromEveryCategory() {
        val issues = List(50) { issue(ErrorCategory.Article, "article $it") } +
            List(3) { issue(ErrorCategory.Tense, "tense $it") } +
            issue(ErrorCategory.Preposition, "preposition")

        val samples = WeeklyReportPromptInput.sampleIssues(issues)

        assertEquals(WeeklyReportPromptInput.MAX_ISSUE_SAMPLES, samples.size)
        assertEquals(
            listOf("article 0", "tense 0", "preposition", "article 1", "tense 1", "article 2", "tense 2", "article 3"),
            samples.take(8).map { it.original },
        )
        assertEquals(3, samples.count { it.category == ErrorCategory.Tense })
    }

    @Test
    fun longTextsAreShortened() {
        val long = "x".repeat(1_000)
        val samples = WeeklyReportPromptInput.sampleIssues(
            listOf(issue(ErrorCategory.Clarity).copy(original = long, suggestion = long, explanation = long)),
        )

        val sample = samples.single()
        assertEquals(WeeklyReportPromptInput.MAX_SPAN_LENGTH, sample.original.length)
        assertEquals(WeeklyReportPromptInput.MAX_SPAN_LENGTH, sample.suggestion.length)
        assertEquals(WeeklyReportPromptInput.MAX_EXPLANATION_LENGTH, sample.explanation.length)
        assertTrue(sample.original.endsWith("…"))
    }

    @Test
    fun expressionsAreCapped() {
        val many = stats.copy(
            expressions = List(100) {
                WeeklyStats.WeeklyExpression("expression $it", TranslationResult.ExpressionType.Phrase, "意味")
            },
        )

        val encoded = encode(WeeklyReportInput(many, emptyList(), isInProgress = false))

        val expressions = encoded.getValue("stats").jsonObject.getValue("expressions").jsonArray
        assertEquals(WeeklyReportPromptInput.MAX_EXPRESSIONS, expressions.size)
    }

    @Test
    fun instructionsStateLimitsLanguageAndCategories() {
        val instructions = Prompts.weeklyReportInstructions()

        assertTrue("at most ${Prompts.MAX_RECURRING_MISTAKES}" in instructions)
        assertTrue("at most ${Prompts.MAX_EXPRESSIONS_TO_REVIEW}" in instructions)
        assertTrue("in Japanese" in instructions)
        assertTrue("in_progress" in instructions)
        assertTrue("Never invent numbers" in instructions)
        assertTrue("- literal_translation:" in instructions)
    }
}
