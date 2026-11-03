package me.lucky.wasted.fragment

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment

import me.lucky.wasted.Preferences
import me.lucky.wasted.R
import me.lucky.wasted.Trigger
import me.lucky.wasted.Utils
import me.lucky.wasted.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var ctx: Context
    private lateinit var prefs: Preferences
    private lateinit var prefsdb: Preferences
    private val utils by lazy { Utils(ctx) }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        prefs.copyTo(prefsdb, key)
    }

    private val finePermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    // combined requests ignore BACKGROUND; it must be requested on its own (Q+)
    private val fineRequester =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it.values.any { granted -> granted }) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    backgroundRequester.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                else finalizeGeofenceEnable(true)
            } else finalizeGeofenceEnable(false)
        }

    private val backgroundRequester =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // a denied background permission still allows while-in-use fixes
            // from a foreground service that was started while in use
            finalizeGeofenceEnable(true)
        }

    private fun finalizeGeofenceEnable(ok: Boolean) {
        if (ok) {
            prefs.triggers = Utils.setFlag(prefs.triggers, Trigger.GEOFENCE.value, true)
            binding.geofence.isChecked = true
            utils.setGeofenceEnabled(true)
        } else {
            prefs.triggers = Utils.setFlag(prefs.triggers, Trigger.GEOFENCE.value, false)
            binding.geofence.isChecked = false
            Toast.makeText(
                ctx,
                R.string.geofence_no_location,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        init()
        setup()
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        prefs.registerListener(prefsListener)
    }

    override fun onStop() {
        super.onStop()
        prefs.unregisterListener(prefsListener)
    }

    private fun init() {
        ctx = requireContext()
        prefs = Preferences(ctx)
        prefsdb = Preferences(ctx, encrypted = false)
        binding.apply {
            val triggers = prefs.triggers
            panicKit.isChecked = triggers.and(Trigger.PANIC_KIT.value) != 0
            tile.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            tile.isChecked = triggers.and(Trigger.TILE.value) != 0
            shortcut.isChecked = triggers.and(Trigger.SHORTCUT.value) != 0
            broadcast.isChecked = triggers.and(Trigger.BROADCAST.value) != 0
            notification.isChecked = triggers.and(Trigger.NOTIFICATION.value) != 0
            lock.isChecked = triggers.and(Trigger.LOCK.value) != 0
            usb.isChecked = triggers.and(Trigger.USB.value) != 0
            airplane.isChecked = triggers.and(Trigger.AIRPLANE_MODE.value) != 0
            application.isChecked = triggers.and(Trigger.APPLICATION.value) != 0
            geofence.isChecked = triggers.and(Trigger.GEOFENCE.value) != 0
        }
    }

    private fun setup() = binding.apply {
        panicKit.setOnCheckedChangeListener { _, isChecked ->
            prefs.triggers = Utils.setFlag(prefs.triggers, Trigger.PANIC_KIT.value, isChecked)
            utils.setPanicKitEnabled(isChecked && prefs.isEnabled)
        }
        tile.setOnCheckedChangeListener { _, isChecked ->
            prefs.triggers = Utils.setFlag(prefs.triggers, Trigger.TILE.value, isChecked)
            utils.setTileEnabled(isChecked && prefs.isEnabled)
        }
        shortcut.setOnCheckedChangeListener { _, isChecked ->
            prefs.triggers = Utils.setFlag(prefs.triggers, Trigger.SHORTCUT.value, isChecked)
            utils.setShortcutEnabled(isChecked && prefs.isEnabled)
        }
        broadcast.setOnCheckedChangeListener { _, isChecked ->
            prefs.triggers = Utils.setFlag(prefs.triggers, Trigger.BROADCAST.value, isChecked)
            utils.setBroadcastEnabled(isChecked && prefs.isEnabled)
        }
        notification.setOnCheckedChangeListener { _, isChecked ->
            prefs.triggers =
                Utils.setFlag(prefs.triggers, Trigger.NOTIFICATION.value, isChecked)
            utils.setNotificationEnabled(isChecked && prefs.isEnabled)
        }
        lock.setOnCheckedChangeListener { _, isChecked ->
            prefs.triggers = Utils.setFlag(prefs.triggers, Trigger.LOCK.value, isChecked)
            utils.updateForegroundRequiredEnabled()
        }
        usb.setOnCheckedChangeListener { _, isChecked ->
            prefs.triggers = Utils.setFlag(prefs.triggers, Trigger.USB.value, isChecked)
            utils.updateForegroundRequiredEnabled()
        }
        airplane.setOnCheckedChangeListener { _, isChecked ->
            prefs.triggers = Utils.setFlag(prefs.triggers, Trigger.AIRPLANE_MODE.value, isChecked)
            utils.updateForegroundRequiredEnabled()
        }
        application.setOnCheckedChangeListener { _, isChecked ->
            prefs.triggers = Utils.setFlag(prefs.triggers, Trigger.APPLICATION.value, isChecked)
            utils.updateApplicationEnabled()
        }
        geofence.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // the flag is only written once permissions have been settled
                binding.geofence.isChecked = false
                fineRequester.launch(finePermissions)
            } else {
                prefs.triggers = Utils.setFlag(prefs.triggers, Trigger.GEOFENCE.value, false)
                utils.setGeofenceEnabled(false)
            }
        }
    }

    private fun hasLocationPermissions() = finePermissions.all {
        ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
    }
}