package com.phicdy.redraft.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InputModeTest {
    @Test
    fun japaneseTextIsTranslate() {
        assertEquals(InputMode.Translate, detectInputMode("会議を金曜日に変更したいです"))
    }

    @Test
    fun japaneseTextWithEnglishWordsIsTranslate() {
        assertEquals(InputMode.Translate, detectInputMode("このPRをreviewしてください"))
    }

    @Test
    fun englishTextIsProofread() {
        assertEquals(InputMode.Proofread, detectInputMode("I look forward to see you."))
    }

    @Test
    fun textWithoutLettersIsUndecided() {
        assertNull(detectInputMode("123 !?"))
        assertNull(detectInputMode(""))
    }
}
