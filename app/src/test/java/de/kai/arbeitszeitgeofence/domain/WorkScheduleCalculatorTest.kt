package de.kai.arbeitszeitgeofence.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WorkScheduleCalculatorTest {
    private val mondayToFriday = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )

    @Test
    fun parsesWholeAndFractionalHoursIntoMinutes() {
        assertEquals(480, WorkScheduleCalculator.parseHoursToMinutes("8"))
        assertEquals(450, WorkScheduleCalculator.parseHoursToMinutes("7,5"))
        assertEquals(450, WorkScheduleCalculator.parseHoursToMinutes("7.5"))
        assertEquals(1, WorkScheduleCalculator.parseHoursToMinutes("0,02"))
    }

    @Test
    fun rejectsInvalidOrOutOfRangeHours() {
        assertNull(WorkScheduleCalculator.parseHoursToMinutes(""))
        assertNull(WorkScheduleCalculator.parseHoursToMinutes("abc"))
        assertNull(WorkScheduleCalculator.parseHoursToMinutes("-1"))
        assertNull(WorkScheduleCalculator.parseHoursToMinutes("24,1"))
    }

    @Test
    fun formatsStoredMinutesAsHoursForSettings() {
        assertEquals("8", WorkScheduleCalculator.formatMinutesAsHours(480))
        assertEquals("7,5", WorkScheduleCalculator.formatMinutesAsHours(450))
        assertEquals("8,02", WorkScheduleCalculator.formatMinutesAsHours(481))
    }

    @Test
    fun paidAbsenceKeepsTargetAndCreditsScheduledHours() {
        val date = LocalDate.parse("2026-10-19") // Monday
        listOf("Urlaub", "Feiertag", "Krank", "Krankheit").forEach { type ->
            val target = WorkScheduleCalculator.targetForDate(480, mondayToFriday, date, type)
            assertEquals(DayTarget(true, targetMinutes = 480, creditedMinutes = 480), target)
        }
    }

    @Test
    fun freeDayRemovesTargetWithoutCredit() {
        val target = WorkScheduleCalculator.targetForDate(
            480,
            mondayToFriday,
            LocalDate.parse("2026-10-19"),
            "Frei"
        )
        assertEquals(DayTarget(true, targetMinutes = 0, creditedMinutes = 0), target)
    }

    @Test
    fun unbookedScheduledWeekdayStillHasTargetAndWeekendDoesNot() {
        val monday = WorkScheduleCalculator.targetForDate(
            480,
            mondayToFriday,
            LocalDate.parse("2026-10-19"),
            null
        )
        val saturday = WorkScheduleCalculator.targetForDate(
            480,
            mondayToFriday,
            LocalDate.parse("2026-10-24"),
            null
        )
        assertEquals(DayTarget(true, targetMinutes = 480, creditedMinutes = 0), monday)
        assertEquals(DayTarget(false, targetMinutes = 0, creditedMinutes = 0), saturday)
    }

    @Test
    fun mondayToFridayWeekHasFortyHoursEvenWithoutEntries() {
        val monday = LocalDate.parse("2026-10-19")
        val weeklyTarget = (0L..6L)
            .map { monday.plusDays(it) }
            .sumOf { date ->
                WorkScheduleCalculator.targetForDate(480, mondayToFriday, date, null).targetMinutes
            }

        assertEquals(2_400, weeklyTarget)
    }
}
