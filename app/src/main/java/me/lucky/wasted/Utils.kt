package me.lucky.wasted

import android.app.KeyguardManager
import android.content.Context

class Utils(private val ctx: Context) {
    fun isDeviceLocked() = ctx.getSystemService(KeyguardManager::class.java).isDeviceLocked
}