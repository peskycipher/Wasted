package me.lucky.wasted.fire

import android.content.Context
import android.content.Intent
import android.util.Log

import me.lucky.wasted.Preferences
import me.lucky.wasted.Trigger
import me.lucky.wasted.admin.DeviceAdminManager

// the fire policy for a trigger, decided up front so the destructive
// sequence is inspectable before anything runs
data class FirePlan(
    // false = trigger gate failed, nothing should execute
    val fires: Boolean,
    val lock: Boolean,
    val wipeData: Boolean,
    val recast: Boolean,
    // destructive action is configured and a UI caller should confirm first
    val requiresConfirmation: Boolean,
)

data class FireOutcome(
    val plan: FirePlan,
    val lockDone: Boolean,
    val wipeDone: Boolean,
    val recastDone: Boolean,
)

// core policy, pure over plain values so it is unit-testable
object Firing {
    fun decide(
        trigger: Trigger,
        enabled: Boolean,
        triggers: Int,
        isWipeData: Boolean,
        isRecastEnabled: Boolean,
        confirmable: Boolean,
    ): FirePlan {
        val destructive = isWipeData || isRecastEnabled
        return FirePlan(
            fires = enabled && triggers.and(trigger.value) != 0,
            lock = enabled && triggers.and(trigger.value) != 0,
            wipeData = isWipeData && confirmable,
            recast = isRecastEnabled && confirmable,
            requiresConfirmation = destructive,
        )
    }

    fun execute(plan: FirePlan, ctx: Context): FireOutcome {
        if (!plan.fires) return FireOutcome(plan, false, false, false)
        val prefs = Preferences.new(ctx)
        var lockDone = false
        var wipeDone = false
        var recastDone = false
        // per-step: an unknown failure must not mis-report the other steps
        try {
            DeviceAdminManager(ctx).lockNow()
            lockDone = true
        } catch (exc: SecurityException) {
            Log.w(TAG, "fire: lockNow denied", exc)
        }
        if (plan.wipeData) {
            try {
                DeviceAdminManager(ctx).wipeData()
                wipeDone = true
            } catch (exc: SecurityException) {
                Log.w(TAG, "fire: wipeData denied", exc)
            }
        }
        if (plan.recast) {
            recastDone = recast(prefs, ctx)
        }
        return FireOutcome(plan, lockDone, wipeDone, recastDone)
    }

    private const val TAG = "Firing"

    private fun recast(prefs: Preferences, ctx: Context): Boolean {
        val action = prefs.recastAction
        if (action.isEmpty()) return false
        ctx.sendBroadcast(Intent(action).apply {
            val cls = prefs.recastReceiver.split('/')
            val packageName = cls.firstOrNull() ?: ""
            if (packageName.isNotEmpty()) {
                setPackage(packageName)
                if (cls.size == 2)
                    setClassName(
                        packageName,
                        "$packageName.${cls[1].trimStart('.')}",
                    )
            }
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            val extraKey = prefs.recastExtraKey
            if (extraKey.isNotEmpty()) putExtra(extraKey, prefs.recastExtraValue)
        })
        return true
    }
}

// convenience facade for one-shot callers: gate + plan + execute in one call
class Fire(private val ctx: Context) {
    private val prefs by lazy { Preferences.new(ctx) }

    fun decide(trigger: Trigger, confirmable: Boolean = true) = Firing.decide(
        trigger,
        prefs.isEnabled,
        prefs.triggers,
        prefs.isWipeData,
        prefs.isRecastEnabled,
        confirmable,
    )

    fun execute(plan: FirePlan) = Firing.execute(plan, ctx)

    fun fire(trigger: Trigger, confirmable: Boolean = true): FireOutcome =
        execute(decide(trigger, confirmable))
}