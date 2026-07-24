package de.kai.arbeitszeitgeofence.domain
enum class TrackingSource { Geofence, Manual, ManualCorrection, DayClose }
data class TimeBlockResult(val grossMinutes: Int, val breakMinutes: Int, val netMinutes: Int, val targetMinutes: Int, val balanceMinutes: Int, val warning: String?)
