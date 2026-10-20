package de.kai.arbeitszeitgeofence.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.kai.arbeitszeitgeofence.ArbeitszeitApp
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wiederholt die Geofence-Registrierung mit exponentiellem Backoff, wenn der Restore im
 * BroadcastReceiver fehlgeschlagen ist (z. B. weil nach GEOFENCE_NOT_AVAILABLE die
 * Standortdienste noch nicht wieder verfügbar sind).
 */
class GeofenceRestoreWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        if (!GeofenceStateStore.isRestoreWanted(applicationContext)) return Result.success()

        val settings = (applicationContext as ArbeitszeitApp).database.workTimeDao().getSettings()
        val latitude = settings?.geofenceLatitude
        val longitude = settings?.geofenceLongitude
        if (latitude == null || longitude == null || !hasRequiredPermissions()) {
            GeofenceStateStore.markRegistered(applicationContext, false)
            return Result.failure()
        }

        return try {
            suspendCancellableCoroutine<Unit> { continuation ->
                GeofenceManager(applicationContext)
                    .registerWorkplaceGeofence(latitude, longitude, settings.geofenceRadiusMeters)
                    .addOnSuccessListener { continuation.resume(Unit) }
                    .addOnFailureListener { exception -> continuation.resumeWithException(exception) }
            }
            GeofenceStateStore.markRegistered(applicationContext, true)
            Result.success()
        } catch (exception: Exception) {
            Log.w(TAG, "Geofence restore attempt $runAttemptCount failed", exception)
            if (runAttemptCount >= MAX_ATTEMPTS) {
                GeofenceStateStore.markRegistered(applicationContext, false)
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean =
        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val TAG = "GeofenceRestoreWorker"
        private const val UNIQUE_WORK_NAME = "geofence-restore-retry"
        private const val MAX_ATTEMPTS = 10

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<GeofenceRestoreWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
