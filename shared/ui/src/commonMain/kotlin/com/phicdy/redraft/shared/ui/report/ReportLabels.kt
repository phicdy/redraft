package com.phicdy.redraft.shared.ui.report

import com.phicdy.redraft.shared.data.report.ReportWeek
import com.phicdy.redraft.shared.model.WeeklyStats
import com.phicdy.redraft.shared.ui.main.displayName
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/** `11/16〜11/22`. */
internal fun ReportWeek.rangeLabel(): String = "${start.shortLabel()}〜${lastDay.shortLabel()}"

/** `2026/11/16〜11/22`, with the year for lists that span years. */
internal fun ReportWeek.titleLabel(): String = "${start.year}/${rangeLabel()}"

internal fun ReportTarget.displayName(): String = when (this) {
    ReportTarget.LastWeek -> "先週"
    ReportTarget.ThisWeek -> "今週（途中）"
}

/** A change from the week before: `+3`, `−2` or `±0`. */
internal fun deltaLabel(delta: Int): String = when {
    delta > 0 -> "+$delta"
    delta < 0 -> "−${-delta}"
    else -> "±0"
}

/** Counts for a report list row, such as `英訳3・校正5・指摘7`. */
internal fun WeeklyStats.countsLabel(): String {
    val modes = entryCounts.map { "${it.mode.displayName()}${it.current}" }
    return (modes + "指摘${issueCounts.sumOf { it.current }}").joinToString("・")
}

private fun LocalDate.shortLabel(): String = "${month.number}/$day"
