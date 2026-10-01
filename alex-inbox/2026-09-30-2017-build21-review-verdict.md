# Muse review-loop verdict: Imp Catcher Build 21 (patch-591) -- 2026-09-30 20:17 EDT

**Verdict: PASS, no blocking defects.** Read-only review (Alex owns Imp Catcher releases).

- hot.json: plugin=impcatcher, patch=591, build=21, sha256=97959620e6325b1fba4d4dd7078e30e8f647e6601b6c2362e538b6c241cb3fe8.
- Numbering: 590 -> 591 clean; version.txt=591 caught up (no lag). No overwrite of existing patch zips.
- patch-591.zip: root `net/` correct (199 entries, 184 .class), overlay-safe; same META-INF/MANIFEST.MF (220B, dated 18:33) as patch-590 -- stable carried pattern, no new defect.
- Diff vs Build 20: ONLY ImpCatcherScript + Frame/Pending/HeldFrame changed. Frame gains `mizgogPos`/`impPos` WorldPoint fields alongside the existing `mizgog`/`imp` NPC fields.
- The NPC scan (`client.getNpcs()` + `npc.getWorldLocation()`) runs INSIDE the blocking `ClientThread.invoke(Supplier)` in tick() -- snapshot happens on the client thread. Tick-thread walkers (APPROACH_MIZGOG/APPROACH_IMP) now consume the snapshotted WorldPoint instead of calling npc.getWorldLocation() off-thread. This DIRECTLY ANSWERS the carried client-thread-safety question (Build 517 crash pattern): the NPC side is fixed.
- Mizgog match: id 7746 OR name "Wizard Mizgog" (case-insensitive); null-location NPCs skipped.
- Gaps: hot.json sha256 cross-check vs impcatcher-21.jar not possible -- no such artifact visible in repo listings this run (only patch-591.zip). The zip payload itself was inspected and diffed instead.
- Carried (unchanged): tick-block class (blocking walkWithStateUntil ~30s) now serves 5 paths (12/18/19/20/21); Rs2Player.getWorldLocation() still inside blocking suppliers (player side; Microbot caches it, unconfirmed).
- Feed dark ~155 min (newest frame 17:44:02 PIRATESTREASURE_DONE, already seen). Build 21 NOT live-observed. Acceptance: fresh RUNNING_BUILD=21 + client-thread frame snapshots in diag.
