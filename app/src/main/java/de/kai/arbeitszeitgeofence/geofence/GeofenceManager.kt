package de.kai.arbeitszeitgeofence.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task

class GeofenceManager(context: Context) {
    private val applicationContext = context.applicationContext
    private val client: GeofencingClient = LocationServices.getGeofencingClient(applicationContext)

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun registerWorkplaceGeofence(
        latitude: Double,
        longitude: Double,
        radiusMeters: Float
    ): Task<Void> {
        val geofence = Geofence.Builder()
            .setRequestId(WORKPLACE_REQUEST_ID)
            .setCircularRegion(latitude, longitude, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .setNotificationResponsiveness(60_000)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        return client.addGeofences(request, pendingIntent())
    }

    fun unregisterWorkplaceGeofence(): Task<Void> =
        client.removeGeofences(listOf(WORKPLACE_REQUEST_ID))

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(applicationContext, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            applicationContext,
            PENDING_INTENT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    companion object {
        const val WORKPLACE_REQUEST_ID = "workplace-geofence"
        private const val PENDING_INTENT_REQUEST_CODE = 1001
    }
}
