package de.kai.arbeitszeitgeofence.domain
import java.time.Duration
import java.time.Instant
object TimeCalculator { fun calculate(start: Instant, end: Instant, breakMinutes: Int, targetMinutes: Int, warnIfMissingPause: Boolean = true): TimeBlockResult { require(!end.isBefore(start)) { "Endzeit darf nicht vor Startzeit liegen." }; require(breakMinutes >= 0) { "Pausenzeit darf nicht negativ sein." }; val gross = Duration.between(start,end).toMinutes().toInt(); val boundedBreak = breakMinutes.coerceAtMost(gross); val net = gross - boundedBreak; val warning = if (warnIfMissingPause && boundedBreak == 0 && gross > 0) "Warnung: keine Pause erfasst" else null; return TimeBlockResult(gross,boundedBreak,net,targetMinutes,net-targetMinutes,warning) } }
