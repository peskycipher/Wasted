package me.lucky.wasted.reconciler

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

import me.lucky.wasted.ApplicationOption
import me.lucky.wasted.Preferences
import me.lucky.wasted.Trigger
import me.lucky.wasted.TriggerReceiver
import me.lucky.wasted.trigger.geofence.GeofenceService
import me.lucky.wasted.trigger.notification.NotificationListenerService
import me.lucky.wasted.trigger.panic.PanicConnectionActivity
import me.lucky.wasted.trigger.panic.PanicResponderActivity
import me.lucky.wasted.trigger.shared.ForegroundService
import me.lucky.wasted.trigger.shared.RestartReceiver
import me.lucky.wasted.trigger.shortcut.ShortcutActivity
import me.lucky.wasted.trigger.shortcut.ShortcutManager
import me.lucky.wasted.trigger.tile.TileService
import me.lucky.wasted.trigger.usb.UsbReceiver

// per-trigger knowledge: which components exist for which trigger.
// adding a trigger edits its own package and one entry here.
internal object TriggerRegistry {
    // TileService is API 24+: safe to reference as a class literal (class objects
    // load lazily and the reconciler only enables TILE when tileSupported)
    @android.annotation.SuppressLint("NewApi")
    val components: List<Pair<Trigger, List<Class<*>>>> = listOf(
        Trigger.PANIC_KIT to listOf(
            PanicConnectionActivity::class.java,
            PanicResponderActivity::class.java,
        ),
        Trigger.TILE to listOf(TileService::class.java),
        Trigger.BROADCAST to listOf(TriggerReceiver::class.java),
        Trigger.NOTIFICATION to listOf(NotificationListenerService::class.java),
    )

    // application option → alias suffix under "$pkg.trigger.application."
    val applicationAliases = linkedMapOf(
        ApplicationOption.SIGNAL to "SignalActivity",
        ApplicationOption.TELEGRAM to "TelegramActivity",
        ApplicationOption.THREEMA to "ThreemaActivity",
        ApplicationOption.SESSION to "SessionActivity",
    )
}

// desired runtime state, derived purely from prefs + permission/platform facts
internal data class DesiredState(
    val enabledTriggers: Set<Trigger>,
    // application options, gated by enablement of the APPLICATION trigger
    val applicationOptions: Int,
    // LOCK | USB | AIRPLANE_MODE need the ForegroundService
    val foregroundRequired: Boolean,
    val usbReceiver: Boolean,
    val restartReceiver: Boolean,
    val geofenceService: Boolean,
)

internal fun derive(
    enabled: Boolean,
    triggers: Int,
    options: Int,
    fineLocationGranted: Boolean,
    tileSupported: Boolean,
): DesiredState {
    fun bit(t: Trigger) = triggers.and(t.value) != 0
    fun on(t: Trigger) = enabled && bit(t)
    val foregroundRequired = on(Trigger.LOCK) || on(Trigger.USB) || on(Trigger.AIRPLANE_MODE)
    return DesiredState(
        enabledTriggers = Trigger.values().filter { on(it) && (it != Trigger.TILE || tileSupported) }.toSet(),
        applicationOptions = if (on(Trigger.APPLICATION)) options else 0,
        foregroundRequired = foregroundRequired,
        usbReceiver = on(Trigger.USB),
        restartReceiver = foregroundRequired || on(Trigger.GEOFENCE),
        geofenceService = on(Trigger.GEOFENCE) && fineLocationGranted,
    )
}

class TriggerReconciler(private val ctx: Context) {
    private val prefs by lazy { Preferences.new(ctx) }

    // reconciles the whole runtime state to the desired one; idempotent and
    // safe to call for any trigger change (changed triggers need no special
    // treatment — the desired state fully determines the runtime state)
    fun sync() {
        val d = derive(
            enabled = prefs.isEnabled,
            triggers = prefs.triggers,
            options = prefs.triggerApplicationOptions,
            fineLocationGranted = ctx.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED,
            tileSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N,
        )
        apply(d)
    }

    private fun apply(d: DesiredState) {
        TriggerRegistry.components.forEach { (trigger, classes) ->
            val on = d.enabledTriggers.contains(trigger)
            classes.forEach { setComponentEnabled(it, on) }
        }
        setForegroundEnabled(d)
        setComponentEnabled(UsbReceiver::class.java, d.usbReceiver)
        setComponentEnabled(RestartReceiver::class.java, d.restartReceiver)
        setGeofenceService(d.geofenceService)
        setShortcutEnabled(d.enabledTriggers.contains(Trigger.SHORTCUT))
        val prefix = "${ctx.packageName}.trigger.application."
        TriggerRegistry.applicationAliases.forEach { (option, alias) ->
            setComponentEnabled(
                prefix + alias,
                d.applicationOptions.and(option.value) != 0,
            )
        }
    }

    private fun setForegroundEnabled(d: DesiredState) =
        Intent(ctx.applicationContext, ForegroundService::class.java).also {
            if (d.enabledTriggers.contains(Trigger.LOCK))
                it.putExtra(ForegroundService.EXTRA_REGISTER_LOCK, true)
            if (d.enabledTriggers.contains(Trigger.AIRPLANE_MODE))
                it.putExtra(ForegroundService.EXTRA_REGISTER_AIRPLANE, true)
            if (d.foregroundRequired)
                ContextCompat.startForegroundService(ctx.applicationContext, it)
            else ctx.stopService(it)
        }

    private fun setGeofenceService(enabled: Boolean) {
        Intent(ctx.applicationContext, GeofenceService::class.java).also {
            if (!enabled) ctx.stopService(it)
            else ContextCompat.startForegroundService(ctx.applicationContext, it)
        }
    }

    private fun setShortcutEnabled(enabled: Boolean) {
        val shortcut = ShortcutManager(ctx)
        if (!enabled) shortcut.remove()
        setComponentEnabled(ShortcutActivity::class.java, enabled)
        if (enabled) shortcut.push()
    }

    private fun setComponentEnabled(cls: Class<*>, enabled: Boolean) =
        setComponentEnabled(ComponentName(ctx, cls), enabled)

    private fun setComponentEnabled(cls: String, enabled: Boolean) =
        setComponentEnabled(ComponentName(ctx, cls), enabled)

    private fun setComponentEnabled(componentName: ComponentName, enabled: Boolean) =
        ctx.packageManager.setComponentEnabledSetting(
            componentName,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
}