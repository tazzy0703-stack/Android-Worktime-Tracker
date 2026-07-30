package de.kai.arbeitszeitgeofence.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_overrides")
data class DayOverrideEntity(
    @PrimaryKey val localDate: String,
    val type: String,
    val comment: String = ""
)
