package me.lucky.wasted.trigger.geofence

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.Priority

import me.lucky.wasted.Preferences
import me.lucky.wasted.R
import me.lucky.wasted.Trigger
import me.lucky.wasted.fire.Fire
import me.lucky.wasted.trigger.shared.NotificationManager

class GeofenceService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var prefs: Preferences
    private lateinit var controller: DwellController

    private val prefState = object : DwellState {
        override var sinceMs: Long
            get() = prefs.triggerGeofenceSince
            set(value) { prefs.triggerGeofenceSince = value }
    }

    private val callback = object : LocationCallback() {
        override fun onLocationAvailability(availability: LocationAvailability) {
            if (!availability.isLocationAvailable) showNotification(unavailable = true)
        }

        override fun onLocationResult(result: LocationResult) {
            showNotification(unavailable = false)
            val loc = result.lastLocation ?: return
            when (controller.onFix(
                loc.latitude,
                loc.longitude,
                loc.accuracy,
                loc.time,
                DwellMachine.DWELL_MS.toInt() // minutes
            )) {
                DwellEffect.Fired -> stopSelf()
                else -> {}
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Preferences.new(this)
        controller = DwellController(
            DwellMachine(StationStore(this).get()),
            prefState,
            fire = { Fire(this).fire(Trigger.GEOFENCE) },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForegroundCompat()
        val client = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            DwellMachine.FIX_INTERVAL_MS,
        ).build()
        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (exc: SecurityException) {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            LocationServices.getFusedLocationProviderClient(this)
                .removeLocationUpdates(callback)
        } catch (exc: SecurityException) {}
        super.onDestroy()
    }

    private fun startForegroundCompat(unavailable: Boolean = false) {
        val notification = buildNotification(unavailable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun showNotification(unavailable: Boolean) {
        // startForeground again updates the existing FGS notification
        try {
            startForegroundCompat(unavailable)
        } catch (exc: SecurityException) {}
    }

    private fun buildNotification(unavailable: Boolean = false) =
        NotificationCompat.Builder(this, NotificationManager.CHANNEL_DEFAULT_ID)
            .setContentTitle(getString(R.string.foreground_service_notification_title))
            .setContentText(
                if (unavailable) getString(R.string.geofence_no_location) else null
            )
            .setSmallIcon(android.R.drawable.ic_delete)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
}