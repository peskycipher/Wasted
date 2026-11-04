package me.lucky.wasted

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

import me.lucky.wasted.reconciler.derive

class TriggerReconcilerDeriveTest {
    companion object {
        val ALL = Trigger.PANIC_KIT.value or Trigger.TILE.value or
            Trigger.SHORTCUT.value or Trigger.BROADCAST.value or
            Trigger.NOTIFICATION.value or Trigger.LOCK.value or
            Trigger.USB.value or Trigger.APPLICATION.value or
            Trigger.GEOFENCE.value or Trigger.AIRPLANE_MODE.value
        val OPTIONS = ApplicationOption.SIGNAL.value or
            ApplicationOption.SESSION.value
    }

    @Test
    fun `all bits on with permission and tile support`() {
        val d = derive(
            enabled = true,
            triggers = ALL,
            options = OPTIONS,
            fineLocationGranted = true,
            tileSupported = true,
        )
        assertEquals(Trigger.values().toSet(), d.enabledTriggers)
        assertEquals(OPTIONS, d.applicationOptions)
        assertTrue(d.foregroundRequired && d.restartReceiver)
        assertTrue(d.usbReceiver && d.geofenceService)
    }

    @Test
    fun `app disabled disables everything`() {
        val d = derive(
            enabled = false,
            triggers = ALL,
            options = OPTIONS,
            fineLocationGranted = false,
            tileSupported = true,
        )
        assertTrue(d.enabledTriggers.isEmpty())
        assertEquals(0, d.applicationOptions)
        assertFalse(d.foregroundRequired || d.restartReceiver)
        assertFalse(d.usbReceiver || d.geofenceService)
    }

    @Test
    fun `geofence without fine location starts no service but keeps restart`() {
        val d = derive(
            enabled = true,
            triggers = Trigger.GEOFENCE.value,
            options = 0,
            fineLocationGranted = false,
            tileSupported = true,
        )
        assertFalse(d.geofenceService)
        assertTrue(d.restartReceiver)
    }

    @Test
    fun `lock only needs foreground service and restart receiver`() {
        val d = derive(
            enabled = true,
            triggers = Trigger.LOCK.value,
            options = 0,
            fineLocationGranted = true,
            tileSupported = true,
        )
        assertTrue(d.foregroundRequired && d.restartReceiver)
        assertFalse(d.usbReceiver || d.geofenceService)
        assertTrue(d.enabledTriggers.contains(Trigger.LOCK))
    }

    @Test
    fun `usb only does not need the foreground service (ADR 0005)`() {
        val d = derive(
            enabled = true,
            triggers = Trigger.USB.value,
            options = 0,
            fineLocationGranted = false,
            tileSupported = true,
        )
        assertFalse(d.foregroundRequired)
        assertTrue(d.usbReceiver)
        assertFalse(d.restartReceiver)
    }

    @Test
    fun `tile requested without tile support stays off`() {
        val d = derive(
            enabled = true,
            triggers = Trigger.TILE.value,
            options = 0,
            fineLocationGranted = true,
            tileSupported = false,
        )
        assertFalse(d.enabledTriggers.contains(Trigger.TILE))
    }
}