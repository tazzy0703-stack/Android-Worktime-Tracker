package de.kai.arbeitszeitgeofence.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val geofenceLatitude: Double? = null,
    val geofenceLongitude: Double? = null,
    val geofenceRadiusMeters: Float = 120f,
    val targetMinutesPerDay: Int = 480,
    val defaultBreakMinutes: Int = 45,
    val autoDayCloseTime: String = "23:59",
    val workMonday: Boolean = true,
    val workTuesday: Boolean = true,
    val workWednesday: Boolean = true,
    val workThursday: Boolean = true,
    val workFriday: Boolean = true,
    val workSaturday: Boolean = false,
    val workSunday: Boolean = false
)
