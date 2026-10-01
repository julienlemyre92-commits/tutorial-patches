# Prince Ali Rescue Build 86 Review Verdict — 2026-10-01 19:15 EDT

**Verdict: PASS**

- Build: 86 | Patch: 779 | Commit: 596159a0 ("select exact cooked Trout from verified live search results group162 chil…")
- Scope: read-only review (Alex owns implementation/releases). No source edited, nothing compiled, nothing uploaded.

## Custody (byte-level, verified this run)

- `patches/patch-779.zip` (blob 97dc02eb…): 246 entries, root `net/`, in-zip `version.txt` = `779`.
- `patches/princealirescue-86.jar` (blob 7e19a710…): sha256 `71aacccfa9aafc22da57b4a1df9ae36825c9cad49a4bc2a7d974af6c9490b9b0` — EXACT match to `patch-779.hot.json`.
- Script classes zip↔jar: 28/28 byte-identical. Plugin/Config as before.
- `BUILD_NUMBER` = 86 via `javap -constants`; RUNNING_BUILD log, `getBuildNumber()`, status.properties all 86 — banner honest.
- Commit 596159a0 single-purpose (9 files).

## Semantic delta (B85 → B86; 98 changed CFR lines)

1. `frame.searchRoot`: `client.getWidget(162, 52)` → `client.getWidget(162, 53)` — the live-verified GE search-results container (per commit message, "group162 child53"). `exactResult` itself is unchanged: it scans `searchRoot`'s children for a visible widget whose cleaned text exactly equals "trout" (case-insensitive, tag-stripped — the exact-match discipline, no `contains()`), and clicks the sibling widget before it (the result icon). `searchPromptWidget` was already (162, 53); now both reads target the same verified container.
2. Startup recovery for the exact failure this fixes: if held on `NATIVE_GE_BUY` with error containing `reason=exact item search result absent` and checkpoint prefix `GE2|333|4|1000|HOLD|29|0|0|-1|` (price=29, slot=0, initialItem=0, coinsAfterPlace=-1 — a claimed slot with the search stuck), the checkpoint is surgically rewritten (`HOLD`→`WAIT_RESULT`, fresh `phaseAt`), the in-memory buyer is discarded, and the HOLD clears. On the next tick the buyer is reconstructed from the mutated checkpoint and resumes waiting for search results against the corrected (162,53) container — a legitimate transient-state recovery, not a blind retry (slot-0 claim and all proofs are preserved and re-verified by the buyer's own phase guards).

## API / thread-safety verification

- No new APIs. The recovery mutates only the checkpoint string and buyer reference on the script's restore path (single-threaded at startup), then the normal client-thread-safe buyer path takes over. Index use verified against `checkpoint()` field order: [4]=phase, [10]=phaseAt ✓; `restore()` re-validates the 20-field format and item/qty/cap binding.
- Exact-string matching (`clean(text).equals(clean("Trout"))`) avoids the substring trap (e.g. "Raw trout" would not match) — consistent with the codebase's exact-item discipline.

## FINDINGS

- **[carried] MEDIUM — silent idle livelock in `nativeGeBuy` COMPLETE branch** (B81, unchanged): `frame.count(333) < sourceGoal` → bare `return`, no HOLD/log/state change; next tick repeats forever. Low reachability (buyer verifies the inventory delta at WAIT_SLOT_CLEAR), total silence on hit.
- **[carried] MEDIUMs (all five):** poison soft-lock (B77/B79); stranded probe dumps; bronze pickaxe 1265 never withdrawn; B70 empty-`getQuestion()` furnace HOLD; B74 reloaded-walk arrival gate. None touched by B86.
- No new findings. No API or thread-safety defects in this delta.
