package me.lucky.wasted.wallet

import java.math.BigDecimal
import java.util.Locale

/**
 * Amount in minor units + its normalized currency. Minor units:
 * fiat = cents, bitcoin-family = satoshis, XMR = piconero.
 * The per-currency display and BTCPay-amount knowledge lives here, nowhere else.
 */
data class Money private constructor(val minor: Long, val currency: String) {
    companion object {
        private val SAT_CURRENCIES = setOf("BTC", "XBT", "SATS")
        private val XMR_CURRENCIES = setOf("XMR")
        private val CURRENCY_CODE = Regex("^[A-Z]{3}$")

        fun isValidCurrency(code: String): Boolean {
            val cur = code.uppercase()
            return CURRENCY_CODE.matches(cur) || cur in SAT_CURRENCIES || cur in XMR_CURRENCIES
        }

        // normalizing constructor: currency case is canonical from birth, so
        // display(), invoices and ledger JSON can never leak raw input case
        operator fun invoke(minor: Long, currency: String): Money {
            val cur = currency.uppercase()
            check(CURRENCY_CODE.matches(cur) || cur in SAT_CURRENCIES || cur in XMR_CURRENCIES) {
                "currency must be ISO 4217 or a supported crypto unit: $currency"
            }
            return Money(minor, cur)
        }

        // parses user input in major units ("10.00" → 1000 minor for fiat,
        // "1000" → 1000 sats, "0.5" → 0.5 XMR in piconero). Over-precision
        // input beyond the smallest representable unit is rejected (null),
        // never truncated
        fun fromMajorString(input: String, currency: String): Money? {
            val cur = currency.uppercase()
            val text = input.trim()
            if (text.isEmpty()) return null
            val bd = text.toBigDecimalOrNull() ?: return null
            if (bd.signum() <= 0) return null
            return try {
                when {
                    cur in SAT_CURRENCIES -> Money(bd.longValueExact(), cur)
                    cur in XMR_CURRENCIES ->
                        Money(bd.movePointRight(12).longValueExact(), cur)
                    CURRENCY_CODE.matches(cur) ->
                        Money(bd.movePointRight(2).longValueExact(), cur)
                    else -> null
                }
            } catch (exc: ArithmeticException) {
                null
            }
        }
    }

    fun isSats() = currency in SAT_CURRENCIES

    fun isXmr() = currency in XMR_CURRENCIES

    // string BTCPay invoice amounts: sats as integer, XMR/fiat as major decimal
    fun toInvoiceAmount(): String = when {
        isSats() -> minor.toString()
        isXmr() -> BigDecimal(minor).movePointLeft(12).stripTrailingZeros().toPlainString()
        else -> String.format(Locale.ROOT, "%d.%02d", minor / 100, minor % 100)
    }

    fun display(): String = when {
        isSats() -> "$minor sats"
        isXmr() -> "${BigDecimal(minor).movePointLeft(12).stripTrailingZeros().toPlainString()} XMR"
        else -> String.format(Locale.ROOT, "%d.%02d %s", minor / 100, minor % 100, currency)
    }
}