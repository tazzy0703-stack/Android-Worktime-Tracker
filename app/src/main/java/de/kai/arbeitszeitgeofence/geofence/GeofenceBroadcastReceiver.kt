package de.kai.arbeitszeitgeofence.geofence

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import de.kai.arbeitszeitgeofence.service.TrackingForegroundService

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            if (event.errorCode == GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE) {
                GeofenceRestoreReceiver.requestRestore(context)
            }
            Log.e(TAG, "Geofence transition error: ${event.errorCode}")
            return
        }

        val preferences = context.getSharedPreferences(GEOFENCE_PREFERENCES, Context.MODE_PRIVATE)
        val isDesiredEnabled = preferences.getBoolean(KEY_ENABLED, preferences.getBoolean(KEY_REGISTERED, false))
        if (!isDesiredEnabled) return

        if (!hasRequiredPermissions(context)) {
            markRegistrationInactive(context)
            Log.w(TAG, "Background location permission is no longer granted")
            return
        }

        val serviceAction = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> TrackingForegroundService.ACTION_GEOFENCE_ENTER
            Geofence.GEOFENCE_TRANSITION_EXIT -> TrackingForegroundService.ACTION_GEOFENCE_EXIT
            else -> return
        }

        val serviceIntent = Intent(context, TrackingForegroundService::class.java)
            .setAction(serviceAction)
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (exception: SecurityException) {
            markRegistrationInactive(context)
            Log.e(TAG, "Unable to start tracking service for geofence transition", exception)
        } catch (exception: ForegroundServiceStartNotAllowedException) {
            Log.e(TAG, "Android disallowed the foreground-service start for geofence transition", exception)
        }
    }

    private fun hasRequiredPermissions(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun markRegistrationInactive(context: Context) {
        context.getSharedPreferences(GEOFENCE_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_REGISTERED, false)
            .putLong(KEY_REGISTERED_AT, 0L)
            .apply()
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
        private const val GEOFENCE_PREFERENCES = "geofence_state"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_REGISTERED = "registered"
        private const val KEY_REGISTERED_AT = "registeredAt"
    }
}
