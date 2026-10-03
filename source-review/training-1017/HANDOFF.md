# Combat-training provider candidate

This is an isolated source/compile candidate only. It does not modify a quest, the shared service host, a live plugin JAR, or a RuneLite profile, and it was not deployed or run against the game.

## Files and class-loader boundary

- **Parent contract:** `CombatTrainingGoal.java`. This request type must be loaded by the same parent class loader as `QuestServiceHub` because it crosses the quest/provider boundary.
- **Provider implementation:** `CombatTrainingService.java` and `CombatTrainingMicrobotDriver.java`. These can belong to the replaceable provider implementation bundle. The service implements the already-present `QuestServiceHub.Kind.TRAINING` contract.
- `NavigationGoal`, `NavigationService`, and `QuestServiceHub` remain the existing parent-loaded shared APIs. The provider delegates entry and return movement to the registered guarded `NAVIGATION` service. It does not call the global walker directly.

## Behavior in this candidate

The quest supplies a target combat level and HP level, explicit target-NPC ID/name allowlists, an allowed rectangular training region, the exact interior entry tile, a quest return tile, food IDs and reserve, required equipped IDs, membership/region constraints, a deadline and finite action/replan budgets. The service does not choose a quest's training location, buy supplies, or bank items.

Before dispatch, it requires a fresh same-account client frame, checks F2P/member state when requested, checks food and required equipment, and verifies that the entry tile is inside the configured zone. It delegates entry through the shared navigation lease, then requires a fresh exact-tile and reachability proof before training. This lets the installed navigation provider handle a gate; the training service does not treat gate proximity as a crossing.

During training it only attacks allowlisted NPCs inside the zone, below the configured combat-level cap, with current line-of-sight and reachability evidence. It re-resolves the NPC by index and ID at dispatch time. One attack or Eat may be outstanding at once. Attack progress requires fresh combat/HP XP change; Eat requires inventory loss, with HP gain recorded when observed. An unresolved action is never reissued. Empty target scenes trigger one bounded interior rescan. Death, unexpected combat, telemetry loss, unresolved input, or a failed navigation proof stops the service with an explicit HOLD/UNAVAILABLE result.

Low HP is checked before ordinary combat handling and before the requested-level success shortcut. It may dispatch one allowlisted Eat while in combat, after rechecking account, current HP, and food at the moment of dispatch. Inventory loss proves consumption; HP gain is recorded when available. At critical HP, reserve food may be used. After one low-HP Eat, continued low HP or an emergency meal requests a guarded retreat instead of continuing combat. If there is no food, it requests the guarded retreat rather than continuing combat. If the navigation provider cannot prove the safe return, the service holds the quest lease and reports the unproved escape path; it does not claim unattended safety.

`COMPLETE` is sent only after both requested levels remain verified and a fresh same-account frame proves the return tile and reachability. Returning early for a depleted reserve/deadline reports `UNAVAILABLE`; unresolved risk keeps a `HOLD` lease. Death recovery is deliberately not attempted here and must be delegated to the existing death-recovery provider by a future coordinator after verifying that integration contract.

## Source-backed API choices

Reviewed against `C:\Users\No 1\Desktop\New folder\microbot-tutorial-island.jar` with `javap`, and against `work/belowicemountain/BelowIceMountainScript.java` and `work/questcommon/navigation/NavigationMicrobotDriver.java`.

- NPC enumeration uses the installed `Microbot.getRs2NpcCache().query().within(...).toListOnClientThread()` and the non-deprecated `api.npc.models.Rs2NpcModel` wrapper for LOS and `click("Attack")`.
- All reads/clicks are wrapped in `Microbot.getClientThread().invoke(...)`.
- Movement is requested through `QuestServiceHub.delegate` as a `NavigationGoal`; arrival is independently re-observed after the child provider returns.
- The account key follows the existing navigation adapter's SHA-256 of normalized launcher username + character name.
- Eating uses the existing installed `Rs2Inventory.interact(id, "Eat")` convention.

## Compile and scope

Compiled the three Java sources against the installed `microbot-tutorial-island.jar` into the local `build/` directory. `javac` exited 0. A concrete Corsair-style request was constructed with HP 20, target combat 25/HP 25, two tuna (361), two cooked trout (333), and eat/retreat thresholds 12/6; its preflight returned `READY`. This was a local request/preflight probe, not a game run. The compiler emitted three deprecation warnings for the installed RuneLite `InventoryID` constants and `Client.getUsername()`; these are still present and usable in the installed JAR, and the identity call matches the existing navigation adapter. There is no large JAR, overlay, release artifact, live profile change, or game run here.

## Required integration and live validation

1. Add `CombatTrainingGoal` to the parent/common build, not only the reloadable child bundle; increment the shared parent build marker and install it with the required cold parent-JAR restart. The provider source alone cannot safely introduce this cross-loader request class.
2. Add the service/driver to the provider bundle and register it beside the existing providers. Confirm the `TRAINING` registration and `NAVIGATION` child provider exist in the same hub registry/class loader.
3. Supply a request only after the quest has separately planned a safe F2P/member-appropriate training zone and a reachable return destination; set allowlists, region, target cap, food reserve, gear IDs, forbidden regions, time budget, and parent identity from current observations.
4. Verify hot replacement only after the active lease drains and no route child/callback remains.
5. First live validation must inspect actual inventory/equipment and entry proof, then observe a low-HP Eat during combat, attack XP, XP/no-target stalls, an aggressor, gate entry and return, death, account change/logout, navigation-provider absence, deadline and plugin disable. Do not claim unattended safe retreat until the configured NAVIGATION provider proves that path on the live build.

No current Corsair request or live state was changed by this candidate.
