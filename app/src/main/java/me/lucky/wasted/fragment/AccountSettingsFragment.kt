package me.lucky.wasted.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.widget.doAfterTextChanged

import androidx.fragment.app.Fragment

import me.lucky.wasted.Preferences
import me.lucky.wasted.wallet.Money
import me.lucky.wasted.R
import me.lucky.wasted.databinding.FragmentAccountSettingsBinding
import me.lucky.wasted.trigger.shared.NotificationManager

class AccountSettingsFragment : Fragment() {
    private lateinit var binding: FragmentAccountSettingsBinding
    private lateinit var ctx: Context
    private lateinit var prefs: Preferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAccountSettingsBinding.inflate(inflater, container, false)
        ctx = requireContext()
        prefs = Preferences(ctx)
        init()
        setup()
        return binding.root
    }

    private fun init() {
        binding.apply {
            currency.editText?.setText(prefs.walletTopupCurrency)
            language.editText?.setText(if (prefs.languageTag.isEmpty()) "" else prefs.languageTag)
            notifications.isChecked = prefs.isAppNotifications
        }
    }

    private fun setup() = binding.apply {
        currency.editText?.doAfterTextChanged { text ->
            val c = text?.toString()?.trim()?.uppercase().orEmpty()
            // BTCPay accepts ISO 4217 currencies; keep 3-letter uppercase only
            if (Money.isValidCurrency(c)) {
                prefs.walletTopupCurrency = c
                currency.error = null
            } else if (c.isNotEmpty()) {
                currency.error = ctx.getString(R.string.wallet_currency_error)
            }
        }
        language.editText?.doAfterTextChanged { text ->
            // empty = system default; otherwise a BCP-47 tag (en, de, …)
            val tag = text?.toString()?.trim()?.lowercase().orEmpty()
            if (tag.isEmpty() || tag.matches(Regex("^[a-z]{2,3}(-[A-Za-z0-9]+)?$"))) {
                prefs.languageTag = tag
                language.error = null
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(tag),
                )
            } else if (tag.isNotEmpty()) {
                language.error = ctx.getString(R.string.wallet_language_error)
            }
        }
        notifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.isAppNotifications = isChecked
            NotificationManager(ctx).setNotificationsEnabled(isChecked)
        }
    }
}