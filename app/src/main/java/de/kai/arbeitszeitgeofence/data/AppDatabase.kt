package de.kai.arbeitszeitgeofence.data
import android.content.Context
import androidx.room.*
@Database(entities = [WorkTimeEntryEntity::class, ActiveStateEntity::class, SettingsEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() { abstract fun workTimeDao(): WorkTimeDao; companion object { fun create(context: Context): AppDatabase = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "arbeitszeit-geofence.db").build() } }
