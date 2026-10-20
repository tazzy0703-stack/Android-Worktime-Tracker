package de.kai.arbeitszeitgeofence.geofence

import android.content.Context

/**
 * Zentraler Zugriff auf die Geofence-Flags in den SharedPreferences ("geofence_state").
 *
 * Die Keys "enabled", "registered" und "registeredAt" werden zusätzlich direkt von
 * MainActivity und GeofenceRestoreReceiver verwendet und müssen stabil bleiben.
 */
object GeofenceStateStore {
    const val PREFERENCES = "geofence_state"
    const val KEY_ENABLED = "enabled"
    const val KEY_REGISTERED = "registered"
    const val KEY_REGISTERED_AT = "registeredAt"

    /**
     * true, solange die Zeiterfassung am Arbeitsplatz bewusst beendet wurde (manueller Stop
     * oder Tagesabschluss). Verhindert, dass der Anwesenheitsabgleich beim App-Start die
     * Zeit sofort wieder startet. Wird bei EXIT und bei einem echten ENTER zurückgesetzt.
     */
    private const val KEY_SUPPRESS_AUTO_START = "suppressAutoStart"

    fun isRestoreWanted(context: Context): Boolean {
        val preferences = preferences(context)
        return preferences.getBoolean(KEY_ENABLED, preferences.getBoolean(KEY_REGISTERED, false))
    }

    fun markRegistered(context: Context, registered: Boolean) {
        preferences(context).edit()
            .putBoolean(KEY_REGISTERED, registered)
            .putLong(KEY_REGISTERED_AT, if (registered) System.currentTimeMillis() else 0L)
            .apply()
    }

    fun isSuppressAutoStart(context: Context): Boolean =
        preferences(context).getBoolean(KEY_SUPPRESS_AUTO_START, false)

    fun setSuppressAutoStart(context: Context, suppress: Boolean) {
        preferences(context).edit().putBoolean(KEY_SUPPRESS_AUTO_START, suppress).apply()
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
