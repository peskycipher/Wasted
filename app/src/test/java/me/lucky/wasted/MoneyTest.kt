package me.lucky.wasted

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

import me.lucky.wasted.wallet.Money

class MoneyTest {
    @Test
    fun `fiat input converts minor units`() {
        assertEquals(Money(1000, "USD"), Money.fromMajorString("10.00", "USD"))
        assertEquals(Money(1000, "USD"), Money.fromMajorString("10", "USD"))
        assertEquals(Money(1, "USD"), Money.fromMajorString("0.01", "USD"))
        assertEquals(Money(1000, "EUR"), Money.fromMajorString("10.00", "eur"))
    }

    @Test
    fun `sats input is minor units as-is`() {
        assertEquals(Money(12345, "BTC"), Money.fromMajorString("12345", "BTC"))
        assertEquals(Money(12345, "SATS"), Money.fromMajorString("12345", "sats"))
        assertNull(Money.fromMajorString("10.5", "BTC"))
    }

    @Test
    fun `malformed input rejected`() {
        assertNull(Money.fromMajorString("", "USD"))
        assertNull(Money.fromMajorString("abc", "USD"))
        assertNull(Money.fromMajorString("-1", "USD"))
        assertNull(Money.fromMajorString("0", "USD"))
        assertNull(Money.fromMajorString("10", "XX"))
    }

    @Test
    fun `display formats fiat by cents, sats raw`() {
        assertEquals("10.00 USD", Money(1000, "USD").display())
        assertEquals("0.01 EUR", Money(1, "EUR").display())
        assertEquals("12345 sats", Money(12345, "BTC").display())
    }

    @Test
    fun `over-precision input is rejected, not truncated`() {
        // 0.5 piconero cannot exist; silently truncating to 0 loses money
        assertNull(Money.fromMajorString("0.0000000000005", "XMR"))
        assertNull(Money.fromMajorString("0.0000000000005", "xmr"))
        assertNull(Money.fromMajorString("10.999", "USD"))
        assertNull(Money.fromMajorString("10.5", "BTC"))
        // one piconero is representable
        assertEquals(Money(1, "XMR"), Money.fromMajorString("0.000000000001", "XMR"))
    }

    @Test
    fun `currency case is normalized at construction`() {
        assertEquals("USD", Money(1000, "usd").currency)
        assertEquals("USD", Money(1000, "Usd").currency)
        assertEquals("10.00 USD", Money(1000, "usd").display())
        assertEquals(Money(1000, "EUR"), Money.fromMajorString("10.00", "eur"))
    }

    @Test
    fun `xmr input converts piconero, display and invoice amounts`() {
        assertEquals(Money(500_000_000_000L, "XMR"), Money.fromMajorString("0.5", "XMR"))
        assertEquals(Money(1_000_000_000_000L, "XMR"), Money.fromMajorString("1", "xmr"))
        assertNull(Money.fromMajorString("0", "XMR"))
        assertEquals("0.5 XMR", Money(500_000_000_000L, "XMR").display())
        assertEquals("0.5", Money(500_000_000_000L, "XMR").toInvoiceAmount())
        assertEquals("1 XMR", Money(1_000_000_000_000L, "XMR").display())
    }

    @Test
    fun `invoice amounts fiat major decimal sats integer`() {
        assertEquals("10.00", Money(1000, "USD").toInvoiceAmount())
        assertEquals("0.01", Money(1, "EUR").toInvoiceAmount())
        assertEquals("12345", Money(12345, "BTC").toInvoiceAmount())
    }
}