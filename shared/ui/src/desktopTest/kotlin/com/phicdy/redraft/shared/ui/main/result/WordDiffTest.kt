package com.phicdy.redraft.shared.ui.main.result

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WordDiffTest {
    @Test
    fun replacedWordIsRemovedThenAdded() {
        assertEquals(
            listOf(
                DiffSegment(DiffKind.Unchanged, "I look forward to "),
                DiffSegment(DiffKind.Removed, "see "),
                DiffSegment(DiffKind.Added, "seeing "),
                DiffSegment(DiffKind.Unchanged, "you."),
            ),
            wordDiff("I look forward to see you.", "I look forward to seeing you."),
        )
    }

    @Test
    fun insertedWord() {
        assertEquals(
            listOf(
                DiffSegment(DiffKind.Unchanged, "about "),
                DiffSegment(DiffKind.Added, "the "),
                DiffSegment(DiffKind.Unchanged, "information."),
            ),
            wordDiff("about information.", "about the information."),
        )
    }

    @Test
    fun identicalTextIsUnchanged() {
        assertEquals(
            listOf(DiffSegment(DiffKind.Unchanged, "Thanks.\nSee you.")),
            wordDiff("Thanks. See you.", "Thanks.\nSee you."),
        )
    }

    @Test
    fun hugeTextIsNotDiffed() {
        val long = "word ".repeat(600)
        assertNull(wordDiff(long, long))
    }
}
