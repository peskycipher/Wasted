package me.lucky.wasted.fragment

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

import me.lucky.wasted.Preferences
import me.lucky.wasted.STORE_URL
import me.lucky.wasted.R
import me.lucky.wasted.btcpay.BtcpayClient
import me.lucky.wasted.btcpay.BtcpayApi
import me.lucky.wasted.databinding.DialogWalletTopupBinding
import me.lucky.wasted.databinding.FragmentWalletBinding
import me.lucky.wasted.databinding.ItemWalletInvoiceBinding
import me.lucky.wasted.wallet.Money
import me.lucky.wasted.wallet.TopupFlow
import me.lucky.wasted.wallet.WalletLedger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

// layout: variant C of wallet-ui-PROTOTYPE.html (branch proto/wallet-ui-variants)
class WalletFragment : Fragment() {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val statusColor = mapOf(
        WalletLedger.STATUS_SETTLED to Color.parseColor("#1b5e20"),
        WalletLedger.STATUS_NEW to Color.parseColor("#b58a2e"),
        WalletLedger.STATUS_EXPIRED to Color.parseColor("#b3261e"),
    )
    private lateinit var binding: FragmentWalletBinding
    private lateinit var ctx: Context
    private lateinit var prefs: Preferences
    private lateinit var flow: TopupFlow

    // one worker: invoice creation and refresh happen in order, no races
    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r).apply { isDaemon = true }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentWalletBinding.inflate(inflater, container, false)
        ctx = requireContext()
        prefs = Preferences(ctx)
        flow = TopupFlow(BtcpayClient(BtcpayClient.fromBuildConfig()), prefs)
        init()
        setup()
        return binding.root
    }

    override fun onDestroyView() {
        executor.shutdown()
        super.onDestroyView()
    }

    private fun init() {
        if (!flow.isConfigured()) {
            binding.topupCard.isEnabled = false
            binding.refreshCard.isEnabled = false
            binding.topupCard.alpha = 0.5f
            binding.refreshCard.alpha = 0.5f
        }
        render()
    }

    private fun setup() = binding.apply {
        topupCard.setOnClickListener { onTopup() }
        storeCard.setOnClickListener {
            // doorway to the public web storefront — no API is called (ADR 0003)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(STORE_URL)))
        }
        refreshCard.setOnClickListener { onRefresh() }
        whatsThisCard.setOnClickListener { showWhatsThis() }
    }

    private fun render() {
        binding.balance.text = flow.balance().display()
        binding.pendingCount.text =
            flow.ledger().invoices.count { it.status == WalletLedger.STATUS_NEW }.toString()

        val history = binding.history
        history.removeAllViews()
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val statusColor = mapOf(
            WalletLedger.STATUS_SETTLED to Color.parseColor("#1b5e20"),
            WalletLedger.STATUS_NEW to Color.parseColor("#b58a2e"),
            WalletLedger.STATUS_EXPIRED to Color.parseColor("#b3261e"),
        )
        flow.ledger().invoices.chunked(2).forEach { pair ->
            val row = LinearLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                orientation = LinearLayout.HORIZONTAL
            }
            pair.forEach { inv ->
                val item = ItemWalletInvoiceBinding.inflate(layoutInflater, row, false)
                item.apply {
                    root.layoutParams = LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f,
                    )
                    subtitle.text = "…${inv.id.takeLast(6)}"
                    status.text = inv.status
                    status.setTextColor(statusColor[inv.status] ?: Color.GRAY)
                    title.text = inv.amount.display()
                    date.text = fmt.format(Date(inv.createdAt))
                }
                row.addView(item.root)
            }
            history.addView(row)
        }
    }

    private fun onTopup() {
        val currency = prefs.walletTopupCurrency
        // switching currency mid-life would strand settled invoices in the old
        // denomination (no conversion exists): require a clean ledger
        if (flow.requiresLedgerReset(currency)) {
            MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.wallet_currency_switch_title)
                .setMessage(
                    getString(
                        R.string.wallet_currency_switch_message,
                        flow.balance().currency,
                        currency,
                    ),
                )
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    flow.resetCurrency(currency)
                    render()
                    showTopupDialog(currency)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()
            return
        }
        showTopupDialog(currency)
    }

    private fun showTopupDialog(currency: String) {
        val view = layoutInflater.inflate(R.layout.dialog_wallet_topup, null)
        val amount = view.findViewById<TextInputLayout>(R.id.dialogAmount)
        val dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle(getString(R.string.wallet_dialog_title, currency))
            .setView(view)
            .setPositiveButton(R.string.wallet_topup) { _, _ -> }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val money = Money.fromMajorString(
                amount.editText?.text?.toString().orEmpty(),
                currency,
            )
            if (money == null) {
                toast(R.string.wallet_amount_error)
                return@setOnClickListener
            }
            dialog.dismiss()
            toast(R.string.wallet_open_checkout)
            executor.execute {
                try {
                    val result = flow.createTopup(money)
                    if (result.checkoutLink.isNotEmpty()) {
                        activity?.runOnUiThread {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.checkoutLink)))
                        }
                    }
                } catch (exc: Exception) {
                    activity?.runOnUiThread { toast(R.string.wallet_topup_failed) }
                }
                activity?.runOnUiThread { render() }
            }
        }
    }

    private fun showWhatsThis() {
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.wallet_card_whats_this)
            .setMessage(R.string.wallet_whats_this_message)
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .show()
    }

    private fun onRefresh() {
        executor.execute {
            try {
                flow.refreshPending()
            } catch (exc: Exception) {
                activity?.runOnUiThread { toast(R.string.wallet_topup_failed) }
            }
            activity?.runOnUiThread { render() }
        }
    }

    private fun toast(res: Int) =
        Toast.makeText(ctx, res, Toast.LENGTH_SHORT).show()

}