# Review verdict: Below Ice Mountain Build 3 (patch-868) — PASS

Read-only review of `source-review/belowicemountain-build3/` (575 lines), patch-868.zip,
patches/patch-868.hot.json, patches/belowicemountain-3.jar. No shipping action taken.

## Fingerprint verification (all match)
- hot.json: plugin=belowicemountain, patch=868, hostVersion=1, build=3,
  sha256=`28c0b624fa13744f917589e7f7230cde8d8f5ab7388acc89e0a5d1f71ab060b6`
- `belowicemountain-3.jar` on repo: SHA-256 matches hot.json exactly
- Script-only jar contains ONLY `BelowIceMountainScript` + 5 nested classes, `net/` root — safe for hot reload
- New script class SHA-256: `205b145884f198de493b2b6d9e74c0dcc2c2c3cba31c323114303143601e474e`

## Diff vs Build 2 (exact)
Two changes only:
1. `BUILD_NUMBER` 2 → 3.
2. `classSha()` rewritten: previously `Class.getResourceAsStream("BelowIceMountainScript.class")`
   resolved through the parent class loader and reported the embedded Build 1 class hash when
   Build 2 was hot-loaded (observed live on PID 40424). Build 3 reads the exact `.class` entry
   from the class's own `ProtectionDomain`/`CodeSource` JAR. This is the correct fix — the arming
   gate (control.properties: PID + build + class SHA) now fingerprints the actually-loaded class.

Correctness check of the new `classSha()`: `location.toURI()` assumes a `file:` URL (true for a
jar on disk); if the class were ever loaded from a directory the outer catch returns "UNKNOWN",
which fails the arming gate safely (no action) rather than mis-hashing. Acceptable.

## Verdict: PASS
Minimal, targeted, and correctly scoped. No behavior change to the early route, no guardian actions.
Live acceptance OBSERVED: stream companion panel shows "RUNTIME BUILD: 3 / confirmed" at
~23:13 EDT — the corrected hash now reports the compiled class SHA, so the control-file arming
gate is satisfiable. Same-PID hot reload from the Build 2/3 chain works.

## Carried-open findings (for a Build 4 candidate)
- MEDIUM BIM2-1 (from the Build 2 verdict): `dialogue()` expected-gate race — if BIM_MAIN advances
  to 10 while Willow's dialogue window is still open, the bot holds with the dialogue open.
  Still present in Build 3 (no dialogue change in the diff). Not blocking for the armed test since
  the first live dialogue will show whether the race fires.
- LOW BIM2-2: Mining<10 HOLD over-constrains the early route (pillar-plan gate, acknowledged).

## Live state at review time (~23:13 EDT)
Stream (T-Uj1Rxo4a8) confirmed LIVE: panel shows CURRENT MISSION "Belowicemountain",
QUEST STATUS "Not started", SCRIPT STEP "Walk island boat", CLIENT FEED "Live · 0s ago",
OBSERVED THIS STAGE 39s, quest milestone "Prince Ali Rescue" marked complete/observed
(stream-observed only, not independently verified). Game client visible at the welcome/lobby
screen ("You last logged in 3 minutes ago") — not logged in at that moment. Julien (@OG_Bumbaa)
active in live chat. No chat reply needed — nothing asked of us.
