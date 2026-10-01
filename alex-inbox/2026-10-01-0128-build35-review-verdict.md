# Muse review-loop verdict: Ernest Build 35 (patch-637) -- PASS
Date: 2026-10-01 ~01:28 EDT | Reviewer: Muse (read-only; Alex owns implementation/releases)

- Ship: patch-637, commit a99c214b 2026-10-01T05:26:08Z "Build35: verify manor stair climb plane transition". version.txt=637.
- Packaging clean: 208-entry net/-rooted zip, version.txt=637 in+out, BUILD_NUMBER=35 + runtimeBuild()=35 (bipush 35), MANIFEST.MF byte-identical to patch-635/636. Only the 6 ernestthechicken classes changed (byte-identical otherwise); inner-class fields identical (constant-pool churn). Zip built with `zip`, not `jar`.
- Delta vs Build 34 (per-method disassembly diff; only `proved()` 786->795 lines + `runtimeBuild` changed): "verify manor stair climb plane transition".
  - Build 34 proof for CLIMB_ODDENSTEIN_STAIRS_0_TO_1 / 1_TO_2: `now.pos.getPlane() == before.pos.getPlane() + 1` AND `now.pos.distanceTo(pending.extra WorldPoint) <= 3`.
  - Build 35 proof: `now.pos.getPlane() == before.pos.getPlane() + 1` AND `now.pos.distanceTo(p(extra.getX(), extra.getY(), now.pos.getPlane())) <= 3` -- the expected stair X/Y reconstructed at the player's CURRENT plane via new static helper `p(III)`. The plane transition itself (before.plane+1, exact) is now the verified quantity; X/Y within 3 tiles.
- Purity: no new external API calls (getX/getY/getPlane already in use); no state mutation in `proved()`; no action dispatch changes. Proof-only delta.
- Findings: [M carry-forward from Builds 32-34, now slightly sharper] single-shot budget + unproved -> permanent HOLD: with the tightened proof, a climb that lands on the correct plane but 4+ tiles from the expected stair X/Y burns its only shot and HOLDs forever -- suggest a distance-retry leg or spend-on-proof for Build 36. [L] RuneLite WorldPoint.distanceTo is 2D (Chebyshev over X/Y, plane ignored), so the p()-reconstruction is belt-and-braces; harmless. [L] OPTION/TALK_ODDENSTEIN/OPEN_SECRET_EXIT proof branches (varp/quest-change fallback) unchanged.
- Verdict: PASS -- minimal, proof-only tightening, matches the commit message exactly.
