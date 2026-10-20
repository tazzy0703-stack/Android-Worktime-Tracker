package de.kai.arbeitszeitgeofence.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

data class DayTarget(
    val isScheduledWorkday: Boolean,
    val targetMinutes: Int,
    val creditedMinutes: Int
)

object WorkScheduleCalculator {
    private val paidAbsenceTypes = setOf("urlaub", "feiertag", "krank", "krankheit")

    fun parseHoursToMinutes(input: String): Int? {
        val normalized = input.trim().replace(',', '.')
        val hours = normalized.toBigDecimalOrNull() ?: return null
        if (hours < BigDecimal.ZERO || hours > BigDecimal.valueOf(24)) return null

        return runCatching {
            hours.multiply(BigDecimal.valueOf(60))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact()
        }.getOrNull()
    }

    fun formatMinutesAsHours(minutes: Int): String {
        require(minutes >= 0) { "Sollzeit darf nicht negativ sein." }
        return BigDecimal.valueOf(minutes.toLong())
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
            .replace('.', ',')
    }

    fun sanitizeHoursInput(input: String): String {
        val result = StringBuilder()
        var separatorSeen = false
        input.forEach { character ->
            when {
                character in '0'..'9' -> result.append(character)
                (character == ',' || character == '.') && !separatorSeen -> {
                    result.append(',')
                    separatorSeen = true
                }
            }
        }
        return result.toString()
    }

    fun targetForDate(
        dailyTargetMinutes: Int,
        scheduledDays: Set<DayOfWeek>,
        date: LocalDate,
        overrideType: String?
    ): DayTarget {
        require(dailyTargetMinutes >= 0) { "Sollzeit darf nicht negativ sein." }
        val isScheduledWorkday = date.dayOfWeek in scheduledDays
        if (!isScheduledWorkday || dailyTargetMinutes == 0) {
            return DayTarget(isScheduledWorkday, targetMinutes = 0, creditedMinutes = 0)
        }

        val normalizedType = overrideType?.trim()?.lowercase(Locale.ROOT)
        return when {
            normalizedType == null -> DayTarget(isScheduledWorkday, dailyTargetMinutes, 0)
            normalizedType in paidAbsenceTypes -> DayTarget(
                isScheduledWorkday = true,
                targetMinutes = dailyTargetMinutes,
                creditedMinutes = dailyTargetMinutes
            )
            else -> DayTarget(isScheduledWorkday, targetMinutes = 0, creditedMinutes = 0)
        }
    }
}
