package de.kai.arbeitszeitgeofence
import android.app.Application
import de.kai.arbeitszeitgeofence.data.AppDatabase
class ArbeitszeitApp : Application() { val database: AppDatabase by lazy { AppDatabase.create(this) } }
