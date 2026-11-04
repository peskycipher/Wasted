package me.lucky.wasted.trigger.shared

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

import me.lucky.wasted.R

class NotificationManager(private val ctx: Context) {
    companion object {
        const val CHANNEL_DEFAULT_ID = "default"
    }

    private val manager = NotificationManagerCompat.from(ctx)

    fun createNotificationChannels() {
        manager.createNotificationChannel(appChannel())
    }

    // "app notifications" toggle in the account settings screen: off recreates
    // the channel at blocked importance, on restores it
    fun setNotificationsEnabled(enabled: Boolean) {
        manager.deleteNotificationChannel(CHANNEL_DEFAULT_ID)
        manager.createNotificationChannel(appChannel(enabled))
    }

    private fun appChannel(enabled: Boolean = true) = NotificationChannelCompat.Builder(
        CHANNEL_DEFAULT_ID,
        if (enabled) NotificationManagerCompat.IMPORTANCE_LOW
        else NotificationManagerCompat.IMPORTANCE_NONE,
    ).setName(ctx.getString(R.string.notification_channel_default_name)).setShowBadge(false).build()
}