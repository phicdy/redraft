package com.phicdy.redraft.shared.data.report

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.previousOrSame
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * The period a weekly report covers: from Monday 00:00 until the next Monday 00:00 in a time zone.
 * The week is identified by the date of its Monday, which does not depend on the time zone.
 */
data class ReportWeek(
    /** The Monday the week starts on. */
    val start: LocalDate,
) {
    init {
        require(start.dayOfWeek == DayOfWeek.MONDAY) { "A report week starts on a Monday, but $start is a ${start.dayOfWeek}" }
    }

    /** The Monday after the week, the first day not included. */
    val endExclusive: LocalDate
        get() = start.plus(DAYS_PER_WEEK, DateTimeUnit.DAY)

    /** The last day of the week (Sunday). */
    val lastDay: LocalDate
        get() = start.plus(DAYS_PER_WEEK - 1, DateTimeUnit.DAY)

    fun previous(): ReportWeek = ReportWeek(start.minus(DAYS_PER_WEEK, DateTimeUnit.DAY))

    /** Monday 00:00 in [timeZone], inclusive. */
    fun startInstant(timeZone: TimeZone): Instant = start.atStartOfDayIn(timeZone)

    /** The next Monday 00:00 in [timeZone], exclusive. */
    fun endInstant(timeZone: TimeZone): Instant = endExclusive.atStartOfDayIn(timeZone)

    /** True when the week has not ended at [instant], so its counts may still grow. */
    fun isInProgressAt(instant: Instant, timeZone: TimeZone): Boolean = instant < endInstant(timeZone)

    companion object {
        private const val DAYS_PER_WEEK: Int = 7

        /** The week that [instant] falls in. */
        fun containing(instant: Instant, timeZone: TimeZone): ReportWeek =
            ReportWeek(instant.toLocalDateTime(timeZone).date.previousOrSame(DayOfWeek.MONDAY))

        /** The most recent week that has ended at [now]: the week before the current one. */
        fun lastCompleted(now: Instant, timeZone: TimeZone): ReportWeek = containing(now, timeZone).previous()
    }
}
