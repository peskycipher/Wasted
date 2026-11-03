package me.lucky.wasted

import org.junit.Assert.assertEquals
import org.junit.Test

import me.lucky.wasted.trigger.geofence.DwellMachine
import me.lucky.wasted.trigger.geofence.StationStore

class DwellMachineTest {
    companion object {
        // one station in Melbourne CBD
        val STATION = StationStore.Station(lon = 144.95, lat = -37.81)
        const val DWELL_MS = 15 * 60_000L
        const val T0 = 1_000_000L
    }

    private fun machine() = DwellMachine(listOf(STATION))

    @Test
    fun `starts dwell but does not fire before N minutes`() {
        val m = machine()
        assertEquals(DwellMachine.Outcome.NONE, m.onFix(-37.8104, 144.95, 10f, T0, DWELL_MS))
        assertEquals(T0, m.sinceMs)
        val out = m.onFix(-37.8104, 144.95, 10f, T0 + DWELL_MS - 1, DWELL_MS)
        assertEquals(DwellMachine.Outcome.NONE, out)
    }

    @Test
    fun `fires after N minutes of continuous dwell`() {
        val m = machine()
        m.onFix(-37.8104, 144.95, 10f, T0, DWELL_MS)
        val out = m.onFix(-37.8104, 144.95, 10f, T0 + DWELL_MS, DWELL_MS)
        assertEquals(DwellMachine.Outcome.FIRED, out)
    }

    @Test
    fun `leaving resets dwell`() {
        val m = machine()
        m.onFix(-37.8104, 144.95, 10f, T0, DWELL_MS)
        m.onFix(-38.00, 144.95, 10f, T0 + 60_000, DWELL_MS)
        assertEquals(null, m.sinceMs)
        val out = m.onFix(-37.8104, 144.95, 10f, T0 + 60_000 + DWELL_MS - 30_000, DWELL_MS)
        assertEquals(DwellMachine.Outcome.NONE, out)
    }

    @Test
    fun `low accuracy fixes are ignored entirely`() {
        // cannot start dwell
        val m = machine()
        m.onFix(-37.8104, 144.95, 30f, T0, DWELL_MS)
        assertEquals(null, m.sinceMs)
        // and cannot reset existing dwell
        m.onFix(-37.8104, 144.95, 10f, T0, DWELL_MS)
        m.onFix(-38.00, 144.95, 60f, T0 + 60_000, DWELL_MS)
        assertEquals(T0, m.sinceMs)
        val out = m.onFix(-37.8104, 144.95, 10f, T0 + DWELL_MS, DWELL_MS)
        assertEquals(DwellMachine.Outcome.FIRED, out)
    }

    @Test
    fun `dwelling survives service restart via restore`() {
        val m = machine()
        m.onFix(-37.8104, 144.95, 10f, T0, DWELL_MS)
        val m2 = machine().also { it.restore(m.sinceMs) }
        val out = m2.onFix(-37.8104, 144.95, 10f, T0 + DWELL_MS + 1000, DWELL_MS)
        assertEquals(DwellMachine.Outcome.FIRED, out)
    }

    @Test
    fun `restore with 0 or negative is a no-op`() {
        val m = machine()
        m.restore(0)
        m.restore(-5)
        assertEquals(null, m.sinceMs)
    }

    @Test
    fun `~44m from the station is inside the fence`() {
        // fixture is about 44m south of the station point, within the 50m radius
        val m = machine()
        m.onFix(-37.8104, 144.95, 10f, T0, DWELL_MS)
        assertEquals(T0, m.sinceMs)
    }
}