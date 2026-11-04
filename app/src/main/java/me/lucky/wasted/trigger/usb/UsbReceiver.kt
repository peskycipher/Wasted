package me.lucky.wasted.trigger.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager

import me.lucky.wasted.Trigger
import me.lucky.wasted.Utils
import me.lucky.wasted.fire.Fire

class UsbReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != UsbManager.ACTION_USB_DEVICE_ATTACHED &&
            intent?.action != UsbManager.ACTION_USB_ACCESSORY_ATTACHED) return
        if (!Utils(context ?: return).isDeviceLocked()) return
        Fire(context).fire(Trigger.USB)
    }
}