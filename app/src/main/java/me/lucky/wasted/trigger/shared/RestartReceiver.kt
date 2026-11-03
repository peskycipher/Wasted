package me.lucky.wasted.trigger.shared

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

import me.lucky.wasted.Preferences
import me.lucky.wasted.Trigger
import me.lucky.wasted.trigger.geofence.GeofenceService

class RestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val prefs = Preferences.new(context ?: return)
        val triggers = prefs.triggers
        if (!prefs.isEnabled || (
                triggers.and(Trigger.LOCK.value) == 0 &&
                triggers.and(Trigger.USB.value) == 0 &&
                triggers.and(Trigger.AIRPLANE_MODE.value) == 0 &&
                triggers.and(Trigger.GEOFENCE.value) == 0)) return
        if (triggers.and(Trigger.LOCK.value) != 0 ||
            triggers.and(Trigger.USB.value) != 0 ||
            triggers.and(Trigger.AIRPLANE_MODE.value) != 0) {
            ContextCompat.startForegroundService(
                context.applicationContext,
                Intent(context.applicationContext, ForegroundService::class.java),
            )
        }
        if (triggers.and(Trigger.GEOFENCE.value) != 0) {
            ContextCompat.startForegroundService(
                context.applicationContext,
                Intent(context.applicationContext, GeofenceService::class.java),
            )
        }
    }
}