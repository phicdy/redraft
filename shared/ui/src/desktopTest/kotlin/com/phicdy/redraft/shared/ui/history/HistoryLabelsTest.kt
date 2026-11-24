package com.phicdy.redraft.shared.ui.history

import com.phicdy.redraft.shared.data.history.HistoryEntrySummary
import com.phicdy.redraft.shared.model.InputMode
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class HistoryLabelsTest {
    @Test
    fun formatsInTheGivenTimeZone() {
        val instant = Instant.parse("2026-11-24T00:05:00Z")

        assertEquals("2026/11/24 09:05", formatDateTime(instant, TimeZone.of("Asia/Tokyo")))
        assertEquals("2026/11/23 16:05", formatDateTime(instant, TimeZone.of("America/Los_Angeles")))
    }

    @Test
    fun countsShowOnlyNonZeroKinds() {
        assertEquals("誤り2・改善1", summary(InputMode.Proofread, errors = 2, improvements = 1).countsLabel())
        assertEquals("改善3", summary(InputMode.Proofread, improvements = 3).countsLabel())
        assertEquals("指摘なし", summary(InputMode.Proofread).countsLabel())
        assertEquals("文法2・表現1", summary(InputMode.Translate, grammarPoints = 2, expressions = 1).countsLabel())
        assertEquals("ポイントなし", summary(InputMode.Translate).countsLabel())
    }

    @Test
    fun firstLineSkipsBlankLines() {
        assertEquals("Hello,", firstLine("\n  Hello,  \nworld"))
        assertEquals("", firstLine(" \n "))
    }

    private fun summary(
        mode: InputMode,
        errors: Int = 0,
        improvements: Int = 0,
        grammarPoints: Int = 0,
        expressions: Int = 0,
    ): HistoryEntrySummary = HistoryEntrySummary(
        id = "id",
        createdAt = Instant.fromEpochMilliseconds(0),
        mode = mode,
        input = "input",
        output = "output",
        errorCount = errors,
        improvementCount = improvements,
        grammarPointCount = grammarPoints,
        expressionCount = expressions,
    )
}
