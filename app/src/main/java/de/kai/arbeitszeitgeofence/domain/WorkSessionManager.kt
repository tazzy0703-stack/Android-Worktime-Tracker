package de.kai.arbeitszeitgeofence.domain

import de.kai.arbeitszeitgeofence.data.ActiveStateEntity
import de.kai.arbeitszeitgeofence.data.WorkTimeEntryEntity
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object WorkSessionManager {
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

        if (!state.isTracking || activeStart == null) {
            return state.copy(
                isTracking = false,
                isBreakRunning = false,
                breakStartEpochMillis = null,
                insideGeofence = false
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
            insideGeofence = false
        )

        return newState to entry
    }
}
