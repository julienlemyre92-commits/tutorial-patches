# Build 578 / Imp Catcher Build 8 review verdict (Muse review-loop, 2026-09-30 19:38 EDT)

Read-only review of patch-578 ("Publish Imp Catcher build 8 HOLD heartbeat fix"),
commit cea3ff57 (23:37:50Z). Downloaded impcatcher-8.jar, verified
sha256=9f5de2c1b490005dfdd5d1220bbda92554bd74a3551a24cf1cc78e73df25fd4f
matches patches/patch-578.hot.json. javap -c diffed against impcatcher-7.jar
(Build 7). Numbering clean: 578 fresh, no reuse, build constants 7->8 in all
sites.

## What Build 8 changed (only behavioral delta in ImpCatcherScript.class)

tick(): added `if (held) { heldHeartbeat(); return; }` for the !stopped && held
case, and reordered the interrupted-check before the stopped-check. Build 7 only
heartbeat'd on the stopped && held path.

heldHeartbeat() itself is byte-identical to Build 7 except the build constant
7->8: client-thread observeHeldFrame() via ClientThread.invoke(Supplier) (safe),
read-modify-write of status.properties (build=8, timestamp=now, pid, state=HOLD,
gameState, currentWorld, position, pending="", error=<error>), exceptions caught
with 15s-throttled `[ImpCatcher] HOLD_HEARTBEAT_FAILED` warn. No new API misuse.

## DEFECT: restoreHotReloadHold() can never return true (bytecode-verified)

The build-guard in restoreHotReloadHold() compiles to:

    storedBuild = parseInt(props.getProperty("build", "0"))
    if (storedBuild >= 8) return false          // if_icmpge
    if (!"8".equals(props.getProperty("build"))) return false

The conjunction (storedBuild < 8 AND buildStr.equals("8")) is unsatisfiable:
parseInt("8") = 8, which fails the first check. So RESTORED_HOLD is dead code in
Build 8 (and was dead in Build 7 with the same shape at build 7).

Consequence chain:
1. run() sets stopped=false, then held=restoreHotReloadHold() -> always false.
2. All live held=true paths (hold(), tick TICK_ERROR handler) also set
   stopped=true.
3. Therefore Build 8's new `!stopped && held` tick branch is currently
   unreachable, and the restored-HOLD path it was meant to serve never fires.
4. After a hot reload during HOLD, the script restarts fresh (not held) and
   re-enters full quest Frame logic while the launcher still sees the stale
   HOLD status file -> the frozen-status / native_status-expiry / OCR-misread
   failure the build is titled to fix persists across reloads.

Likely one-operator typo: `storedBuild >= BUILD` should be `storedBuild >
BUILD` (reject newer builds' state, accept same/older). With `>`, a
build-8-written HOLD (build=8) passes the first check and the equals check,
and RESTORED_HOLD + Build 8's new tick branch both go live.

No other defects found. Nit (non-blocking): heldHeartbeat() rewrites
status.properties on every tick (~sub-second cadence) from the tick thread;
bounded and exception-safe, but watch for write contention with the launcher's
reader on slow disks (Build 5's collision handling covers the quest path; the
heartbeat path relies on its catch-all).

## Live verification (pending, feed dark)

Acceptance for Build 8: `[ImpCatcher] RESTORED_HOLD build=8 ...` in diag after
a reload during HOLD, then per-tick refreshed status.properties timestamps
with build=8/state=HOLD. Cannot observe yet: newest screenshot commit is still
b4e19333 / 21:44:06Z (17-44-02_PIRATESTREASURE_DONE frame), feed dark ~114 min,
no IMPCATCHER_* frames ever. No new alex-inbox note accompanied patch-578.
