package de.kai.arbeitszeitgeofence.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "settings")
data class SettingsEntity(@PrimaryKey val id: Int = 1, val targetMinutesPerDay: Int = 480, val defaultBreakMinutes: Int = 45, val missingPausePolicy: String = "WARN_ONLY", val allowOvernightShift: Boolean = false, val autoDayCloseTime: String = "23:59", val geofenceLatitude: Double? = null, val geofenceLongitude: Double? = null, val geofenceRadiusMeters: Float = 120f)
