package me.lucky.wasted.trigger.geofence

/**
 * Dwell state carried between fix and service: what happened on [onFix].
 */
sealed interface DwellEffect {
    /** still-or-started dwelling at [sinceMs]; caller need not act */
    data class Persist(val sinceMs: Long) : DwellEffect
    /** dwell confirmed; firing already happened */
    object Fired : DwellEffect
    object Nothing : DwellEffect
}

interface DwellState {
    var sinceMs: Long
}

/**
 * Controls one dweller: fix → machine → persistence → fire decision.
 * The service stays Android plumbing; this layer is testable with a fake
 * [DwellState] (second adapter — the pref-backed one lives in GeofenceService).
 */
class DwellController(
    private val machine: DwellMachine,
    private val state: DwellState,
    private val fire: () -> Unit,
) {
    init {
        machine.restore(state.sinceMs)
    }

    fun onFix(lat: Double, lon: Double, accuracyM: Float, nowMs: Long, dwellMinutes: Int): DwellEffect {
        when (machine.onFix(lat, lon, accuracyM, nowMs, dwellMinutes * 60_000L)) {
            DwellMachine.Outcome.FIRED -> {
                state.sinceMs = 0
                fire()
                return DwellEffect.Fired
            }
            DwellMachine.Outcome.NONE -> {
                val since = machine.sinceMs ?: 0
                if (state.sinceMs != since) state.sinceMs = since
                return if (since > 0) DwellEffect.Persist(since) else DwellEffect.Nothing
            }
        }
    }
}