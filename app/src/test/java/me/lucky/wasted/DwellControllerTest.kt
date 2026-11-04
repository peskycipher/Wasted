package me.lucky.wasted

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

import me.lucky.wasted.trigger.geofence.DwellController
import me.lucky.wasted.trigger.geofence.DwellEffect
import me.lucky.wasted.trigger.geofence.DwellMachine
import me.lucky.wasted.trigger.geofence.DwellState
import me.lucky.wasted.trigger.geofence.StationStore

class DwellControllerTest {
    companion object {
        val STATION = StationStore.Station(lon = 144.95, lat = -37.81)
        const val DWELL_MINUTES = 15
        const val DWELL_MS = DWELL_MINUTES * 60_000L
        const val T0 = 1_000_000L
    }

    // fake store — the second adapter proving the seam
    private class FakeState : DwellState {
        var value = 0L
        var writes = 0
        override var sinceMs: Long
            get() = value
            set(v) { value = v; writes++ }
    }

    private class Rig(var fired: Boolean = false) {
        val machine = DwellMachine(listOf(STATION))
        val state = FakeState()
        var fireCount = 0
        val controller = DwellController(machine, state, fire = { fireCount++ })
    }

    @Test
    fun `starts dwell, persists once, does not fire early`() {
        val rig = Rig()
        assertEquals(
            DwellEffect.Persist(T0),
            rig.controller.onFix(-37.8104, 144.95, 10f, T0, DWELL_MINUTES),
        )
        assertEquals(T0, rig.state.value)
        assertEquals(0, rig.fireCount)
        // same-state fix is not re-persisted
        val before = rig.state.writes
        rig.controller.onFix(-37.8104, 144.95, 10f, T0 + 60_000L, DWELL_MINUTES)
        assertEquals(before, rig.state.writes)
    }

    @Test
    fun `fix past dwell fires once, clears state and cleans up`() {
        val rig = Rig()
        rig.state.sinceMs = T0
        rig.machine.restore(T0)
        assertEquals(
            DwellEffect.Fired,
            rig.controller.onFix(-37.8104, 144.95, 10f, T0 + DWELL_MS, DWELL_MINUTES),
        )
        assertEquals(1, rig.fireCount)
        assertEquals(0, rig.state.value)
    }

    @Test
    fun `accurate fix outside resets dwell without firing`() {
        val rig = Rig()
        rig.state.sinceMs = T0
        rig.machine.restore(T0)
        assertEquals(
            DwellEffect.Nothing,
            rig.controller.onFix(-38.0, 145.0, 10f, T0 + 60_000L, DWELL_MINUTES),
        )
        assertEquals(0, rig.state.value)
        assertEquals(0, rig.fireCount)
    }

    @Test
    fun `inaccurate fix is ignored entirely — dwell continues`() {
        val rig = Rig()
        rig.state.sinceMs = T0
        rig.machine.restore(T0)
        assertEquals(
            DwellEffect.Persist(T0),
            rig.controller.onFix(-37.8104, 144.95, 999f, T0 + DWELL_MS, DWELL_MINUTES),
        )
        assertEquals(T0, rig.state.value)
        assertEquals(0, rig.fireCount)
    }
}