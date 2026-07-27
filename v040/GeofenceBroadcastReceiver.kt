package de.kai.arbeitszeitgeofence.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import de.kai.arbeitszeitgeofence.service.TrackingForegroundService

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val serviceAction = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> TrackingForegroundService.ACTION_GEOFENCE_ENTER
            Geofence.GEOFENCE_TRANSITION_EXIT -> TrackingForegroundService.ACTION_GEOFENCE_EXIT
            else -> return
        }

        val serviceIntent = Intent(context, TrackingForegroundService::class.java)
            .setAction(serviceAction)

        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
