# 2026-10-07 13:02 EDT — mystery TI client restarted AGAIN ~12:33:50, logged out, 6-min watchdog; feed dark 26+ min (read-only review note)

## Observed (repo evidence, runtime lines only)
- `2026-10-07_12-34-51_UNKNOWN_diag.txt` (154-line tail): at 12:33:50 a full Tutorial Island script startup banner re-dumped (Build 119 changelog chain, "Build 104: loaded 233 cached object names (rev 241)", "REBUILD: WorldModel registered on event bus") — i.e. a FRESH client/script start ~12:33:50. This is the SECOND restart today (after the 12:25:01 one covered in the 12:45 note).
- Same tail: "Build 390: desktop screenshot saved: 2026-10-07_12-33-50_UNKNOWN_auto_desktop.png", then 12:33:51 "Build 194: logged out 0 min (login screen / disconnect dialog?) -- exit watchdog at 6 min". Post-restart the client found itself LOGGED OUT; the Build-194 watchdog exits the client at ~12:39:51 absent a login.
- 12:34:51 "Screenshot saved: 2026-10-07_12-34-51_UNKNOWN_auto.png" — but the file never landed in screenshots/ (verified via git trees API: no 12:33/12:34 PNGs at all). In fact ZERO .png files landed in screenshots/ all of today; only diag tails. The PNG upload path has been dead all day.
- Last commit touching screenshots/: 2026-10-07T16:34:51Z (12:34:51 EDT). Feed dark ~26 min as of 13:00 EDT. seen-screenshots.txt was already current through 12:34:51 — nothing newer to fetch.

## Inference
- The 12:25 restart trigger was already unproven (command.txt holds only the stale 2026-09-29 STATUS command — re-verified 13:00 EDT). Same verdict for the 12:33:50 restart: no command issued, trigger unknown.
- Feed death at 12:34:51 with the watchdog deadline at ~12:39:51 is CONSISTENT WITH (not proof of) the no-recovery pattern: Build-194 watchdog exits the logged-out client and the Supervisor never relaunches. Uploader death or machine state could also explain it.
- This is a separate client/JVM from the streamed Cook's Assistant BUILD 519 client (per the 12:45 note: the TI feed claimed tutorial ownership with the Cook plugin stopped, while the stream shows Cook 519 actively pathfinding at the wheat field). Ownership of the mystery TI client remains unproven — do not attribute to Alex.

## Candidate leads for Alex
1. If the mystery TI client is yours: the ~8-min restart cycle (12:25:01 -> ~12:33:50) with no issued command and a logged-out landing suggests the launcher clicker re-login is not completing (or the client crashes on start). The Build-194 6-min logged-out watchdog then kills the session and nothing relaunches.
2. PNG uploader dead all day (diag tails upload fine) — worth a look; PNGs are the only visual ground truth when the stream shows a different client.
3. Review-loop note: the ~250-line startup banner re-dump on every restart blinds the diag tail (see 0650 note); a one-line RUNNING_BUILD marker would make restart detection cheap.
