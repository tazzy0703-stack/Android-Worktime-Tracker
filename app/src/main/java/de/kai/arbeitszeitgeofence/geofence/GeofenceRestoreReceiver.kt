package de.kai.arbeitszeitgeofence.geofence

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import de.kai.arbeitszeitgeofence.ArbeitszeitApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class GeofenceRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, ACTION_RESTORE)) return

        val appContext = context.applicationContext
        val preferences = appContext.getSharedPreferences(GEOFENCE_PREFERENCES, Context.MODE_PRIVATE)
        val shouldRestore = preferences.getBoolean(
            KEY_ENABLED,
            preferences.getBoolean(KEY_REGISTERED, false)
        )
        if (!shouldRestore) return

        val pendingResult = goAsync()
        val finished = AtomicBoolean(false)
        fun complete(registered: Boolean) {
            if (!finished.compareAndSet(false, true)) return
            preferences.edit()
                .putBoolean(KEY_REGISTERED, registered)
                .putLong(KEY_REGISTERED_AT, if (registered) System.currentTimeMillis() else 0L)
                .apply()
            pendingResult.finish()
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = appContext as? ArbeitszeitApp
                val settings = app?.database?.workTimeDao()?.getSettings()
                val latitude = settings?.geofenceLatitude
                val longitude = settings?.geofenceLongitude
                if (latitude == null || longitude == null || !hasRequiredPermissions(appContext)) {
                    complete(false)
                    return@launch
                }

                GeofenceManager(appContext)
                    .registerWorkplaceGeofence(latitude, longitude, settings.geofenceRadiusMeters)
                    .addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.e(TAG, "Geofence restore failed", task.exception)
                        }
                        complete(task.isSuccessful)
                    }
            } catch (exception: Exception) {
                Log.e(TAG, "Unable to restore geofence", exception)
                complete(false)
            }
        }
    }

    private fun hasRequiredPermissions(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val ACTION_RESTORE = "de.kai.arbeitszeitgeofence.action.RESTORE_GEOFENCE"
        private const val TAG = "GeofenceRestore"
        private const val GEOFENCE_PREFERENCES = "geofence_state"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_REGISTERED = "registered"
        private const val KEY_REGISTERED_AT = "registeredAt"

        fun requestRestore(context: Context) {
            context.sendBroadcast(
                Intent(context, GeofenceRestoreReceiver::class.java).setAction(ACTION_RESTORE)
            )
        }
    }
}
