package me.lucky.wasted.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import me.lucky.wasted.ApplicationOption
import me.lucky.wasted.Preferences
import me.lucky.wasted.Utils
import me.lucky.wasted.reconciler.TriggerReconciler
import me.lucky.wasted.databinding.FragmentApplicationBinding

class ApplicationFragment : Fragment() {
    private lateinit var binding: FragmentApplicationBinding
    private lateinit var ctx: Context
    private lateinit var prefs: Preferences
    private val reconciler by lazy { TriggerReconciler(ctx) }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentApplicationBinding.inflate(inflater, container, false)
        init()
        setup()
        return binding.root
    }



    private fun init() {
        ctx = requireContext()
        prefs = Preferences(ctx)
        binding.apply {
            val options = prefs.triggerApplicationOptions
            signal.isChecked = options.and(ApplicationOption.SIGNAL.value) != 0
            telegram.isChecked = options.and(ApplicationOption.TELEGRAM.value) != 0
            threema.isChecked = options.and(ApplicationOption.THREEMA.value) != 0
            session.isChecked = options.and(ApplicationOption.SESSION.value) != 0
        }
    }

    private fun setup() = binding.apply {
        signal.setOnCheckedChangeListener { _, isChecked ->
            prefs.setApplicationOption(ApplicationOption.SIGNAL, isChecked)
            reconciler.sync()
        }
        telegram.setOnCheckedChangeListener { _, isChecked ->
            prefs.setApplicationOption(ApplicationOption.TELEGRAM, isChecked)
            reconciler.sync()
        }
        threema.setOnCheckedChangeListener { _, isChecked ->
            prefs.setApplicationOption(ApplicationOption.THREEMA, isChecked)
            reconciler.sync()
        }
        session.setOnCheckedChangeListener { _, isChecked ->
            prefs.setApplicationOption(ApplicationOption.SESSION, isChecked)
            reconciler.sync()
        }
    }
}