package me.lucky.wasted.wallet

import org.json.JSONArray
import org.json.JSONObject

/**
 * Pure wallet ledger: invoice records as JSON, capped. The balance is derived
 * by the caller ([TopupFlow.balance]); settled foreign-currency invoices stay
 * in history but never count towards another currency's balance.
 */
data class WalletInvoice(
    val id: String,
    val amount: Money,
    val status: String,
    val createdAt: Long,
)

class WalletLedger(var invoices: List<WalletInvoice>) {
    companion object {
        const val CAP = 25

        // invoice states, reduced from BTCPay's richer set (see reduceStatus)
        const val STATUS_SETTLED = "Settled"
        const val STATUS_NEW = "New"
        const val STATUS_EXPIRED = "Expired"

        fun fromJson(json: String?): WalletLedger {
            if (json.isNullOrBlank()) return WalletLedger(listOf())
            val arr = JSONArray(json)
            val list = (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                WalletInvoice(
                    o.getString("id"),
                    Money(o.getLong("amountMinor"), o.getString("currency")),
                    o.getString("status"),
                    o.getLong("createdAt"),
                )
            }
            return WalletLedger(list.take(CAP))
        }

        // status: Settled | Expired | New (BTCPay statuses reduced to these)
        fun reduceStatus(btcpayStatus: String): String = when {
            btcpayStatus == "Settled" -> STATUS_SETTLED
            btcpayStatus in listOf("Expired", "Invalid", "Settled (marked)", "Expired (marked)") ->
                STATUS_EXPIRED
            else -> STATUS_NEW
        }
    }

    fun serialize(): String {
        val arr = JSONArray()
        invoices.take(CAP).forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("amountMinor", it.amount.minor)
                    .put("currency", it.amount.currency)
                    .put("status", it.status)
                    .put("createdAt", it.createdAt),
            )
        }
        return arr.toString()
    }

    fun record(invoice: WalletInvoice) {
        invoices = listOf(invoice) + invoices.filterNot { it.id == invoice.id }
        invoices = invoices.take(CAP)
    }

    // applies a status refresh; returns true when this call settled the
    // invoice (settle-once), false otherwise
    fun refresh(id: String, btcpayStatus: String): Boolean {
        val reduced = reduceStatus(btcpayStatus)
        val found = invoices.find { it.id == id } ?: return false
        if (found.status == STATUS_SETTLED) return false
        if (found.status != reduced) {
            invoices = invoices.map {
                if (it.id == id) it.copy(status = reduced) else it
            }
        }
        return reduced == STATUS_SETTLED
    }
}