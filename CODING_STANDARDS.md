# Coding standards

Two mechanical rules are enforced by `scripts/check-standards.sh`, which CI runs before the build. This file holds the judgement rules a reviewer applies by hand: nothing here could be a grep.

## Reviewer rules

1. **`registerForActivityResult` only at construction.** The registry must be registered before `onStart` (fragment or its field-init'd helper object, e.g. `permission/LocationPermission.kt`); launching is separate (`ensure`/`launch`). Flag any registration inside `onCreateView`/callback code, and any duplicated launcher pair that could live in a shared helper instead of being copy-pasted into another fragment.

2. **When an invariant moves into a class, enumerate its consumers' bootstraps.** Moving an invariant (e.g. "plaintext mirror exists" into `Preferences`, ADR 0002) means every caller that used to perform part of the choreography — including one-time bulk initialization, not just per-write work — must be accounted for in the review. The mirror-bootstrap regression (existing installs' pre-existing keys never mirrored) was exactly this class of miss: the delete-half was done, the bootstrap-half wasn't.

3. **Claim-vs-diff audit.** For each commit reviewed, verify its message's claims against the actual diff — a stated refactor/deletion must really appear in the changed files. Found necessary here: a commit once claimed a module fold that a silently-failed find/replace never applied (2026-11-04).

## Naming

Domain nouns come from `GLOSSARY.md` (trigger, fire, wipe, recast, police-station fence, dwell, fix). New review-facing concepts use the codebase-design vocabulary (module, interface, deep/shallow, seam, adapter, leverage, locality) — see docs/adr/ for settled decisions; reviewer candidates must not contradict an ADR.