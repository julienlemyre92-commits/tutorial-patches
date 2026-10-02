# Black Knights' Fortress isolated Build 2 candidate

Owner: Alex. This directory has no canonical edits, deployment, control changes, or client input.

## Deliverable

- `BlackKnightsFortressScript.java`: complete candidate route through Sir Amik, fortress grill, ordinary cabbage hole, return to Sir Amik, native FINISHED proof, and safe-area logout.
- `BlackKnightsFortress-script-2.jar`: script classes only. It has **no `META-INF/MANIFEST.MF`** and contains only `BlackKnightsFortressScript.class` and its nested classes, as required by the installed v1 host.
- JAR SHA-256: `98bb4c9f8ae2b9f2d34822c2a2f82d1867942255773dc347a48d867400b0f655`.
- Main class SHA-256: `363f6291a7815e5ed6e286ccb6b99a16b674f755b74f6217eb5aac2df16099f8`.
- Isolated `BlackKnightsFortressPlugin.java` and `BlackKnightsFortressConfig.java` are unchanged copies for compile context; the deliverable for this live PID is **script-only**.

## Changes from embedded Build 1

- Native `LoginManager.login` chooses a nonmember world once after the matching login index; `WelcomeScreenEvent` dismisses the overlay once. Both require the existing action/PID/build/class-SHA and exclusive-input gate. An uncertain login/overlay holds instead of clicking repeatedly.
- Binds the session to `Client.getAccountHash()`; a missing hash or different account holds. Fresh quest-state and 12 QP preflight remain mandatory.
- Before every ordinary action, writes `.runelite/blackknightsfortress/pending-action.properties`. Rejected/exception/timeout actions hold without blind replay; success clears the journal only after later-tick proof. A journal found on startup holds for reconciliation. A disconnect preserves in-memory pending proof, and a possible death/teleport back south triggers bank/re-supply only when no action is unresolved.
- Ladder proof now requires the **expected** destination plane near the source ladder. Same-floor doors require actual crossing to a named tile; the entry door may instead prove its guard dialogue. The grill requires a fresh dialogue transition, and the cabbage hole requires quest-varp progress. Object clicks require live reachability; an unreachable object triggers a bounded walk to a reachable adjacent tile or holds with scene evidence.
- The route worker must stop before a subsequent object click. Unknown inventory is preserved rather than bulk-deposited; bank preparation holds if three free slots cannot be established safely. Food is taken from available bank stock, gear from bank or observed F2P shops, and ordinary cabbage ID 1965 from bank or Edgeville Monastery. Draynor Manor cabbage ID 1967 is never used.
- Eating begins at or below 75% max HP (minimum 8, never at full HP). One emergency eat is permitted while another action waits for proof, without clearing that action. An unsafe upper-floor retreat holds. Completion and logout state survive script hot reload.

## Compile and source evidence

`javac` against the installed `C:\Users\No 1\Desktop\New folder\microbot-tutorial-island.jar` exited 0. Two warnings: installed `Rs2GroundItem` is deprecated and marked for removal. No tests or live gameplay were run. The installed API exposes `LoginManager`, `WelcomeScreenEvent`, `Rs2TileObjectModel.isReachable`, and `Rs2Walker.nearestReachable`; compilation verifies these call signatures, not private-server behavior.

Route and stage basis: installed Microbot QuestHelper `BlackKnightFortress` (varp 130 stages 0/1/2/3; object and item IDs) and [OSRS Wiki quick guide](https://oldschool.runescape.wiki/w/Black_Knights%27_Fortress/Quick_guide). The guide requires 12 quest points, iron chainbody, bronze med helm, ordinary cabbage, listening at the grill, and returning to Sir Amik. It warns about level-33 knights. Local prior `C:\Users\No 1\.runelite\blackknightsfortress\status.properties` reported NOT_STARTED/varp0 on an older PID; that is **not** current-account proof. Current `C:\Users\No 1\.runelite\belowicemountain\status.properties` reported 29 QP and HP20 on PID40060; Black Knights' Fortress remains unverified for that account.

## Integration checklist for Alex

1. Keep live PID40060 and the embedded Build1 host. Its current `WAIT_LOGIN`/disarmed state is a safe reload boundary. Inspect fresh host status and loaded class marker; require fresh same-account NOT_STARTED/varp130=0 and QP>=12 before arming Build2.
2. Place the **class-only** JAR in the existing `.runelite/blackknightsfortress-hot` request flow using the exact JAR SHA above and `build=2`. Confirm `RELOAD_APPLIED` and `RUNNING_BUILD=2`, class SHA above, same PID. Do not infer load from file names or request status.
3. Arm only with live PID40060, Build2, and its class SHA. Native login/welcome then inspect actual bank stock, equipment, food, world, and starting position. If account hash is zero in this private client, diagnose before actions; this candidate intentionally holds.
4. Compare each private-server door/ladder landing and dialogue with fresh same-PID status/log/frame before widening routes. Retain the current held action journal on uncertain inputs. Require `QuestState.FINISHED` and verified login screen after the final logout; a varp or click alone is insufficient.
5. When live proof exists, route preparation requests through the existing shared BANKING/FOOD_RESTOCK/NAVIGATION providers. This candidate currently uses the local bounded bank/food/shop/walker path; it does **not** claim a shared-provider handoff or automatic acquisition when the bank and shops cannot supply items. Preserve the provider input lease and quiescence contract during that integration.

## Remaining validation gaps

No current-account Black Knights quest-state read, bank stock, fortress door sides, floor landing tiles, guard dialogue, grill transition, cabbage-hole transition, upper-floor escape, death inventory, or full unattended run is verified. Shop prices/stock and the private-server item IDs remain live checks. The v1 parent host can permanently reject a temporarily busy hot request; this script-only artifact cannot modify its already-loaded host class. Reissue a request after a safe boundary or integrate a host-level deferred-busy fix in a separately authorized host update; do not restart the client for this candidate.
