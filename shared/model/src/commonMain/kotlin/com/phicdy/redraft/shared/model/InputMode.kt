package com.phicdy.redraft.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The serial names are stored in weekly reports, so never rename them. */
@Serializable
enum class InputMode {
    /** Japanese input: translate into English. */
    @SerialName("translate")
    Translate,

    /** English input: proofread. */
    @SerialName("proofread")
    Proofread,
}

/**
 * Decides the mode locally before calling the LLM.
 *
 * Any hiragana, katakana or kanji makes the input Japanese, because a Japanese
 * sentence often contains English words while the reverse is rare.
 * Returns null when the text has no letters to judge from.
 */
fun detectInputMode(text: String): InputMode? {
    var hasLatinLetter = false
    for (char in text) {
        if (char.isJapanese()) return InputMode.Translate
        if (char in 'a'..'z' || char in 'A'..'Z') hasLatinLetter = true
    }
    return if (hasLatinLetter) InputMode.Proofread else null
}

private fun Char.isJapanese(): Boolean = this in '぀'..'ゟ' || // Hiragana
    this in '゠'..'ヿ' || // Katakana
    this in '一'..'鿿' || // CJK unified ideographs
    this in '㐀'..'䶿' || // CJK extension A
    this in 'ｦ'..'ﾟ' // Half-width katakana
