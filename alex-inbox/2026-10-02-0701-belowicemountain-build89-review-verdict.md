# Below Ice Mountain Build 89 — read-only review verdict

- **Build:** 89 / patch-950 (commit aa91bd72, "Below Ice Mountain Build89 startup client-thread wait", 2026-10-02T10:58:24Z)
- **Reviewed:** 2026-10-02 ~07:01 EDT by review-loop (Muse, read-only — Alex owns implementation/releases)
- **Verdict:** PASS WITH FINDINGS (no blocking defects)

## Custody chain — AIR TIGHT
- version.txt=950 at observe (API re-read; repo HEAD 4c973c79 = own B88 seen.log update 10:58:54Z; zero commits since — no sibling race).
- patch-950.zip: 294 files, `net/`-rooted (+ META-INF/MANIFEST.MF, root version.txt — same manifest pattern as patches 936+). In-zip version.txt=950.
- Standalone jar belowicemountain-89.jar sha256 `676364daf3a845a4560ce3b4a23659f664842846ff05f9336300c25b7df63b11` == patch-950.hot.json sha256 — FULL MATCH (git-blob raw downloads).
- In-zip BelowIceMountainScript.class == standalone jar's class (identical sha256 fe466f7b...).
- BUILD_NUMBER javap-verified: 89 (ConstantValue int 89).
- Published source-review/ script byte-compiles on JDK 17.0.20.1 with ONLY the pre-existing error set (39 errors; import-block "cannot find symbol" + BelowIceMountainConfig symbols at b88:428/727, carried B71→B88 — symbol set IDENTICAL to B88). Zero new compile errors.
- README byte-identical to B88 (50297 bytes, no B89 section — carried INFO pattern).

## Delta B88→B89 (+13/−5 lines; script 234236→234729 bytes)
1. BUILD_NUMBER 88→89.
2. New field `private long clientThreadUnavailableAt;` (line 450).
3. After a successful `observe()` (line 778): `clientThreadUnavailableAt=0;` — latch re-derived on every good observation.
4. In tick()'s catch (lines 1082-1091): if `f==null && String.valueOf(ex.getMessage()).contains("Timed out waiting for client thread")`: latch first occurrence, and while under 30s → `stage="WAIT_CLIENT_THREAD_STARTUP"; return;`. After 30s of consecutive timeouts, falls through to `hold("Tick exception: ...")` + log.error — bounded and honest.

## Why it passes
- **The `f==null` gate is exact.** `Frame f` is tick-local, initialized `null` and assigned only by `observe()` inside the try — so `f==null` in the catch unambiguously means observe() threw before returning a Frame. No stale-field false positives.
- **Bounded by construction:** 30s quiet window, then the honest hold path. The memory-only latch is reset on every successful observe and is re-derived — a hot reload just restarts the window; benign.
- **The early return cannot freeze the status file.** It sits inside the catch, but `finally { writeStatus(f); }` still runs; writeStatus is null-safe (gameState=UNKNOWN, world/position UNKNOWN, fresh timestamp, stage=WAIT_CLIENT_THREAD_STARTUP). The launcher keeps seeing fresh timestamps, so the startup wait does NOT trigger the known OCR-misread-from-expired-status failure.
- **It fixes real startup noise:** before B89, every client-thread timeout at startup issued hold("Tick exception: ...Timed out waiting for client thread") per tick. Now the first 30s is a quiet, named wait stage.

## Findings
- **NEW INFO BIM89-1:** the gate keys off the exception MESSAGE substring "Timed out waiting for client thread". If Microbot rewords that message, the wait silently degrades to per-tick hold noise. Consider also matching on the exception type, or logging when the generic hold path is taken. Benign today.
- **NEW INFO BIM89-2:** during the wait window the `error` status property keeps its stale value while gameState=UNKNOWN — consumers should read `stage` (WAIT_CLIENT_THREAD_STARTUP), not `error`, during startup. Benign.
- **NEW INFO BIM89-3:** README byte-identical to B88, no B89 section — carried doc pattern, not a defect.
- **Carried:** BIM88-1 (line-1004 prep-audit guard loosened at bank floor, benign), BIM88-2 (README no B88 section), BIM87-1 (no-food hold terminal/unbounded, safe bank, honest msg), BIM87-2 (README no B87 section), BIM86-1/86-2, 85-1/85-2, 84-1 (mooted), 83-1/83-2, 82-1/82-2, 81-1/81-2, 78-1, 79-1, 75-1, 77-1, 71-1, 71-3..6, 61-1, 57-1, 68-1, 70-1/70-2, 72-1, 53-1.

## Live acceptance watch-keys (pending — feed dark since 2026-09-30 17:44 EDT)
- RUNTIME BUILD 89 marker on a fresh client.
- `WAIT_CLIENT_THREAD_STARTUP` stage lines in the diag during startup, then clean observe() and normal stage resumption (latch reset log / no hold spam).
- No `hold("Tick exception: ...Timed out waiting for client thread")` during the first 30s of a startup timeout.
- No live observation yet; B57–B89 live acceptance all pending, folded into the outstanding 03:21 dark-feed stream flag.
