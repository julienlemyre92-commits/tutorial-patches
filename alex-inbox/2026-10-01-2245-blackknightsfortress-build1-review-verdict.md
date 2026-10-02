# Muse read-only review verdict: Black Knights Fortress Build 1 (patch-865)

2026-10-01 ~22:45 EDT | Reviewer: Muse (read-only; Alex owns implementation/releases)

## Custody — airtight
- hot.json (`patch-865.hot.json`): plugin=blackknightsfortress, patch=865, build=1,
  sha256=df4cbd7098d0926b96033b7d93f4abd2845d1e5849c0b122de98f70d1bdfd6e5
  == sha256 of `patches/blackknightsfortress-1.jar` — FULL MATCH.
- `patch-865.zip`: 267 entries, root is `net/` + `version.txt` (252 net classes),
  NO `runelite/client/...` one-level-deep junk — overwrite-injection safe.
- in-zip `version.txt`=865; repo `version.txt` 864→865 in the same commit.
  No version reuse, no overwrite of an existing patch-N.zip (patch-865 is new).
- `BUILD_NUMBER=1` via javap on the zipped Script class; all script classes
  byte-identical zip↔jar; genuine RuneLite Main-Class manifest.
- README plugin-jar SHA `edf9e127...5cb655c` == sha256 of
  `patches/blackknightsfortress-plugin-1.jar` — MATCHES.
- Compiled Script class contains the published source's key strings
  (PREFLIGHT_ACTIONS_DISABLED, allowActions, hot paths) — binary matches
  `source-review/blackknightsfortress-build1/BlackKnightsFortressScript.java`.
- Single-purpose commit (a936c404b0, "Black Knights Fortress Build1 candidate").

## What Build 1 is
Self-declared build-only, untested candidate (README says so explicitly).
QuestHelper-derived route, varp 130 stages 0–3: start at Sir Amik → grill
Listen-at → cabbage in hole → return to Amik. QP preflight ≥12 on varp 0.
Disguise: bronze med helm + iron chainbody. Ordinary cabbage only
(`ItemID.CABBAGE`=1965, distinct from Draynor magic cabbage 1967 — correct).
Prep: bank once (deposit excess, withdraw gear/food/cabbage/coins), buy from
Peksa/Wayne if bank is empty, loot monastery cabbage; food unmet = HOLD.
Arming gate: `allowActions` default false; `armed()` needs approvedPid,
approvedBuild, approved class-SHA — plus QuestState.FINISHED immediate stop.
Plugin descriptor `enabledByDefault=false`; exclusivity gate vs 16 sibling
quest plugins; script-only hot-reload host with SHA verification; quiescence
only at action boundaries. Status written atomically (tmp+move), deleted on
shutdown; write failures are warn-only (same surface as MM B63-2, bounded).
Tick proof model: every action gets a bounded pending-proof (3 attempts →
HOLD), worker-thread `walkWithStateUntil` with 15s/45s stall detection,
death hold, low-HP retreat = terminal safe HOLD (food acquisition not
implemented — README documents this).

## Findings
- BKF1-1 LOW: retry/failure counters and in-flight flags are memory-only.
  `quiesceForReload()` persists only prepDone/bankChecked — failure counts
  reset across a hot reload (same class as MM B73-1/B76-1). A reload during a
  failing sequence re-arms the full 3-attempt budget.
- BKF1-2 LOW: `issue()` captures the action's boolean return (`accepted`) but
  never uses it — a rejected dispatch still creates a pending proof. Bounded
  retry covers it, but the ACTION log line may mislead (`accepted=false`
  masked).
- BKF1-3 LOW: same-plane door MOVED proofs rely on expected destination tiles
  (fortress door → (3018,3515,0), secret walls, ladders). README flags these as
  unproven on the private server; live scene evidence may require adjustment
  before arming.
- BKF1-4 INFO: README names the script jar `BlackKnightsFortress-script-1.jar`;
  the repo file is `blackknightsfortress-1.jar` (SHA matches — naming drift
  only).
- BKF1-5 INFO: whole route untested (README: "build-only, untested" + explicit
  first-live-check protocol before arming). Account QP ≥19 (verified) clears the
  12-QP gate under any Ernest/Misthalin-provenance reading. Arming must follow
  the README's locked first-live-check; do not arm from this review alone.

## Verdict: PASS WITH FINDINGS (read-only)
Custody airtight, logic reviewed against the QuestHelper model and the
step-predicate architecture rules. No defects that must block a future live
preflight — but the route is UNTESTED; the arming gate and first-live-check
protocol in the README are the acceptance path, not this review. No shipping
action taken (reviewer is read-only on Alex's builds).

Live acceptance pending: RUNNING_BUILD=1 banner, allowActions arming, first
preflight status, varp 130 progression 0→1 observed in-game.
