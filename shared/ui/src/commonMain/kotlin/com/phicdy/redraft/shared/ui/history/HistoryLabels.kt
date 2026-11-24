package com.phicdy.redraft.shared.ui.history

import com.phicdy.redraft.shared.data.history.HistoryEntrySummary
import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.model.Severity
import com.phicdy.redraft.shared.ui.main.displayName
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** `2026/11/24 09:05` in [timeZone]. The zone is passed in so previews and tests do not depend on the machine. */
internal fun formatDateTime(instant: Instant, timeZone: TimeZone): String {
    val local = instant.toLocalDateTime(timeZone)
    return "${local.year}/${local.month.number.twoDigits()}/${local.day.twoDigits()} " +
        "${local.hour.twoDigits()}:${local.minute.twoDigits()}"
}

/** Finding counts for a list row, such as `誤り2・改善1`. */
internal fun HistoryEntrySummary.countsLabel(): String {
    val parts = when (mode) {
        InputMode.Proofread -> listOf(
            Severity.Error.displayName() to errorCount,
            Severity.Improvement.displayName() to improvementCount,
        )

        InputMode.Translate -> listOf("文法" to grammarPointCount, "表現" to expressionCount)
    }.filter { (_, count) -> count > 0 }
    if (parts.isEmpty()) {
        return when (mode) {
            InputMode.Proofread -> "指摘なし"
            InputMode.Translate -> "ポイントなし"
        }
    }
    return parts.joinToString("・") { (label, count) -> "$label$count" }
}

/** The first non-blank line, for one-line list rows. */
internal fun firstLine(text: String): String = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()

private fun Int.twoDigits(): String = toString().padStart(2, '0')
