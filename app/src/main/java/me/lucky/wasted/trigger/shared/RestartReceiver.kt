package me.lucky.wasted.trigger.shared

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import me.lucky.wasted.reconciler.TriggerReconciler

class RestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        TriggerReconciler(context?.applicationContext ?: return).sync()
    }
}