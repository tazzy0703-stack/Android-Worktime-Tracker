package de.kai.arbeitszeitgeofence.geofence
import android.content.*
import com.google.android.gms.location.*
import de.kai.arbeitszeitgeofence.ArbeitszeitApp
import de.kai.arbeitszeitgeofence.domain.*
import kotlinx.coroutines.*
import java.time.*
class GeofenceBroadcastReceiver: BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent) { val event=GeofencingEvent.fromIntent(intent) ?: return; if (event.hasError()) return; val app=context.applicationContext as ArbeitszeitApp; val pending=goAsync(); CoroutineScope(Dispatchers.IO).launch { try { val dao=app.database.workTimeDao(); val now=Instant.now(); val today=LocalDate.now(); val state=dao.getActiveState() ?: WorkSessionManager.initialState(); when(event.geofenceTransition){ Geofence.GEOFENCE_TRANSITION_ENTER -> dao.upsertActiveState(WorkSessionManager.startWork(state,now,true)); Geofence.GEOFENCE_TRANSITION_EXIT -> { val (newState,entry)=WorkSessionManager.stopWork(state,now,today,TrackingSource.Geofence,"Arbeitsbereich verlassen - Arbeitszeit angehalten"); if(entry!=null) dao.insertEntry(entry); dao.upsertActiveState(newState) } } } finally { pending.finish() } } } }
