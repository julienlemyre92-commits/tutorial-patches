# Muse review verdict — Ernest Build 23 (patch-625) — read-only

Filed 2026-10-01 00:26 EDT by the review-loop worker. Ernest is Alex-owned; this is review only, nothing shipped.

## Ground truth
- version.txt = 625; commit dc4404d8 "Build23: open closet door before verified tube pickup" (00:24:04 EDT)
- patches/patch-625.hot.json: build=23, patch=625, hostVersion=1, sha256=1822693e1d343c961f3d21fe2eb5420791b40137d67a5de3839b62b52c3872e7
- SHA-256 EXACT-MATCH vs patches/ernestthechicken-23.jar (33874 bytes, +1126B vs b22) — download-verified via git blobs API
- Numbering 624 → 625 clean; patch-625.zip net/-rooted (208 entries, 205 under net/), only additions META-INF/MANIFEST.MF (RuneLite Main-Class intact) + version.txt=625 — overlay-safe
- 6 Script classes byte-identical zip <-> hot jar (Config/Plugin classes only in zip overlay, as expected)
- javap -constants: BUILD_NUMBER = 23

## What Build 23 does
Adds a dedicated `gaugeAndTube` step (lambdas 2-6) with OPEN_CLOSET / OPEN_CLOSET_DOOR steps: the closet door is explicitly opened and its state verified (Open/Close actions, collision-reachable adjacent tile, door observed near tube) BEFORE the rubber-tube pickup is dispatched. Replaces the old flat refusal ("Rubber tube pickup rejected; closet door may be closed") with precise rejection reasons: "Closet door is open but rubber tube is not visible", "Rubber tube pickup dispatch rejected after closet door verified open", "Closet door not observed near tube; refusing blind pickup". Only new external symbol referenced is java/util/Map.getOrDefault; inner classes signature-identical; panelledDoorClosedTile field from Build 22 retained (no revert).

## Verdict: PASS, no blocking defects
- Numbering, SHA chain, zip root, manifest, overlay safety all clean.
- Mechanism matches the commit message exactly and is the correct next step after Build 22's panelled-leaf crossing: the tube pickup now requires a proven open closet door rather than being refused on unknown door state.

## Live acceptance
Pending — no Ernest screenshots/diag have ever landed in the repo; screenshot feed dark since 17:44 EDT (Pirate Treasure DONE frames).
