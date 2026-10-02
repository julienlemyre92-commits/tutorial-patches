# Read-only review: Below Ice Mountain Build 45 (patch-910) — PASS WITH FINDINGS

Reviewed 2026-10-02 ~02:27 EDT by Muse (read-only; Alex / OSRS BOT MAKER (2) own implementation and releases).

## Custody — AIR TIGHT
- Commit `28aad8cb` "Below Ice Mountain Build45 level-up Skills-tab cue" (2026-10-02T06:24:44Z); `version.txt` = 910 (909 → 910 sequential, no reuse).
- `patch-910.zip`: 287 files, `net/` root (+`META-INF/MANIFEST.MF`, `version.txt` — standard); in-zip `version.txt` = 910 == repo.
- Class set vs patch-909: 3 new classes (`BelowIceMountainScript$LevelUpTabCue`, `$LevelUpTabCue$Result`, `$LevelEvent`); only `BelowIceMountainScript` + nested classes differ; `BelowIceMountainConfig`/`Plugin` and ALL other quest plugins byte-identical to patch-909.
- javap: `BUILD_NUMBER = 45` in the shipped class; matches published source.
- `belowicemountain-45.jar` sha256 `3b94e5932bceda3d7bf8d519668ca39eb7803f1ea037199b1f56929401ae4c79` == `patch-910.hot.json` — FULL MATCH (same pattern as B44).

## Source diff 44 → 45 (the LevelUpTabCue feature)
1. `BUILD_NUMBER` 44 → 45.
2. New `LevelUpTabCue` inner class: seeds real skill levels from the client on the first logged-in frame; `StatChanged` events are enqueued as immutable `LevelEvent`s and drained on the script thread; the cue fires only when a real level INCREASES (XP noise cannot trigger it).
3. `EventBus` subscriber registered in `run()`, unregistered in `quiesceForReload()`/`onStop()`; events accepted only while logged in; queue cleared and session reset on logout.
4. `tickLevelUpCue(f)` runs early in tick: computes `safe` (logged in, inside `overworldPrepArea` — x2500–3500/y3000–3800 plane 0, which covers the chicken farm 3238,3298 — scene stable 500ms, no pending/route/dialogue/bank/shop/GE/production, no interacting NPC, no aggressor, HP healthy, no retreat). When safe: one `Rs2Tab.switchTo(SKILLS)` attempt → `WAITING_PROOF`; next-tick proof via `isCurrentTab` → `PROVED`/`REJECTED`. Consumes one action slot (`nextAt`+350ms, stage `VERIFY_LEVEL_UP_TAB`). Logs `LEVEL_UP_BASELINE_READY`, `LEVEL_UP_TAB skill=… result=…`, `LEVEL_UP_TAB_CANCELLED_SESSION` lines.
5. Hot-reload snapshot/restore of cue state (levels, baseline, result, skill, queuedAt); `tick()` is now `synchronized`, but the subscriber never acquires `this` — no deadlock.
6. Remainder of the diff is a whitespace reformat of `trainChickens` (verified `diff -b`: no logic change).

## API safety (verified against installed microbot-base.jar)
- `net.runelite.client.eventbus.EventBus.register(Class, Consumer, float)` → `Subscriber`, and `unregister(Subscriber)` — both exist as used.
- `net.runelite.api.events.StatChanged.getSkill()` / `getLevel()` exist.
- `Rs2Tab.switchTo(InterfaceTab)` / `isCurrentTab(InterfaceTab)` exist. No `NoSuchMethodError` risk.
- Threading: subscriber enqueues under `synchronized(levelEvents)` and touches only the queue + flags; drain happens on the script thread without holding the lock while `Rs2Tab` may run client-thread work (the source says this explicitly). `ConcurrentLinkedQueue` makes the asymmetric lock discipline benign.

## Findings
- INFO BIM45-1: `source-review/belowicemountain-build45/README.md` has NO Build 45 section — it still ends at Build 44 / patch 909. Intent ("level-up Skills-tab cue") comes from the commit message only. Process gap: the README is the review guide; this verdict is based on the source diff + commit message instead.
- INFO BIM45-2: after `PROVED`, the Skills tab stays open until other logic switches tabs. If later logic assumes the inventory tab, it must switch explicitly (out of scope to trace this run).
- INFO BIM45-3: lock discipline is asymmetric (subscriber adds under lock, drain polls without). Benign via `ConcurrentLinkedQueue`.

## Carried open items (unchanged)
- BIM44-1/44-2/44-3, BIM43-1/43-2/43-3, BIM42-1/42-2, BIM41-1, BIM40-1..4, bankCoins discrepancy (script debug 3023 vs bot-maker panel "1,023"), guardian controller unimplemented, BIM38-1/38-2/38-3.

## Live acceptance (PENDING)
Watch for `LEVEL_UP_BASELINE_READY` + `LEVEL_UP_TAB … PROVED` on the next HP level-up during chicken training, plus `RUNTIME BUILD: 45 / confirmed`. Screenshot feed dark since 2026-09-30 17:44 EDT (~32.7h); stream is the only live source.
