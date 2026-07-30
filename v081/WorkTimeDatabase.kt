package de.kai.arbeitszeitgeofence.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkTimeEntryEntity::class,
        ActiveStateEntity::class,
        SettingsEntity::class,
        DayOverrideEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class WorkTimeDatabase : RoomDatabase() {
    abstract fun workTimeDao(): WorkTimeDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE settings ADD COLUMN workMonday INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE settings ADD COLUMN workTuesday INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE settings ADD COLUMN workWednesday INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE settings ADD COLUMN workThursday INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE settings ADD COLUMN workFriday INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE settings ADD COLUMN workSaturday INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE settings ADD COLUMN workSunday INTEGER NOT NULL DEFAULT 0")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS day_overrides (" +
                        "localDate TEXT NOT NULL PRIMARY KEY, " +
                        "type TEXT NOT NULL, " +
                        "comment TEXT NOT NULL DEFAULT '')"
                )
            }
        }
    }
}
