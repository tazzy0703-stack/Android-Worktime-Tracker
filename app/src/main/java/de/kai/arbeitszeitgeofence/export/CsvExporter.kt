package de.kai.arbeitszeitgeofence.export
import de.kai.arbeitszeitgeofence.data.WorkTimeEntryEntity
import de.kai.arbeitszeitgeofence.domain.TimeCalculator
import java.time.Instant
object CsvExporter { fun toCsv(entries: List<WorkTimeEntryEntity>, targetMinutesPerDay: Int): String { val header=listOf("Datum","StartEpochMillis","EndEpochMillis","BruttoMinuten","PauseMinuten","NettoMinuten","SollMinuten","SaldoMinuten","Quelle","Kommentar"); val rows=entries.map{ e -> val r=TimeCalculator.calculate(Instant.ofEpochMilli(e.startEpochMillis), Instant.ofEpochMilli(e.endEpochMillis), e.breakMinutes, targetMinutesPerDay); listOf(e.localDate,e.startEpochMillis.toString(),e.endEpochMillis.toString(),r.grossMinutes.toString(),r.breakMinutes.toString(),r.netMinutes.toString(),r.targetMinutes.toString(),r.balanceMinutes.toString(),e.source,e.comment)}; return (listOf(header)+rows).joinToString("
"){ row -> row.joinToString(","){ v -> quote(v) } } }; private fun quote(value: String)= """ + value.replace(""","""") + """ }
