# Muse review: Corsair Curse Build 31 (patch-1075, shipped 2026-10-03 06:16:55 EDT)

Read-only review. No code touched, nothing shipped over Alex's tree.

## What changed (bytecode-verified diff patch-1074 -> patch-1075)

Only `CorsairCurseScript.class` + `CorsairCurseScript$TrainingMaintenanceProtocol*.class`
changed (482 classes in both zips, identical class list, `net/` root OK).
Build number constant bumped 30 -> 31. All remaining diff is one functional
change in `collectCowhides(Frame)` (now `throws IOException`, caller handles it):

1. **Write side** — when dispatching a Take on a ground cowhide, the script now
   writes `~/.../cowhide-pending.properties` ATOMICALLY (`.tmp` + ATOMIC_MOVE +
   REPLACE_EXISTING) with keys `account`, `before` (inv cowhide count),
   `at` (epoch ms), `target` (ground-item WorldPoint). Comment in code:
   "Take must reconcile before another dispatch".
2. **Read side** — on entry, if `cowhidePendingAt==0` and the receipt file
   exists, it restores `cowhideBefore`/`cowhidePendingAt` and validates the
   `account` key against the live frame: mismatch -> `hold("Cowhide receipt
   belongs to another account")`.
3. **Reconcile** — proved (count increased vs `before`) -> delete receipt, log
   `COWHIDE_PICKUP_PROVED`, `pickups += delta`. Unproved past 5000ms -> delete
   receipt + `finishCowhides("pickup unproved; no repeat")`. Both terminal paths
   clean up the file; a stale receipt can never wait forever.

## Verdict: PASS (mechanism sound, closes the open watch item)

This is the fix for the carried watch item "hot reload resets memory-only
flags — checkpoints must persist proofs too": the pending cowhide proof is no
longer memory-only, so a hot-reload mid-pickup no longer loses the
`cowhidePendingAt`/`cowhideBefore` state. Atomic write, account-key guard, and
TTL-bounded verify (5s) are all correct. No terminal HOLD on stale receipts.

Two minor observations (non-blocking):
- `cowhideStarted` (90s collection deadline) is NOT in the receipt; it resets
  to `now` after a reload, so the 90s window restarts once per reload. Bounded
  (~90s extra collecting max).
- The `target` WorldPoint is written but never read back — harmless metadata.

## What this build does NOT fix

RELOAD_HELD is unchanged. Nothing in patch-1075 touches the host-side
`[CorsairCurseHot]` trigger, which lives PC-side (not in these patch zips).
The host will still fire reload attempts while the training service owns input,
so expect the next reload cycle to print RELOAD_HELD again until the trigger is
serialized behind the input lease host-side (see the 06:15 note).

Acceptance for Build 31: live overlay runtime marker must read BUILD 31
(it was still BUILD 29 at 06:14-06:15). The 06:15 "Build-30-recovery" flag is
superseded — new flag: verify Build 31 hot-load + whether RELOAD_HELD persists.

Reviewed from source-review/corsaircurse-build31 + bytecode diff 1074->1075.
