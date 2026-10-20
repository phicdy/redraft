package com.phicdy.redraft.shared.ui.main.result

import com.phicdy.redraft.shared.model.ProofreadResult

/** Where [ProofreadResult.Issue.original] of `issues[issueIndex]` was found in the input. */
internal data class IssueHighlight(
    val start: Int,
    val end: Int,
    val issueIndex: Int,
)

/**
 * Locates each issue's `original` in [text] by string search, because offsets reported by the
 * model are unreliable. When the same substring appears several times, each issue takes the
 * first occurrence not already taken by an earlier issue. Exact matches are tried before
 * case-insensitive ones. Issues that cannot be found get no highlight but are still listed.
 *
 * The result is sorted by [IssueHighlight.start] and never overlaps.
 */
internal fun locateIssues(text: String, issues: List<ProofreadResult.Issue>): List<IssueHighlight> {
    val highlights = mutableListOf<IssueHighlight>()
    issues.forEachIndexed { index, issue ->
        val needle = issue.original
        if (needle.isBlank()) return@forEachIndexed
        val found = findFree(text, needle, highlights, ignoreCase = false)
            ?: findFree(text, needle, highlights, ignoreCase = true)
        if (found != null) {
            highlights += IssueHighlight(start = found, end = found + needle.length, issueIndex = index)
        }
    }
    return highlights.sortedBy { it.start }
}

private fun findFree(
    text: String,
    needle: String,
    taken: List<IssueHighlight>,
    ignoreCase: Boolean,
): Int? {
    var from = 0
    while (from <= text.length - needle.length) {
        val start = text.indexOf(needle, startIndex = from, ignoreCase = ignoreCase)
        if (start < 0) return null
        val end = start + needle.length
        if (taken.none { start < it.end && it.start < end }) return start
        from = start + 1
    }
    return null
}
