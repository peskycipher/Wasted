package me.lucky.wasted

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

import me.lucky.wasted.wallet.Money
import me.lucky.wasted.wallet.WalletInvoice
import me.lucky.wasted.wallet.WalletLedger

class WalletLedgerTest {
    companion object {
        fun invoice(
            id: String,
            cents: Long = 1000,
            currency: String = "USD",
            status: String = "New",
        ) = WalletInvoice(id, Money(cents, currency), status, createdAt = 1L)

        fun ledger(vararg invoices: WalletInvoice) = WalletLedger(listOf(*invoices))
    }

    @Test
    fun `refresh settles once and is idempotent`() {
        val l = ledger(invoice("a"))
        assertTrue(l.refresh("a", "Settled"))
        assertFalse(l.refresh("a", "Settled"))
    }

    @Test
    fun `record caps at 25 newest first`() {
        val l = WalletLedger(listOf())
        (0 until 30).forEach { l.record(invoice("$it")) }
        assertEquals(25, l.invoices.size)
        assertEquals("29", l.invoices.first().id)
        assertFalse(l.invoices.any { it.id == "0" })
    }

    @Test
    fun `serialize round-trip`() {
        val l = ledger(invoice("a", status = "Settled"), invoice("b", cents = 42))
        assertEquals(l.invoices, WalletLedger.fromJson(l.serialize()).invoices)
    }

    @Test
    fun `btcpay statuses reduce to three`() {
        assertEquals("Settled", WalletLedger.reduceStatus("Settled"))
        assertEquals("Expired", WalletLedger.reduceStatus("Invalid"))
        assertEquals("Expired", WalletLedger.reduceStatus("Expired (marked)"))
        assertEquals("New", WalletLedger.reduceStatus("New"))
        assertEquals("New", WalletLedger.reduceStatus("Processing"))
    }
}