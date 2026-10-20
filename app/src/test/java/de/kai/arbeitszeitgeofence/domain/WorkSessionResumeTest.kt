package de.kai.arbeitszeitgeofence.domain

import de.kai.arbeitszeitgeofence.data.WorkTimeEntryEntity
import org.junit.Assert
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class WorkSessionResumeTest {
    private val start = Instant.parse("2026-10-20T06:00:00Z")
    private val exitTime = Instant.parse("2026-10-20T10:00:00Z")

    private fun entry(source: TrackingSource = TrackingSource.Geofence, breakMinutes: Int = 15) = WorkTimeEntryEntity(
        id = 7,
        localDate = "2026-10-20",
        startEpochMillis = start.toEpochMilli(),
        endEpochMillis = exitTime.toEpochMilli(),
        breakMinutes = breakMinutes,
        source = source.name,
        comment = "Arbeitsbereich verlassen",
        createdAtEpochMillis = exitTime.toEpochMilli(),
        modifiedAtEpochMillis = exitTime.toEpochMilli()
    )

    @Test
    fun resumesWhenReturningWithinWindow() {
        Assert.assertTrue(WorkSessionManager.canResume(entry(), exitTime.plusSeconds(3 * 60)))
    }

    @Test
    fun resumesExactlyAtWindowBoundary() {
        Assert.assertTrue(WorkSessionManager.canResume(entry(), exitTime.plusSeconds(5 * 60)))
    }

    @Test
    fun doesNotResumeAfterWindow() {
        Assert.assertFalse(WorkSessionManager.canResume(entry(), exitTime.plusSeconds(5 * 60 + 1)))
    }

    @Test
    fun doesNotResumeManualOrDayCloseEntries() {
        Assert.assertFalse(WorkSessionManager.canResume(entry(TrackingSource.Manual), exitTime.plusSeconds(60)))
        Assert.assertFalse(WorkSessionManager.canResume(entry(TrackingSource.DayClose), exitTime.plusSeconds(60)))
    }

    @Test
    fun doesNotResumeWithoutEntryOrBeforeExit() {
        Assert.assertFalse(WorkSessionManager.canResume(null, exitTime))
        Assert.assertFalse(WorkSessionManager.canResume(entry(), exitTime.minusSeconds(1)))
    }

    @Test
    fun resumeKeepsOriginalStartAndBreakMinutes() {
        val resumed = WorkSessionManager.resumeFromEntry(WorkSessionManager.initialState(), entry(breakMinutes = 15))

        Assert.assertTrue(resumed.isTracking)
        Assert.assertTrue(resumed.insideGeofence)
        Assert.assertFalse(resumed.isBreakRunning)
        Assert.assertEquals(start.toEpochMilli(), resumed.activeStartEpochMillis)
        Assert.assertEquals(15, resumed.accumulatedBreakMinutes)
    }

    @Test
    fun manualStopKeepsInsideGeofenceFlag() {
        val started = WorkSessionManager.startWork(WorkSessionManager.initialState(), start, insideGeofence = true)
        val (state, entry) = WorkSessionManager.stopWork(
            started, exitTime, LocalDate.parse("2026-10-20"), TrackingSource.Manual, "Manuell"
        )

        Assert.assertNotNull(entry)
        Assert.assertFalse(state.isTracking)
        Assert.assertTrue(state.insideGeofence)
    }

    @Test
    fun geofenceStopClearsInsideGeofenceFlag() {
        val started = WorkSessionManager.startWork(WorkSessionManager.initialState(), start, insideGeofence = true)
        val (state, _) = WorkSessionManager.stopWork(
            started, exitTime, LocalDate.parse("2026-10-20"), TrackingSource.Geofence, "Exit"
        )

        Assert.assertFalse(state.insideGeofence)
    }
}
