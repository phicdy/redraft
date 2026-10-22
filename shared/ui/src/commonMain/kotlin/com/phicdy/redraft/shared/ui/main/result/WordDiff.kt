package com.phicdy.redraft.shared.ui.main.result

internal enum class DiffKind {
    Unchanged,
    Removed,
    Added,
}

internal data class DiffSegment(
    val kind: DiffKind,
    val text: String,
)

/** Inputs beyond this many word pairs are not diffed, to keep the table small. */
private const val MAX_DIFF_CELLS: Int = 250_000

private val wordWithTrailingSpace = Regex("""\S+\s*""")

/**
 * Word-level diff from [original] to [corrected], based on the longest common subsequence
 * of words. Each word keeps its trailing whitespace so line breaks survive; words are compared
 * without it. Unchanged words use the corrected text's spacing.
 *
 * Returns null when the texts are too long to diff cheaply.
 */
internal fun wordDiff(original: String, corrected: String): List<DiffSegment>? {
    val before = wordWithTrailingSpace.findAll(original).map { it.value }.toList()
    val after = wordWithTrailingSpace.findAll(corrected).map { it.value }.toList()
    val rows = before.size + 1
    val columns = after.size + 1
    if (rows.toLong() * columns > MAX_DIFF_CELLS) return null

    // lcs[i * columns + j] = length of the LCS of before[i..] and after[j..].
    val lcs = IntArray(rows * columns)
    for (i in before.indices.reversed()) {
        for (j in after.indices.reversed()) {
            lcs[i * columns + j] = if (sameWord(before[i], after[j])) {
                lcs[(i + 1) * columns + j + 1] + 1
            } else {
                maxOf(lcs[(i + 1) * columns + j], lcs[i * columns + j + 1])
            }
        }
    }

    val segments = mutableListOf<DiffSegment>()
    fun append(kind: DiffKind, text: String) {
        val last = segments.lastOrNull()
        if (last != null && last.kind == kind) {
            segments[segments.lastIndex] = last.copy(text = last.text + text)
        } else {
            segments += DiffSegment(kind, text)
        }
    }

    var i = 0
    var j = 0
    while (i < before.size && j < after.size) {
        when {
            sameWord(before[i], after[j]) -> {
                append(DiffKind.Unchanged, after[j])
                i++
                j++
            }

            lcs[(i + 1) * columns + j] >= lcs[i * columns + j + 1] -> {
                append(DiffKind.Removed, before[i])
                i++
            }

            else -> {
                append(DiffKind.Added, after[j])
                j++
            }
        }
    }
    while (i < before.size) append(DiffKind.Removed, before[i++])
    while (j < after.size) append(DiffKind.Added, after[j++])
    return segments
}

private fun sameWord(a: String, b: String): Boolean = a.trimEnd() == b.trimEnd()
