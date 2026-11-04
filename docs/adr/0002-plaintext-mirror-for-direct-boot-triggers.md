# Plaintext device-protected mirror of preferences

## Status

Accepted (2026-11-04)

## Context

Runtime preferences are stored in `EncryptedSharedPreferences` (credential-encrypted
storage). However, the trigger components that matter most — the unlock-code
`BroadcastReceiver`, the USB receiver, and the lock countdown — are
`directBootAware` and must work pre-unlock, at the very moment (device rebooted
and repossessed) when the wipe is needed. Pre-unlock, the credential-encrypted
file cannot be read.

## Decision

`Preferences` keeps a second copy of every changed key — including the `secret`
unlock code — in a **plaintext, device-protected** `SharedPreferences` file,
maintained automatically by the class itself (every write to an encrypted
instance is mirrored). `Preferences.new()` returns the encrypted instance when
the user is unlocked and the device-protected mirror when locked, so all
direct-boot consumers read one interface without knowing which file backs it.

The plaintext mirror of the secret is deliberate and load-bearing: excluding it
would break pre-unlock firing, i.e. the wipe at boot. Device-protected storage
already provides the platform's threat model for pre-unlock operation
(sandboxed to the app, on encrypted userdata, erased by `wipeData` itself).

## Consequences

- Wipe works before first unlock with no network and no UI.
- The secret exists in plaintext in app-private device-protected storage — an
  accepted trade, revisit only if direct-boot wiping is dropped.
- The mirroring is an invariant of `Preferences`, not of its callers; screens
  must not construct a second device-protected instance (enforced by
  `copyTo`/listener choreography being deleted from fragments).