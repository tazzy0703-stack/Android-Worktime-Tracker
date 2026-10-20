package de.kai.arbeitszeitgeofence.domain

import de.kai.arbeitszeitgeofence.data.ActiveStateEntity
import de.kai.arbeitszeitgeofence.data.WorkTimeEntryEntity
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object WorkSessionManager {
    /** Kehrt man innerhalb dieser Zeit nach einem Geofence-EXIT zurück, wird der letzte Block fortgesetzt. */
    const val RESUME_WINDOW_MINUTES = 5L

    fun initialState(): ActiveStateEntity {
        return ActiveStateEntity(
            isTracking = false,
            activeStartEpochMillis = null,
            isBreakRunning = false,
            breakStartEpochMillis = null,
            accumulatedBreakMinutes = 0,
            insideGeofence = false,
            closedLocalDate = null
        )
    }

    fun startWork(
        state: ActiveStateEntity,
        now: Instant,
        insideGeofence: Boolean
    ): ActiveStateEntity {
        if (state.isTracking) {
            return state.copy(insideGeofence = insideGeofence)
        }

        return state.copy(
            isTracking = true,
            activeStartEpochMillis = now.toEpochMilli(),
            isBreakRunning = false,
            breakStartEpochMillis = null,
            accumulatedBreakMinutes = 0,
            insideGeofence = insideGeofence
        )
    }

    /**
     * Ein Eintrag darf fortgesetzt werden, wenn er durch ein Geofence-EXIT entstanden ist und
     * dieses EXIT höchstens [windowMinutes] zurückliegt (fängt GPS-Jitter und kurze Gänge ab).
     */
    fun canResume(
        lastEntry: WorkTimeEntryEntity?,
        now: Instant,
        windowMinutes: Long = RESUME_WINDOW_MINUTES
    ): Boolean {
        if (lastEntry == null || lastEntry.source != TrackingSource.Geofence.name) return false
        val exitedAt = Instant.ofEpochMilli(lastEntry.endEpochMillis)
        if (now.isBefore(exitedAt)) return false
        return Duration.between(exitedAt, now) <= Duration.ofMinutes(windowMinutes)
    }

    /** Setzt den Zustand so, als wäre der Block des Eintrags nie beendet worden (Start und Pause bleiben erhalten). */
    fun resumeFromEntry(
        state: ActiveStateEntity,
        entry: WorkTimeEntryEntity
    ): ActiveStateEntity {
        return state.copy(
            isTracking = true,
            activeStartEpochMillis = entry.startEpochMillis,
            isBreakRunning = false,
            breakStartEpochMillis = null,
            accumulatedBreakMinutes = entry.breakMinutes,
            insideGeofence = true
        )
    }

    fun startBreak(
        state: ActiveStateEntity,
        now: Instant
    ): ActiveStateEntity {
        if (!state.isTracking || state.isBreakRunning) {
            return state
        }

        return state.copy(
            isBreakRunning = true,
            breakStartEpochMillis = now.toEpochMilli()
        )
    }

    fun stopBreak(
        state: ActiveStateEntity,
        now: Instant
    ): ActiveStateEntity {
        val breakStart = state.breakStartEpochMillis

        if (!state.isBreakRunning || breakStart == null) {
            return state
        }

        val additionalBreakMinutes = ChronoUnit.MINUTES.between(
            Instant.ofEpochMilli(breakStart),
            now
        ).coerceAtLeast(0).toInt()

        return state.copy(
            isBreakRunning = false,
            breakStartEpochMillis = null,
            accumulatedBreakMinutes = state.accumulatedBreakMinutes + additionalBreakMinutes
        )
    }

    fun stopWork(
        state: ActiveStateEntity,
        now: Instant,
        localDate: LocalDate,
        source: TrackingSource,
        comment: String
    ): Pair<ActiveStateEntity, WorkTimeEntryEntity?> {
        val activeStart = state.activeStartEpochMillis
        // Nur ein Geofence-EXIT bedeutet "nicht mehr am Arbeitsplatz"; Stop/Tagesabschluss ändern die Anwesenheit nicht.
        val insideAfterStop = if (source == TrackingSource.Geofence) false else state.insideGeofence

        if (!state.isTracking || activeStart == null) {
            return state.copy(
                isTracking = false,
                isBreakRunning = false,
                breakStartEpochMillis = null,
                insideGeofence = insideAfterStop
            ) to null
        }

        val finalState = if (state.isBreakRunning) {
            stopBreak(state, now)
        } else {
            state
        }

        val effectiveComment = if (finalState.accumulatedBreakMinutes == 0) {
            listOf(
                comment,
                "Warnung: keine Pause erfasst"
            ).filter { it.isNotBlank() }
                .joinToString(" | ")
        } else {
            comment
        }

        val entry = WorkTimeEntryEntity(
            localDate = localDate.toString(),
            startEpochMillis = activeStart,
            endEpochMillis = now.toEpochMilli(),
            breakMinutes = finalState.accumulatedBreakMinutes,
            source = source.name,
            comment = effectiveComment,
            createdAtEpochMillis = now.toEpochMilli(),
            modifiedAtEpochMillis = now.toEpochMilli()
        )

        val newState = finalState.copy(
            isTracking = false,
            activeStartEpochMillis = null,
            isBreakRunning = false,
            breakStartEpochMillis = null,
            accumulatedBreakMinutes = 0,
            insideGeofence = insideAfterStop
        )

        return newState to entry
    }
}
