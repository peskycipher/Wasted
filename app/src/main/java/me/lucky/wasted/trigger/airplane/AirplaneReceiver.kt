package me.lucky.wasted.trigger.airplane

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

import me.lucky.wasted.Trigger
import me.lucky.wasted.Utils

class AirplaneReceiver : BroadcastReceiver() {
    companion object {
        fun isOn(ctx: Context) =
            Settings.Global.getInt(
                ctx.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0,
            ) != 0
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intent.ACTION_AIRPLANE_MODE_CHANGED) return
        if (!intent.getBooleanExtra("state", false)) return
        Utils(context ?: return).fire(Trigger.AIRPLANE_MODE)
    }
}