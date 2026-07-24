package de.kai.arbeitszeitgeofence.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "work_time_entries")
data class WorkTimeEntryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val localDate: String, val startEpochMillis: Long, val endEpochMillis: Long, val breakMinutes: Int, val source: String, val comment: String, val createdAtEpochMillis: Long, val modifiedAtEpochMillis: Long)
