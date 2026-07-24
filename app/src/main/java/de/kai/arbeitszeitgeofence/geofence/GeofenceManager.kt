package de.kai.arbeitszeitgeofence.geofence
import android.Manifest
import android.app.PendingIntent
import android.content.*
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.*
class GeofenceManager(private val context: Context) { private val client: GeofencingClient = LocationServices.getGeofencingClient(context); @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION) fun registerWorkplaceGeofence(latitude: Double, longitude: Double, radiusMeters: Float) { val geofence=Geofence.Builder().setRequestId(WORKPLACE_REQUEST_ID).setCircularRegion(latitude,longitude,radiusMeters).setExpirationDuration(Geofence.NEVER_EXPIRE).setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT).setNotificationResponsiveness(60000).build(); val request=GeofencingRequest.Builder().setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER).addGeofence(geofence).build(); client.addGeofences(request,pendingIntent()) }; fun unregisterWorkplaceGeofence(){ client.removeGeofences(listOf(WORKPLACE_REQUEST_ID)) }; private fun pendingIntent(): PendingIntent { val intent=Intent(context, GeofenceBroadcastReceiver::class.java); return PendingIntent.getBroadcast(context,1001,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE) }; companion object { const val WORKPLACE_REQUEST_ID="workplace-geofence" } }
