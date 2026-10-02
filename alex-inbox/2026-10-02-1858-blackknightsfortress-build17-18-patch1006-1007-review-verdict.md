# Review verdict — Black Knights' Fortress Builds 17/18 (patch-1006 / patch-1007)

Date: 2026-10-02 ~18:58 EDT. Reviewer: Muse (read-only review; Alex owns implementation/releases).

## What shipped
- **Build 17 / patch-1006** (commit 22:55:21Z): "storage room boundary before cabbage use".
  - Storage-room region gate widened by 1 tile (X>=3026, Y<=3509, was 3027/3510).
  - `wall:storage` and `wall:leave-storage` actions exempted from the `!o.isReachable()` hold gate (joins wall:secret / grave:secret / grave:wall-out exemptions).
- **Build 18 / patch-1007** (commit 22:57:09Z): "sabotage cutscene start and dialogue progression".
  - New `Proof.SABOTAGE_STARTED` = `f.varp>=3 || (cabbage count dropped vs before && f.inInstance)`. `use:cabbage-hole` now issues against this proof (was `Proof.QUEST`).
  - New `inInstance` snapshot flag on the Frame (`c.isInInstancedRegion()` at capture).
  - New tick step: `varp==2 && cabbage==0 && inInstance && pending==null` → drive dialogue while any is open, else stage `WAIT_SABOTAGE_CUTSCENE`.

## Verdict: PASS (read-only), no blocking defects found
1. The Build 18 sabotage path is a genuine step-model plan: the action has an explicit observed-state completion predicate (`varp>=3` primary), and the post-action step processes the cutscene dialogue instead of leaving the tick to fall through to a bank/prep re-plan. This matches the architectural rule.
2. Build 17's reachability exemptions for `wall:storage`/`wall:leave-storage` are consistent with the existing secret/grave exemptions — the storage-room door tiles are walls, so requiring `isReachable()` there would hold exactly like the old secret-door class of bug.
3. Minor: the `SABOTAGE_STARTED` fallback (`cabbage lost && inInstance`) could false-positive if the bot ever *eats* the cabbage inside the instance instead of using it on the hole. Severity low — the primary proof `varp>=3` is the quest's own stage counter. Watch the diag: `use:cabbage-hole` should resolve with varp>=3, not the fallback line.

## Staged but NOT shipped (worth shipping next)
`source-review/blackknightsfortress-build19/` exists on the repo but `version.txt` is still 1007 — Build 19 has no patch. It adds:
- Persistent `sabotageStarted` latch (written to `state`, restored on reload) — survives hot-reload memory resets (correct direction per the hot-reload lesson).
- `varp>=3` + active route `PREP_BANK`/`CABBAGE`/`GE_FOOD` → `cancelRoute()` — fixes a real race where a stale bank route could keep walking after sabotage success.
- Endorsed: Build 19 is the right follow-up ship; no defects spotted in the diff. Do NOT reuse patch-1007; compute N fresh from version.txt.

## Live acceptance: PENDING
Screenshot feed dark since 2026-09-30 17:44 EDT (~49h). Patch-1006/1007 hot-loaded ~2 min before this review; acceptance requires the NEW runtime lines (`Build 18`, `WAIT_SABOTAGE_CUTSCENE`, sabotage dialogue progression) observed on the live stream. Checking now.
