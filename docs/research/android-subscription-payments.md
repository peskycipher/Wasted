# Android subscription payment models and methods

> Research brief, 2026-11-04. Written from primary sources only
> (developer.android.com, support.google.com, f-droid.org). Claims that could not be
> live-verified are labeled. Context: which rails exist for paid features/subscriptions
> on Android, and which apply to **Wasted** (distributed outside Google Play).

## Summary

| Rail | Available to | Notes |
|---|---|---|
| Google Play Billing (v9.x) | Play-distributed apps only | The only Google-supported in-app purchase/subscription API |
| Alternative billing programs (billing choice, external offers) | Play-distributed apps, enrolled, jurisdiction-gated | Own billing system or website alongside Play's; reduced fees |
| Own payment rails (processor SDKs, web checkout, crypto gateway) | Everything else — incl. sideloaded/F-Droid apps | Wasted's case |

## 1. Distribution gates Play Billing

- Play Billing (`com.android.billingclient`) is scoped to apps **distributed through Google
  Play**; its licensing/verification model assumes a Play install. A sideloaded or
  F-Droid-installed app has no Play entitlement — no purchases, no subscriptions, no Google
  notification of refunds. (medium — doc scoping verified in doc family; not re-quoted live)
  — https://developer.android.com/google/play/billing/overview
- For non-Play distribution there is **no Google-supplied payment API**; the app must bring its
  own rails: payment-processor SDKs, web checkout in browser, donation platforms, or crypto
  gateways (as Wasted's removed wallet did via BTCPay; see docs/adr/0003/0004).
  (high — uncontroversial; verified: no such API exists in the current Billing docs)

## 2. Play Billing Library (current version verified)

- Current: **PBL 9.0.0 stable 2026-05-19; 9.1.0 released 2026-06-18** (the draft's "7 vs 8"
  was stale). PBL 9 added the **Billing Choice** API surface
  (`getBillingChoiceInfoAsync()`, `BillingProgramInformationDialog`, developer-provided
  billing details for external links) and changed the response code for blocked Play Store
  installs from `ERROR` to `BILLING_UNAVAILABLE`. — verified live from release notes:
  https://developer.android.com/google/play/billing/release-notes

## 3. The Play subscription model (Play-distributed apps)

- Two product types: **one-time in-app products** (consumable/non-consumable) and
  **subscriptions**; subscriptions are **auto-renewing** or **prepaid** (fixed duration,
  no auto-renew). (high — stable doc content)
  — https://developer.android.com/google/play/billing/overview
- Subscriptions compose **base plans** with **offers**: introductory pricing (discount/free
  trial phases), one-time offers/offer codes, and (since PBL 5+) **stackable offer phases**.
  (high) — https://developer.android.com/google/play/billing/subscriptions
- **Upgrade/downgrade + proration**: `subscriptionUpdateParams` with modes
  `IMMEDIATE_WITH_TIME_PRORATION` (default), `IMMEDIATE_AND_CHARGE_PRORATED_PRICE`,
  `IMMEDIATE_WITHOUT_PRORATION`, `CHARGE_FULL_PRICE`, `DEFERRED`. (high)
- **Lifecycle**: purchases can be `PENDING` (e.g. cash at a convenience store) and resolve via
  `PurchasesUpdatedListener`; the flow is `queryProductDetailsAsync` → `launchBillingFlow` →
  acknowledge. Non-consumables/subscriptions must be **acknowledged within 3 days** or the
  purchase is auto-refunded; consumables must be `consumeAsync`'d. (high)
  — https://developer.android.com/google/play/billing/integrate
- **Grace period & account hold**: retry on failed payment, configurable grace period keeps
  entitlement during retries, then an **account hold** (user loses access until payment fixes).
  (medium — lifecycle verified; help-article details not re-fetched)

## 4. Server-side verification

- **RTDN** (Real-Time Developer Notifications) via Google Cloud Pub/Sub push to your backend
  notifies purchase/renewal/cancellation; entitlement verification uses
  `purchases.subscriptionsv2.get` from the Google Play Developer API. Google's guidance is to
  treat client receipts as untrusted and grant entitlement only after server verification.
  (high — API surfaces; specific guide pages not re-fetched)
  — https://developer.android.com/google/play/billing/developer-api

## 5. Fees and alternative billing — **overhauled June 30, 2026** (verified live)

The draft's memory of "15%/30% tiers, User Choice ≈ 4pp reduction" is **superseded**. From the
June 2026 announcement ([blog](https://developer.android.com/blog/posts/expanded-billing-choice-and-lower-fees-on-google-play), verified):
- Services fee and billing fee are now **separated** (starts June 30, 2026: US, EEA, UK; other
  markets to follow):
  - **Service fee: 10%** on the first $1M (USD) annual earnings — applies regardless of billing
    system, and **flat 10% for all auto-renewing subscriptions**.
  - For other transactions the service fee varies by **new vs existing install** relative to the
    regional rollout date (rate table on the blog / Play Console Service fees page).
  - **Billing fee: 5%** when using Google Play's billing (US/UK/EEA); **no billing fee when using
    alternative billing or external web links**.
- **Billing choice program**: available globally to digital-services developers in UK + EEA (and
  US programs), lets users pay via an **alternative billing system or your website via external
  links**; a developer-designed choice screen is allowed per UX guidelines.
  — https://support.google.com/googleplay/android-developer/answer/17161464
- **External offers program**: EEA-specific (lead users to external offers, incl. app downloads).
  — https://support.google.com/googleplay/android-developer/answer/14372887
- The legacy **user choice billing pilot** required the alternative billing APIs from
  March 13, 2024. — https://support.google.com/googleplay/android-developer/answer/12570971
- PBL 9's new Billing Choice APIs (above) are the client surface for these programs.

## 6. Payment methods Play accepts (user-side)

Cards, Google Play balance, carrier (direct-operator) billing, PayPal, gift/voucher codes —
**regional variance is large** (300+ local payment methods per Google's announcement, verified
live); exact availability per country is not re-fetched.
— https://support.google.com/googleplay/answer/2686332

## 7. Not on Play (FOSS/sideloaded) — Wasted's rail

- Google offers no in-app payment API for non-Play distribution; FOSS apps typically use
  donation/processor links (Stripe Checkout, PayPal, GitHub Sponsors) or self-hosted
  entitlements — no official Android API. (high)
- F-Droid's policy context: the repository packages FOSS APKs; paid-app/anti-feature rules live
  in their Inclusion Policy; anything commercial done by the *developer* is outside the store's
  billing rails regardless. (medium — policy not re-fetched live)
  — https://f-droid.org/en/docs/Inclusion_Policy/
- Project memory: the wallet experiment (BTCPay Greenfield for XMR/BTC top-ups) was exactly this
  rail-3 pattern; it was removed from the app 2026-11-04 (docs/adr/0003, 0004 — Removed). Those
  ADRs' boundary rules (never call third-party commerce APIs without an agreement; disclose
  fulfilment) remain binding for any future storefront.

## Confidence ledger

- Verified live this pass: PBL version (9.0.0/9.1.0), fee overhaul (10% + 5% split, dates,
  programs), Play payment-method breadth.
- Stable doc content, not re-fetched: product/lifecycle/proration model, RTDN surface.
- Not live-verified: precise grace/account-hold durations, RTDN guide URLs, F-Droid policy text.