package me.lucky.wasted.fragment

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment

import me.lucky.wasted.Preferences
import me.lucky.wasted.R
import me.lucky.wasted.Trigger
import me.lucky.wasted.Utils
import me.lucky.wasted.databinding.FragmentSettingsBinding
import me.lucky.wasted.permission.LocationPermission
import me.lucky.wasted.reconciler.TriggerReconciler

class SettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var ctx: Context
    private lateinit var prefs: Preferences
    private val reconciler by lazy { TriggerReconciler(ctx) }
    private val locPermission = LocationPermission(this)


    private fun finalizeGeofenceEnable(ok: Boolean) {
        if (ok) {
            prefs.setTrigger(Trigger.GEOFENCE, true)
            binding.geofence.isChecked = true
            reconciler.sync()
        } else {
            prefs.setTrigger(Trigger.GEOFENCE, false)
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



    private fun init() {
        ctx = requireContext()
        prefs = Preferences(ctx)
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
            prefs.setTrigger(Trigger.PANIC_KIT, isChecked)
            reconciler.sync()
        }
        tile.setOnCheckedChangeListener { _, isChecked ->
            prefs.setTrigger(Trigger.TILE, isChecked)
            reconciler.sync()
        }
        shortcut.setOnCheckedChangeListener { _, isChecked ->
            prefs.setTrigger(Trigger.SHORTCUT, isChecked)
            reconciler.sync()
        }
        broadcast.setOnCheckedChangeListener { _, isChecked ->
            prefs.setTrigger(Trigger.BROADCAST, isChecked)
            reconciler.sync()
        }
        notification.setOnCheckedChangeListener { _, isChecked ->
            prefs.setTrigger(Trigger.NOTIFICATION, isChecked)
            reconciler.sync()
        }
        lock.setOnCheckedChangeListener { _, isChecked ->
            prefs.setTrigger(Trigger.LOCK, isChecked)
            reconciler.sync()
        }
        usb.setOnCheckedChangeListener { _, isChecked ->
            prefs.setTrigger(Trigger.USB, isChecked)
            reconciler.sync()
        }
        airplane.setOnCheckedChangeListener { _, isChecked ->
            prefs.setTrigger(Trigger.AIRPLANE_MODE, isChecked)
            reconciler.sync()
        }
        application.setOnCheckedChangeListener { _, isChecked ->
            prefs.setTrigger(Trigger.APPLICATION, isChecked)
            reconciler.sync()
        }
        geofence.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // the flag is only written once permissions have been settled
                binding.geofence.isChecked = false
                locPermission.ensure { ok -> finalizeGeofenceEnable(ok) }
            } else {
                prefs.setTrigger(Trigger.GEOFENCE, false)
                reconciler.sync()
            }
        }
    }
}