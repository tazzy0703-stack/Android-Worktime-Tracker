package de.kai.arbeitszeitgeofence.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import de.kai.arbeitszeitgeofence.ArbeitszeitApp
import de.kai.arbeitszeitgeofence.domain.WorkSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val app = context.applicationContext as ArbeitszeitApp
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = app.database.workTimeDao()
                val state = dao.getActiveState() ?: WorkSessionManager.initialState()

                val newState = when (event.geofenceTransition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> state.copy(insideGeofence = true)
                    Geofence.GEOFENCE_TRANSITION_EXIT -> state.copy(insideGeofence = false)
                    else -> state
                }

                dao.upsertActiveState(newState)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
