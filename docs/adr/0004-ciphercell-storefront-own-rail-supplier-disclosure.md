# CipherCell storefront: own payment rail, disclosed fulfilment

## Status

Accepted (2026-11-04)

## Context

An eSIM store within the app was requested, with Monero collection and a
purchasing layer that forwards customer funds to an unnamed third-party
supplier whose commerce API the operator does not own. Reversing that
supplier's client to automate purchases while presenting the app as the only
counterparty was rejected earlier (ADR 0003: no pikasim.com API, ever; its
checkout is an unauthorized payment surface from this app).

## Decision

The store front is branded **CipherCell** and the payment rail is the
operator-owned BTCPay Server (Bitcoin, Lightning, and the Monero plugin —
currency `XMR`). Customer payments collected in the app are invoices on that
server; the wallet balance derives from them (ADR 0003 unchanged).

Two binding rules:

1. **Fulfilment disclosure.** The moment any purchase (e.g. an eSIM) is
   fulfilled by a third party through this app, the checkout screen names that
   supplier in plain terms. Branding the app as the only counterparty while a
   third party fulfils is rejected as a design goal and must not be built.
2. **Supplier access is contractual, not reverse-engineered.** Purchasing from
   any eSIM supplier happens through their official reseller/affiliate API
   under an agreement, or not at all.

## Consequences

- `wallet/Money` supports XMR (piconero minor units) so customers can pay in
  Monero through the operator's BTCPay checkout.
- Until a supplier agreement exists, the app sells nothing — wallet is
  top-up + history only (ADR 0003 consequence unchanged).
- The rejected design (XMR collection + unnamed forwarding + concealed
  counterparty) must not be re-suggested by future architecture reviews.
