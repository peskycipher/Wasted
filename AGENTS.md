## Agent skills

### Issue tracker

Issues live in GitHub Issues on `peskycipher/Wasted` (this is a fork of `x13a/Wasted` — always pass `-R peskycipher/Wasted` to `gh`). See `docs/agents/issue-tracker.md`.

### Triage labels

Default five-role vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `GLOSSARY.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.
### Building & testing

JDK version trap (AGP 7.x rejects Java 21), stale AVD SDK root, emulator boot + GPS spoofing, signing: see `docs/agents/building.md`.

### Coding standards

Judgement rules for reviewers: `CODING_STANDARDS.md`. Mechanical rules run in CI (`.github/workflows/android.yml` → `scripts/check-standards.sh`).
