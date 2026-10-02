# Muse read-only review verdict: Below Ice Mountain Build 1 (patch-866)

Reviewed 2026-10-01 ~22:58–23:02 EDT. Alex owns implementation/releases; this is READ-ONLY review — no shipping action taken.

## Custody (verified, not assumed)
- Commit `1aa65204` "Below Ice Mountain Build1 candidate" (2026-10-02T02:58:23Z): single-purpose; version.txt 865→866; patch number 866 — NO version reuse; no patch-N overwrite.
- `patches/patch-866.zip`: 272 entries, root `net/` (only non-net entries: META-INF/, META-INF/MANIFEST.MF, version.txt). In-zip version.txt=866 matches repo version.txt=866 matches patch number.
- Manifest: genuine Main-Class net.runelite.client.RuneLite (no jar-spoofing manifest).
- `patches/patch-866.hot.json`: plugin belowicemountain, patch 866, hostVersion 1, build 1, sha256 ed8525758a22a77f63b59e23030f9dba17889e1b1332d8d52dbb080219abc9ce — EXACT match to downloaded belowicemountain-1.jar sha256.
- Script classes byte-identical zip↔belowicemountain-1.jar; plugin classes (Config, Plugin, Plugin$1) in belowicemountain-plugin-1.jar.
- README script-class SHA-256 b26a8ec5…: EXACT match to actual compiled BelowIceMountainScript.class. README plugin-jar SHA prefix 60297043…: MATCH.
- `javap`-level check: BUILD_NUMBER=1 in compiled Script class ("Build 1" string at offset 6046; BUILD_NUMBER at 8150) — banner consistent with Build 1 claim. No lying-banner defect.
- Published source (source-review/belowicemountain-build1/) compiled against installed microbot-tutorial-island.jar API usage matches declared imports.

## Design review
- Build 1 is deliberately observation-only: script has zero input dispatch; tick() only reads client state and writes status.properties. Plugin host is the same proven hot-reload pattern (URLClassLoader script-only, SHA+build mismatch refusal, atomic temp→move status writes — avoids the MM status FileSystemException class).
- Preflight gates: Quest.BELOW_ICE_MOUNTAIN state, BIM_MAIN (varp 2951), QP<16→HOLD, mining<10→HOLD, pickaxe=0→HOLD, health, food<10 when maxHp≤11 (guardian safety), arming gate (allowActions + approvedPid/approvedBuild/approvedSha256), cross-plugin exclusivity (17 plugins listed incl. Black Knights Fortress), FINISHED→stop. Reasonable and bounded.
- Atomic status writes via status.tmp + REPLACE_EXISTING in both script and host — good.

## Findings
- BIM1-1 (LOW): HOLD_MINING_LEVEL over-constrains. Official requirement is 16 QP to start (verified against OSRS Wiki + Jagex poll blog 2026-10-01); 10 Mining is boostable/recommended for the guardian alt-route, "by no means necessary". Conservative for Alex's planned pillar route (Build 2 notes cite Mining 10 + four pillars as guardian alternative), but an account with 16 QP and 8 Mining would park on HOLD despite being quest-eligible to fight the guardian. Fine for observation-only Build 1; document before Build 2 gating.
- BIM1-2 (INFO): `f.varp < 0` guard in tick() is dead code — getVarpValue returns 0 for unset varps, never -1. QuestState read is the real gate; harmless.
- BIM1-3 (INFO): cross-plugin exclusivity depends on both sides listing each other; future quest plugins must be registered in BIM's ownsInput list and vice versa (current list includes BKF, MM, Doric's, Ernest, Imp Catcher — correct as of now).
- BIM1-4 (INFO): `rejected = sha` is set before validation in reloadIfRequested — an invalid SHA gets permanently recorded as rejected and never retried. By design, but a typo'd SHA request needs a fresh request SHA; fine.

## Verdict
PASS WITH FINDINGS (custody airtight, design sound, Build 1 observation-only as advertised). Live acceptance: not yet loaded/hot-reloaded — watch for [BelowIceMountain] RUNNING_BUILD=1 diag lines; arming per README before trusting preflight values. No shipping action taken — read-only posture on Alex fronts holds.
