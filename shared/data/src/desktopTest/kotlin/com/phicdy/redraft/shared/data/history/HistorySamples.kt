package com.phicdy.redraft.shared.data.history

import com.phicdy.redraft.shared.model.ErrorCategory
import com.phicdy.redraft.shared.model.ProofreadResult
import com.phicdy.redraft.shared.model.Severity
import com.phicdy.redraft.shared.model.TranslationResult

internal object HistorySamples {
    val context: HistoryContext = HistoryContext(purpose = HistoryPurpose.Email, politeness = HistoryPoliteness.Formal)
    val usage: HistoryUsage = HistoryUsage(inputTokens = 120, outputTokens = 340)

    val translation: HistoryResult.Translation = HistoryResult.Translation(
        TranslationResult(
            translation = "Could we move the meeting to Friday?",
            alternatives = listOf(TranslationResult.Alternative(tone = "丁寧", text = "Would it be possible to move the meeting?")),
            grammarPoints = listOf(
                TranslationResult.GrammarPoint(
                    category = ErrorCategory.Tense,
                    title = "未来の予定",
                    explanation = "確定した予定には現在進行形も使えます。",
                    example = "We are meeting on Friday.",
                ),
                TranslationResult.GrammarPoint(
                    category = ErrorCategory.Preposition,
                    title = "曜日の前置詞",
                    explanation = "曜日には on を使います。",
                    example = "on Friday",
                ),
            ),
            expressions = listOf(
                TranslationResult.Expression(
                    expression = "move the meeting",
                    type = TranslationResult.ExpressionType.Phrase,
                    meaning = "会議を移動する",
                    example = "Can we move the meeting to 3 p.m.?",
                ),
            ),
        ),
    )

    val proofread: HistoryResult.Proofread = HistoryResult.Proofread(
        ProofreadResult(
            correctedText = "I look forward to seeing you.",
            issues = listOf(
                ProofreadResult.Issue(
                    original = "to see",
                    suggestion = "to seeing",
                    category = ErrorCategory.VerbForm,
                    severity = Severity.Error,
                    explanation = "look forward to の後は動名詞です。",
                ),
                ProofreadResult.Issue(
                    original = "I look",
                    suggestion = "I'm looking",
                    category = ErrorCategory.Naturalness,
                    severity = Severity.Improvement,
                    explanation = "進行形の方が自然です。",
                ),
            ),
            naturalAlternatives = listOf(ProofreadResult.NaturalAlternative(text = "See you soon.", nuance = "カジュアル")),
            overallComment = "よく書けています。",
        ),
    )
}
