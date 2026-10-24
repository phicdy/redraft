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
import com.phicdy.redraft.shared.model.TranslationResult
import com.phicdy.redraft.shared.ui.main.result.TranslationResultView
import com.phicdy.redraft.shared.ui.theme.RedraftTheme

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

/** The translation result alone, so the lower sections are visible in screenshots. */
@Preview
@Composable
internal fun TranslationResultViewPreview() {
    ResultViewPreview {
        TranslationResultView(result = sampleTranslation, usage = sampleUsage)
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
