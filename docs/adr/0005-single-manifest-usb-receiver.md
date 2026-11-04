# USB trigger: one manifest receiver, no in-service receiver

## Status

Accepted (2026-11-04)

## Context

Upstream shipped two USB receivers: `trigger/usb/UsbReceiver` (manifest,
DEVICE/ACCESSORY_ATTACHED, lock-gated, direct-boot-aware) and a nested
`UsbReceiver` inside `ForegroundService` (runtime-registered for USB_STATE,
lock-gated). With the USB trigger on, one physical plug delivers both `USB_STATE`
and `DEVICE_ATTACHED`, so a single attach **fired the trigger twice** —
double recasts and two wipe attempts.

Merging the events into the manifest receiver looked naive: `USB_STATE` is a
restricted implicit broadcast (not manifest-deliverable on apps targeting
API 26+), which is why the runtime registration existed. But `USB_DEVICE_ATTACHED`
and `USB_ACCESSORY_ATTACHED` **are** exempt from that ban, are delivered
direct-boot-aware, and cover the anti-theft scenario (locked device + flash
drive) — the in-service receiver added only a second fire path.

## Decision

The manifest receiver is the only USB fire path. `ForegroundService` no longer
registers a USB receiver; it no longer appears in the registry or desired
state. The reconciler passes the remaining FGS receiver set (LOCK, AIRPLANE)
as intent extras (`EXTRA_REGISTER_LOCK`/`EXTRA_REGISTER_AIRPLANE`); sticky
restarts of the service re-derive it from prefs (null intent).

Lock-job scheduling retries are capped (5 attempts, exponential backoff, loud
give-up in logs — a refused schedule means the wipe-on-lock trigger is dead).

## Consequences

- Adding a USB-plug trigger no longer requires the ForegroundService to be alive.
- Future triggers must not re-add an in-service USB receiver; if a new event
  is restricted from manifest delivery, runtime registration with a single
  fire path and an explicit dedupe decision is required (record it then).
