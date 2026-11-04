package me.lucky.wasted

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

import me.lucky.wasted.fire.Firing
import me.lucky.wasted.fire.FirePlan

class FiringDecideTest {
    companion object {
        val T = Trigger.TILE.value
    }

    @Test
    fun `gate closed when app disabled or trigger bit unset`() {
        planFor(enabled = false, triggers = T, wipe = true, recast = true).let {
            assertFalse(it.fires || it.wipeData || it.recast)
        }
        planFor(enabled = true, triggers = 0, wipe = true, recast = true).let {
            assertFalse(it.fires)
        }
    }

    @Test
    fun `wipe and recast only when confirmed`() {
        val p = planFor(enabled = true, triggers = T, wipe = true, recast = true, confirmable = false)
        assertTrue(p.fires)
        assertFalse(p.wipeData || p.recast)
        assertTrue(p.requiresConfirmation)
    }

    @Test
    fun `confirmed plan wipes and recasts when configured`() {
        val p = planFor(enabled = true, triggers = T, wipe = true, recast = true, confirmable = true)
        assertTrue(p.fires && p.wipeData && p.recast)
        assertTrue(p.requiresConfirmation)
    }

    @Test
    fun `unconfirmed plan when nothing destructive is configured`() {
        val p = planFor(enabled = true, triggers = T, wipe = false, recast = false, confirmable = false)
        assertTrue(p.fires)
        assertFalse(p.wipeData || p.recast || p.requiresConfirmation)
    }

    private fun planFor(
        enabled: Boolean,
        triggers: Int,
        wipe: Boolean,
        recast: Boolean,
        confirmable: Boolean = true,
    ) = Firing.decide(
        Trigger.TILE,
        enabled,
        triggers,
        wipe,
        recast,
        confirmable,
    )
}