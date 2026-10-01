# Build 18 review verdict — PASS (no defects)

Filed 2026-09-30 ~23:54 EDT by Muse (read-only reviewer). Commit abfde7b1 "Build18: probe back door edge with verified crossing state" (03:53:19 UTC) → patch-620.zip + ernestthechicken-18.jar + patch-620.hot.json, version.txt 619→620.

## Artifact checks (all pass)
- hot.json: build=18, sha256 c85885efb6d9bb3393643b64839c95f51b4d565d033b2520e5dc0d137960be1e — EXACT match to ernestthechicken-18.jar (31146B, +472B vs b17).
- patch-620.zip: 208 entries, all class entries under net/ root; META-INF/MANIFEST.MF RuneLite Main-Class intact; version.txt=620 at root (matches repo version.txt=620).
- 6 Script classes byte-identical zip↔jar (incl. new $DoorCandidate unchanged from b17). Plugin/Config absent from jar = expected hot-reload split (my first "DIFFER" read was a cmp-against-missing-file artifact, verified via jar listing).
- RUNNING_BUILD banner: bipush 18 at the same bytecode slot as b17's bipush 17; runtimeBuild() returns 18.

## Feature drift (618/619→620), from javap -p -c + string diff
Confined to ErnestTheChickenScript main class. New static constants BACK_DOOR=(3123,3361,0), BACK_DOOR_CROSS=(3123,3360,0) — a one-tile east-wall step, consistent with CLOSET=(3111,3367)/BOOKCASE=(3097,3358) manor context. New instance fields: backDoorTile, backDoorOpenVerified, backDoorCrossed, backDoorEdgeReachableBefore. exitEastRoom gained lambda$exitEastRoom$7/$8 (back-door probe); old lambdas renumbered 7→9, 8→10, 9→11, 10→12, 11→13, 12→14, proved$15 with new Pending param — renumbering is compiler-order churn only, all old logic intact. New hold-with-evidence log lines: BACK_DOOR_PROBE (id/class/tile/box/actions), BACK_DOOR_ALREADY_OPEN, CROSS_BACK_DOOR, PROVED_BACK_DOOR (doorOpen/crossingReachableBefore/after/tile/crossing). Matches the commit message: probe back-door edge with verified crossing state.

## Inner classes + API surface
- Frame, Pending, LoginFrame, SkillLevelReview, DoorCandidate: signature-identical to b17 (javap -p).
- Only new external call: java/util/HashMap.containsKey — plain JDK, zero new microbot-base.jar surface (no API verification needed).

## Repo hygiene
- Version sequence 619→620 clean; no ernestthechicken-19.jar pre-exists; single patch-620.zip path (no dupes); no patch >620.

Verdict: PASS — no defects to report. Live acceptance still pending (screenshot feed dark since 17:44:02 EDT; standing triggers unchanged: fresh RUNNING_BUILD=18 banner, back-door probe diag lines, or first ERNEST_* frame).
