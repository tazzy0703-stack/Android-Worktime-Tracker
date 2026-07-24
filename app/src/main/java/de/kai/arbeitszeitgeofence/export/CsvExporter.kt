package de.kai.arbeitszeitgeofence.export

import de.kai.arbeitszeitgeofence.data.WorkTimeEntryEntity
import de.kai.arbeitszeitgeofence.domain.TimeCalculator
import java.time.Instant

object CsvExporter {
    fun toCsv(
        entries: List<WorkTimeEntryEntity>,
        targetMinutesPerDay: Int
    ): String {
        val header = listOf(
            "Datum",
            "StartEpochMillis",
            "EndEpochMillis",
            "BruttoMinuten",
            "PauseMinuten",
            "NettoMinuten",
            "SollMinuten",
            "SaldoMinuten",
            "Quelle",
            "Kommentar"
        )

        val rows = entries.map { entry ->
            val result = TimeCalculator.calculate(
                start = Instant.ofEpochMilli(entry.startEpochMillis),
                end = Instant.ofEpochMilli(entry.endEpochMillis),
                breakMinutes = entry.breakMinutes,
                targetMinutes = targetMinutesPerDay
            )

            listOf(
                entry.localDate,
                entry.startEpochMillis.toString(),
                entry.endEpochMillis.toString(),
                result.grossMinutes.toString(),
                result.breakMinutes.toString(),
                result.netMinutes.toString(),
                result.targetMinutes.toString(),
                result.balanceMinutes.toString(),
                entry.source,
                entry.comment
            )
        }

        return (listOf(header) + rows).joinToString("\n") { row ->
            row.joinToString(",") { value ->
                quote(value)
            }
        }
    }

    private fun quote(value: String): String {
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}
