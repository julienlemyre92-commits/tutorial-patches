# Prince Ali Rescue Build 71 — read-only review verdict (Muse)
Date: 2026-10-01

## Header
- Build: 71 (in-script `BUILD_NUMBER = 71` confirmed via `javap -constants` on shipped class)
- Patch: 764 → `patches/patch-764.zip` (version.txt = 764, net/-rooted, 231 entries)
- Commit: `5fe32e796b` "Prince Ali Rescue Build71: support observed ale dialogue and safer Rimmington ore source plus bank proof and health telemetry"
- Commit scope: single-purpose — patch-764.hot.json, patch-764.zip, princealirescue-71.jar, princealirescue-plugin-71.jar, source-review/princealirescue-build71/*, version.txt. Intervening commit between B70 and B71 was only an alex-brief.md update (no code).

## Custody — PASS
- `patch-764.hot.json`: sha256 `40cca276a0e3ffd1b427cf5607f4cba772dd40b041ef599c01ea6fdafee16c00`, build=71.
- Script jar `patches/princealirescue-71.jar` fetched via git-blobs API with `Accept: application/vnd.github.v3.raw`: sha256 matches hot.json EXACTLY.
- patch-764.zip: 231 net/-rooted entries; all 13 `.../microbot/princealirescue/PrinceAliRescueScript*.class` classes byte-identical zip↔script-jar. (PrinceAliRescuePlugin/Config classes are zip-only by design — they don't hot-swap.)
- Compiled classes contain the new code strings ("finest ale please" ×3, "RESUME_VERIFIED_ALE_OPTION", Rimmington bronze mine) — no stale classes.

## Delta summary (B70 → B71, source diff, 15 changed hunks)
1. Beer source now prefers the OBSERVED bartender option `"A glass of your finest ale please."` when present in `f.options`, falling back to `"Could I buy a beer please?"`; click dispatches `SOURCE_BEER_OPTION` with the chosen text.
2. `restoreReloadState` gains a one-shot resume: a held run whose error starts with `"Bartender dialogue lacks verified beer option: A glass of your finest ale please.|"` is un-held and moved to phase `RESUME_VERIFIED_ALE_OPTION` (re-enters via observed state on the next tick — phase is a label, routing is state-based, which is the correct pattern).
3. Bronze ore source switched from Al Kharid scorpion mine to Rimmington mine (`RIMMINGTON_MINE_WAYPOINT = (2985,3238,0)` — same tile as the pre-existing clay waypoint); furnace stays Al Kharid (3273,3184); F2P/world/HP guards unchanged.
4. Key-furnace confirmation question check tightened from `contains("key")` to exact `.equals("Create a key using the key print?")` (text was live-observed in B70).
5. Health telemetry: Frame gains `hp/maxHp/combatLevel/inCombat` (`getBoostedSkillLevel/getRealSkillLevel/combatLevel/Rs2Player.isInCombat()`), all captured inside `observe()` which runs on the client thread via `getClientThread().invoke` — thread-safe; surfaced as bank properties in diagnostics.
6. `DEPOSIT_UNNEEDED` proof strengthened: now requires bank open AND bank-contents available on both frames AND bank-item delta == inventory delta. Stricter against false-success.

## Findings
- [MEDIUM, conditional, CARRIED] Empty `getQuestion()` on the furnace Yes|No → `hold("Unexpected furnace confirmation question: ")` with no recovery path (timedOut resume only matches `"Unproved MAKE_BRONZE_KEY;"`). B71's exact-match tightening keeps — and slightly widens — this trap. LEFT OPEN.
- [MEDIUM, conditional, CARRIED] Banked bronze pickaxe (1265) is never withdrawn: the ore-withdraw loop covers only 436/438 and SOFT_CLAY only covers CLAY/WATER; `SOFT_CLAY_LOCAL_MINE` holds terminally if 1265 is not in inventory. B71's "bank proof" change touches deposit accounting only. LEFT OPEN.
- [LOW, new] Stale hold message: BronzeBarSource still holds with `"Al Kharid source requires ground plane"` though the route is now Rimmington — confusing for log review.
- [LOW, new/watch] Stricter DEPOSIT_UNNEEDED proof can false-negative on a lagging bank-container snapshot (same-tick capture makes this unlikely, but an unproved timeout → terminal `hold("Unproved DEPOSIT_UNNEEDED;...")` with no ROUTE_RESCAN-style retry).
- [LOW, CARRIED] B68 second-respawn held-requirement near-unreachable. LEFT OPEN (untouched).
- [LOW, CARRIED] B67 partial-set direct-loot 5-item hardcode. LEFT OPEN (untouched).
- [LOW, CARRIED] Dead-code Shantay resume log. LEFT OPEN (untouched).
- [LOW, CARRIED] Stale source-review README template (README.md byte-identical across B70/B71/B72). LEFT OPEN.

## API / thread-safety
All new calls verified present in installed `~/workspace/microbot-base.jar`: `Client.getBoostedSkillLevel/getRealSkillLevel`, `Actor.getCombatLevel`, `Rs2Player.isInCombat()`, `ClientThread.invoke(Supplier<T>)`. All client reads stay inside the client-thread-dispatched `observe()` — no B517-class off-client-thread violation.

## Verdict: PASS WITH FINDINGS
No blocking defect. Two carried MEDIUM-conditionals remain open (neither touched by this delta); new findings are LOW. The ale-dialogue support and Rimmington mine switch match observed state and are safe.
