# Prince Ali Rescue Build 85 Review Verdict — 2026-10-01 19:15 EDT

**Verdict: PASS**

- Build: 85 | Patch: 778 | Commit: 41982cd6 ("capture fresh GE offers and widget evidence at every buyer tran…")
- Scope: read-only review (Alex owns implementation/releases). No source edited, nothing compiled, nothing uploaded.

## Custody (byte-level, verified this run)

- `patches/patch-778.zip` (blob 0302baa2…): 246 entries, root `net/`, in-zip `version.txt` = `778`.
- `patches/princealirescue-85.jar` (blob ebe3baf3…): sha256 `c6791a011991d43c7a170b897a2224d1548fb75b156658d57244fed5dbeffe21` — EXACT match to `patch-778.hot.json`.
- Script classes zip↔jar: 28/28 byte-identical. Plugin/Config as before.
- `BUILD_NUMBER` = 85 via `javap -constants`; RUNNING_BUILD log, `getBuildNumber()`, status.properties all 85 — banner honest.
- Commit 41982cd6 single-purpose (9 files).

## Semantic delta (B84 → B85; 40 changed CFR lines)

Diagnostics only — new `captureNativeGeRuntime()`:

- Runs entirely inside `Microbot.getClientThread().invoke(...)`: dumps all `GrandExchangeOffer`s (state/item/price/qty/filled/spent) plus a bounded widget-tree walk of groups 465 and 162 (top 100 children each, depth ≤ 12, cycle-guarded via `IdentityHashMap` set, text truncated to 120 chars) to `ge-native-runtime-probe.txt` (overwrite, not append).
- Triggered (a) once per NATIVE_GE_ hold (`nativeGeHoldProbed` flag) and (b) on every buyer outcome/phase transition in `nativeGeBuy`. Client-thread confinement is correct; cost is bounded (a few hundred widgets per capture).

## API / thread-safety verification

- No new game APIs. All widget reads inside the client-thread invoke; file write on the tick thread with a warned (not thrown) failure path — a failed probe write can never HOLD the bot. Correct.

## FINDINGS

- **[carried] MEDIUM — silent idle livelock in `nativeGeBuy` COMPLETE branch** (B81, unchanged).
- **[carried] MEDIUMs (all five):** poison soft-lock (B77/B79); stranded probe dumps (this build's new `ge-native-runtime-probe.txt` overwrites rather than accumulates, so it does not worsen the accumulation); bronze pickaxe 1265 never withdrawn; B70 empty-`getQuestion()` furnace HOLD; B74 reloaded-walk arrival gate. None fixed by B85.
- No new findings. No API or thread-safety defects in this delta.
