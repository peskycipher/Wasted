package me.lucky.wasted.trigger.geofence

import android.content.Context
import me.lucky.wasted.BuildConfig
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// police-station dwell debugging aid: logcat for everyone (ephemeral), a
// cache-dir file only for debug builds — release never writes the dwell trail
// to storage (the shipped app promises "your location is never stored").
// Clear via app settings / uninstall; capped so a long test can't fill storage.
object GeofenceDebugLog {
    private const val TAG = "GeofenceDebug"
    private const val MAX_BYTES = 512 * 1024
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT)

    fun d(ctx: Context?, msg: String) {
        Log.d(TAG, msg)
        if (ctx == null || !BuildConfig.DEBUG) return
        try {
            val f = file(ctx)
            if (f.exists() && f.length() > MAX_BYTES) f.delete() // cap: restart the trail
            f.appendText("${fmt.format(Date())} $msg\n")
        } catch (_: Exception) {}
    }

    fun clear(ctx: Context) {
        try { file(ctx).delete() } catch (_: Exception) {}
    }

    fun file(ctx: Context): File = File(ctx.cacheDir, "geofence-debug.log")
}