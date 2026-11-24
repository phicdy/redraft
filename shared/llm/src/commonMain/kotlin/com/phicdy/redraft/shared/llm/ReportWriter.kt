package com.phicdy.redraft.shared.llm

import com.phicdy.redraft.shared.llm.prompt.Prompts
import com.phicdy.redraft.shared.llm.prompt.WeeklyReportInput
import com.phicdy.redraft.shared.model.WeeklyReportNarrative

/**
 * Writes the narrative part of the weekly report on any [LlmClient]. The numbers are computed
 * by the caller; the model only explains them. Failures are thrown as [LlmException].
 */
class ReportWriter(
    private val client: LlmClient,
) {
    suspend fun write(input: WeeklyReportInput): LlmResult<WeeklyReportNarrative> = client.generate(
        Prompts.weeklyReport(input),
        WeeklyReportNarrative.serializer(),
    )
}
