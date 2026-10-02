# Read-only review: Below Ice Mountain Build 104 / patch-967 (2026-10-02 09:20 EDT)

VERDICT: **PASS (INFO only)**. Custody air-tight; mechanism read-only and fail-closed.

## Custody
- version.txt 966 -> 967 (fresh N, no reuse), commit 28adba6997, HEAD true via git/refs.
- patch-967.zip: net-rooted, 315 entries / 311 under net/, in-zip version.txt=967.
- hot.json build=104, patch=967, sha256 e639d05c... — VERIFIED equal to actual belowicemountain-104.jar bytes.
- BUILD_NUMBER=104 confirmed in compiled class (javap -constants).
- zip<->jar class overlap byte-identical; patch jar (300 classes = full plugin overlay) + hot jar (27 script classes).

## Mechanism (41-line diff vs Build 103)
- `marketProbeReadOnlyRecovery()`: if a stale Build103 probe checkpoint exists (schema MARKET_PROBE_1, same account, same PID, build=103, phase=OVERVIEW), dump the GE widget tree read-only and HOLD. Gate is strong:
  - pins B103's exact script class sha fcc883bc... — verified against B103's real class bytes from belowicemountain-103.jar: MATCH.
  - live re-verify: GE must still be open AND exactly 1 uncut sapphire in inventory, else IllegalStateException -> caught by the existing catch -> HOLD (fail-closed).
  - new dumps: OVERVIEW_BEFORE_SELECTOR + per-slot widget dump (GE interface 465 children 7..14 and their child 1), all inside clientThread.invoke — thread-safe.
- No new actions; probe remains control-file-armed. The recovery path is dormant unless B103's probe was armed on the PC.

## INFO findings
- The recovery gate's PID check means it only fires on the same process that wrote the checkpoint — correct post-disconnect behavior is to skip (checkpoint foreign/pid mismatch -> marketProbeLoad path), noted as by-design.
- Build103's probe checkpoint may never have been armed on the live client (client disconnected ~09:12 EDT); Build104's hot-load and the probe both await the reconnect.

Read-only; Alex owns implementation/releases. Muse reviewed, did not modify code.
