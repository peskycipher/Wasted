# Wallet uses the operator-owned BTCPay server only

## Status

Accepted (2026-11-04) — **Removed 2026-11-04**: the wallet, settings screens and store doorway feature set was reverted from the app by operator decision. This ADR records why the feature set must not be re-added without its own decision; the boundary rules (never call pikasim.com's API; supplier access contractual; fulfilment disclosure) remain binding for any future storefront.

## Context

The merged wallet/checkout behaviour was inspected from the Pikasim APK, whose
client features (orders, eSIM linking, Stripe/BTCPay checkout, support, push)
are backed by `pikasim.com`'s private API. The operator of this fork does not
control that service; the operator **does** control their own BTCPay Server
instance.

## Decision

The in-app wallet only ever talks to **the operator's own BTCPay Server** via
the Greenfield API, configured at build time through uncommitted Gradle
properties `BTCPAY_URL`, `BTCPAY_STORE_ID`, `BTCPAY_API_KEY` (empty defaults,
`BuildConfig` fields). `me.lucky.wasted.btcpay.BtcpayClient` is the only
network caller for payments. The `eSIM Store` navigation item is a doorway to
the public web pages (browser intent), not an API client.

Balance is a local one-way ledger: settled invoices in the balance currency
sum into it (settle-once, in `wallet.WalletLedger`); foreign-currency invoices
appear in history only. Invoice history is a capped JSON array in prefs.

## Consequences

- Purchasing eSIMs with wallet balance is impossible until some backend owns
  orders; the wallet is top-up + history only.
- No calls to `pikasim.com`'s API from this app, ever (enforced by review; the
  `check-standards` script guards destructive DPM ops, this boundary is a
  documented convention).
- Currency switching clears balance/history instead of converting (no fx
  backend).
