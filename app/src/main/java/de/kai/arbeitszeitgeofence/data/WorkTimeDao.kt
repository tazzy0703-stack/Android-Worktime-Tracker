package de.kai.arbeitszeitgeofence.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkTimeDao {
    @Query("SELECT * FROM work_time_entries ORDER BY startEpochMillis DESC")
    fun observeEntries(): Flow<List<WorkTimeEntryEntity>>

    @Insert
    suspend fun insertEntry(entry: WorkTimeEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: WorkTimeEntryEntity)

    @Query("DELETE FROM work_time_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("DELETE FROM work_time_entries")
    suspend fun deleteAllEntries()

    @Query("SELECT * FROM active_state WHERE id = 1")
    suspend fun getActiveState(): ActiveStateEntity?

    @Query("SELECT * FROM active_state WHERE id = 1")
    fun observeActiveState(): Flow<ActiveStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActiveState(state: ActiveStateEntity)

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettings(): SettingsEntity?

    @Query("SELECT * FROM settings WHERE id = 1")
    fun observeSettings(): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: SettingsEntity)
}
