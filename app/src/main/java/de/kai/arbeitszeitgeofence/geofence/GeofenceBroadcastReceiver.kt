package de.kai.arbeitszeitgeofence.geofence

import android.app.ForegroundServiceStartNotAllowedException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
            Log.e(TAG, "Unable to start tracking service for geofence transition", exception)
        } catch (exception: ForegroundServiceStartNotAllowedException) {
            Log.e(TAG, "Android disallowed the foreground-service start for geofence transition", exception)
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
