# 2026-10-03 06:26 EDT — Corsair Curse Build 32 shipped, static review (read-only)

## Ship
- Corsair Curse Build 32 shipped 06:24:03 EDT (commit f8b9a5d97c, ~1 min before this run's OBSERVE).
  - `patches/corsaircurse-32.jar`, `patches/patch-1077.zip`, `patches/patch-1077.hot.json`
  - `source-review/corsaircurse-build32/CorsairCurseScript.java` (1476 lines, full publish)
  - `source-review/corsaircurse-build32/build32-identity.json` (script SHA 2127ae9d…de7d1a44, 14 script classes)
  - version.txt=1077 (note: skips 1076 — intermediate "Expose patch1076 provider hot artifacts" 5add08ced4 at 10:23:17Z, "Bank route chooses policy-verified F2P walk" 9a70cd1094 at 10:22:58Z)
- Directly answers this morning's 06:23 watch: the 06:19:40 NAVIGATION_UNAVAILABLE HOLD
  ("teleport, consumable, or nonlocal transport requires separate proof", Port Sarim->Corsair leg).

## What Build 32 + patch-1076 contain (static read, read-only)
- Script owns NO route code. Header comment: "A provider owns all walking and gate/transport decisions.
  It must prove arrival." Sail leg is `p==10 → npc("sail:Tock", CORSAIR_CAPTAIN_1OP, DOCK)` with
  dialogue "Okay, I'm ready go to Corsair Cove." / "Let's go." — a dialogue-driven, free ship voyage.
- patch-1076 ships `source-review/navigation-1076/VerifiedRoutePolicy.java` (172 lines):
  conservative full-route preflight for the navigation driver. Allows zero-cost walking and
  simple zero-cost transport ONLY. Hard gates: F2P world only (members-world route = separate access
  policy), not instanced, no live combat, zero risk budget (nonzero → needs account/gear/food model),
  teleports need exact rune/item + spend proof, walker config must enforce zero-risk
  (avoidWilderness, avoidDangerousNpcs, !useBankItems), fresh (≤3s) same-account origin via
  SHA-256(username+displayName) account key, and it never approves a route from destination
  proximity alone.
- File-backed cowhide pickup receipt (Build 31) carried into Build 32 source — the
  hot-reload-checkpoint-persistence fix stands.

## Verdict: PASS (static)
No mechanism defects found in the shipped artifacts. One concrete open question for Alex,
answerable only live (provider is PC-side): does Captain Tock's free dialogue-driven voyage
classify as "simple zero-cost transport" under VerifiedRoutePolicy (→ HOLD clears) or does the
provider still refuse it (→ HOLD persists, script stays at WAIT_NAVIGATION/HOLD in Port Sarim)?

## Pending live verification (dispatched this run)
Browser task on the confirmed live stream (p-yTeVjh7vU) reading: RUNTIME BUILD marker
(31 vs 32 — Build 32 landed <2 min before observation, reload may still be in flight),
HOLD-clear status, player activity/location, chatbox verbatim lines, live chat. Results to be
reconciled by the next loop run.

Read-only scope: Alex owns Corsair Curse implementation/releases. No ship.
