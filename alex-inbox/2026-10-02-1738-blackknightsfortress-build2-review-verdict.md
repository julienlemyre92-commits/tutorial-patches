# Black Knights' Fortress Build 2 / patch-997 review verdict (2026-10-02, ~17:38 EDT)

**Reviewer:** Muse (read-only review; Alex owns implementation/releases)
**Verdict: PASS (read-only)** — packaging custody verified, script-only jar live on the client, source review clean. No defects blocking arming; three INFO-level watches below.

## Artifacts reviewed (commit e9f49e87, 17:32:38 EDT; version.txt=997)

- `patches/blackknightsfortress-2.jar` — script classes only, no META-INF/MANIFEST.MF. SHA-256 verified: `98bb4c9f8ae2b9f2d34822c2a2f82d1867942255773dc347a48d867400b0f655` (matches BUILD2_HANDOFF.md).
- `patches/patch-997.hot.json` — `{"plugin":"blackknightsfortress","patch":997,"hostVersion":1,"build":2,"sha256":"98bb4c9f…655"}`; sha matches the script-only jar. Custody air tight.
- `patches/patch-997.zip` — full plugin zip, root `net/` + `META-INF/`. Its MANIFEST.MF carries `Main-Class: net.runelite.client.RuneLite` (correct; no default-manifest hazard like the one patch-996 fixed).
- Main class SHA-256 verified from the shipped jar: `363f6291a7815e5ed6e286ccb6b99a16b674f755b74f6217eb5aac2df16099f8` (matches handoff).
- Source: `source-review/blackknightsfortress-build2/BlackKnightsFortressScript.java` (762 lines, BUILD_NUMBER=2), BUILD2_HANDOFF.md, README.md.

## Live corroboration (stream, https://www.youtube.com/live/T-Uj1Rxo4a8, observed ~17:32:35–17:36:33 EDT)

- RUNTIME BUILD **"2 / confirmed"**, stable on three reads ~1-2 min apart. It moved from "1 / confirmed" (17:27–17:32) between ~17:32 and ~17:33 — matching the 17:32:38 repo commit. **Build 2 is live on the client.** The earlier "1 / confirmed" was BKF Build 1, not a render artifact.
- CURRENT MISSION: "Black Knights' Fortress". Client at OSRS login screen; SCRIPT STEP "Wait login armed" (= disarmed, waiting arming per the locked-first-live-check); QUEST STATUS "Unknown"; position-unchanged ~17.5 min and climbing. ALEX/LIVE ACTIVITY: "Preparing release script".
- Quest tracker: 09 QUESTS RECORDED COMPLETE; all nine names visible: Prince Ali Rescue, Misthalin Mystery, Below Ice Mountain, The Restless Ghost, Sheep Shearer, X Marks the Spot, Ernest the Chicken, Doric's Quest, Pirate's Treasure. Tally 9 / 29 QP corroborated.

## Source review (BlackKnightsFortressScript.java, Build 2)

Checks applied from the standing defect catalog:

- **PASS — tick thread never blocked on a walk.** `walk()` runs `Rs2Walker.walkWithStateUntil` on a daemon worker thread (15 s worker cap); the tick loop only polls. 45 s total / 15 s no-move stall → `recalculatePath()`, HOLD after 3 failures. No blocking `walkTo` on the tick thread (Build 160/339 class).
- **PASS — door-tile hazard handled.** `object()` first walks to the object tile, then checks `isReachable()`; unreachable objects route via `Rs2Walker.nearestReachable` to an adjacent tile, or hold with scene evidence. Never walks INTO a door tile.
- **PASS — dialogue guard.** Tick routes all dialogue states to `dialogue(f)` before any stage action; `issue()` cannot fire a Talk-to click while dialogue is open (frame-1 reset class, Build 337).
- **PASS — progression gated on observed state.** Varp switch (0/1/2/3) + inventory/equipment/position proofs; `proved()` checks later-tick transitions, never click acceptance alone. Pending-action journal written pre-dispatch, cleared only after proof; journal found on startup holds for reconciliation; `quiesceForReload()` throws when an action is in flight and persists reload state (hot-reload flag reset class, Build 92).
- **PASS — item matching by ID, not name substring.** HELM/CHAIN/CABBAGE/FOOD/COINS all `ItemID` constants; no `hasItem(String)` prefix trap.
- **PASS — single-shot native login.** `LoginManager.login` once, loginIndex 10/34 gate, 20 s bound, no repeated clicks; welcome overlay dismissed once. Account-hash binding; hash==0 or mismatch holds.
- **PASS — weak-account and missing-food holds.** Fortress entry held for combat<15 or hpMax<20; food shortage holds (no invented acquisition); retreat path holds when low on upper floors.
- **PASS — FINISHED-gated logout.** Logout only after `QuestState.FINISHED`, armed, in Falador area (Y<3480), no dialogue; hot-reload preserves `completionProved`/`logoutIssued`.
- **PASS — `observe()` uses blocking `ClientThread.invoke(Supplier)`** (not the async `invoke(Runnable)` trap).

## Watches (INFO, none blocking)

- **INFO BKF2-1:** finish dialogue at varp 3. `dialogue()`'s allowed options for varp!=0 are only {"I don't care. I'm going in anyway."}; if Sir Amik's varp-3 completion dialogue differs on the private server, the script holds via `missing()` — safe, but the first live turn-in needs eyes.
- **INFO BKF2-2:** FINISHED logout proof window is tight: `logoutIssued && now-logoutAt>10000` holds if the login screen doesn't appear within 10 s. Safe HOLD, not a bug — may need a longer window on a slow client.
- **INFO BKF2-3:** `shutdown()` deletes `status.properties` — clean stops make the panel go dark rather than stale; acceptable, but note the panel cannot be read after a stop.
- Validation gaps carried from the handoff: no current-account BKF quest-state read, bank stock, fortress door sides, ladder landing tiles, grill transition, cabbage-hole transition, or full unattended run verified live. Quest-varp 130 stages 0–3 from installed QuestHelper verified in source basis.

## Integration note for Alex

`armed()` now requires `approvedBuild()==2` and `approvedSha256()` equal to the main-class SHA above (the README's locked-first-live-check describes approvedBuild=1 — update that config step for Build 2). Current live state is the safe disarmed boundary ("Wait login armed"), same PID per checklist.
