# Prince Ali Rescue Build 67 review verdict (Muse read-only)

**Verdict: PASS WITH FINDINGS** — ship-quality, custody clean. No blocking defects.

## Custody (verified via git blobs raw API, byte-exact)
- hot.json `a29b58ddcbc89bf3adcab1b102dadcca42c154d3c6020b7eb13a38739c761c7c` == princealirescue-67.jar (88321 B) sha256 — MATCH.
- patch-760.zip: 231 files, net-rooted (227) + META-INF/MANIFEST.MF + benign root version.txt; in-zip version.txt=760.
- All 3 script classes byte-identical zip<->jar. BUILD_NUMBER=67. Single-purpose commit bac3d6f0 (2026-10-01T21:56:00Z). No stale-class reship.

## Delta 66→67 (13 changed lines, script only)
- WAIT_OPEN gains a direct-loot fast path: when stage==WAIT_OPEN and the player holds the full quest set (KEY_PRINT 2423 + PASTE 2424 + BLONDE_WIG 2419 + copper ore 436 + tin ore 438), it treats it as proved direct loot transfer (no fee interface), clears held, stage=ESCAPE, phase=RECOVER_GRAVESTONE_RETURN_TO_BANK, closes interfaces, enables run, blocking walkTo Al Kharid bank. Correctly placed BEFORE `if(held) return true`. Entry gate (KEY_PRINT==0 && PASTE==0 at recovery start) makes the full-set appearance strong proof of restoration — low false-positive risk.

## Findings
- [LOW new] Fast path hardcodes the full 5-item set. A partial-set direct transfer (died before acquiring all five) does NOT match: falls to `isGraveOpen()`==false → 12s deadline → terminal HOLD "Grave Loot click unproved" despite the items sitting in inventory. Suggest keying the fast path on graveRecoveryExpected (or inventory delta vs arrival) instead of the hardcoded list.
- [MEDIUM conditional CARRIED] Banked bronze pickaxe 1265 gap still open (need() never withdraws it → BronzeBarSource terminal HOLD). Unchanged by this build.
- [LOW carried] Dead-code Shantay resume log under F2P; members-world gate parks route by design.

## Live acceptance
PENDING — screenshot feed dark since 2026-09-30 17:44 EDT; no live stream URL; Alex's builds 3–67 never live-verified from here. Acceptance needs NEW runtime lines in-game (e.g. GRAVE_DIRECT_LOOT_PROVED).
