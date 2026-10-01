# Review verdict: Imp Catcher Build 23 (patch-593) + Build 24 (patch-594) — PASS, no blocking defects

Filed 2026-09-30 ~20:28 EDT (Muse review-loop, read-only review; Alex owns implementation/releases).

## Artifacts (both builds download-verified via raw.githubusercontent)
- Build 23 / patch-593 (commit 7811ffc, 20:23:00 EDT, "Build23: verify imp attack against same NPC"):
  hot.json {plugin:impcatcher, patch:593, hostVersion:1, build:23,
  sha256:8a27b7dd9a2b2031bedd2831b6ac93cb443cf3862c8242fe7da93693e9a93281}
  EXACT MATCH impcatcher-23.jar (21252 bytes). version.txt (API) = 593, version.txt inside
  patch-593.zip = "593". patch-593.zip: 729887 bytes, 199 files, root net/ correct
  (only META-INF/, MANIFEST.MF, version.txt outside — expected). Numbering 592->593 clean.
- Build 24 / patch-594 (commit 7d4cdc9, 20:25:46 EDT, "Build24: bound post-combat drop wait and
  report target evidence"): hot.json {build:24, patch:594,
  sha256:5baa0def13979f05dc675bc20505ab3ef29a424bdb0552025fa28b142106a5db}
  EXACT MATCH impcatcher-24.jar (22159 bytes). version.txt (API) = 594, in-zip version.txt = 594.
  patch-594.zip: 199 files, root net/ correct. Numbering 593->594 clean. Both zips overlay-safe.

## Bytecode diff vs previous build (javap -c)
- 22->23: only ImpCatcherScript + Frame/Pending/HeldFrame inner classes changed
  (HeldFrame fields identical — constant-pool churn only).
- 23->24: same scope (+LoginFrame churn). No API-surface changes.

## Build 23 semantics — "verify imp attack against same NPC"
- Frame gains `impIndex:int`, snapshotted from `imp.getIndex()` in observe().
- proved(ATTACK_IMP): was `inCombat || imp==null || impHealth<before`.
  Now `inCombat || (impIndex>=0 && frame.impIndex==before.impIndex && both healths>=0
  && frame.impHealth<before.impHealth)`.
- This kills the false-positive where the nearest imp changed between the attack snapshot
  and the verify tick (old code compared health across possibly-different NPC objects).
- Behavior delta (minor, non-blocking): a one-shot kill (imp despawns) no longer completes the
  Pending early via the old `imp==null→true` branch. Instead the 9s UNPROVED timeout fires,
  pending clears, retries++, state machine re-attacks. Bounded and self-recovering; worst case
  ~9s stall in a rare case. (Edge: an attacked imp with server index 0 that despawns leaves
  impIndex=0==0 with health 0<before → proves — accidentally the correct outcome.)

## Build 24 semantics — "bound post-combat drop wait and report target evidence"
- observe() now snapshots the player's live interaction target: `player.getInteracting()`,
  instanceof-NPC-guarded, recording `interactingImpIndex` / `interactingImpHealth` /
  `interactingImpPos`. Independent of the nearest-imp scan — true "target evidence".
- Combat lifecycle tracking: `wasInCombat`, `lastCombatNpcIndex`, `lastCombatPos`,
  `lootUntil`, `combatEndsWithoutBead`.
  - COMBAT_STARTED: logs npcIndex/npcHp/npcPos from the interacting snapshot.
  - COMBAT_ENDED: `lootUntil = now+4000`, counter++, logs npcIndex/lastPos/noBeadEncounters.
  - Drop-wait window: tick returns early while `now < lootUntil` (bounded 4s, not a block).
  - DROP_WAIT_EXPIRED: logged with npcIndex/lastPos/beads when the window passes with no pickup.
  - PICKUP_ proved → `combatEndsWithoutBead=0`, `lootUntil=0` (counter reset confirmed in bytecode).
  - 12 consecutive combat-endings with no bead gain → hold("12 combat endings without bead
    inventory gain; inspect target/drop evidence") — a diagnostic hold with evidence, not a
    silent stall. Reasonable watchdog.
- publishStatus now reports interactingImpIndex/interactingImpHp/dropWaitMs/lastCombatPos.

## Thread-safety — carried question CLOSED
Every WorldLocation/NPC/varp/player read in the blocking paths executes on the client thread:
observe() in its entirety runs inside tick()'s blocking ClientThread.invoke(Supplier), and the
walk suppliers call Rs2Player.getWorldLocation() inside their own ClientThread.invoke bodies.
The Build 517 off-client-thread crash class does not apply to any path reviewed. The remaining
tick-block property (up to ~30s heartbeat stall per walk path) is by design, unchanged.

## Carried forward
- Tick-block class still serves the walk paths (12/18/19/20/21/22); drop-wait is an early-return,
  not a new blocking path.
- Acceptance (unchanged, none observed): fresh RUNNING_BUILD=23/24, COMBAT_STARTED/ENDED and
  DROP_WAIT_EXPIRED diag lines, or first IMPCATCHER_* screenshot. Feed dark ~164 min
  (newest commit b4e19333 / 17:44:02 EDT PIRATESTREASURE_DONE frame, already seen);
  zero IMPCATCHER_* frames ever — still structural (status.properties only).
- Standing state per 18:34 EDT brief: PID 19476, gameState=LOGGED_IN, currentWorld=308; do NOT
  report stuck-at-login from OCR other-text or expired status file.
