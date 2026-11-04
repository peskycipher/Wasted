package me.lucky.wasted.trigger.geofence

/**
 * Pure dwell state machine: tracks continuous time inside a station fence.
 * A fix is ignored (state untouched) when its accuracy is worse than [maxAccuracyM].
 * Dwell resets when an accepted fix lands outside every fence.
 */
class DwellMachine(
    private val stations: List<StationStore.Station>,
    radiusM: Double = FENCE_RADIUS_M,
    private val maxAccuracyM: Float = MAX_FIX_ACCURACY_M,
) {
    companion object {
        const val FENCE_RADIUS_M = 50.0
        const val MAX_FIX_ACCURACY_M = 25f
        // fused-location fix cadence for the dweller
        const val FIX_INTERVAL_MS = 2 * 60 * 1000L
        // dwell time is fixed and equals the fix cadence: entering a fence
        // starts the timer, the next fix (>= 2 min later) confirms the fire.
        // User-configurable dwell removed 2026-11-04 (ADR 0001 update)
        const val DWELL_MS = FIX_INTERVAL_MS
        private const val EARTH_M = 6371000.0
        private const val DEG = Math.PI / 180.0
    }

    private val maxDelta = radiusM / EARTH_M // max angle from a station

    var sinceMs: Long? = null
        private set

    fun restore(sinceMs: Long?) { if (sinceMs != null && sinceMs > 0) this.sinceMs = sinceMs }

    enum class Outcome { NONE, FIRED }

    fun onFix(lat: Double, lon: Double, accuracyM: Float, nowMs: Long, dwellMs: Long): Outcome {
        if (accuracyM > maxAccuracyM) return Outcome.NONE
        val cosLat = Math.cos(lat * DEG)
        val inside = stations.any {
            Math.hypot(
                (it.lat - lat) * DEG,
                (it.lon - lon) * DEG * cosLat,
            ) <= maxDelta
        }
        when {
            !inside -> sinceMs = null
            sinceMs == null -> sinceMs = nowMs
        }
        val since = sinceMs ?: return Outcome.NONE
        return if (nowMs - since >= dwellMs) Outcome.FIRED else Outcome.NONE
    }
}