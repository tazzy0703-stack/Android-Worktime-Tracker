package de.kai.arbeitszeitgeofence

import android.app.Application
import androidx.room.Room
import de.kai.arbeitszeitgeofence.data.WorkTimeDatabase

class ArbeitszeitApp : Application() {
    val database: WorkTimeDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            WorkTimeDatabase::class.java,
            "arbeitszeit-geofence.db"
        )
            .addMigrations(WorkTimeDatabase.MIGRATION_1_2)
            .build()
    }
}
