package me.lucky.wasted

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

import me.lucky.wasted.wallet.Money
import me.lucky.wasted.wallet.TopupFlow
import me.lucky.wasted.wallet.WalletInvoice
import me.lucky.wasted.wallet.WalletLedger
import me.lucky.wasted.wallet.WalletStore
import me.lucky.wasted.btcpay.BtcpayApi
import me.lucky.wasted.btcpay.BtcpayInvoice

class TopupFlowTest {
    companion object {
        val FIVE_USD = Money(500, "USD")
    }

    // the second adapter proving the seam: in-memory store + scripted API
    private class FakeStore(var invoicesJson: String = "", var currency: String = "USD") :
        WalletStore {
        override var walletInvoices: String
            get() = invoicesJson
            set(v) { invoicesJson = v }
        override var walletTopupCurrency: String
            get() = currency
            set(v) { currency = v }
    }

    private class FakeApi(
        val created: MutableList<Pair<String, String>> = mutableListOf(),
        var status: String = "New",
    ) : BtcpayApi {
        var counter = 0
        override val configured = true
        override fun createInvoice(amount: String, currency: String): BtcpayInvoice {
            created += amount to currency
            counter++
            return BtcpayInvoice("id$counter", "https://pay/$counter", status)
        }
        override fun getInvoice(id: String): BtcpayInvoice {
            val idx = id.removePrefix("id").toInt()
            return BtcpayInvoice(id, "https://pay/$idx", status)
        }
    }

    private fun flow(store: WalletStore = FakeStore(), api: BtcpayApi = FakeApi()) =
        TopupFlow(api, store) to (api as FakeApi)

    @Test
    fun `createTopup records ledger and requests invoice in minor units`() {
        val (flow, _) = flow()
        val result = flow.createTopup(FIVE_USD)
        assertEquals(1, flow.ledger().invoices.size)
        assertEquals(FIVE_USD, result.invoice.amount)
        assertEquals("New", result.invoice.status)
        assertEquals("https://pay/1", result.checkoutLink)
    }

    @Test
    fun `balance derives from settled ledger only`() {
        val store = FakeStore()
        val api = FakeApi()
        val flow = TopupFlow(api, store)
        flow.createTopup(FIVE_USD)
        // unsettled: nothing derived yet
        assertEquals(Money(0, "USD"), flow.balance())
        api.status = "Settled"
        flow.refreshPending()
        assertEquals(FIVE_USD, flow.balance())
    }

    @Test
    fun `refresh settles exactly once`() {
        val api = FakeApi()
        val flow = TopupFlow(api, FakeStore())
        val result = flow.createTopup(FIVE_USD)
        api.status = "Settled"
        flow.refreshPending()
        val afterFirst = flow.balance()
        flow.refreshPending()
        assertEquals(afterFirst, flow.balance())
        assertEquals("Settled", flow.ledger().invoices.first { it.id == result.invoice.id }.status)
    }

    @Test
    fun `resetLedger clears history and derived balance`() {
        val store = FakeStore()
        val (flow, _) = flow(store)
        flow.createTopup(FIVE_USD)
        flow.refreshPending()
        flow.resetLedger()
        assertEquals(Money(0, "USD"), flow.balance())
        assertTrue(flow.ledger().invoices.isEmpty())
    }

    @Test
    fun `sats top-up passes integer amount, fiat passes major decimal`() {
        val fiatApi = FakeApi()
        val satsApi = FakeApi()
        val fiatFlow = TopupFlow(fiatApi, FakeStore())
        val satsFlow = TopupFlow(satsApi, FakeStore(currency = "SATS"))
        fiatFlow.createTopup(Money(1000, "USD"))
        satsFlow.createTopup(Money(1000, "SATS"))
        assertEquals("10.00", fiatApi.created.first().first)
        assertEquals("1000", satsApi.created.first().first)
    }
}