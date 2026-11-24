package com.phicdy.redraft.shared.ui.history

import com.phicdy.redraft.shared.data.history.HistoryContext
import com.phicdy.redraft.shared.data.history.HistoryPoliteness
import com.phicdy.redraft.shared.data.history.HistoryPurpose
import com.phicdy.redraft.shared.data.history.HistoryUsage
import com.phicdy.redraft.shared.llm.TokenUsage
import com.phicdy.redraft.shared.llm.prompt.Politeness
import com.phicdy.redraft.shared.llm.prompt.WritingContext
import com.phicdy.redraft.shared.llm.prompt.WritingPurpose

// The LLM and data layers must not depend on each other, so the UI layer maps between their types.
// Every `when` is exhaustive so that a new value fails to compile until it is mapped.

internal fun WritingContext.toHistoryContext(): HistoryContext = HistoryContext(
    purpose = when (purpose) {
        WritingPurpose.Email -> HistoryPurpose.Email
        WritingPurpose.Chat -> HistoryPurpose.Chat
        WritingPurpose.Document -> HistoryPurpose.Document
    },
    politeness = when (politeness) {
        Politeness.Casual -> HistoryPoliteness.Casual
        Politeness.Neutral -> HistoryPoliteness.Neutral
        Politeness.Formal -> HistoryPoliteness.Formal
    },
)

internal fun HistoryContext.toWritingContext(): WritingContext = WritingContext(
    purpose = when (purpose) {
        HistoryPurpose.Email -> WritingPurpose.Email
        HistoryPurpose.Chat -> WritingPurpose.Chat
        HistoryPurpose.Document -> WritingPurpose.Document
    },
    politeness = when (politeness) {
        HistoryPoliteness.Casual -> Politeness.Casual
        HistoryPoliteness.Neutral -> Politeness.Neutral
        HistoryPoliteness.Formal -> Politeness.Formal
    },
)

internal fun TokenUsage.toHistoryUsage(): HistoryUsage =
    HistoryUsage(inputTokens = inputTokens, outputTokens = outputTokens)

internal fun HistoryUsage.toTokenUsage(): TokenUsage =
    TokenUsage(inputTokens = inputTokens, outputTokens = outputTokens)
