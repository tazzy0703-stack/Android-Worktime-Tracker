package de.kai.arbeitszeitgeofence.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "active_state")
data class ActiveStateEntity(@PrimaryKey val id: Int = 1, val isTracking: Boolean, val activeStartEpochMillis: Long?, val isBreakRunning: Boolean, val breakStartEpochMillis: Long?, val accumulatedBreakMinutes: Int, val insideGeofence: Boolean, val closedLocalDate: String?)
