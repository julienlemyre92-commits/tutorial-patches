# Read-only review verdict — Below Ice Mountain Build 98 / patch-960

- Ship commit: `9eb3eb55` (2026-10-02 11:52:40Z; commit message again says "Build91 empty equipment bridge", ships 98)
- Files: `patches/belowicemountain-98.jar`, `patches/patch-960.zip`, `patches/patch-960.hot.json`, `source-review/belowicemountain-build98/`, `version.txt` 959 -> 960
- Reviewed by: Muse (read-only; Alex owns implementation/releases — no edits, no ship)
- Verdict: **PASS WITH FINDINGS** (INFO-level only, no blocking defects)

## What Build 98 changes (diff b97 -> b98 = 59 diff lines, ~35 real)

1. `BUILD_NUMBER` 97 -> 98.
2. **New one-shot stage-35 safety-logout reset** (new code ~893-902): when LOGGED_IN and
   `questStage(varp)==35` and `overworldPrepArea(position)` and `!guardianActive` and
   `!guardianActionsAllowed()`, the tick clears `safetyLogoutIssued`, sets the diagnostic
   stage label `STAGE35_SURFACE_AFTER_SAFETY_LOGOUT`, logs warn, and returns. On the next
   tick the general stage-35 overworld recovery path (bank -> food/coin rebuy -> supervised
   re-entry check) takes over. In B97 the flag cleared only via the 5-second-wait fallthrough
   later in the same block; B98 clears it immediately for the surface case and labels it.
   Conditions are tight: overworld prep area only, no active guardian session, and explicitly
   NOT in supervised guardian mode, so supervised play is unaffected.
3. **Guardian instance-canvas fallback in `startGuardianWalk`** (new code ~3452, ~3457-3461,
   ~3506-3518): inside `inKnownCaveInstance(position)`, approach-tile selection skips
   `Rs2Walker.isWalkableInCollisionMap` / `canReach` (instance collision data is unreliable on
   this mapping) while still honoring `guardianBlockedApproaches`; dispatch uses
   `Rs2Walker.walkFastCanvas(target, false)` instead of `walkStep(target, 0)`. A rejected
   canvas walk marks the target blocked, logs `GUARDIAN_INSTANCE_CANVAS_REJECTED`, sets
   `stage=GUARDIAN_APPROACH_RESCAN`, and returns. Empty approach set returns false as before
   (line 3467) — the rescan loop is bounded by blocked-set growth.

## Correctness notes

- The new `STAGE35_SURFACE_AFTER_SAFETY_LOGOUT` string is **write-only diagnostic**: the
  `stage` field is never read for control flow (only `status.properties` at line 3839 and
  log lines). The functional effect is the one-shot `safetyLogoutIssued=false`; the label
  never appears if the flag was already cleared (cosmetic only). No dangling-stage risk.
- The canvas fallback is consistent with the private-server instance mapping problem the
  B93/B94 checkpoint work already acknowledged; rejection path is bounded and logged.
- No changes to the death-recovery GE checkpoint, trout latch, guardian eat/retreat
  hardening, or supervised-entry gates from B92-B97.

## Custody (independently verified)

- `hot.json` sha256 == `belowicemountain-98.jar` sha256 (`e6fe1c7787e2c6d5d8951eab8254d8d6b8fb270bbbd545d0ae077399d5025ccf`) — MATCH
- patch-960.zip root `net/` (313 entries); in-zip `version.txt` = 960 == repo `version.txt`
- `BUILD_NUMBER=98` in shipped source AND javap bytecode `ConstantValue: int 98`
- zip `BelowIceMountainScript.class` sha256 == jar class sha256 (`671c42aa...dd1bf3`) — MATCH

## Findings (INFO)

- I1: commit message "Build91 empty equipment bridge" ships Build 98 — 7th straight stale message.
- I2: `source-review/belowicemountain-build98/README.md` still stops at Build 68 (stale since Build 1).
- I3: official `META-INF/MANIFEST.MF` again present in patch zip (benign, recurring).

## Live acceptance

Pending: screenshot feed dark since 2026-09-30 17:44 EDT (~38.2h); no confirmed live stream URL.
Accept only on new runtime lines: `STAGE35_SURFACE_AFTER_SAFETY_LOGOUT` warn on a post-safety-logout
login, `GUARDIAN_INSTANCE_CANVAS_REJECTED` / `instanceCanvasFallback=true` lines in the cave,
plus the RUNTIME BUILD 98 marker — never the banner alone.
