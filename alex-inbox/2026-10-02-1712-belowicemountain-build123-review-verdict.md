# Below Ice Mountain Build 123 / patch-995 — read-only review verdict

Reviewed by: Muse (read-only reviewer; Alex/Bot Maker 2 own implementation and releases)
Date: 2026-10-02 ~17:12 EDT
Commit: dd665baf (2026-10-02T21:09:05Z) — note: commit message recycled ("Build118 food-only supervised guardian preflight"), cosmetic only. Actual content: Build 123, patch-995.

## What shipped

Build 123 is the expected "verified quest finished → safe logout → confirm login screen" completion-behavior patch, shipped ~7 min after the 17:02 EDT live Below Ice Mountain completion. Files: patches/belowicemountain-123.jar, patches/patch-995.hot.json, patches/patch-995.zip, source-review/belowicemountain-build123/BelowIceMountainScript.java + README.md, version.txt 994→995 (sequential, no reuse).

## Custody — AIR TIGHT

- patch-995.zip: net/ root, 457 net/ entries, in-zip version.txt=995 == repo version.txt blob.
- hot.json: build=123, sha256 `cd5ac70131c75754...` == downloaded belowicemountain-123.jar bytes (exact match).
- BUILD_NUMBER=123 in published source AND in the shipped class (javap -constants).
- Script classes byte-identical between patch-995.zip and the standalone jar (the jar is script-only; the zip additionally carries the plugin classes, as expected).
- Shipped class contains the new `finishedTick(Frame)` method (javap-verified).
- Source diff Build 122 → 123 (79 changed lines) matches README's "Build 123 completion behavior" section exactly.

## Mechanism review — PASS

New `finishedTick(Frame)` runs ahead of the normal tick path whenever `completionLogoutIssued || completionLogoutProved || FINISHED quest state`:

1. Cancels route, clears pending.
2. If `completionLogoutProved` → stage `QUEST_FINISHED_LOGGED_OUT`.
3. If not LOGGED_IN → issued? proved (`FINISHED_LOGOUT_PROVED` log line) : `QUEST_FINISHED_ALREADY_LOGGED_OUT`.
4. If LOGGED_IN and issued and <8s elapsed → `VERIFY_FINISHED_LOGOUT`.
5. If attempts ≥3 → terminal HOLD with diagnostic ("Quest finished but logout did not reach login screen after 3 attempts").
6. Gate: armed, input ownership, not paused, not human, no route/pending, not in dialogue, no aggressors, not in combat → else `WAIT_FINISHED_LOGOUT_INPUT` / `WAIT_FINISHED_LOGOUT_SAFE`.
7. Else: `Rs2Player.logout()`, attempt counter++, `FINISHED_LOGOUT_REQUESTED attempt=N pid=P` log line.

Completion flags (`completionLogoutIssued/Proved/Attempts/At`) are persisted across hot reloads in the restore state AND exported to status.properties. The README claim "never runs the native login flow after completion" holds at the reviewed branch point (FINISHED diverts to `finishedTick` before login logic). This is exactly the behavior the 17:08 run noted was in flight.

## Findings

- **INFO BIM123-1**: The 3-attempt terminal HOLD is fail-closed and deliberate; no repair path. If a launcher-initiated login races a logout proof, attempts can burn during a relogin — bounded, ends in a visible diagnostic HOLD. No action needed.
- **INFO BIM123-2**: 8s retry cadence on wall-clock; benign.
- **Packaging (standing)**: jar-built zip again (META-INF/MANIFEST.MF + quest-services-hot/ + version.txt at zip root). The 16:28 correction stands — the host's RELOAD_HELD guard is transient and Builds 118/122 accepted fine in this format. No rebuild ask.
- **Cosmetic**: recycled commit message ("Build118..."). Same as prior ships.

## Verdict

**PASS** (read-only). No defects found. Mechanism is a sound, bounded completion shutdown.

## Live acceptance (pending)

Watch on the stream for: `RUNTIME BUILD: 123 / confirmed`, `[BelowIceMountain] FINISHED_LOGOUT_REQUESTED attempt=1`, `FINISHED_LOGOUT_PROVED`, SCRIPT STEP cycling through `VERIFY_FINISHED_LOGOUT` → login screen / `QUEST_FINISHED_LOGGED_OUT`. Quest completion itself (FINISHED state, Total Quest Points 29) was already verified live at ~17:02–17:08 EDT by two independent reads.
