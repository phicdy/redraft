package com.phicdy.redraft.shared.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phicdy.redraft.shared.llm.TokenUsage
import com.phicdy.redraft.shared.model.ErrorCategory
import com.phicdy.redraft.shared.model.ProofreadResult
import com.phicdy.redraft.shared.model.Severity
import com.phicdy.redraft.shared.model.TranslationResult
import com.phicdy.redraft.shared.ui.main.result.ProofreadResultView
import com.phicdy.redraft.shared.ui.main.result.TranslationResultView
import com.phicdy.redraft.shared.ui.theme.RedraftTheme

private const val SAMPLE_ENGLISH =
    "I look forward to see you in the meeting tomorrow. " +
        "Please let me know if you have any question about informations."

private val sampleUsage = TokenUsage(inputTokens = 1_234, outputTokens = 567)

private val sampleTranslation = TranslationResult(
    translation = "Could we move next week's regular meeting to Friday afternoon? " +
        "I'll share the materials by the day before.",
    alternatives = listOf(
        TranslationResult.Alternative(
            tone = "カジュアル",
            text = "Can we push next week's meeting to Friday afternoon? I'll send the slides the day before.",
        ),
        TranslationResult.Alternative(
            tone = "丁寧",
            text = "Would it be possible to reschedule next week's regular meeting to Friday afternoon? " +
                "I will share the materials by the previous day.",
        ),
    ),
    grammarPoints = listOf(
        TranslationResult.GrammarPoint(
            category = ErrorCategory.Preposition,
            title = "by と until の違い",
            explanation = "by は「〜までに（期限）」、until は「〜までずっと（継続）」を表します。" +
                "資料の共有は期限なので by を使います。",
            example = "Please submit the report by Friday.",
        ),
        TranslationResult.GrammarPoint(
            category = ErrorCategory.Article,
            title = "the day before",
            explanation = "会議の「前日」と特定の日を指すので the を付けます。",
            example = "We arrived the day before the conference.",
        ),
    ),
    expressions = listOf(
        TranslationResult.Expression(
            expression = "move A to B",
            type = TranslationResult.ExpressionType.Phrase,
            meaning = "（予定）A を B に移す・変更する",
            example = "Can we move the call to 3 p.m.?",
        ),
        TranslationResult.Expression(
            expression = "reschedule",
            type = TranslationResult.ExpressionType.Word,
            meaning = "予定を変更する",
            example = "I need to reschedule our 1:1.",
        ),
    ),
)

private val sampleProofread = ProofreadResult(
    correctedText = "I look forward to seeing you at the meeting tomorrow. " +
        "Please let me know if you have any questions about the information.",
    issues = listOf(
        ProofreadResult.Issue(
            original = "to see",
            suggestion = "to seeing",
            category = ErrorCategory.VerbForm,
            severity = Severity.Error,
            explanation = "look forward to の to は前置詞なので、後ろは動名詞（-ing）になります。",
        ),
        ProofreadResult.Issue(
            original = "in the meeting",
            suggestion = "at the meeting",
            category = ErrorCategory.Preposition,
            severity = Severity.Improvement,
            explanation = "会議という「場・イベント」で会う場合は at の方が自然です。",
        ),
        ProofreadResult.Issue(
            original = "any question",
            suggestion = "any questions",
            category = ErrorCategory.Number,
            severity = Severity.Error,
            explanation = "疑問文・条件文の any の後ろの可算名詞は通常複数形にします。",
        ),
        ProofreadResult.Issue(
            original = "informations",
            suggestion = "the information",
            category = ErrorCategory.Number,
            severity = Severity.Error,
            explanation = "information は不可算名詞なので複数形にしません。",
        ),
    ),
    naturalAlternatives = listOf(
        ProofreadResult.NaturalAlternative(
            text = "See you at tomorrow's meeting! Feel free to reach out if you have any questions.",
            nuance = "よりカジュアルで、社内チャット向きの言い方です。",
        ),
    ),
    overallComment = "意図は明確に伝わっています。look forward to の後は動名詞になる点と、" +
        "information が不可算名詞である点に注意しましょう。",
)

/** The translation result alone, so the lower sections are visible in screenshots. */
@Preview
@Composable
internal fun TranslationResultViewPreview() {
    ResultViewPreview {
        TranslationResultView(result = sampleTranslation, usage = sampleUsage)
    }
}

/** The proofread result alone, so the highlighted original and issues are visible in screenshots. */
@Preview
@Composable
internal fun ProofreadResultViewPreview() {
    ResultViewPreview {
        ProofreadResultView(input = SAMPLE_ENGLISH, result = sampleProofread, usage = sampleUsage)
    }
}

@Composable
private fun ResultViewPreview(content: @Composable () -> Unit) {
    RedraftTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                content()
            }
        }
    }
}
