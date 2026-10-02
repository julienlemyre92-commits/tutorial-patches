# Read-only review verdict — Below Ice Mountain Build 37 (patch-902)

**Verdict: PASS WITH FINDINGS** (read-only; Alex owns implementation/releases — no ship)

## Custody
- Repo HEAD: version.txt=902 (commit e9e1a72d "Below Ice Mountain Build37 stage30 safety logout", 2026-10-02T05:06:38Z, shipped ~16s after this run's 01:06:22 EDT start). 901->902 sequential, no reuse.
- patch-902.zip: 1,120,599 bytes, 277 entries, root = net/ (META-INF/ + version.txt extras only). in-zip version.txt=902 == repo HEAD == patch number.
- BUILD_NUMBER=37 in published source (line 63) AND compiled class (javap -constants).
- Class set byte-identical to patch-901.zip (diff of class lists: empty); the content delta is confined to BelowIceMountainScript + nested classes, consistent with the surgical source diff.
- Source-review published: source-review/belowicemountain-build37/{BelowIceMountainScript.java (1185 lines), README.md (177 lines)}.

## Delta B36 -> B37 (source diff, 42 diff lines — surgical)
Matches the README claim exactly:
1. `import net.runelite.client.plugins.microbot.util.player.Rs2Player;` added.
2. BUILD_NUMBER 36->37.
3. New memory fields: `safetyLogoutIssued` (boolean), `safetyLogoutAt` (long).
4. New trigger (tick, after LOGGED_IN/welcome/paused checks): stage==30 && plane==0 && x in [10000,11000) && y in [12000,13000) && !dungeonEntryAllowed(f) -> set flags, log.warn SAFETY_LOGOUT_REQUEST with hp/maxHp/food/position, Rs2Player.logout(), return.
5. New verification block BEFORE the normal loginTick path: if safetyLogoutIssued: gameState!=LOGGED_IN -> hold("Safety logout proved from stage-30 cave; prepare before re-entry"); else if 12s elapsed -> hold("Safety logout did not change game state within 12 seconds"); else stage=VERIFY_SAFETY_LOGOUT; return.
- Bytecode confirms: invokestatic Rs2Player.logout:()V, both hold strings, VERIFY_SAFETY_LOGOUT, SAFETY_LOGOUT_REQUEST warn all present in the compiled class.
- Placement is correct: logout issues only while LOGGED_IN; a proved logout can never reach loginTick (no auto-relogin).

## Findings
- INFO BIM37-1 (new, refines BIM34-1): hot.json sha256 (530ebefaaa...) is the SHA of patches/belowicemountain-37.jar, NOT of patch-902.zip (72d3fee2...). So the recorded sha cannot serve as a zip integrity check — the "pre-final-pack" theory was wrong; it's simply the wrong artifact's sha. Host provably doesn't sha-gate (Build 35 hot-loaded despite mismatch), so cosmetic; recommend recording the zip sha in hot.json or documenting which artifact the host verifies.
- INFO BIM37-2 (new): a PC-side launcher relogin defeats the safety logout. After logout proves, the script terminal-HOLDs by design. But if the launcher login clicker logs the account back in, the next tick sees safetyLogoutIssued=true + LOGGED_IN + 12s elapsed -> hold("Safety logout did not change game state within 12 seconds") — logged in, in the cave, i.e. B36-equivalent exposure. Consider having the launcher respect the safety-logout hold (cf. Build 388/389 login-clicker flap lesson), or re-issuing logout on relogin-into-cave.
- INFO BIM37-3 (new, minor): the trigger region excludes the stage-30 entrance cutscene (13864,12647). Logout fires only in the cave scene; the entrance cutscene keeps B36's logged-in HOLD. Presumably intentional (cutscene is safe) — flagging in case the entrance was meant to be covered.
- Carried: BIM33-1 (willowYes option matcher overworld-anchored), BIM31-1, BIM30-2.

## Live acceptance (pending)
- RUNTIME BUILD: 37 / confirmed on the overlay, plus SAFETY_LOGOUT_REQUEST warn line, plus the client leaving LOGGED_IN (login screen) — or the 12-second hold line if logout fails.
- The 01:04 EDT stream check for Build 36 live acceptance was never reconciled and is superseded by Build 37 (shipped 01:06:38 EDT); a fresh stream check for Build 37 is the live source. Screenshot feed dark since 2026-09-30 17:44 EDT (~31.4h).

Reviewer: Muse (read-only) — 2026-10-02 ~01:08 EDT.
