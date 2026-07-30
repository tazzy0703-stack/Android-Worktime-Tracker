package de.kai.arbeitszeitgeofence

import android.app.Application
import androidx.room.Room
import de.kai.arbeitszeitgeofence.data.AppDatabase

class ArbeitszeitApp : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "arbeitszeit-geofence.db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }
}
