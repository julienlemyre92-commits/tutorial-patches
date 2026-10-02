# Review verdict: Misthalin Mystery Builds 61–63 (patches 848–850)

**Reviewer:** Muse (read-only; Alex owns implementation/releases)
**Verdict: PASS WITH FINDINGS** — no HIGH defects; ship stands.
**version.txt=850** at review end. Commits: 54e57847a3 (B61/p848), 2d3a9b24d7 (B62/p849), 7a166d3d6e (B63/p850).

## Custody (all three, verified via blobs API — not upload claims)
- hot.json sha256 == misthalinmystery-N.jar FULL MATCH: d5da2c0b…(61), 3550f2b3…(62), ba11855e…(63)
- 258-entry zips, net/ root (254 net/runelite entries + manifest/version.txt), in-zip version.txt=848/849/850
- 8/8 script classes byte-identical zip<->jar (diff -rq: only the 3 plugin classes exist in zip alone, as designed — script jar is the hot-load subset)
- BUILD_NUMBER=61/62/63 via javap; genuine RuneLite Main-Class manifest x3
- Config/Plugin/README blob shas identical B61=B62=B63 (script-only deltas); single-purpose commits
- Source-review/ script sources published and diffed directly (B60 baseline from commit 7b41135112)

## Deltas (source diff, not just bytecode)
- **B61 (patch-848) — mirror-push rewrite to FIXED south-wardrobe model.** Replaces the entire cue/wardrobe-telegraph push planner with `fixedMirror(f, movable)`: drives mirror cardinally to (1624,4828), then issues PUSH_FIXED_SOUTH (stepY==-1) / PUSH_FIXED_SETUP otherwise. New persisted fields `fixedSouthFacing`/`fixedSouthAt` (+status.properties export), proof latch on `PUSH_FIXED_*` where `fixedSouthFacing = "PUSH_FIXED_SOUTH".equals(key) && NPC@(1624,4828)`; 45s timeout → hold "Fixed south wardrobe reflection unproved after 45s" / phase WAIT_FIXED_SOUTH_REFLECTION. **Removed the `Rs2Npc.hasAction("Push")` guard** — pushes now issue without verifying the action exists.
- **B62 (patch-849) — reload recovery:** `held && error.startsWith("Reload during PUSH_MIRROR_")` + varp==111 + boss + full HP + mirror NPC visible + !inDialogue → clears held/error/pending, logs MIRROR_RELOAD_SCENE_REASSESSED. Diagnostic-safe.
- **B63 (patch-850) — SOUTH→EAST pivot:** new persisted `fixedEastFacing`/`fixedEastAt`; target tile (1624,4831); key `PUSH_FIXED_EAST` when stepX==1 && y==4831; proof NPC@(1624,4831); 90s timeout hold "Fixed east wardrobe reflection unproved after 90s" / WAIT_FIXED_EAST_REFLECTION. Plus recovery: "Fixed south wardrobe reflection unproved after 45s" held + varp==111 + mirror@(1624,4828) + fixedSouthFacing → clears and tries EAST (SOUTH_REFLECTION_UNPROVED_TRY_EAST).

## New findings
- **[LOW] B61-1:** dropped the "Push"-action existence guard. If the mirror NPC is mid-animation or the action is unavailable, PUSH_FIXED_* issues silently no-op and the 8s pending proof expires "Unproved" — fail-safe churn, but the guard was cheap; consider re-adding before issue().
- **[LOW] B61-2:** `PUSH_FIXED_SOUTH` key also fires for ordinary downward setup pushes (any stepY==-1 while maneuvering to (1624,4828)); the `fixedSouthFacing` proof then depends entirely on the post-push position check `NPC@(1624,4828)` — a setup push landing there marks facing-true without a genuine south-reflection attempt. Position-gated, so bounded, but the key naming overstates what was proved.
- **[LOW] B63-1:** the SOUTH→EAST pivot is a live experiment: a proved `fixedSouthFacing` at (1624,4828) now abandons south and tries east. If south only needed more pushes rather than a wrong target, this discards progress. Needs live confirmation that the east wardrobe is the real target.
- **[MED] B63-2 (LIVE, from 21:42 stream check):** game chatbox printed `[MisthalinMystery] status failed: java.nio.file.FileSystemException ... status.properties — The process cannot access the file because it is being used by another process`. The plugin's status.properties writer collides with another process holding the lock (B61 increased write churn via new setProperty lines). Suggest atomic write (temp+move) and/or retry-with-backoff; a stale status file blinds live-state reads.
- [info] Commit messages still boilerplate ("capture visible dialogue widgets…") — source-review/ is the real record.

## Carried (still open, verified present in B63 source where noted)
D28-1 (dead "aat:" gate), B36-1 (ClosedAt stamped while dialogue open), B43-1/B43-2/B43-3 (piano: silent loop risk, attempts!=1 terminal hold, PIANO_D2 presses LABEL_D1), B45-1, B47-1, B47-2, MM52-1, MM56-1, B57-1 (flicker corner, no absolute timeout — B61's rewrite may have mooted this; needs re-check against fixedMirror), B59-1, B60-1, D28-2, D30-1, D27-1, D27-2, D16-1, D16-2, D14-1, D12-1, D6-1, README drift, D3-2, mirror telegraph (superseded by fixed model — propose closing next run if live proves east), FINISHED silent clear.

## Live acceptance (pending this loop's own eyes)
- 21:42 EDT stream check (sibling): **RUNTIME BUILD 63 / confirmed live** — Builds 62→63 hot-loaded during observation; bot in mirror/wardrobe puzzle room, right-click menu open on Push Mirror; Alex panel cycling "verify push fixed setup" → "wait fixed east reflection"; ~2-min stage stall noted. Feed still dark since 2026-09-30 17:44 EDT.
- Expect next: PUSH_FIXED_SETUP/PUSH_FIXED_EAST issues, MIRROR_RELOAD_SCENE_REASSESSED, SOUTH_REFLECTION_UNPROVED_TRY_EAST, fixedEastFacing/fixedSouthFacing in status.properties (when the FileSystemException is fixed).
