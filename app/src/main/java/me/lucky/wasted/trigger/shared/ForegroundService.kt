package me.lucky.wasted.trigger.shared

import android.app.KeyguardManager
import android.app.Service
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

import me.lucky.wasted.Preferences
import me.lucky.wasted.R
import me.lucky.wasted.Trigger
import me.lucky.wasted.fire.Fire
import me.lucky.wasted.trigger.airplane.AirplaneReceiver
import me.lucky.wasted.trigger.lock.LockJobManager

class ForegroundService : Service() {
    companion object {
        // desired receiver registration, carried as extras by the reconciler;
        // sticky restarts (null intent) re-derive from prefs
        const val EXTRA_REGISTER_LOCK = "register_lock"
        const val EXTRA_REGISTER_AIRPLANE = "register_airplane"
        private const val NOTIFICATION_ID = 1000
    }

    private lateinit var prefs: Preferences
    private var lockReceiver: LockReceiver? = null
    private val airplaneReceiver = AirplaneReceiver()
    private var registeredLock = false
    private var registeredAirplane = false

    override fun onCreate() {
        super.onCreate()
        updateReceivers(null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // manifest UsbReceiver owns USB events outright (USB_STATE cannot be
        // manifest-delivered, while DEVICE/ACCESSORY_ATTACHED can and fire
        // regardless of this service — a second receiver here only double-fires)
        val register = receiverSet(intent)
        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, NotificationManager.CHANNEL_DEFAULT_ID)
                .setContentTitle(getString(R.string.foreground_service_notification_title))
                .setSmallIcon(android.R.drawable.ic_delete)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        )
        updateReceivers(intent)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? { return null }

    private fun updateReceivers(intent: Intent?) {
        val (registerLock, registerAirplane) = receiverSet(intent)
        if (registerLock && !registeredLock) {
            lockReceiver = LockReceiver(
                getSystemService(KeyguardManager::class.java).isDeviceLocked,
            )
            registerReceiver(lockReceiver, IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
            })
            registeredLock = true
        }
        if (registerAirplane && !registeredAirplane) {
            registerReceiver(
                airplaneReceiver,
                IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED),
            )
            registeredAirplane = true
            // catch airplane mode turned on while the service was dead
            if (AirplaneReceiver.isOn(this)) Fire(this).fire(Trigger.AIRPLANE_MODE)
        }
    }

    private fun receiverSet(intent: Intent?): Pair<Boolean, Boolean> =
        if (intent != null) {
            intent.getBooleanExtra(EXTRA_REGISTER_LOCK, false) to
                intent.getBooleanExtra(EXTRA_REGISTER_AIRPLANE, false)
        } else {
            val triggers = Preferences.new(this).triggers
            (triggers.and(Trigger.LOCK.value) != 0) to
                (triggers.and(Trigger.AIRPLANE_MODE.value) != 0)
        }

    private fun deinit() {
        val unregister: (BroadcastReceiver) -> Unit = {
            try { unregisterReceiver(it) } catch (exc: IllegalArgumentException) {}
        }
        if (registeredLock) lockReceiver?.let { unregister(it) }
        if (registeredAirplane) unregister(airplaneReceiver)
    }

    private class LockReceiver(private var locked: Boolean) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (Preferences.new(context ?: return).triggers.and(Trigger.LOCK.value) == 0)
                return
            when (intent?.action) {
                Intent.ACTION_USER_PRESENT -> {
                    locked = false
                    LockJobManager(context).cancel()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    if (locked) return
                    locked = true
                    Thread(Runner(context, goAsync())).start()
                }
            }
        }

        private class Runner(
            private val ctx: Context,
            private val pendingResult: PendingResult,
        ) : Runnable {
            companion object { private const val MAX_ATTEMPTS = 5 }
            override fun run() {
                val job = LockJobManager(ctx)
                var delay = 1000L
                for (attempt in 1..MAX_ATTEMPTS) {
                    if (job.schedule() == JobScheduler.RESULT_SUCCESS) {
                        pendingResult.finish()
                        return
                    }
                    Log.w("ForegroundService", "lock job schedule refused (attempt $attempt)")
                    try { Thread.sleep(delay); delay = delay.shl(1) } catch (_: InterruptedException) { return }
                }
                // loud give-up: a refused schedule means no wipe-on-lock
                Log.w("ForegroundService", "lock job scheduling gave up after $MAX_ATTEMPTS attempts")
                pendingResult.finish()
            }
        }
    }
}