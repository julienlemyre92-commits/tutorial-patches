# Prince Ali Rescue Build 95 / patch-788 — read-only review (Muse, Alex owns releases)

Date: 2026-10-01 ~19:45 EDT. Reviewer: Muse (read-only; Alex owns integration).
No edits, no publish. Current head at review: version.txt=788.

## Custody: CLEAN (byte-level, independently verified via git blobs API)

- Ship commit `893852dd` (23:39:40Z): "Prince Ali Rescue Build95: bind plugin handoff requests to PID nonce and fresh timestamp for safe retry". Single-purpose 9-file commit (hot json, zip, script jar, plugin jar, 4 source-review files, version.txt).
- `patches/patch-788.hot.json`: `{"plugin":"princealirescue","patch":788,"hostVersion":1,"build":95,"sha256":"eca1ff74383dcfd0a04c3ffe85e5692a98f34586841c625f4e8370c6d792582a"}` — sha256 == `patches/princealirescue-95.jar` bytes (148,121 B). MATCH.
- `patches/patch-788.zip`: 247 entries, root `net/` (+ benign META-INF/, `version.txt`); in-zip `version.txt` = 788.
- All 29 script-jar classes byte-identical zip<->jar; all 32 plugin-jar classes present in zip, zero missing; extra 3 zip-only classes are PrinceAliRescuePlugin/Plugin$1/Config (established packaging, same as B94). Zero byte mismatches.
- `BUILD_NUMBER` = 95 confirmed in the compiled class (javap -constants).
- `PrinceAliRescuePlugin.java` unchanged B94->B95 (0 diff lines). Source delta is script-only, 47 diff lines.

## Delta B94 -> B95 (QuestPluginHandoff.check only)

1. `attempted` changes from `volatile boolean` to `volatile String` holding the last processed `request.id` — each request id processed at most once; new ids retryable.
2. New gates before processing: `id` non-empty and not already attempted; `expectedPid` must equal `ProcessHandle.current().pid()` (stale requests from dead PIDs ignored); `timestamp` age must be 0–120s (stale requests ignored). `attempted=id` is assigned only AFTER these gates — a rejected request does not poison the nonce. Good ordering.
3. Existing-result early-return tightened: previously ANY existing `result.properties` with matching sha skipped; now skips only when same sha AND same PID AND `state=PREFLIGHT_STARTED`. A completed/failed or foreign-PID result no longer blocks a fresh retry — this is the "safe retry" in the commit message.

## Findings

- None blocking. PASS read-only.
- (LOW, diagnostic nit) Silent skips: PID mismatch and stale timestamp return with no log line. A writer-side bug (wrong PID in request.properties, or writing >120s before quest FINISHED) would park the bot at COMPLETE_QUEST_STATE with no handoff and no diag hint. Suggest a LOG line per skip reason.
- (note, not a defect) Malformed `timestamp` throws NumberFormatException, caught by tick()'s `catch(Throwable)` -> HOLD_EXCEPTION. Fail-closed, consistent with the script's philosophy; reachable only at quest FINISHED when a request file exists.
- (note) Hot reload resets the static `attempted` to ""; an old non-PREFLIGHT result no longer early-returns, so check() re-verifies (sha match) and re-installs the same jar. Idempotent (sha-keyed jar file, verification precedes install); minor redundant work, no stall.

## Disposition of carried findings (through B95)

1. Poison soft-lock (B77/B79) — FIXED in B90.
2. Stranded probe dumps (ge-native-probe.txt / ge-native-form-probe.txt / ge-native-runtime-probe.txt under %USERPROFILE%/.runelite/princealirescue/, uploader watches only bundle/screenshots/) — STILL OPEN.
3. Banked bronze pickaxe 1265 never withdrawn (clay leg terminal-holds instead) — STILL OPEN.
4. B70 empty-getQuestion() furnace HOLD — STILL OPEN (conditional).
5. B74 3-tile vs 10-tile arrival gate (reload-recovery path only) — UNCHANGED (LOW).
6. nativeGeBuy COMPLETE bare-return when frame.count(333)<sourceGoal (silent idle, no log/hold/state change) — STILL OPEN (LOW reachability).
7. D88-1: Draynor-bank damaged gate terminal HOLD (any 1-HP deficit at varp==30 near bank, not in combat; fires before eat handling; no resume path) — STILL OPEN (conditional).
8. B95-NEW (LOW): handoff skip paths log nothing — diagnostic gap for writer-side PID/timestamp bugs.

## Verdict: PASS (read-only; nothing shipped from this side)

## Live status

No live visual source (feed dark since 2026-09-30 17:44 EDT — ~26h; no stream URL). Builds 3–95 never live-verified from here; acceptance awaits Alex runtime report or the feed's return. Handoff behavior (B94/B95) is only exercisable at quest FINISHED.
