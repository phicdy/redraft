package com.phicdy.redraft.shared.ui.main.result

import com.phicdy.redraft.shared.model.ErrorCategory
import com.phicdy.redraft.shared.model.ProofreadResult
import com.phicdy.redraft.shared.model.Severity
import kotlin.test.Test
import kotlin.test.assertEquals

class IssueHighlightsTest {
    private fun issue(original: String): ProofreadResult.Issue = ProofreadResult.Issue(
        original = original,
        suggestion = "",
        category = ErrorCategory.WordChoice,
        severity = Severity.Error,
        explanation = "",
    )

    @Test
    fun locatesIssuesAndSortsByPosition() {
        val text = "I look forward to see you in the meeting."
        val highlights = locateIssues(text, listOf(issue("in the meeting"), issue("to see")))
        assertEquals(
            listOf(
                IssueHighlight(start = 15, end = 21, issueIndex = 1),
                IssueHighlight(start = 26, end = 40, issueIndex = 0),
            ),
            highlights,
        )
    }

    @Test
    fun repeatedSubstringTakesNextFreeOccurrence() {
        val text = "a apple and a orange"
        val highlights = locateIssues(text, listOf(issue("a "), issue("a ")))
        assertEquals(listOf(0, 12), highlights.map { it.start })
    }

    @Test
    fun missingOrBlankIssuesAreSkipped() {
        val highlights = locateIssues("Hello world.", listOf(issue("goodbye"), issue(" "), issue("world")))
        assertEquals(listOf(IssueHighlight(start = 6, end = 11, issueIndex = 2)), highlights)
    }

    @Test
    fun fallsBackToCaseInsensitiveMatch() {
        val highlights = locateIssues("Informations are here.", listOf(issue("informations")))
        assertEquals(listOf(IssueHighlight(start = 0, end = 12, issueIndex = 0)), highlights)
    }
}
