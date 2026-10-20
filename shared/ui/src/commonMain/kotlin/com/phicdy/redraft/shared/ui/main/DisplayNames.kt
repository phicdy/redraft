package com.phicdy.redraft.shared.ui.main

import com.phicdy.redraft.shared.llm.prompt.Politeness
import com.phicdy.redraft.shared.llm.prompt.WritingPurpose
import com.phicdy.redraft.shared.model.ErrorCategory
import com.phicdy.redraft.shared.model.ErrorCategoryGroup
import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.model.Severity
import com.phicdy.redraft.shared.model.TranslationResult

// Japanese names shown in the UI. Every `when` is exhaustive so that a new enum entry
// fails to compile until it gets a name.

internal fun ErrorCategory.displayName(): String = when (this) {
    ErrorCategory.Article -> "冠詞"
    ErrorCategory.Number -> "単数・複数"
    ErrorCategory.Tense -> "時制"
    ErrorCategory.Agreement -> "主語と動詞の一致"
    ErrorCategory.Preposition -> "前置詞"
    ErrorCategory.VerbForm -> "動詞の形"
    ErrorCategory.WordOrder -> "語順"
    ErrorCategory.SentenceStructure -> "文の構造"
    ErrorCategory.Pronoun -> "代名詞・指示語"
    ErrorCategory.WordChoice -> "単語の選択"
    ErrorCategory.Collocation -> "コロケーション"
    ErrorCategory.LiteralTranslation -> "直訳・和製英語"
    ErrorCategory.Register -> "トーン・丁寧さ"
    ErrorCategory.Conciseness -> "簡潔さ"
    ErrorCategory.Clarity -> "明確さ"
    ErrorCategory.Naturalness -> "自然さ"
    ErrorCategory.SpellingPunctuation -> "スペル・句読点"
}

internal fun ErrorCategoryGroup.displayName(): String = when (this) {
    ErrorCategoryGroup.Grammar -> "文法"
    ErrorCategoryGroup.Vocabulary -> "語彙"
    ErrorCategoryGroup.Style -> "スタイル"
    ErrorCategoryGroup.Mechanics -> "表記"
}

internal fun Severity.displayName(): String = when (this) {
    Severity.Error -> "誤り"
    Severity.Improvement -> "改善"
}

internal fun TranslationResult.ExpressionType.displayName(): String = when (this) {
    TranslationResult.ExpressionType.Word -> "単語"
    TranslationResult.ExpressionType.Phrase -> "フレーズ"
    TranslationResult.ExpressionType.Idiom -> "イディオム"
}

internal fun WritingPurpose.displayName(): String = when (this) {
    WritingPurpose.Email -> "メール"
    WritingPurpose.Chat -> "チャット"
    WritingPurpose.Document -> "ドキュメント"
}

internal fun Politeness.displayName(): String = when (this) {
    Politeness.Casual -> "カジュアル"
    Politeness.Neutral -> "普通"
    Politeness.Formal -> "丁寧"
}

internal fun InputMode.displayName(): String = when (this) {
    InputMode.Translate -> "英訳"
    InputMode.Proofread -> "校正"
}

/** Label of the primary button. */
internal fun InputMode?.runLabel(): String = when (this) {
    InputMode.Translate -> "英訳する"
    InputMode.Proofread -> "校正する"
    null -> "英訳・校正する"
}

/** Shown next to the progress indicator. */
internal fun InputMode.runningLabel(): String = when (this) {
    InputMode.Translate -> "英訳しています…"
    InputMode.Proofread -> "校正しています…"
}
