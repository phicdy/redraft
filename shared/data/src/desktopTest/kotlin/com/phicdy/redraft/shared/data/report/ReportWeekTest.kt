package com.phicdy.redraft.shared.data.report

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class ReportWeekTest {
    private val tokyo = TimeZone.of("Asia/Tokyo")
    private val newYork = TimeZone.of("America/New_York")

    @Test
    fun weekStartsOnMondayInTheTimeZone() {
        // Sunday 2026-11-22 23:30 in Tokyo is still the week of Monday 2026-11-16.
        assertEquals(LocalDate(2026, 11, 16), ReportWeek.containing(Instant.parse("2026-11-22T14:30:00Z"), tokyo).start)
        // Monday 00:00 in Tokyo starts the next week.
        assertEquals(LocalDate(2026, 11, 23), ReportWeek.containing(Instant.parse("2026-11-22T15:00:00Z"), tokyo).start)
        // The same instant is still Sunday in New York.
        assertEquals(LocalDate(2026, 11, 16), ReportWeek.containing(Instant.parse("2026-11-22T15:00:00Z"), newYork).start)
    }

    @Test
    fun rangeIsMondayMidnightToNextMondayMidnight() {
        val week = ReportWeek(LocalDate(2026, 11, 16))

        assertEquals(Instant.parse("2026-11-15T15:00:00Z"), week.startInstant(tokyo))
        assertEquals(Instant.parse("2026-11-22T15:00:00Z"), week.endInstant(tokyo))
        assertEquals(LocalDate(2026, 11, 22), week.lastDay)
        assertEquals(LocalDate(2026, 11, 23), week.endExclusive)
    }

    @Test
    fun weekAcrossDaylightSavingTimeEndIsSevenCalendarDays() {
        // New York leaves daylight saving time on 2026-11-01, so this week is 169 hours long.
        val week = ReportWeek(LocalDate(2026, 10, 26))

        assertEquals(Instant.parse("2026-10-26T04:00:00Z"), week.startInstant(newYork))
        assertEquals(Instant.parse("2026-11-02T05:00:00Z"), week.endInstant(newYork))
    }

    @Test
    fun lastCompletedIsTheWeekBeforeTheCurrentOne() {
        val wednesday = Instant.parse("2026-11-25T03:00:00Z")

        assertEquals(LocalDate(2026, 11, 23), ReportWeek.containing(wednesday, tokyo).start)
        assertEquals(LocalDate(2026, 11, 16), ReportWeek.lastCompleted(wednesday, tokyo).start)
        assertEquals(LocalDate(2026, 11, 9), ReportWeek.lastCompleted(wednesday, tokyo).previous().start)
    }

    @Test
    fun inProgressUntilTheWeekEnds() {
        val week = ReportWeek(LocalDate(2026, 11, 16))

        assertTrue(week.isInProgressAt(Instant.parse("2026-11-22T14:59:59Z"), tokyo))
        assertFalse(week.isInProgressAt(Instant.parse("2026-11-22T15:00:00Z"), tokyo))
    }

    @Test
    fun startMustBeAMonday() {
        assertFailsWith<IllegalArgumentException> { ReportWeek(LocalDate(2026, 11, 17)) }
    }
}
