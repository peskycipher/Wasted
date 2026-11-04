package me.lucky.wasted.wallet

import me.lucky.wasted.Preferences
import me.lucky.wasted.btcpay.BtcpayApi

// persistence seam for the wallet: Preferences implements it, tests fake it
data class TopupResult(val invoice: WalletInvoice, val checkoutLink: String)

interface WalletStore {
    var walletInvoices: String
    var walletTopupCurrency: String
}

/**
 * Money policy for the wallet, out of the Fragment: create a Top-up, refresh
 * pending invoice states, reset the ledger. Sync, testable — call from an
 * executor. The balance is always derived from the ledger, never stored.
 */
class TopupFlow(val api: BtcpayApi, private val store: WalletStore) {

    fun isConfigured(): Boolean = api.configured


    // returns the recorded invoice plus the checkout link the create call
    // already returned — no follow-up GET needed
    fun createTopup(money: Money): TopupResult {
        val invoice = api.createInvoice(money.toInvoiceAmount(), money.currency)
        val ledger = ledger()
        val created = WalletInvoice(
            id = invoice.id,
            amount = Money(money.minor, money.currency),
            status = WalletLedger.reduceStatus(invoice.status),
            createdAt = System.currentTimeMillis(),
        )
        ledger.record(created)
        persist(ledger)
        return TopupResult(created, invoice.checkoutLink)
    }

    fun refreshPending(): Int {
        val ledger = ledger()
        var changed = 0
        ledger.invoices.filter { it.status == WalletLedger.STATUS_NEW }.forEach { inv ->
            val invoice = api.getInvoice(inv.id)
            // each settle is persisted immediately: a later failure must not
            // drop it (settle-once is protected by WalletLedger.refresh)
            if (ledger.refresh(inv.id, invoice.status)) {
                changed++
                persist(ledger)
            }
        }
        return changed
    }

    fun checkoutLink(id: String): String = api.getInvoice(id).checkoutLink

    fun resetLedger() {
        store.walletInvoices = ""
    }

    fun ledger(): WalletLedger = WalletLedger.fromJson(store.walletInvoices)

    // balance currency: the ledger's own (first settled) currency, which
    // freezes at the first settled top-up, else the upcoming top-up currency
    fun balance(): Money {
        val ledger = ledger()
        val currency = ledger.invoices
            .firstOrNull { it.status == WalletLedger.STATUS_SETTLED }?.amount?.currency
            ?: store.walletTopupCurrency
        return Money(ledger.invoices
            .filter { it.status == WalletLedger.STATUS_SETTLED && it.amount.currency == currency }
            .sumOf { it.amount.minor }, currency)
    }

    private fun persist(ledger: WalletLedger) {
        store.walletInvoices = ledger.serialize()
    }
}