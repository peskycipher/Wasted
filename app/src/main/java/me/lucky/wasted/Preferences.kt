package me.lucky.wasted

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.UserManager
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class Preferences(ctx: Context, encrypted: Boolean = true) {
    companion object {
        private const val DEFAULT_TRIGGER_LOCK_COUNT = 7 * 24 * 60
        private const val DEFAULT_TRIGGER_TILE_DELAY = 2000L
        private const val DEFAULT_TRIGGER_GEOFENCE_DWELL = 2
        const val MIN_TRIGGER_GEOFENCE_DWELL = 2

        private const val ENABLED = "enabled"
        private const val SECRET = "secret"
        private const val WIPE_DATA = "wipe_data"
        private const val WIPE_EMBEDDED_SIM = "wipe_embedded_sim"

        private const val RECAST_ENABLED = "recast_enabled"
        private const val RECAST_ACTION = "recast_action"
        private const val RECAST_RECEIVER = "recast_receiver"
        private const val RECAST_EXTRA_KEY = "recast_extra_key"
        private const val RECAST_EXTRA_VALUE = "recast_extra_value"

        private const val TRIGGERS = "triggers"
        private const val TRIGGER_LOCK_COUNT = "trigger_lock_count"
        private const val TRIGGER_TILE_DELAY = "trigger_tile_delay"
        private const val TRIGGER_APPLICATION_OPTIONS = "trigger_application_options"
        private const val TRIGGER_GEOFENCE_DWELL = "trigger_geofence_dwell"
        private const val TRIGGER_GEOFENCE_SINCE = "trigger_geofence_since"

        private const val FILE_NAME = "sec_shared_prefs"

        // migration
        private const val AUTHENTICATION_CODE = "authentication_code"
        private const val WIPE_ON_INACTIVITY_COUNT = "wipe_on_inactivity_count"

        // ADR 0002: the plaintext mirror exists to serve direct-boot consumers
        // pre-unlock — it carries exactly these keys and nothing else. Adding a
        // direct-boot consumer? add its key here (review-enforced).
        private val DIRECT_BOOT_KEYS = setOf(
            ENABLED, SECRET, AUTHENTICATION_CODE,
            WIPE_DATA, WIPE_EMBEDDED_SIM,
            TRIGGERS,
            RECAST_ENABLED, RECAST_ACTION, RECAST_RECEIVER,
            RECAST_EXTRA_KEY, RECAST_EXTRA_VALUE,
            TRIGGER_LOCK_COUNT, WIPE_ON_INACTIVITY_COUNT,
        )

        fun new(ctx: Context) = Preferences(
            ctx,
            encrypted = Build.VERSION.SDK_INT < Build.VERSION_CODES.N ||
                ctx.getSystemService(UserManager::class.java).isUserUnlocked,
        )
    }

    private val prefs: SharedPreferences = if (encrypted) {
        val mk = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            FILE_NAME,
            mk,
            ctx,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } else {
        val context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            ctx.createDeviceProtectedStorageContext() else ctx
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    // direct-boot mirror: the device-protected plaintext copy that
    // directBootAware trigger components (broadcast, usb, lock) read pre-unlock,
    // when the credential-encrypted file is unreadable — see docs/adr/0002.
    // API < 24 has no direct boot: legacy storage, no mirror
    private val mirror: SharedPreferences? =
        if (encrypted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) PreferenceManager
            .getDefaultSharedPreferences(ctx.createDeviceProtectedStorageContext())
        else null

    // SharedPreferences keeps listeners weakly — hold a strong reference
    private val mirrorListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (mirror != null && key != null) copyKey(sp, mirror, key)
        }

    init {
        if (mirror != null)
            prefs.registerOnSharedPreferenceChangeListener(mirrorListener)
        // one-time bootstrap: the listener mirrors writes only, so keys stored
        // before this version (incl. secret) must be copied wholesale once —
        // direct-boot consumers read the mirror pre-unlock (ADR 0002)
        if (mirror != null) {
            for (k in prefs.all.keys) {
                if (k in DIRECT_BOOT_KEYS && !mirror.all.containsKey(k))
                    copyKey(prefs, mirror, k)
            }
        }
    }

    // mirror the single changed key; anything with an unsupported type is dropped
    private fun copyKey(src: SharedPreferences, dst: SharedPreferences, key: String) {
        if (key !in DIRECT_BOOT_KEYS) return // mirror scope == direct-boot readers
        val v = src.all[key] ?: return
        dst.edit {
            when (v) {
                is Boolean -> putBoolean(key, v)
                is Int -> putInt(key, v)
                is Long -> putLong(key, v)
                is String -> putString(key, v)
                // a silently dropped type would break direct-boot reads later;
                // make any new pref type visible immediately
                else -> android.util.Log.w(
                    "Preferences",
                    "mirror: unsupported pref type for $key (${v::class.simpleName})",
                )
            }
        }
    }

    var isEnabled: Boolean
        get() = prefs.getBoolean(ENABLED, false)
        set(value) = prefs.edit { putBoolean(ENABLED, value) }

    var triggers: Int
        get() = prefs.getInt(TRIGGERS, 0)
        set(value) = prefs.edit { putInt(TRIGGERS, value) }

    var secret: String
        get() = prefs.getString(
            SECRET,
            prefs.getString(AUTHENTICATION_CODE, "") ?: "",
        ) ?: ""
        set(value) = prefs.edit { putString(SECRET, value) }

    var isWipeData: Boolean
        get() = prefs.getBoolean(WIPE_DATA, false)
        set(value) = prefs.edit { putBoolean(WIPE_DATA, value) }

    var isWipeEmbeddedSim: Boolean
        get() = prefs.getBoolean(WIPE_EMBEDDED_SIM, false)
        set(value) = prefs.edit { putBoolean(WIPE_EMBEDDED_SIM, value) }

    var triggerLockCount: Int
        get() = prefs.getInt(
            TRIGGER_LOCK_COUNT,
            prefs.getInt(WIPE_ON_INACTIVITY_COUNT, DEFAULT_TRIGGER_LOCK_COUNT),
        )
        set(value) = prefs.edit { putInt(TRIGGER_LOCK_COUNT, value) }

    var triggerTileDelay: Long
        get() = prefs.getLong(TRIGGER_TILE_DELAY, DEFAULT_TRIGGER_TILE_DELAY)
        set(value) = prefs.edit { putLong(TRIGGER_TILE_DELAY, value) }

    var triggerApplicationOptions: Int
        get() = prefs.getInt(TRIGGER_APPLICATION_OPTIONS, 0)
        set(value) = prefs.edit { putInt(TRIGGER_APPLICATION_OPTIONS, value) }

    // police-station dwell minutes
    var triggerGeofenceDwell: Int
        get() = prefs.getInt(TRIGGER_GEOFENCE_DWELL, DEFAULT_TRIGGER_GEOFENCE_DWELL)
        set(value) = prefs.edit { putInt(TRIGGER_GEOFENCE_DWELL, value) }

    // epoch ms when the current dwell started, 0 = not dwelling
    var triggerGeofenceSince: Long
        get() = prefs.getLong(TRIGGER_GEOFENCE_SINCE, 0L)
        set(value) = prefs.edit { putLong(TRIGGER_GEOFENCE_SINCE, value) }

    fun setTrigger(trigger: Trigger, on: Boolean) {
        val cur = triggers
        triggers = if (on) cur.or(trigger.value) else cur.and(trigger.value.inv())
    }

    fun setApplicationOption(option: ApplicationOption, on: Boolean) {
        val cur = triggerApplicationOptions
        triggerApplicationOptions =
            if (on) cur.or(option.value) else cur.and(option.value.inv())
    }

    var isRecastEnabled: Boolean
        get() = prefs.getBoolean(RECAST_ENABLED, false)
        set(value) = prefs.edit { putBoolean(RECAST_ENABLED, value) }

    var recastAction: String
        get() = prefs.getString(RECAST_ACTION, "") ?: ""
        set(value) = prefs.edit { putString(RECAST_ACTION, value) }

    var recastReceiver: String
        get() = prefs.getString(RECAST_RECEIVER, "") ?: ""
        set(value) = prefs.edit { putString(RECAST_RECEIVER, value) }

    var recastExtraKey: String
        get() = prefs.getString(RECAST_EXTRA_KEY, "") ?: ""
        set(value) = prefs.edit { putString(RECAST_EXTRA_KEY, value) }

    var recastExtraValue: String
        get() = prefs.getString(RECAST_EXTRA_VALUE, "") ?: ""
        set(value) = prefs.edit { putString(RECAST_EXTRA_VALUE, value) }

  // no external mirroring choreography — Preferences owns the direct-boot mirror
}

enum class Trigger(val value: Int) {
    PANIC_KIT(1),
    TILE(1 shl 1),
    SHORTCUT(1 shl 2),
    BROADCAST(1 shl 3),
    NOTIFICATION(1 shl 4),
    LOCK(1 shl 5),
    USB(1 shl 6),
    APPLICATION(1 shl 7),
    GEOFENCE(1 shl 8),
    AIRPLANE_MODE(1 shl 9),
}

enum class ApplicationOption(val value: Int) {
    SIGNAL(1),
    TELEGRAM(1 shl 1),
    THREEMA(1 shl 2),
    SESSION(1 shl 3),
}