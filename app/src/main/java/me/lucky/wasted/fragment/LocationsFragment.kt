package me.lucky.wasted.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment

import me.lucky.wasted.Preferences
import me.lucky.wasted.R
import me.lucky.wasted.databinding.FragmentLocationsBinding

class LocationsFragment : Fragment() {
    private lateinit var binding: FragmentLocationsBinding
    private lateinit var ctx: Context
    private lateinit var prefs: Preferences


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLocationsBinding.inflate(inflater, container, false)
        init()
        setup()
        return binding.root
    }



    private fun init() {
        ctx = requireContext()
        prefs = Preferences(ctx)
        binding.apply {
            val dwell = prefs.triggerGeofenceDwell
            geofenceDwell.editText?.setText(if (dwell % 60 == 0) "${dwell / 60}h" else "${dwell}m")
        }
    }

    private fun setup() = binding.apply {
        geofenceDwell.editText?.doAfterTextChanged { text ->
            val str = text?.toString() ?: ""
            if (str.isEmpty()) return@doAfterTextChanged
            val minutes = parseDuration(str)
            if (minutes == null || minutes < 2) {
                geofenceDwell.error = ctx.getString(R.string.trigger_geofence_dwell_error)
                return@doAfterTextChanged
            }
            prefs.triggerGeofenceDwell = minutes
            geofenceDwell.error = null
        }
    }

    // "15m" / "2h" / "1d" -> minutes, null if malformed
    private fun parseDuration(str: String): Int? {
        if (!str.matches(Regex("^[1-9]\\d*[dhm]$"))) return null
        val i = str.dropLast(1).toInt()
        return when (str.last()) {
            'd' -> i * 24 * 60
            'h' -> i * 60
            'm' -> i
            else -> null
        }
    }
}